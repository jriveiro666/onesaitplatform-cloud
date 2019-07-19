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
package com.minsait.onesait.platform.controlpanel.services.jenkins;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jclouds.Constants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.common.Error;
import com.cdancy.jenkins.rest.domain.common.IntegerResponse;
import com.cdancy.jenkins.rest.domain.common.RequestStatus;
import com.cdancy.jenkins.rest.domain.job.BuildInfo;
import com.cdancy.jenkins.rest.domain.job.JobInfo;
import com.cdancy.jenkins.rest.domain.queue.Executable;
import com.cdancy.jenkins.rest.domain.queue.QueueItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.components.JenkinsConfiguration;

@Service
public class JenkinsServiceImpl implements JenkinsService {

	private static final String PROPERTY_PATH = "property";
	private static final String CLASS_PATH = "_class";
	private static final String PROPERTY_CLASS = "hudson.model.ParametersDefinitionProperty";
	private static final String PARAM_DEFINITION = "parameterDefinitions";

	@Override
	public JobInfo getJobInfo(String jenkinsUrl, String username, String token, String jobName, String folderName) {
		return getJenkinsClient(jenkinsUrl, username, token).api().jobsApi().jobInfo(folderName, jobName);
	}

	@Override
	public void createJob(String jenkinsUrl, String username, String token, String jobName, String folderName,
			String jobConfigXML) {
		final RequestStatus status = getJenkinsClient(jenkinsUrl, username, token).api().jobsApi().create(folderName,
				jobName, jobConfigXML);
		if (!status.errors().isEmpty()) {
			throwException(status.errors());
		}
	}

	@Override
	public void createJobFolder(String jenkinsUrl, String username, String token, String folderName,
			String folderConfigXML) {
		final RequestStatus status = getJenkinsClient(jenkinsUrl, username, token).api().jobsApi().create(null,
				folderName, folderConfigXML);
		if (!status.errors().isEmpty()) {
			throwException(status.errors());
		}
	}

	@Override
	public void deleteJob(String jenkinsUrl, String username, String token, String jobName, String folderName) {
		final RequestStatus status = getJenkinsClient(jenkinsUrl, username, token).api().jobsApi().delete(folderName,
				jobName);
		if (!status.errors().isEmpty()) {
			throwException(status.errors());
		}
	}

	@Override
	public int buildWithParameters(String jenkinsUrl, String username, String token, String jobName, String folderName,
			Map<String, List<String>> parameters) {
		final IntegerResponse response = getJenkinsClient(jenkinsUrl, username, token).api().jobsApi()
				.buildWithParameters(folderName, jobName, parameters);
		if (response.value() == null)
			throwException(response.errors());
		return response.value();

	}

	@Override
	public Map<String, String> getParametersFromJob(String jenkinsUrl, String username, String token, String jobName) {
		final String url = jenkinsUrl.concat("/job/").concat(jobName).concat("/api/json");
		final JsonNode response = requestJenkins(url, username, token, JsonNode.class).getBody();
		return extractParameters(response);
	}

	private <T> ResponseEntity<T> requestJenkins(String url, String username, String token, Class<T> responseType) {
		final RestTemplate rt = new RestTemplate(SSLUtil.getHttpRequestFactoryAvoidingSSLVerification());
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization",
				"Basic " + Base64.getEncoder().encodeToString(username.concat(":").concat(token).getBytes()));
		final HttpEntity<?> entity = new HttpEntity<>(headers);
		return rt.exchange(url, HttpMethod.GET, entity, responseType);

	}

	@Override
	public JenkinsClient getJenkinsClient(String jenkinsUrl, String username, String token) {
		return JenkinsClient.builder().endPoint(jenkinsUrl).credentials(username.concat(":").concat(token))
				.overrides(overrideSSLProperties()).build();
	}

	private Properties overrideSSLProperties() {
		final Properties overrides = new Properties();
		overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
		overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
		return overrides;
	}

	private void throwException(List<Error> errors) {
		final StringBuilder sb = new StringBuilder();
		errors.stream().forEach(
				e -> sb.append(e.exceptionName().concat(" : ").concat(e.message().concat(System.lineSeparator()))));
		throw new JenkinsException(sb.toString());
	}

	private Map<String, String> extractParameters(JsonNode node) {
		final ObjectMapper mapper = new ObjectMapper();
		final Map<String, String> parameters = new HashMap<>();
		final ArrayNode properties = (ArrayNode) node.get(PROPERTY_PATH);
		final ArrayNode params = mapper.createArrayNode();
		if (properties.size() > 0) {
			properties.forEach(n -> {
				if (n.get(CLASS_PATH).asText().equals(PROPERTY_CLASS))
					params.add(n.get(PARAM_DEFINITION));
			});
			if (params.size() > 0) {
				params.get(0).forEach(n -> {
					parameters.put(n.get("name").asText(), n.at("/defaultParameterValue/value").asText());
				});
			}
		}
		return parameters;
	}

	@Override
	public BuildInfo buildInfo(JenkinsConfiguration config, String jobName, String folderName, int queueId) {
		final JenkinsClient client = getJenkinsClient(config.getJenkinsUrl(), config.getUsername(), config.getToken());

		final QueueItem queueItem = client.api().queueApi().queueItem(queueId);
		final Executable executable = queueItem.executable();
		if (executable != null) {
			return client.api().jobsApi().buildInfo(null, jobName, executable.number());
		}

		return null;
	}

	@Override
	public BuildInfo lastBuildInfo(JenkinsConfiguration config, String jobName, String folderName) {
		return getJenkinsClient(config.getJenkinsUrl(), config.getUsername(), config.getToken()).api().jobsApi()
				.jobInfo(folderName, jobName).lastBuild();
	}

}
