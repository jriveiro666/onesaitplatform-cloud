/**
 * Copyright Indra Soluciones Tecnologías de la Información, S.L.U.
 * 2013-2019 SPAIN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minsait.onesait.platform.controlpanel.services.gravitee;

import static com.minsait.onesait.platform.controlpanel.gravitee.dto.TransformHeadersPolicy.TRANSFORM_HEADERS_DESCRIPTION;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jline.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.model.Api;
import com.minsait.onesait.platform.config.model.Api.ApiType;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiCreate;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiPage;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiPlan;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiUpdate;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.CorsApi;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.GraviteeApi;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.GraviteeException;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.HttpHeader;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.IdentityProvider;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ImportSwaggerDescriptor;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ImportSwaggerDescriptor.Type;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.PathPolicy;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.TransformHeadersPolicy;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.Module;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.ServiceUrl;

import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(value = "gravitee.enable", havingValue = "true")
@Slf4j
public class GraviteeServiceImpl implements GraviteeService {

	private RestTemplate restTemplate;

	private final static String OAUTH_EXCHANGE_TOKEN_URL = "/auth/oauth2/onesait-account/exchange";
	private final static String IDENTITIES_ENDPOINT = "/configuration/identities";
	private final static String IDENTITY_PROVIDER_ID = "onesait-account";

	private final static String APIS_ENDPOINT = "/apis";
	private final static String PLANS_ENDPOINT = "/plans";
	private final static String PAGES_ENDPOINT = "/pages";
	private final static String DEPLOY_ENDPOINT = "/deploy";
	private final static String IMPORT_ENDPOINT = "/import";
	private final static String SWAGGER_ENDPOINT = "/swagger";

	private final static String TOKEN_PARAM = "token";
	private final static String ACTION_PARAM = "action";
	private final static String BEARER_PREFIX = "Bearer ";
	private final static String STOP = "stop";
	private final static String START = "start";

	private final static String REQUEST = "REQUEST";

	private final static String CORS = "cors";
	private final static String SSL = "ssl";
	private final static String HTTP_HEADER_REQUESTED_WITH = "X-Requested-With";
	@Value("${gravitee.headerValue:Gravitee-Server}")
	private String GRAVITEE_HEADER_VALUE;
	@Value("${gravitee.clientId}")
	private String clientId;
	@Value("${gravitee.clientSecret}")
	private String clientSecret;

	@Autowired
	private AppWebUtils utils;
	@Autowired
	private IntegrationResourcesService resourcesService;
	@Autowired
	private ObjectMapper mapper;

	@PostConstruct
	void initRestTemplate() {
		restTemplate = new RestTemplate(SSLUtil.getHttpRequestFactoryAvoidingSSLVerification());

		if (!oauthIdentityManagerExists())
			try {
				createDefaultIdentityProvider();
			} catch (final Exception e) {
				Log.error("Could not create identity provider");
			}
	}

	@Override
	public GraviteeApi processApi(Api api) {
		// Create DTO
		try {
			ApiCreate apiCreate = new ApiCreate();
			ApiUpdate apiUpdate = new ApiUpdate();
			if (!api.getApiType().equals(ApiType.EXTERNAL_FROM_JSON)) {
				apiCreate = ApiCreate.createFromOPApi(api);
				apiUpdate = createApi(apiCreate);
			} else
				apiUpdate = getApiDTOFromSwagger(api);

			// make public

			apiUpdate.setVisibility("public");

			// Save id for later
			final String apiId = apiUpdate.getId();

			// if api is internal then create header policy
			if (!api.getApiType().equals(ApiType.EXTERNAL_FROM_JSON))
				((ObjectNode) apiUpdate.getPaths()).set("/", createHttpHeaderPolicy());

			// update public api
			updateApi(apiUpdate);

			// create Docs if external
			if (api.getApiType().equals(ApiType.EXTERNAL_FROM_JSON))
				createSwaggerDocPage(apiId, api);

			// create plan
			createDefaultPlan(apiId);

			// start api & deploy
			startApi(apiId);
			deployApi(apiId);

			return GraviteeApi.builder().apiId(apiId).endpoint(
					resourcesService.getUrl(Module.gravitee, ServiceUrl.gateway) + ApiCreate.getApiContextPath(api))
					.build();
		} catch (final Exception e) {
			log.error("Something went wrong while publishing API to gravitee");
			throw e;
		}

	}

	@Override
	public ApiUpdate createApi(ApiCreate api) throws GraviteeException {

		return executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT,
				HttpMethod.POST, this.getRequestEntity(api), ApiUpdate.class).getBody();

	}

	@Override
	public ApiUpdate updateApi(ApiUpdate api) throws GraviteeException {
		final String apiId = api.getId();
		api.setId(null);
		((ObjectNode) api.getProxy()).set(CORS, CorsApi.defaultCorsPolicy().toJsonNode());
		try {
			((ObjectNode) api.getProxy().path("groups").path(0).path("endpoints").path(0)).set(SSL,
					mapper.readTree("{\"trustAll\":true,\"hostnameVerifier\":false}"));
		} catch (final Exception e) {
			log.error("could not sett trustall certificates in API");
		}
		return this.executeRequest(
				resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/" + apiId,
				HttpMethod.PUT, this.getRequestEntity(api), ApiUpdate.class).getBody();

	}

	@Override
	public ApiPlan createDefaultPlan(String apiId) throws GraviteeException {
		ApiPlan plan = null;
		try {
			plan = ApiPlan.defaultPlan(apiId);

		} catch (final IOException e) {
			throw new GraviteeException("Could not create default plan");
		}

		return executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
				+ apiId + PLANS_ENDPOINT, HttpMethod.POST, this.getRequestEntity(plan), ApiPlan.class).getBody();

	}

	@Override
	public void startApi(String apiId) throws GraviteeException {

		final UriComponentsBuilder uri = UriComponentsBuilder
				.fromHttpUrl(
						resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/" + apiId)
				.queryParam(ACTION_PARAM, START);

		executeRequest(uri.toUriString(), HttpMethod.POST, this.getRequestEntity(null), JsonNode.class);

	}

	@Override
	public void stopApi(String apiId) throws GraviteeException {

		final UriComponentsBuilder uri = UriComponentsBuilder
				.fromHttpUrl(
						resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/" + apiId)
				.queryParam(ACTION_PARAM, STOP);

		executeRequest(uri.toUriString(), HttpMethod.POST, this.getRequestEntity(null), JsonNode.class);

	}

	@Override
	public void deployApi(String apiId) throws GraviteeException {

		this.executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
				+ apiId + DEPLOY_ENDPOINT, HttpMethod.POST, this.getRequestEntity(null), JsonNode.class);
	}

	@Override
	public ApiUpdate getApi(String apiId) throws GraviteeException {
		final String url = resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
				+ apiId;
		return this.executeRequest(url, HttpMethod.GET, this.getRequestEntity(null), ApiUpdate.class).getBody();
	}

	@Override
	public void deleteApi(String apiId) throws GraviteeException {
		stopApi(apiId);
		getApiPlans(apiId).stream().forEach(p -> {
			deletePlan(apiId, p.getId());
		});
		final String url = resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
				+ apiId;
		executeRequest(url, HttpMethod.DELETE, this.getRequestEntity(null), JsonNode.class);
	}

	@Override
	public void deletePlan(String apiId, String planId) throws GraviteeException {

		this.executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
				+ apiId + PLANS_ENDPOINT + "/" + planId, HttpMethod.DELETE, this.getRequestEntity(null), String.class);

	}

	@Override
	public List<ApiPlan> getApiPlans(String apiId) throws GraviteeException {

		return this.executeRequest(
				resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/" + apiId
						+ PLANS_ENDPOINT,
				HttpMethod.GET, this.getRequestEntity(null), new ParameterizedTypeReference<List<ApiPlan>>() {
				}).getBody();
	}

	@Override
	public ApiPage createSwaggerDocPage(String apiId, Api api) throws GraviteeException {
		final ApiPage apiPage = ApiPage.defaultSwaggerDocPage(api.getSwaggerJson());

		return this
				.executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + "/"
						+ apiId + PAGES_ENDPOINT, HttpMethod.POST, this.getRequestEntity(apiPage), ApiPage.class)
				.getBody();
	}

	private <T> ResponseEntity<T> executeRequest(String url, HttpMethod method, HttpEntity<?> reqEntity,
			Class<T> responseType) throws GraviteeException {
		try {
			return restTemplate.exchange(url, method, reqEntity, responseType);

		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			throw new GraviteeException(
					"HttpResponse code : " + e.getStatusCode() + " , cause: " + e.getResponseBodyAsString());
		}

	}

	private HttpHeaders getHeaders() throws GraviteeException {
		final String graviteeOauthToken = exchangeOauthToken();
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX.concat(graviteeOauthToken));
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
		return headers;
	}

	private <T> HttpEntity<T> getRequestEntity(T body) throws GraviteeException {
		HttpEntity<T> reqEntity = null;
		if (null == body)
			reqEntity = new HttpEntity<>(getHeaders());
		else
			reqEntity = new HttpEntity<>(body, getHeaders());
		return reqEntity;
	}

	private String exchangeOauthToken() throws GraviteeException {

		final UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromHttpUrl(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + OAUTH_EXCHANGE_TOKEN_URL)
				.queryParam(TOKEN_PARAM, utils.getCurrentUserOauthToken());

		final ResponseEntity<JsonNode> response = this.executeRequest(uriBuilder.toUriString(), HttpMethod.POST, null,
				JsonNode.class);
		return response.getBody().get("token").asText();

	}

	private <T> ResponseEntity<List<T>> executeRequest(String url, HttpMethod method, HttpEntity<Object> requestEntity,
			ParameterizedTypeReference<List<T>> parameterizedTypeReference) throws GraviteeException {
		try {
			return restTemplate.exchange(url, method, requestEntity, parameterizedTypeReference);

		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			throw new GraviteeException(
					"HttpResponse code : " + e.getStatusCode() + " , cause: " + e.getResponseBodyAsString());
		}

	}

	private ApiUpdate getApiDTOFromSwagger(Api api) {

		final ApiUpdate apiUpdate = this.executeRequest(
				resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + APIS_ENDPOINT + IMPORT_ENDPOINT
						+ SWAGGER_ENDPOINT,
				HttpMethod.POST,
				this.getRequestEntity(ImportSwaggerDescriptor.builder().withDocumentation(true).withPathMapping(true)
						.withPolicyPaths(true).type(Type.INLINE).payload(api.getSwaggerJson()).build()),
				ApiUpdate.class).getBody();
		((ObjectNode) apiUpdate.getProxy()).put("context_path", ApiCreate.getApiContextPath(api));
		return apiUpdate;
	}

	private boolean oauthIdentityManagerExists() {
		try {
			final HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.AUTHORIZATION,
					"Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
			this.executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + IDENTITIES_ENDPOINT
					+ "/" + IDENTITY_PROVIDER_ID, HttpMethod.GET, new HttpEntity<>(headers), String.class);
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	private void createDefaultIdentityProvider() throws GraviteeException, IOException {
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION,
				"Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));
		this.executeRequest(resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + IDENTITIES_ENDPOINT,
				HttpMethod.POST,
				new HttpEntity<>(IdentityProvider.getFromString(IdentityProvider.DEFAULT_OAUTH_RESOURCE_2_CREATE),
						headers),
				IdentityProvider.class);
		this.executeRequest(
				resourcesService.getUrl(Module.gravitee, ServiceUrl.management) + IDENTITIES_ENDPOINT + "/"
						+ IDENTITY_PROVIDER_ID,
				HttpMethod.PUT,
				new HttpEntity<>(IdentityProvider.getFromString(IdentityProvider.DEFAULT_OAUTH_RESOURCE_2_UPDATE),
						headers),
				IdentityProvider.class);

	}

	private ArrayNode createHttpHeaderPolicy() {

		final ArrayNode policy = mapper.createArrayNode();
		policy.add(PathPolicy.builder().enabled(true).policy(TransformHeadersPolicy.builder().scope(REQUEST)
				.removeHeaders(new String[] {})
				.addHeaders(new HttpHeader[] {
						HttpHeader.builder().name(HTTP_HEADER_REQUESTED_WITH).value(GRAVITEE_HEADER_VALUE).build() })
				.build()).description(TRANSFORM_HEADERS_DESCRIPTION).build().toJsonNode());
		return policy;
	}

}
