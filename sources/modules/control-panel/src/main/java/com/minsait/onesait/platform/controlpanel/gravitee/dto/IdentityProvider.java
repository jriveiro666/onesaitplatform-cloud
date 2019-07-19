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
package com.minsait.onesait.platform.controlpanel.gravitee.dto;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class IdentityProvider {

	private String description;
	private String name;
	private boolean enabled;
	private JsonNode configuration;
	private ArrayNode roleMappings;
	private JsonNode userProfileMapping;
	private String type;

	private static final String SERVER_NAME = "SERVER_NAME";
	private static final String DEFAULT_SERVER_NAME = "localhost";
	private static final int DEFAULT_OAUTH_PORT = 21000;

	public static final String DEFAULT_OAUTH_RESOURCE_2_UPDATE = "{\n" + "    \"name\": \"onesait account\",\n"
			+ "    \"enabled\": true,\n" + "    \"configuration\": {\n" + "        \"scopes\": [\n"
			+ "            \"openid\"\n" + "        ],\n" + "        \"clientId\": \"onesaitplatform\",\n"
			+ "        \"clientSecret\": \"onesaitplatform\",\n"
			+ "        \"tokenEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/token\",\n"
			+ "        \"tokenIntrospectionEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/openplatform-oauth/tokenInfo\",\n"
			+ "        \"checkTokenEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/check_token\",\n"
			+ "        \"authorizeEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/authorize\",\n"
			+ "        \"userInfoEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oidc/userinfo\",\n"
			+ "        \"userLogoutEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/logout\",\n"
			+ "        \"color\": \"#74e1f1\"\n" + "    },\n" + "    \"roleMappings\": [\n" + "        {\n"
			+ "            \"condition\": \"{#jsonPath(#profile, '$.role') == \\\"ROLE_USER\\\"}\",\n"
			+ "            \"portal\": \"USER\",\n" + "            \"management\": \"USER\"\n" + "        },\n"
			+ "        {\n"
			+ "            \"condition\": \"{#jsonPath(#profile, '$.role') == \\\"ROLE_DEVELOPER\\\"}\",\n"
			+ "            \"portal\": \"USER\",\n" + "            \"management\": \"API_PUBLISHER\"\n" + "        },\n"
			+ "        {\n"
			+ "            \"condition\": \"{#jsonPath(#profile, '$.role') == \\\"ROLE_DATASCIENTIST\\\"}\",\n"
			+ "            \"portal\": \"USER\",\n" + "            \"management\": \"API_PUBLISHER\"\n" + "        },\n"
			+ "        {\n"
			+ "            \"condition\": \"{#jsonPath(#profile, '$.role') == \\\"ROLE_ADMINISTRATOR\\\"}\",\n"
			+ "            \"portal\": \"ADMIN\",\n" + "            \"management\": \"ADMIN\"\n" + "        }\n"
			+ "    ],\n" + "    \"userProfileMapping\": {\n" + "        \"id\": \"username\",\n"
			+ "        \"firstname\": \"name\",\n" + "        \"lastname\": \"userid\",\n"
			+ "        \"email\": \"mail\"\n" + "    }\n" + "}";

	public static final String DEFAULT_OAUTH_RESOURCE_2_CREATE = "{\n" + "    \"name\": \"onesait account\",\n"
			+ "    \"description\": \"onesait oauth server\",\n" + "    \"type\": \"oidc\",\n"
			+ "    \"enabled\": true,\n" + "    \"configuration\": {\n" + "        \"scopes\": [\n"
			+ "            \"openid\"\n" + "        ],\n" + "        \"clientId\": \"onesaitplatform\",\n"
			+ "        \"clientSecret\": \"onesaitplatform\",\n"
			+ "        \"tokenEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/token\",\n"
			+ "        \"tokenIntrospectionEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/openplatform-oauth/tokenInfo\",\n"
			+ "        \"checkTokenEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/check_token\",\n"
			+ "        \"authorizeEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oauth/authorize\",\n"
			+ "        \"userInfoEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/oidc/userinfo\",\n"
			+ "        \"userLogoutEndpoint\": \"https://{{SERVER_NAME}}/oauth-server/logout\",\n"
			+ "        \"color\": \"#74e1f1\"\n" + "    },\n" + "    \"userProfileMapping\": {\n"
			+ "        \"id\": \"username\",\n" + "        \"firstname\": \"name\",\n"
			+ "        \"lastname\": \"userid\",\n" + "        \"email\": \"mail\"\n" + "    }\n" + "}";

	public static IdentityProvider getFromString(String identity) throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(compileServerName(identity), IdentityProvider.class);
	}

	private static String compileServerName(String template) {
		final Writer writer = new StringWriter();
		final StringReader reader = new StringReader(template);
		final HashMap<String, String> scopes = new HashMap<>();
		final String serverName = System.getenv(SERVER_NAME);
		if (StringUtils.isEmpty(serverName))
			scopes.put(SERVER_NAME, DEFAULT_SERVER_NAME + ":" + DEFAULT_OAUTH_PORT);
		else
			scopes.put(SERVER_NAME, serverName);
		final MustacheFactory mf = new DefaultMustacheFactory();
		final Mustache mustache = mf.compile(reader, "oauth path");
		mustache.execute(writer, scopes);
		return writer.toString();
	}

}
