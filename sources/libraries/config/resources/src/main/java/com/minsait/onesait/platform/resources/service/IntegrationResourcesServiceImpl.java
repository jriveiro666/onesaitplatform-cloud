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
package com.minsait.onesait.platform.resources.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minsait.onesait.platform.commons.ActiveProfileDetector;
import com.minsait.onesait.platform.config.components.GlobalConfiguration;
import com.minsait.onesait.platform.config.components.Urls;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IntegrationResourcesServiceImpl implements IntegrationResourcesService {

	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private ActiveProfileDetector profileDetector;

	private String profile;

	private Urls urls;
	@Getter
	private GlobalConfiguration globalConfiguration;

	public enum ServiceUrl {
		base, advice, router, hawtio, swaggerUI, api, swaggerUIManagement, swaggerJson, embedded, ui, gateway, management
	}

	public enum Module {
		iotbroker, scriptingEngine, flowEngine, routerStandAlone, apiManager, controlpanel, digitalTwinBroker, domain, monitoringUI, gravitee, RULES_ENGINE

	}

	public final static String SWAGGER_UI_SUFFIX = "swagger-ui.html";
	public final static String LOCALHOST = "localhost";

	@PostConstruct
	public void getActiveProfile() {

		profile = profileDetector.getActiveProfile();
		loadConfigurations();
	}

	@Override
	public String getUrl(Module module, ServiceUrl service) {
		try {
			switch (module) {
			case controlpanel:
				switch (service) {
				case base:
					return urls.getControlpanel().getBase();
				default:
					break;
				}
				break;
			case iotbroker:
				switch (service) {
				case base:
					return urls.getIotbroker().getBase();
				case advice:
					return urls.getIotbroker().getAdvice();
				default:
					break;
				}
				break;
			case scriptingEngine:
				switch (service) {
				case base:
					return urls.getScriptingEngine().getBase();
				case advice:
					return urls.getScriptingEngine().getAdvice();
				default:
					break;
				}
				break;
			case flowEngine:
				switch (service) {
				case base:
					return urls.getFlowEngine().getBase();
				case advice:
					return urls.getFlowEngine().getAdvice();
				default:
					break;
				}
				break;
			case routerStandAlone:
				switch (service) {
				case base:
					return urls.getRouterStandAlone().getBase();
				case advice:
					return urls.getRouterStandAlone().getAdvice();
				case management:
					return urls.getRouterStandAlone().getManagement();
				case router:
					return urls.getRouterStandAlone().getRouter();
				case hawtio:
					return urls.getRouterStandAlone().getHawtio();
				case swaggerUI:
					return urls.getRouterStandAlone().getSwaggerUI();
				default:
					break;
				}
				break;
			case apiManager:
				switch (service) {
				case base:
					return urls.getApiManager().getBase();
				case api:
					return urls.getApiManager().getApi();
				case swaggerUI:
					return urls.getApiManager().getSwaggerUI();
				case swaggerUIManagement:
					return urls.getApiManager().getSwaggerUIManagement();
				case swaggerJson:
					return urls.getApiManager().getSwaggerJson();

				default:
					break;
				}

				break;
			case digitalTwinBroker:
				switch (service) {
				case base:
					return urls.getDigitalTwinBroker().getBase();
				default:
					break;
				}
				break;
			case domain:
				switch (service) {
				case base:
					return urls.getDomain().getBase();

				default:
					break;
				}

			case gravitee:
				switch (service) {
				case ui:
					return urls.getGravitee().getUi();
				case management:
					return urls.getGravitee().getManagement();
				case gateway:
					return urls.getGravitee().getGateway();
				default:
					break;

				}

				break;
			case monitoringUI:
				switch (service) {
				case base:
					return urls.getMonitoringUI().getBase();
				case embedded:
					return urls.getMonitoringUI().getEmbedded();
				default:
					break;
				}
				break;
			case RULES_ENGINE:
				switch (service) {
				case advice:
					return urls.getRulesEngine().getAdvice();
				case base:
					return urls.getRulesEngine().getBase();

				default:
					break;
				}
			default:
				break;
			}
		} catch (final Exception e) {
			log.error("Error : {}", e);
		}
		return "RESOURCE_URL_NOT_FOUND";
	}

	@Override
	public Map<String, String> getSwaggerUrls() {
		final Map<String, String> map = new HashMap<>();
		final String base = urls.getDomain().getBase();
		String controlpanel = base.endsWith("/") ? base.concat("controlpanel") : base.concat("/controlpanel");
		String iotbroker = base.endsWith("/") ? base.concat("iot-broker") : base.concat("/iot-broker");
		String apimanager = base.endsWith("/") ? base.concat("api-manager") : base.concat("/api-manager");
		String router = base.endsWith("/") ? base.concat("router") : base.concat("/router");
		String digitalTwinBroker = base.endsWith("/") ? base.concat("digitaltwinbroker")
				: base.concat("/digitaltwinbroker");
		if (base.contains(LOCALHOST)) {
			controlpanel = urls.getControlpanel().getBase();
			iotbroker = urls.getIotbroker().getBase();
			apimanager = urls.getApiManager().getBase();
			router = urls.getRouterStandAlone().getBase();
			digitalTwinBroker = urls.getDigitalTwinBroker().getBase();
		}
		map.put(Module.controlpanel.name(), controlpanel.endsWith("/") ? controlpanel.concat(SWAGGER_UI_SUFFIX)
				: controlpanel.concat("/").concat(SWAGGER_UI_SUFFIX));
		map.put(Module.iotbroker.name(), iotbroker.endsWith("/") ? iotbroker.concat(SWAGGER_UI_SUFFIX)
				: iotbroker.concat("/").concat(SWAGGER_UI_SUFFIX));
		map.put(Module.apiManager.name(), apimanager.endsWith("/") ? apimanager.concat(SWAGGER_UI_SUFFIX)
				: apimanager.concat("/").concat(SWAGGER_UI_SUFFIX));
		map.put(Module.routerStandAlone.name(),
				router.endsWith("/") ? router.concat(SWAGGER_UI_SUFFIX) : router.concat("/").concat(SWAGGER_UI_SUFFIX));
		map.put(Module.digitalTwinBroker.name(),
				digitalTwinBroker.endsWith("/") ? digitalTwinBroker.concat(SWAGGER_UI_SUFFIX)
						: digitalTwinBroker.concat("/").concat(SWAGGER_UI_SUFFIX));

		return map;
	}

	@Override
	public void reloadConfigurations() {
		loadConfigurations();
	}

	private void loadConfigurations() {
		urls = configurationService.getEndpointsUrls(profile);
		globalConfiguration = configurationService.getGlobalConfiguration(profile);

	}
}
