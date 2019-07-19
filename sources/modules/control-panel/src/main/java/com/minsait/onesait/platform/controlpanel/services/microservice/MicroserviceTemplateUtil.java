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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.InclusionLevel;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.rules.RuleFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.minsait.onesait.platform.config.components.GitlabConfiguration;
import com.minsait.onesait.platform.config.model.ClientPlatform;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.config.model.Token;
import com.minsait.onesait.platform.config.repository.ClientPlatformOntologyRepository;
import com.minsait.onesait.platform.config.services.client.ClientPlatformService;
import com.minsait.onesait.platform.config.services.ontology.OntologyService;
import com.minsait.onesait.platform.config.services.token.TokenService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.services.project.GitOperations;
import com.minsait.onesait.platform.controlpanel.services.project.exceptions.GitlabException;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.Module;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesServiceImpl.ServiceUrl;
import com.sun.codemodel.JCodeModel;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MicroserviceTemplateUtil {

	@Autowired
	private GitOperations gitOperations;
	@Autowired
	private OntologyService ontologyService;
	@Autowired
	private ClientPlatformService clientPlatformService;
	@Autowired
	private ClientPlatformOntologyRepository clientPlatformOntologyRepository;
	@Autowired
	private TokenService tokenService;
	@Autowired
	private IntegrationResourcesService resourcesService;
	@Autowired
	private UserService userService;
	@Autowired
	private AppWebUtils utils;

	@Value("${onesaitplatform.gitlab.scaffolding.directory:/tmp/scaffolding}")
	private String directoryScaffolding;
	private static final String GIT_REPO_URL_NODE = "http_url_to_repo";
	private static final String GIT_NAME_NODE = "name";
	private static final String DEFAULT_BRANCH_PUSH = "master";
	private static final String INITIAL_COMMIT = "Initial commit";
	private static final String MODEL_PACKAGE = "com.minsait.onesait.microservice.model";
	private static final String REPOSITORY_PACKAGE = "com.minsait.onesait.microservice.repository";
	private static final String REST_SERVICE_PACKAGE = "com.minsait.onesait.microservice.rest";
	private static final String CONFIG_PACKAGE = "com.minsait.onesait.microservice.config";
	private static final String SOURCES_PATH = "/sources/src/main/java";
	private static final String DOCKER_PATH = "/sources/docker";
	private Template swaggerConfigTemplate;
	private Template genericRestServiceTemplate;
	private Template genericRepositoryTemplate;
	private Template dockerfileTemplate;

	@PostConstruct
	public void init() {
		final Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		try {
			final TemplateLoader templateLoader = new ClassTemplateLoader(getClass(),
					"/static/microservices/templates");

			cfg.setTemplateLoader(templateLoader);

			genericRepositoryTemplate = cfg.getTemplate("GenericRepository.ftl");
			genericRestServiceTemplate = cfg.getTemplate("GenericRestService.ftl");
			swaggerConfigTemplate = cfg.getTemplate("SwaggerConfig.ftl");
			dockerfileTemplate = cfg.getTemplate("Dockerfile.ftl");

		} catch (final IOException e) {
			log.error("Error configuring the template loader.", e);
		}
	}

	public void generateScaffolding(JsonNode projectInfo, GitlabConfiguration gitlabConfig, String path2Resource,
			Microservice.TemplateType templateType, String ontology) throws GitlabException {
		createAndExtractFiles(path2Resource);
		if (templateType != null && templateType.equals(TemplateType.IOT_CLIENT_ARCHETYPE)) {
			generatePOJOs(ontology);
			processTemplates(ontology, projectInfo.path(GIT_NAME_NODE).asText());
		}
		completeScaffolding(projectInfo, gitlabConfig, path2Resource);

	}

	private void createAndExtractFiles(String path2Resource) throws GitlabException {
		log.info("INIT scafolding project generation");
		gitOperations.createDirectory(directoryScaffolding);
		log.info("Directory created");

		gitOperations.unzipScaffolding(directoryScaffolding, path2Resource);
		log.info("Scafolding project unzipped");
	}

	private void generatePOJOs(String ontology) {

		final JCodeModel codeModel = new JCodeModel();
		final String ontologyCap = ontology.substring(0, 1).toUpperCase() + ontology.substring(1);
		final String source = ontologyService.getOntologyByIdentification(ontology).getJsonSchema();
		final String wrapperClassName = ontologyCap + "Wrapper";
		final File outputPojoDirectory = new File(directoryScaffolding + SOURCES_PATH);
		outputPojoDirectory.mkdirs();
		final String packageName = MODEL_PACKAGE;
		final GenerationConfig config = new DefaultGenerationConfig() {
			@Override
			public boolean isGenerateBuilders() {
				return true;
			}

			@Override
			public SourceType getSourceType() {
				return SourceType.JSONSCHEMA;
			}

			@Override
			public InclusionLevel getInclusionLevel() {
				return InclusionLevel.NON_NULL;
			}

			@Override
			public boolean isIncludeToString() {
				return false;
			}

			@Override
			public boolean isIncludeHashcodeAndEquals() {
				return false;
			}

		};
		final SchemaMapper mapper = new SchemaMapper(
				new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());

		try {
			mapper.generate(codeModel, wrapperClassName, packageName, source);
			codeModel.build(outputPojoDirectory);
		} catch (final IOException e) {
			log.error("Could not complete microservice model domain");
		}
	}

	private void processTemplates(String ontology, String name) {
		final String ontologyCap = ontology.substring(0, 1).toUpperCase() + ontology.substring(1);
		final ClientPlatform client = getDeviceForOntologyAndUser(ontology);
		final Token token = tokenService.getToken(client);
		Writer writer = null;
		final Map<String, Object> map = new HashMap<>();
		map.put("WRAPPER_CLASS", ontologyCap + "Wrapper");
		map.put("ONTOLOGY", ontology);
		map.put("ONTOLOGY_CAP", ontologyCap);
		map.put("DOMAIN", resourcesService.getUrl(Module.domain, ServiceUrl.base));
		map.put("DEVICE_TOKEN", token.getToken());
		map.put("DEVICE_TEMPLATE", client.getIdentification());
		map.put("NAME", StringUtils.isEmpty(name) ? "microservice" : name);
		try {
			writer = new FileWriter(new File(directoryScaffolding + SOURCES_PATH + File.separator
					+ REPOSITORY_PACKAGE.replace(".", "/") + File.separator + ontologyCap.concat("Repository.java")));
			genericRepositoryTemplate.process(map, writer);
			writer.flush();
			writer = new FileWriter(new File(directoryScaffolding + SOURCES_PATH + File.separator
					+ REST_SERVICE_PACKAGE.replace(".", "/") + File.separator + ontologyCap.concat("Service.java")));
			genericRestServiceTemplate.process(map, writer);
			writer.flush();
			writer = new FileWriter(new File(directoryScaffolding + SOURCES_PATH + File.separator
					+ CONFIG_PACKAGE.replace(".", "/") + File.separator + "SwaggerConfig.java"));
			swaggerConfigTemplate.process(map, writer);
			writer.flush();
			writer = new FileWriter(new File(directoryScaffolding + DOCKER_PATH + File.separator + "Dockerfile"));
			dockerfileTemplate.process(map, writer);
			writer.flush();

		} catch (final Exception e) {
			log.error("Error while processing templates", e);
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (final IOException e) {
					log.error("Could not close writer", e);
				}
		}

	}

	public void completeScaffolding(JsonNode projectInfo, GitlabConfiguration gitlabConfig, String path2Resource)
			throws GitlabException {

		gitOperations.configureGitlabAndInit(gitlabConfig.getUser(), gitlabConfig.getEmail(), directoryScaffolding);
		log.info("Gitlab project configured");

		gitOperations.addOrigin(projectInfo.get(GIT_REPO_URL_NODE).asText(), directoryScaffolding, false);
		log.info("Origin added");

		gitOperations.addAll(directoryScaffolding);
		log.info("Add all");

		gitOperations.commit(INITIAL_COMMIT, directoryScaffolding);
		log.info("Initial commit");

		gitOperations.push(projectInfo.get(GIT_REPO_URL_NODE).asText(), gitlabConfig.getUser(),
				gitlabConfig.getPassword() == null ? gitlabConfig.getPrivateToken() : gitlabConfig.getPassword(),
				DEFAULT_BRANCH_PUSH, directoryScaffolding);
		log.info("Pushed to: " + projectInfo.get(GIT_REPO_URL_NODE).asText());

		gitOperations.deleteDirectory(directoryScaffolding);
		log.info("Deleting temp directory {}", directoryScaffolding);
		log.info("END scafolding project generation");

	}

	private ClientPlatform getDeviceForOntologyAndUser(String identification) {
		final String userId = utils.getUserId();
		ClientPlatform client = clientPlatformOntologyRepository
				.findByOntology(ontologyService.getOntologyByIdentification(identification)).stream()
				.filter(r -> r.getClientPlatform().getUser().getUserId().equals(userId)).map(r -> r.getClientPlatform())
				.findFirst().orElse(null);
		if (client == null) {
			client = new ClientPlatform();
			client.setIdentification(identification.concat("DeviceMicroservice"));
			client.setUser(userService.getUser(userId));
			clientPlatformService.createClientAndToken(
					Arrays.asList(ontologyService.getOntologyByIdentification(identification)), client);
		}
		return client;

	}

}
