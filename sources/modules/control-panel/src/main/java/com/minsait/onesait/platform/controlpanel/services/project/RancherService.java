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
package com.minsait.onesait.platform.controlpanel.services.project;

import java.util.List;
import java.util.Map;

import com.minsait.onesait.platform.config.components.RancherConfiguration;
import com.minsait.onesait.platform.config.model.Microservice;

public interface RancherService {

	public List<String> getRancherEnvironments(String rancherConfigId);

	public List<String> getRancherEnvironments(String rancherConfigId, String url);

	public List<String> getRancherEnvironments(RancherConfiguration rancherConfig);

	public List<String> getRancherWorkers(RancherConfiguration rancherConfig, String env);

	public String createRancherEnvironment(String rancherConfigId, String name);

	public String deployRancherEnvironment(String configId, String environment, Map<String, Integer> services);

	public String deployMicroservice(RancherConfiguration config, String environment, String name,
			String dockerImageURL, String onesaitServerName, String contextPath, int port);

	public String deployMicroservice(Microservice microservice, String environment, String worker,
			String onesaitServerName);

	public String upgradeMicroservice(Microservice microservice, Map<String, String> mapEnv);

	public String deployRancherEnvironment(String configId, String environment, Map<String, Integer> services,
			String url, String projectName);

	public String stopStack(RancherConfiguration rancher, String stack, String environment);

	public Map<String, String> getDeployedEnvVariables(Microservice microservice);

}
