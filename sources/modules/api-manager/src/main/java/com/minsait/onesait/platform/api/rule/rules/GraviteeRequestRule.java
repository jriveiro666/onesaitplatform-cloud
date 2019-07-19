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
package com.minsait.onesait.platform.api.rule.rules;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.net.HttpHeaders;
import com.minsait.onesait.platform.api.rule.DefaultRuleBase;
import com.minsait.onesait.platform.api.rule.RuleManager;
import com.minsait.onesait.platform.api.service.ApiServiceInterface;
import com.minsait.onesait.platform.config.model.Api;

@Component
@Rule(description = "Rule to prevent requests to Gravitee APIs not coming from Gravitee Gateway")
public class GraviteeRequestRule extends DefaultRuleBase {

	@Value("${gravitee.header-value:Gravitee-Server}")
	private String GRAVITEE_HEADER_VALUE;

	@Priority
	public int getPriority() {
		return 3;
	}

	@SuppressWarnings("unchecked")
	@Condition
	public boolean isGraviteeApi(Facts facts) {
		final Map<String, Object> data = (Map<String, Object>) facts.get(RuleManager.FACTS);
		final Api api = (Api) data.get(ApiServiceInterface.API);
		if (api != null && !StringUtils.isEmpty(api.getGraviteeId()))
			return true;
		else
			return false;
	}

	@SuppressWarnings("unchecked")
	@Action
	public void allowRequestToApi(Facts facts) {
		final Map<String, Object> data = (Map<String, Object>) facts.get(RuleManager.FACTS);
		final HttpServletRequest request = (HttpServletRequest) data.get(ApiServiceInterface.REQUEST);
		final String requestedWith = request.getHeader(HttpHeaders.X_REQUESTED_WITH);
		if (StringUtils.isEmpty(requestedWith) || !requestedWith.equalsIgnoreCase(GRAVITEE_HEADER_VALUE))
			stopAllNextRules(facts,
					"You are trying to access a Gravitee API from a client that does not have permission",
					DefaultRuleBase.ReasonType.SECURITY);

	}
}
