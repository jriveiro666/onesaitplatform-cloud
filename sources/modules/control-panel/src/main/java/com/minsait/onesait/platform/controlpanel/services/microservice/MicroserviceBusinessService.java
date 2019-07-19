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
package com.minsait.onesait.platform.controlpanel.services.microservice;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.minsait.onesait.platform.config.components.GitlabConfiguration;
import com.minsait.onesait.platform.config.model.DigitalTwinDevice;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.services.microservice.dto.DeployParameters;
import com.minsait.onesait.platform.config.services.microservice.dto.JenkinsParameter;

public interface MicroserviceBusinessService {

	Microservice createMicroservice(Microservice microservice, boolean createGitlab, boolean defaultGitlab,
			boolean defaultJenkins, boolean defaultCaaS, String sources, String docker, File file, String ontology);

	Microservice createMicroserviceFromDigitalTwin(DigitalTwinDevice device, File file,
			GitlabConfiguration configuration, String sources, String docker);

	String createJenkinsPipeline(Microservice microservice);

	String createGitlabRepository(Microservice microservice, File file, String ontology);

	List<JenkinsParameter> getJenkinsJobParameters(Microservice microservice);

	boolean hasPipelineFinished(Microservice microservice);

	int buildJenkins(Microservice microservice, List<JenkinsParameter> parameters);

	String deployMicroservice(Microservice microservice, String environment, String worker, String onesaitServerUrl,
			String dockerImageUrl);

	String upgradeMicroservice(Microservice microservice, String dockerImageUrl, Map<String, String> mapEnv);

	void stopMicroservice(Microservice microservice);

	void deleteMicroservice(Microservice microservice);

	Map<String, String> getEnvMap(Microservice microservice);

	DeployParameters getHosts(Microservice microservice, String environment);

	DeployParameters getEnvironments(Microservice microservice);
}
