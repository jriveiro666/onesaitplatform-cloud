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
package com.minsait.onesait.platform.router.service.app.service.advice;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.services.drools.DroolsRuleService;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.Module;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.ServiceUrl;
import com.minsait.onesait.platform.router.service.app.model.RulesEngineModel;
import com.minsait.onesait.platform.router.service.app.service.RulesEngineNotificationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RulesEngineNotificationServiceImpl implements RulesEngineNotificationService {

	@Autowired
	private DroolsRuleService droolsRuleService;

	@Value("${onesaitplatform.router.avoidsslverification:false}")
	private boolean avoidSSLVerification;

	@Value("${onesaitplatform.router.notifications.pool.rulesengine:10}")
	private int MAX_TOTAL_CONNECTIONS;

	private RestTemplate restTemplate;
	private String NOTIFICATION_URL;

	@Autowired
	private IntegrationResourcesService resourcesService;

	@PostConstruct
	public void init() throws KeyManagementException, NoSuchAlgorithmException {
		if (avoidSSLVerification) {
			SSLUtil.turnOffSslChecking();
		}

		final HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setHttpClient(httpClient());

		restTemplate = new RestTemplate(httpRequestFactory);

		NOTIFICATION_URL = resourcesService.getUrl(Module.RULES_ENGINE, ServiceUrl.advice);

	}

	private HttpClient httpClient() {
		final TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

		SSLContext sslContext;

		try {
			sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy)
					.build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException("Problem configuring SSL verification", e);
		}

		final SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext,
				NoopHostnameVerifier.INSTANCE);

		final RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(10000)
				.setConnectTimeout(10000).setSocketTimeout(10000).build();

		final CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
				.setConnectionManager(connectionManager()).setSSLSocketFactory(csf)
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();

		return httpClient;
	}

	PoolingHttpClientConnectionManager connectionManager() {
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
		cm.setDefaultMaxPerRoute(MAX_TOTAL_CONNECTIONS);
		return cm;
	}

	@Override
	public boolean notifyToEngine(String ontology) {
		return droolsRuleService.getRulesForOntology(ontology).size() > 0;
	}

	@Override
	public void notify(String ontology, String json) {
		log.debug("Sending notification to Rules Engine, ontology: {}", ontology);
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		final HttpEntity<RulesEngineModel> httpEntity = new HttpEntity<>(
				RulesEngineModel.builder().json(json).ontology(ontology).build(), headers);
		restTemplate.exchange(NOTIFICATION_URL, HttpMethod.POST, httpEntity, String.class);
	}

}
