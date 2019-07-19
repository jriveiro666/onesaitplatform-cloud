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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.minsait.onesait.platform.commons.ssl.SSLUtil;
import com.minsait.onesait.platform.config.components.GitlabConfiguration;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.services.microservice.MicroserviceTemplateUtil;
import com.minsait.onesait.platform.controlpanel.services.project.exceptions.GitlabException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GitlabRestServiceImpl implements GitlabRestService {
	private static final String GITLAB_API_PATH = "/api/v4";
	private static final String GITLAB_OAUTH = "/oauth/token";
	private static final String GITLAB_PROJECTS = "/projects";
	private static final String GITLAB_USERS = "/users?per_page=1000";
	private static final String GITLAB_MEMBERS = "/members";
	private static final String GITLAB_GROUPS = "/groups";
	private static final String DEFAULT_BRANCH_PUSH = "master";
	private static final String INITIAL_COMMIT = "Initial commit";
	private static final String GIT_REPO_URL_NODE = "http_url_to_repo";
	private static final String USERNAME_STR = "username";
	private static final String EMAIL_STR = "email";
	private static final String GITLAB_CURRENT_USER = "/user";

	private final static String RESOURCE_PATH_SCAFFOLDING = "static/gitlab/scaffolding-sb-vue.zip";
	private final static String RESOURCE_PATH_MICROSERVICE = "static/microservices/microservice.zip";

	@Value("${onesaitplatform.gitlab.scaffolding.directory:/tmp/scaffolding}")
	private String directoryScaffolding;

	@Autowired
	private UserService userService;
	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private MicroserviceTemplateUtil microserviceTemplateUtil;

	@Override
	public String createGitlabProject(String gitlabConfigId, String projectName, List<String> users, String url,
			boolean scaffolding) throws GitlabException {
		final GitlabConfiguration gitlab = configurationService.getGitlabConfiguration(gitlabConfigId);
		String webUrl = "";
		if (gitlab != null) {
			final String urlGitlab = !StringUtils.isEmpty(url) ? url : gitlab.getSite();
			final String user = gitlab.getUser();
			final String password = gitlab.getPassword();
			boolean projectCreated = false;
			int projectId = 0;
			String accessToken = "";
			if (!StringUtils.isEmpty(urlGitlab) && !StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
				try {
					accessToken = getOauthToken(urlGitlab, user, password);
					if (!StringUtils.isEmpty(accessToken)) {
						final int namespaceId = createNamespace(urlGitlab, projectName, accessToken, false);
						log.info("Namespace created with id: " + namespaceId);
						log.info("Project is going to be created with parameters, url: " + urlGitlab + " accessToken: "
								+ accessToken + " projectName: " + projectName + " namespaceId: " + namespaceId);
						final JsonNode projectInfo = createProject(urlGitlab, accessToken, projectName, namespaceId,
								false);
						projectId = projectInfo.get("id").asInt();
						webUrl = projectInfo.get("web_url").asText();
						projectCreated = true;
						try {
							authorizeUsers(urlGitlab, accessToken, projectId, users, false);
						} catch (final GitlabException e) {
							log.error("Could not add users to project");
						}
						if (scaffolding)
							microserviceTemplateUtil.generateScaffolding(projectInfo, gitlab, RESOURCE_PATH_SCAFFOLDING,
									null, null);

						return webUrl;
					}
				} catch (final Exception e) {
					log.error("Could not create Gitlab project {}", e.getMessage());
					if (projectCreated) {
						log.error(
								"Project was created in gitlab but something went wrong, rolling back and destroying project {}",
								projectName);
						deleteProject(urlGitlab, accessToken, projectId, false);
					}
					throw new GitlabException(e.getMessage());
				}
			}

		} else {
			throw new GitlabException("No configuration found for Gitlab");
		}
		return webUrl;

	}

	@Override
	public String createGitlabProject(String projectName, String url, String privateToken, boolean scaffolding,
			File file, TemplateType type, String ontology) throws GitlabException {
		int projectId = 0;
		boolean projectCreated = false;
		String webUrl = url;
		try {

			// First generate gitlab config for convenience
			final GitlabConfiguration gitlabConfig = getGitlabConfigurationFromPrivateToken(url, privateToken);
			final int namespaceId = createNamespace(url, projectName, privateToken, true);
			final JsonNode projectInfo = createProject(url, privateToken, projectName, namespaceId, true);
			projectId = projectInfo.get("id").asInt();
			webUrl = projectInfo.get("web_url").asText();
			projectCreated = true;
			// TO-DO authorize users into the project
			if (scaffolding)
				if (file == null)
					microserviceTemplateUtil.generateScaffolding(projectInfo, gitlabConfig, RESOURCE_PATH_MICROSERVICE,
							type, ontology);
				else
					microserviceTemplateUtil.generateScaffolding(projectInfo, gitlabConfig, file.getAbsolutePath(),
							type, ontology);
		} catch (final Exception e) {
			log.error("Could not create Gitlab project {}", e.getMessage());
			if (projectCreated) {
				log.error(
						"Project was created in gitlab but something went wrong, rolling back and destroying project {}",
						projectName);
				deleteProject(url, privateToken, projectId, true);
			}
			throw new GitlabException(e.getMessage());
		}

		return webUrl;
	}

	private GitlabConfiguration getGitlabConfigurationFromPrivateToken(String url, String privateToken)
			throws GitlabException {
		try {
			final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_CURRENT_USER),
					HttpMethod.GET, null, privateToken, true);
			return GitlabConfiguration.builder().site(url).email(response.getBody().get(EMAIL_STR).asText())
					.user(response.getBody().get(USERNAME_STR).asText()).privateToken(privateToken).build();
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not retrieve user info from private token {}", e.getResponseBodyAsString());
			throw new GitlabException("Could not retrieve user info from private token" + e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException("Could not retrieve user info from private token" + e.getMessage());
		}
	}

	@Override
	public String getOauthToken(String url, String user, String password) throws GitlabException {
		final String body = "{\"grant_type\":\"password\",\"username\":\"" + user + "\",\"password\":\"" + password
				+ "\"}";
		try {
			final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_OAUTH), HttpMethod.POST, body, null,
					false);
			return response.getBody().get("access_token").asText();
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not get authentication token {}", e.getResponseBodyAsString());
			throw new GitlabException("Could not get authentication token", e);
		} catch (final Exception e) {
			throw new GitlabException("Could not get authentication token", e);
		}

	}

	@Override
	public int createNamespace(String url, String projectName, String token, boolean isPrivateToken)
			throws GitlabException {
		final String body = "{\"name\":\"" + projectName + "\",\"path\":\""
				+ projectName.toLowerCase().replace(" ", "-") + "\", \"visibility\":\"private\"}";
		int namespaceId = 0;
		try {
			final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_GROUPS),
					HttpMethod.POST, body, token, isPrivateToken);
			namespaceId = response.getBody().get("id").asInt();
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not create namespace for project {}", e.getResponseBodyAsString());
			throw new GitlabException("Could not create namespace for project" + e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException("Could not create namespace for project" + e.getMessage());
		}
		return namespaceId;
	}

	@Override
	public JsonNode createProject(String url, String token, String name, int namespaceId, boolean isPrivateToken)
			throws GitlabException {
		final String body = "{\"name\":\"" + name + "\",\"visibility\":\"private\", \"namespace_id\":" + namespaceId
				+ "}";
		try {
			final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_PROJECTS),
					HttpMethod.POST, body, token, isPrivateToken);
			return response.getBody();
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not create project {}", e.getResponseBodyAsString());
			throw new GitlabException(e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException(e.getMessage());
		}

	}

	@Override
	public void deleteProject(String url, String token, int projectId, boolean isPrivateToken) throws GitlabException {
		try {
			sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_PROJECTS).concat("/").concat(String.valueOf(projectId)),
					HttpMethod.DELETE, "", token, isPrivateToken);
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not delete project {}", e.getResponseBodyAsString());
			throw new GitlabException(e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException("Could not delete project" + e.getMessage());
		}

	}

	@Override
	public Map<String, Integer> authorizeUsers(String url, String token, int projectId, List<String> users,
			boolean isPrivateToken) throws IOException, GitlabException, URISyntaxException {
		final ArrayNode repoUsers = (ArrayNode) getRepositoryUsers(url, token, false);
		final List<String> existingUsers = new ArrayList<>();
		try {
			for (final JsonNode user : repoUsers) {
				log.info("Authorize user: " + user.get(USERNAME_STR).asText());
				if (users.contains(user.get(USERNAME_STR).asText())) {
					authorizeUser(url, projectId, user.get("id").asInt(), token, isPrivateToken);
					existingUsers.add(user.get(USERNAME_STR).asText());
					log.info("User authorized!! " + user.get(USERNAME_STR).asText());
				}
			}
			final List<String> newUsers = users.stream().filter(s -> !existingUsers.contains(s))
					.collect(Collectors.toList());
			for (final String user : newUsers) {
				try {
					final int newUserId = createNewUser(url, token, userService.getUser(user), false);
					authorizeUser(url, projectId, newUserId, token, false);
				} catch (final GitlabException e) {
					log.error("Could not create user {}, cause:", user, e.getMessage());
				}

			}
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not get authentication token {}", e.getResponseBodyAsString());
			throw new GitlabException("Could not authorize users " + e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException("Could not authorize users " + e.getMessage());
		}
		return null;
	}

	private int createNewUser(String url, String token, User user, boolean isPrivateToken) throws GitlabException {
		final String body = "{\"email\":\"" + user.getEmail() + "\", \"username\":\"" + user.getUserId()
				+ "\",\"name\":\"" + user.getFullName() + "\",\"reset_password\": true}";
		try {
			final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_USERS),
					HttpMethod.POST, body, token, isPrivateToken);
			return response.getBody().get("id").asInt();
		} catch (final HttpClientErrorException | HttpServerErrorException e) {
			log.error("Could not create user {}", e.getResponseBodyAsString());
			throw new GitlabException("Could not create user " + e.getResponseBodyAsString());
		} catch (final Exception e) {
			throw new GitlabException("Could not create user, your access level is not Administrator", e);
		}
	}

	private void authorizeUser(String url, int projectId, int userId, String token, boolean isPrivateToken)
			throws URISyntaxException, IOException {
		final String body = "{\"id\": " + projectId + ", \"access_level\": 30 , \"user_id\":" + userId + "}";
		sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_PROJECTS).concat("/").concat(String.valueOf(projectId))
				.concat(GITLAB_MEMBERS), HttpMethod.POST, body, token, isPrivateToken);
	}

	private JsonNode getRepositoryUsers(String url, String token, boolean isPrivateToken)
			throws URISyntaxException, IOException {
		final ResponseEntity<JsonNode> response = sendHttp(url.concat(GITLAB_API_PATH).concat(GITLAB_USERS),
				HttpMethod.GET, "", token, isPrivateToken);
		return response.getBody();
	}

	private ResponseEntity<JsonNode> sendHttp(String url, HttpMethod httpMethod, String body, String token,
			boolean isPrivateToken) throws URISyntaxException, ClientProtocolException, IOException {

		final RestTemplate restTemplate = new RestTemplate(SSLUtil.getHttpRequestFactoryAvoidingSSLVerification());
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (!StringUtils.isEmpty(token)) {
			if (!isPrivateToken)
				headers.add("Authorization", "Bearer " + token);
			else
				headers.add("Private-Token", token);
		}

		final org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(body,
				headers);
		ResponseEntity<JsonNode> response = new ResponseEntity<>(HttpStatus.ACCEPTED);
		response = restTemplate.exchange(new URI(url), httpMethod, request, JsonNode.class);

		final HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", response.getHeaders().getContentType().toString());
		return new ResponseEntity<>(response.getBody(), responseHeaders,
				HttpStatus.valueOf(response.getStatusCode().value()));
	}

}
