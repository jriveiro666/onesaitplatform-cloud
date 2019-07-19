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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.minsait.onesait.platform.config.components.RancherConfiguration;
import com.minsait.onesait.platform.config.model.Configuration;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RancherServiceImpl implements RancherService {

	private static final String URL_STR = "--url";
	private static final String SECRET_KEY = "--secret-key";
	private static final String ACCESS_KEY = "--access-key";
	private static final String FORMAT_STR = "--format";
	private static final String LINE_SEPARATOR = "line.separator";
	private static final String COULD_NOT_EXECUTE_COMMAND = "Could not execute command ";
	private static final String CREATE_STR = "create";
	private static final String SERVICES_STR = "services";
	private static final String ENV_STR = "--env";
	private static final String DEFAULT_STACK_NAME_MICROSERVICE = "microservices-stack";

	@Autowired
	private ConfigurationService configurationService;
	public final static String DOCKER_COMPOSE = "docker-compose.yml";
	public final static String RANCHER_COMPOSE = "rancher-compose.yml";
	@Value("${onesaitplatform.docker.rancher.projectname:onesaitplatform}")
	private String PROJECT_NAME;
	@Value("${onesaitplatform.docker.rancher.server_name:s4citiespro.westeurope.cloudapp.azure.com}")
	private String SERVER_NAME;
	@Value("${onesaitplatform.docker.rancher.domain_name:s4citiespro.westeurope.cloudapp.azure.com}")
	private String DOMAIN_NAME;
	@Value("${onesaitplatform.docker.rancher.image_tag:latest}")
	private String IMAGE_TAG;
	@Value("${onesaitplatform.docker.tmp: /tmp/}")
	private String TMP_PATH;
	@Value("${onesaitplatform.docker.mandatory-services:elasticdb,configdb,configinit,quasar,realtimedb,controlpanelservice,schedulerdb,monitoringuiservice,loadbalancerservice,routerservice,cacheservice}")
	private String[] MANDATORY_SERVICES;
	private final static String DEFAULT_STACK_NAME = "onesait-platform";
	public final static String RANCHER = "rancher";

	@Override
	public List<String> getRancherEnvironments(RancherConfiguration rancher) {

		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), "env", "ls", FORMAT_STR,
				"{{.Environment.Name}}");

		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			return Arrays.asList(builder.toString().split("\n"));

		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
		return null;
	}

	@Override
	public List<String> getRancherEnvironments(String rancherConfigId, String url) {

		final RancherConfiguration rancher = configurationService.getRancherConfiguration(rancherConfigId);
		if (!StringUtils.isEmpty(url))
			rancher.setUrl(url);
		return this.getRancherEnvironments(rancher);
	}

	@Override
	public List<String> getRancherEnvironments(String rancherConfigId) {
		return this.getRancherEnvironments(rancherConfigId, null);
	}

	@Override
	public String createRancherEnvironment(String rancherConfigId, String name) {
		final RancherConfiguration rancher = configurationService.getRancherConfiguration(rancherConfigId);
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), "env", CREATE_STR, name);
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			return builder.toString();

		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
		return null;
	}

	@Override
	public String deployRancherEnvironment(String configId, String environment, Map<String, Integer> services,
			String url, String projectName) {

		final RancherConfiguration rancher = configurationService.getRancherConfiguration(configId);
		// TODO let the user select host
		final String worker2deploy = getRancherHosts(rancher, environment).get(0);
		final String dockerTemplate = createOnesaitDockerTemplate4Rancher(services.keySet(), worker2deploy);

		try {
			createTempYmlFile(DOCKER_COMPOSE, dockerTemplate);
			rancherStacks(rancher, environment, url, projectName, false);
			deleteTempYmlFile(DOCKER_COMPOSE);
		} catch (final IOException e) {
			log.error("Could not create tmp file");
		}
		return dockerTemplate;

	}

	@Override
	public String deployRancherEnvironment(String configId, String environment, Map<String, Integer> services) {
		return this.deployRancherEnvironment(configId, environment, services, null, null);
	}

	@Override
	public String deployMicroservice(RancherConfiguration config, String environment, String name,
			String dockerImageURL, String onesaitServerName, String contextPath, int port) {
		final String worker2deploy = getRancherHosts(config, environment).get(0);
		final String dockerTemplate = createMicroserviceDockerTemplate4Rancher(onesaitServerName, onesaitServerName,
				worker2deploy, dockerImageURL, name, contextPath, port);

		try {
			createTempYmlFile(DOCKER_COMPOSE, dockerTemplate);
			rancherStacks(config, environment, config.getUrl(), DEFAULT_STACK_NAME_MICROSERVICE, true);
			deleteTempYmlFile(DOCKER_COMPOSE);

		} catch (final IOException e) {
			log.error("Could not create tmp file");
		}
		return dockerTemplate;
	}

	@SuppressWarnings("unchecked")
	private String createMicroserviceDockerTemplate4Rancher(String serverName, String onesaitServerName,
			String worker2deploy, String dockerImageURL, String serviceName, String contextPath, int port) {
		final Configuration configuration = configurationService.getConfiguration(Configuration.Type.DOCKER,
				"microservice");

		final HashMap<String, Object> scopes = new HashMap<>();
		scopes.put("SERVICE_NAME", serviceName.toLowerCase());
		scopes.put("IMAGE_URL", dockerImageURL);
		scopes.put("ONESAIT_SERVER_NAME", onesaitServerName);
		scopes.put("SERVER_NAME", serverName);
		scopes.put("WORKER2DEPLOY", worker2deploy);
		scopes.put("CONTEXT_PATH", contextPath);
		scopes.put("PORT", port);

		final Writer writer = new StringWriter();
		final MustacheFactory mf = new DefaultMustacheFactory();
		final Mustache mustache = mf.compile(new StringReader(configuration.getYmlConfig()), DOCKER_COMPOSE);
		mustache.execute(writer, scopes);

		final Yaml yaml = new Yaml();
		final Map<String, Map<?, ?>> yamlMap = (Map<String, Map<?, ?>>) yaml.load(writer.toString());

		return yaml.dump(yamlMap);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String createOnesaitDockerTemplate4Rancher(Set<String> services, String worker2deploy) {
		final Configuration configuration = configurationService.getConfiguration(Configuration.Type.DOCKER, RANCHER);

		final HashMap<String, Object> scopes = new HashMap<>();
		scopes.put("DOMAIN_NAME", DOMAIN_NAME);
		scopes.put("IMAGE_TAG", IMAGE_TAG);
		scopes.put("PROJECTNAME", PROJECT_NAME);
		scopes.put("SERVER_NAME", SERVER_NAME);
		scopes.put("WORKER2DEPLOY", worker2deploy);

		final Writer writer = new StringWriter();
		final MustacheFactory mf = new DefaultMustacheFactory();
		final Mustache mustache = mf.compile(new StringReader(configuration.getYmlConfig()), DOCKER_COMPOSE);
		mustache.execute(writer, scopes);

		final Yaml yaml = new Yaml();
		final Map<String, Map> yamlMap = (Map<String, Map>) yaml.load(writer.toString());
		final List<String> mandatoryServices = Arrays.asList(MANDATORY_SERVICES);
		((Map<String, Map>) yamlMap.get(SERVICES_STR)).keySet()
				.removeIf(s -> (!services.contains(s) && !mandatoryServices.contains(s)));
		((Map<String, Map>) yamlMap.get(SERVICES_STR)).entrySet().forEach(e -> {
			final ArrayList<String> links = (ArrayList<String>) e.getValue().get("links");
			if (links != null)
				links.removeIf(
						s -> !services.contains(s.split(":")[0]) && !mandatoryServices.contains(s.split(":")[0]));

		});
		return yaml.dump(yamlMap);
	}

	@SuppressWarnings("unchecked")
	private String createOnesaitRancherTemplate(String rancherTemplate, Map<String, Integer> services) {

		for (final String s : Arrays.asList(MANDATORY_SERVICES)) {
			services.put(s, 1);
		}

		final Yaml yaml = new Yaml();
		@SuppressWarnings("rawtypes")
		final Map<String, Map> yamlMap = (Map<String, Map>) yaml.load(rancherTemplate);
		yamlMap.remove(SERVICES_STR);
		final Map<String, Map<String, Object>> newServicesMap = new HashMap<>();
		services.entrySet().forEach(e -> {
			final Map<String, Object> properties = new HashMap<>();
			properties.put("scale", e.getValue());
			properties.put("start_on_create", false);
			newServicesMap.put(e.getKey(), properties);
		});
		yamlMap.put(SERVICES_STR, newServicesMap);
		return yaml.dump(yamlMap);

	}

	private String createMicroserviceRancherTemplate(String name) {

		final Yaml yaml = new Yaml();
		final Map<String, Object> yamlMap = new HashMap<>();
		yamlMap.put("version", "2");
		final Map<String, Object> properties = new HashMap<>();
		properties.put("scale", 1);
		properties.put("start_on_create", true);
		final Map<String, Object> serviceMap = new HashMap<>();
		serviceMap.put(name, properties);
		yamlMap.put(SERVICES_STR, serviceMap);
		return yaml.dump(yamlMap);

	}

	private void rancherStacks(RancherConfiguration rancher, String environment, String url, String projectName,
			boolean startOnCreate) {
		final ProcessBuilder pb;
		final String stackName = !StringUtils.isEmpty(projectName) ? projectName : DEFAULT_STACK_NAME;
		if (StringUtils.isEmpty(url))
			pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY, rancher.getAccessKey(), SECRET_KEY,
					rancher.getSecretKey(), ENV_STR, environment, "stacks", CREATE_STR, stackName, "--docker-compose",
					TMP_PATH + DOCKER_COMPOSE, "--start=" + startOnCreate);
		else
			pb = new ProcessBuilder(RANCHER, URL_STR, url, ACCESS_KEY, rancher.getAccessKey(), SECRET_KEY,
					rancher.getSecretKey(), ENV_STR, environment, "stacks", CREATE_STR, stackName, "--docker-compose",
					TMP_PATH + DOCKER_COMPOSE, "--start=" + startOnCreate);

		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			builder.toString();
		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}

	}

	private List<String> getRancherHosts(RancherConfiguration rancher, String environment) {
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), ENV_STR, environment, "host", "ls",
				FORMAT_STR, "{{.Labels}}");
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			return Arrays.asList(builder.toString().split("\n"));

		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
		return null;
	}

	private void createTempYmlFile(String filename, String output) throws IOException {
		try (final FileWriter writer = new FileWriter(TMP_PATH + filename)) {
			writer.write(output);
			writer.flush();
		} catch (final Exception e) {
			log.error("Could not open FileWriter: ", e);
		}

	}

	private boolean deleteTempYmlFile(String filename) {
		final File file = new File(TMP_PATH + filename);
		return file.delete();
	}

	private void deleteTmpDir(String dir) throws IOException {
		FileUtils.deleteDirectory(new File(TMP_PATH + dir));
	}

	@Override
	public List<String> getRancherWorkers(RancherConfiguration rancher, String env) {
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), ENV_STR, env, "host", "ls", FORMAT_STR,
				"{{.Labels}}");
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			return Arrays.asList(builder.toString().split("\n"));

		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
		return null;
	}

	@Override
	public String deployMicroservice(Microservice microservice, String environment, String worker,
			String onesaitServerName) {
		try {
			final String dockerTemplate = createMicroserviceDockerTemplate4Rancher(onesaitServerName, onesaitServerName,
					worker, microservice.getDockerImage(), microservice.getIdentification(),
					microservice.getContextPath(), microservice.getPort().intValue());
			final String randomUUID = UUID.randomUUID().toString();
			final String stackName = microservice.getIdentification() + "-" + randomUUID.substring(0, 4);
			createTempYmlFile(DOCKER_COMPOSE, dockerTemplate);
			rancherStacks(microservice.getRancherConfiguration(), environment,
					microservice.getRancherConfiguration().getUrl(), stackName, true);
			deleteTempYmlFile(DOCKER_COMPOSE);
			microservice.setRancherEnv(environment);
			microservice.setRancherStack(stackName);

		} catch (final Exception e) {
			log.error("Could not deploy microservice", e);
		}
		return microservice.getRancherConfiguration().getUrl();
	}

	@Override
	public Map<String, String> getDeployedEnvVariables(Microservice microservice) {
		Map<String, String> env = new HashMap<>();
		try {

			exportStackConfig(microservice);
			env = getEnvMap(microservice);
			deleteTmpDir(microservice.getRancherStack());

		} catch (final Exception e) {
			log.error("Could not get Environment variables of deployed microservice", e);
		}
		return env;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getEnvMap(Microservice microservice) throws FileNotFoundException {

		final Yaml yaml = new Yaml();

		final Map<String, Map<String, Object>> yamlMap = (Map<String, Map<String, Object>>) yaml
				.load(new FileInputStream(
						new File(TMP_PATH.concat(microservice.getRancherStack().concat("/").concat(DOCKER_COMPOSE)))));
		final Map<String, Object> service = (Map<String, Object>) yamlMap.get(SERVICES_STR)
				.get(microservice.getIdentification().toLowerCase());
		return (Map<String, String>) service.get("environment");

	}

	@Override
	public String upgradeMicroservice(Microservice microservice, Map<String, String> mapEnv) {

		try {

			exportStackConfig(microservice);
			final String template = createYmlFromExport(microservice, mapEnv);
			createTempYmlFile(DOCKER_COMPOSE, template);
			upgradeServiceStack(microservice);
			deleteTempYmlFile(DOCKER_COMPOSE);
			deleteTmpDir(microservice.getRancherStack());

		} catch (final Exception e) {
			log.error("Could not upgrade microservice", e);
		}

		return microservice.getRancherConfiguration().getUrl();
	}

	private void upgradeServiceStack(Microservice microservice) {
		final RancherConfiguration rancher = microservice.getRancherConfiguration();
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), ENV_STR, microservice.getRancherEnv(), "up",
				"--force-upgrade", "-s", microservice.getRancherStack(), "-d", "-f", TMP_PATH + DOCKER_COMPOSE, "-c",
				"-u");
		pb.redirectErrorStream(true);
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			builder.toString();
		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
	}

	private void exportStackConfig(Microservice microservice) {
		final RancherConfiguration rancher = microservice.getRancherConfiguration();
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), ENV_STR, microservice.getRancherEnv(),
				"export", microservice.getRancherStack());
		pb.directory(new File(TMP_PATH));
		pb.redirectErrorStream(true);
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			builder.toString();
		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
		}
	}

	@SuppressWarnings("unchecked")
	private String createYmlFromExport(Microservice microservice, Map<String, String> mapEnv)
			throws FileNotFoundException {

		final Yaml yaml = new Yaml();

		final Map<String, Map<String, Object>> yamlMap = (Map<String, Map<String, Object>>) yaml
				.load(new FileInputStream(
						new File(TMP_PATH.concat(microservice.getRancherStack().concat("/").concat(DOCKER_COMPOSE)))));
		final Map<String, Object> service = (Map<String, Object>) yamlMap.get(SERVICES_STR)
				.get(microservice.getIdentification().toLowerCase());
		service.remove("image");
		service.put("image", microservice.getDockerImage());
		final Map<String, String> environment = (Map<String, String>) service.get("environment");
		final Map<String, String> mx = Stream.of(environment, mapEnv).map(Map::entrySet).flatMap(Collection::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (env1, env2) -> env2));
		service.remove("environment");
		service.put("environment", mx);
		yamlMap.get(SERVICES_STR).remove(microservice.getIdentification());
		yamlMap.get(SERVICES_STR).put(microservice.getIdentification(), service);
		return yaml.dump(yamlMap);

	}

	@Override
	public String stopStack(RancherConfiguration rancher, String stack, String environment) {
		final ProcessBuilder pb = new ProcessBuilder(RANCHER, URL_STR, rancher.getUrl(), ACCESS_KEY,
				rancher.getAccessKey(), SECRET_KEY, rancher.getSecretKey(), ENV_STR, environment, "stop", stack);
		pb.redirectErrorStream(true);
		try {
			final Process p = pb.start();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final StringBuilder builder = new StringBuilder();
			String line = null;
			p.waitFor();
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty(LINE_SEPARATOR));
			}
			return builder.toString();
		} catch (IOException | InterruptedException e) {
			log.error(COULD_NOT_EXECUTE_COMMAND + pb.command());
			return null;
		}
	}

}
