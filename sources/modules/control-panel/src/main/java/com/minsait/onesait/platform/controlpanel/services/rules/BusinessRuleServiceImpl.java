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
package com.minsait.onesait.platform.controlpanel.services.rules;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.google.common.net.HttpHeaders;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.minsait.onesait.platform.commons.model.HazelcastMessageNotification;
import com.minsait.onesait.platform.commons.model.HazelcastRuleObject;
import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.model.DroolsRule;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.services.drools.DroolsRuleService;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.Module;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.ServiceUrl;

import lombok.Getter;
import lombok.Setter;

@Service
public class BusinessRuleServiceImpl implements BusinessRuleService {

	@Autowired
	@Qualifier("topicChangedRules")
	private ITopic<String> topicRules;

	@Autowired
	@Qualifier("topicAsyncComm")
	private ITopic<String> topicAsyncComm;

	@Autowired
	private DroolsRuleService droolsRuleService;

	@Autowired
	private AppWebUtils utils;

	@Autowired
	private IntegrationResourcesService resourcesService;

	private RestTemplate restTemplate;

	@PostConstruct
	void setup() {
		restTemplate = new RestTemplate(SSLUtil.getHttpRequestFactoryAvoidingSSLVerification());
		restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + utils.getCurrentUserOauthToken());
				return execution.execute(request, body);
			}
		});
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void save(DroolsRule rule, String userId) throws Exception {
		droolsRuleService.create(rule, userId);
		publishAndHandleHzNotification(rule.getIdentification(), rule.getDRL());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void update(DroolsRule rule, String userId, String identification) throws Exception {
		droolsRuleService.update(identification, rule);
		publishAndHandleHzNotification(rule.getIdentification(), rule.getDRL());
	}

	@Override
	public void delete(String identification) {
		final User user = droolsRuleService.getRule(identification).getUser();
		droolsRuleService.deleteRule(identification);
		publishHzRuleNotification(identification, user, null);
	}

	@Override
	public String test(String identification, String input) {
		return restTemplate.postForObject(
				resourcesService.getUrl(Module.RULES_ENGINE, ServiceUrl.base) + "/execute/rule/" + identification,
				new HttpEntity<>(input), String.class);
	}

	@Override
	public void updateActive(String identification) {
		droolsRuleService.updateActive(identification);
		final DroolsRule rule = droolsRuleService.getRule(identification);
		publishHzRuleNotification(identification, null, rule.getDRL());
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateDRL(String identification, String newDRL) throws Exception {
		droolsRuleService.updateDRL(identification, newDRL);
		publishAndHandleHzNotification(identification, newDRL);
	}

	private void publishHzRuleNotification(String identification, User user, String DRL) {
		final DroolsRule rule = droolsRuleService.getRule(identification);
		final HazelcastRuleObject ruleObj = HazelcastRuleObject.builder().identification(identification)
				.userId(rule == null ? user.getUserId() : rule.getUser().getUserId()).DRL(DRL).build();
		ruleObj.toJson().ifPresent(s -> topicRules.publish(s));

	}

	private void publishAndHandleHzNotification(String identification, String DRL) throws Exception {
		final HazelcastListener listener = new HazelcastListener(identification);
		final String registerId = topicAsyncComm.addMessageListener(listener);
		publishHzRuleNotification(identification, null, DRL);
		final String results = listener.getResults().get(5, TimeUnit.SECONDS);
		if (!results.equalsIgnoreCase(HazelcastMessageNotification.OK))
			throw new RuntimeException(results);
		topicAsyncComm.removeMessageListener(registerId);
	}

	@Getter
	@Setter
	public class HazelcastListener implements MessageListener<String> {

		private boolean messageReceived;
		private String rule;
		private String message;

		public HazelcastListener(String rule) {
			this.rule = rule;
		}

		@Override
		public void onMessage(Message<String> message) {
			HazelcastMessageNotification.fromJson(message.getMessageObject()).ifPresent(h -> {
				if (rule.equals(h.getRule())) {
					messageReceived = true;
					this.message = h.getMessage();
				}
			});
		}

		public Future<String> getResults() {
			return Executors.newSingleThreadExecutor().submit(() -> {
				while (!messageReceived) {
					Thread.sleep(500);
				}
				return message;

			});
		}

	}

}
