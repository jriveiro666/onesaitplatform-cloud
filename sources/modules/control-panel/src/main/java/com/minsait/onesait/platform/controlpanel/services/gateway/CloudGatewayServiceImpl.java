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
package com.minsait.onesait.platform.controlpanel.services.gateway;

import java.io.IOException;
import java.util.Base64;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Microservice.CaaS;
import com.minsait.onesait.platform.config.services.exceptions.MicroserviceException;

@Service
public class CloudGatewayServiceImpl implements CloudGatewayService {

	@Autowired
	private ObjectMapper mapper;

	@Value("${cloud-gateway.user:operations}")
	private String user;
	@Value("${cloud-gateway.password:operations}")
	private String password;
	@Value("${cloud-gateway.url:http://localhost}")
	private String gatewayUrl;

	private final static String ROUTES_PATH = "/actuator/gateway/routes";
	private final static String REFRESH_PATH = "/actuator/gateway/refresh";

	private RestTemplate restTemplate;

	@PostConstruct
	void initRestTemplate() {
		restTemplate = new RestTemplate(SSLUtil.getHttpRequestFactoryAvoidingSSLVerification());

	}

	@Override
	public void publishMicroserviceToGateway(Microservice microservice) throws MicroserviceException {
		try {
			if (microservice.getCaas().equals(CaaS.RANCHER)) {
				final ObjectNode route = generateJsonRoute(microservice.getIdentification(),
						microservice.getContextPath(),
						"http://".concat(microservice.getIdentification()).concat(".").concat(microservice
								.getRancherStack().concat(":").concat(String.valueOf(microservice.getPort()))));
				final HttpEntity<ObjectNode> request = this.getRequestEntity(route);
				this.executeRequest(gatewayUrl.concat(ROUTES_PATH).concat("/").concat(microservice.getIdentification()),
						HttpMethod.POST, request, String.class);
				refreshRoutesCache();
			}
		} catch (final Exception e) {
			throw new MicroserviceException(e.getMessage());
		}
	}

	@Override
	public void refreshRoutesCache() {
		final HttpEntity<ObjectNode> request = this.getRequestEntity(null);
		this.executeRequest(gatewayUrl.concat(REFRESH_PATH), HttpMethod.POST, request, String.class);

	}

	@Override
	public String getDeployedMicroserviceURL(Microservice microservice) {
		if (!StringUtils.isEmpty(microservice.getRancherStack()))
			return gatewayUrl.concat(microservice.getContextPath());
		else
			return new String("");
	}

	// TO-DO refactor/externalize this methods
	private <T> ResponseEntity<T> executeRequest(String url, HttpMethod method, HttpEntity<?> reqEntity,
			Class<T> responseType) {
		try {
			return restTemplate.exchange(url, method, reqEntity, responseType);

		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			throw new MicroserviceException(
					"HttpResponse code : " + e.getStatusCode() + " , cause: " + e.getResponseBodyAsString());
		}

	}

	private HttpHeaders getHeaders() {
		final String basicAuth = getBasicAuthHeader();
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, basicAuth);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
		return headers;
	}

	private <T> HttpEntity<T> getRequestEntity(T body) {
		HttpEntity<T> reqEntity = null;
		if (null == body)
			reqEntity = new HttpEntity<>(getHeaders());
		else
			reqEntity = new HttpEntity<>(body, getHeaders());
		return reqEntity;
	}

	private String getBasicAuthHeader() {

		return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());

	}

	private ObjectNode generateJsonRoute(String id, String contextPath, String redirectToHost) throws IOException {
		final ObjectNode route = mapper.createObjectNode();
		route.put("id", id);
		route.set("predicates", mapper.readValue(
				"[{\"name\": \"Path\",\"args\": {\"_genkey_0\":\"" + contextPath + "/**\"}}]", ArrayNode.class));
		route.set("filters", mapper.readValue("[{\"name\" : \"PreserveHostHeader\"}]", ArrayNode.class));
		route.put("uri", redirectToHost);
		return route;
	}

}
