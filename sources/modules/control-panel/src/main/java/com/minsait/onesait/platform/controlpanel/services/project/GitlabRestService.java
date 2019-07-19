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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.controlpanel.services.project.exceptions.GitlabException;

public interface GitlabRestService {

	public String getOauthToken(String url, String user, String password) throws GitlabException;

	public JsonNode createProject(String url, String token, String name, int namespaceId, boolean isPrivateToken)
			throws GitlabException;

	public Map<String, Integer> authorizeUsers(String url, String token, int projectId, List<String> users,
			boolean isPrivateToken) throws IOException, GitlabException, URISyntaxException;

	public String createGitlabProject(String gitlabConfigId, String projectName, List<String> users, String url,
			boolean scaffolding) throws GitlabException;

	public String createGitlabProject(String projectName, String url, String privateToken, boolean scaffolding,
			File file, TemplateType type, String ontology) throws GitlabException;

	public int createNamespace(String url, String projectName, String token, boolean isPrivateToken)
			throws GitlabException;

	public void deleteProject(String url, String token, int projectId, boolean isPrivateToken) throws GitlabException;
}
