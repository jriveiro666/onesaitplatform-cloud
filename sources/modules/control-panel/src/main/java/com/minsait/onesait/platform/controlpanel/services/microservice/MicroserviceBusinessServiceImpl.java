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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cdancy.jenkins.rest.domain.job.BuildInfo;
import com.cdancy.jenkins.rest.domain.job.JobInfo;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.minsait.onesait.platform.config.components.GitlabConfiguration;
import com.minsait.onesait.platform.config.components.JenkinsConfiguration;
import com.minsait.onesait.platform.config.components.RancherConfiguration;
import com.minsait.onesait.platform.config.model.DigitalTwinDevice;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Microservice.CaaS;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;
import com.minsait.onesait.platform.config.services.exceptions.MicroserviceException;
import com.minsait.onesait.platform.config.services.microservice.MicroserviceService;
import com.minsait.onesait.platform.config.services.microservice.dto.DeployParameters;
import com.minsait.onesait.platform.config.services.microservice.dto.JenkinsParameter;
import com.minsait.onesait.platform.controlpanel.services.gateway.CloudGatewayService;
import com.minsait.onesait.platform.controlpanel.services.jenkins.JenkinsBuildWatcher;
import com.minsait.onesait.platform.controlpanel.services.jenkins.JenkinsService;
import com.minsait.onesait.platform.controlpanel.services.project.GitlabRestService;
import com.minsait.onesait.platform.controlpanel.services.project.RancherService;
import com.minsait.onesait.platform.controlpanel.services.project.exceptions.GitlabException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MicroserviceBusinessServiceImpl implements MicroserviceBusinessService {

	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private MicroserviceService microserviceService;
	@Autowired
	private CloudGatewayService gatewayService;
	@Autowired
	private GitlabRestService gitlabService;
	@Autowired
	private JenkinsService jenkinsService;
	@Autowired
	private RancherService rancherService;

	@Autowired
	private ApplicationContext appContext;
	@Autowired
	private TaskExecutor taskExecutor;

	private final static String DOCKER_USERNAMEVALUE = "DOCKER_USERNAMEVALUE";
	private final static String PRIVATE_REGISTRY = "PRIVATE_REGISTRY";
	private final static String MICROSERVICE_NAME = "MICROSERVICE_NAME";
	private final static String SOURCES_PATH = "SOURCES_PATH";
	private final static String GIT_REPOSITORY = "GIT_REPOSITORY";
	private final static String GIT_URL = "GIT_URL";
	private final static String DOCKER_PATH = "DOCKER_PATH";
	private final static String DOCKER_MODULETAGVALUE = "DOCKER_MODULETAGVALUE";
	private final static String ENV_CTXT_PATH = "CONTEXT_PATH";
	private final static String ENV_PORT = "PORT";
	private final static String DEFAULT_PORT = "30010";

	@Override
	public Microservice createMicroservice(Microservice microservice, boolean createGitlab, boolean defaultGitlab,
			boolean defaultJenkins, boolean defaultCaaS, String sources, String docker, File file, String ontology) {

		if (!validServiceName(microservice.getIdentification()))
			throw new MicroserviceException("Invalid name, it can only contain lower case letters - and _");

		if (StringUtils.isEmpty(microservice.getContextPath()))
			microservice.setContextPath("/" + microservice.getIdentification());
		if (microservice.getPort() == null)
			microservice.setPort(Integer.parseInt(DEFAULT_PORT));
		// set jenkins
		microservice.setJobName(microservice.getIdentification().concat("-pipeline"));
		if (defaultJenkins) {
			final JenkinsConfiguration jenkins = configurationService.getDefaultJenkinsConfiguration();
			if (jenkins == null)
				throw new MicroserviceException("No default Configuration for Jenkins found");
			microservice.setJenkinsConfiguration(jenkins);

		} else {
			if (microservice.getJenkinsConfiguration() == null
					|| StringUtils.isEmpty(microservice.getJenkinsConfiguration().getJenkinsUrl())
					|| StringUtils.isEmpty(microservice.getJenkinsConfiguration().getToken()))
				throw new MicroserviceException("Jenkins configuration parameters are empty");
		}
		// set caas (i.e. Rancher at the moment)

		if (defaultCaaS) {
			final RancherConfiguration rancher = configurationService.getDefaultRancherConfiguration();
			if (rancher == null)
				throw new MicroserviceException("No default Configuration for Rancher found");
			microservice.setRancherConfiguration(rancher);
		} else {
			if (microservice.getRancherConfiguration() == null
					|| StringUtils.isEmpty(microservice.getRancherConfiguration().getAccessKey())
					|| StringUtils.isEmpty(microservice.getRancherConfiguration().getSecretKey()))
				throw new MicroserviceException("Rancher configuration parameters are empty");
		}

		// set gitlab and create repository if needed
		if (createGitlab) {
			if (defaultGitlab) {
				final GitlabConfiguration gitlab = configurationService.getDefautlGitlabConfiguration();
				if (gitlab == null)
					throw new MicroserviceException("No default Configuration for Gitlab found");
				microservice.setGitlabConfiguration(gitlab);

			} else {
				if (microservice.getGitlabConfiguration() == null
						|| StringUtils.isEmpty(microservice.getGitlabConfiguration().getSite())
						|| StringUtils.isEmpty(microservice.getGitlabConfiguration().getPrivateToken()))
					throw new MicroserviceException("Gitlab configuration parameters are empty");
			}
		} else
			microservice.setGitlabConfiguration(null);

		// save entity

		if (createGitlab)
			microservice.setGitlabRepository(createGitlabRepository(microservice, file, ontology));
		microservice.setJenkinsXML(compileXMLTemplate(configurationService.getDefaultJenkinsXML(),
				microservice.getIdentification(), sources, docker, microservice.getGitlabRepository()));
		microservice.setJobUrl(createJenkinsPipeline(microservice));
		microservice.setActive(true);
		return microserviceService.create(microservice);
	}

	@Override
	public Microservice createMicroserviceFromDigitalTwin(DigitalTwinDevice device, File file,
			GitlabConfiguration configuration, String sources, String docker) {
		final Microservice microservice = new Microservice();
		microservice.setActive(true);
		microservice.setCaas(CaaS.RANCHER);
		microservice.setIdentification(device.getIdentification().toLowerCase());
		microservice.setContextPath(device.getContextPath());
		microservice.setPort(device.getPort());
		microservice.setTemplateType(TemplateType.DIGITAL_TWIN);
		microservice.setGitlabConfiguration(configuration);
		microservice.setUser(device.getUser());
		final boolean defaultGitlab = StringUtils.isEmpty(configuration.getPrivateToken())
				|| StringUtils.isEmpty(configuration.getSite());

		return createMicroservice(microservice, true, defaultGitlab, true, true, sources, docker, file, null);
	}

	@Override
	public String createJenkinsPipeline(Microservice microservice) {
		if (StringUtils.isEmpty(microservice.getJobName()) || StringUtils.isEmpty(microservice.getJenkinsXML())
				|| StringUtils.isEmpty(microservice.getJenkinsConfiguration().getJenkinsUrl())
				|| StringUtils.isEmpty(microservice.getJenkinsConfiguration().getToken()))
			throw new MicroserviceException("Jenkins configuration parameters are empty");
		try {
			jenkinsService.createJob(microservice.getJenkinsConfiguration().getJenkinsUrl(),
					microservice.getJenkinsConfiguration().getUsername(),
					microservice.getJenkinsConfiguration().getToken(), microservice.getJobName(), null,
					microservice.getJenkinsXML());
			final JobInfo job = jenkinsService.getJobInfo(microservice.getJenkinsConfiguration().getJenkinsUrl(),
					microservice.getJenkinsConfiguration().getUsername(),
					microservice.getJenkinsConfiguration().getToken(), microservice.getJobName(), null);
			if (job != null)
				return job.url();
			else
				throw new MicroserviceException(
						"Could not create jenkins pipeline, review jenkins configuration parameters");
		} catch (final Exception e) {
			log.error("Could not create jenkins pipeline", e);
			throw new MicroserviceException("Could not create jenkins pipeline " + e);
		}

	}

	@Override
	public String createGitlabRepository(Microservice microservice, File file, String ontology) {
		String gitlabUrl;
		try {
			gitlabUrl = gitlabService.createGitlabProject(microservice.getIdentification(),
					microservice.getGitlabConfiguration().getSite(),
					microservice.getGitlabConfiguration().getPrivateToken(), true, file, microservice.getTemplateType(),
					ontology);
		} catch (final GitlabException e) {
			throw new MicroserviceException("Could not create Gitlab repository", e);
		}
		return gitlabUrl;
	}

	@Override
	public List<JenkinsParameter> getJenkinsJobParameters(Microservice microservice) {
		if (StringUtils.isEmpty(microservice.getJobName()))
			throw new MicroserviceException("This microservice doesn't have a jenkins pipeline associated");
		final Map<String, String> map = jenkinsService.getParametersFromJob(
				microservice.getJenkinsConfiguration().getJenkinsUrl(),
				microservice.getJenkinsConfiguration().getUsername(), microservice.getJenkinsConfiguration().getToken(),
				microservice.getJobName());
		return map.entrySet().stream()
				.map(e -> JenkinsParameter.builder().name(e.getKey())
						.value(e.getKey().equalsIgnoreCase(GIT_URL) ? microservice.getGitlabRepository() : e.getValue())
						.build())
				.collect(Collectors.toList());

	}

	@Override
	public int buildJenkins(Microservice microservice, List<JenkinsParameter> parameters) {
		final Map<String, List<String>> paramMap = parameters.stream()
				.collect(Collectors.toMap(p -> p.getName(), p -> Arrays.asList(p.getValue())));

		final int result = jenkinsService.buildWithParameters(microservice.getJenkinsConfiguration().getJenkinsUrl(),
				microservice.getJenkinsConfiguration().getUsername(), microservice.getJenkinsConfiguration().getToken(),
				microservice.getJobName(), null, paramMap);

		final JenkinsBuildWatcher buildWatcher = appContext.getBean(JenkinsBuildWatcher.class);
		buildWatcher.setJenkinsQueueId(result);
		buildWatcher.setMicroservice(microservice);
		buildWatcher.setClient(jenkinsService.getJenkinsClient(microservice.getJenkinsConfiguration().getJenkinsUrl(),
				microservice.getJenkinsConfiguration().getUsername(),
				microservice.getJenkinsConfiguration().getToken()));
		taskExecutor.execute(buildWatcher);

		final JenkinsParameter username = parameters.stream().filter(jp -> jp.getName().equals(DOCKER_USERNAMEVALUE))
				.findFirst().orElse(null);
		final JenkinsParameter registry = parameters.stream().filter(jp -> jp.getName().equals(PRIVATE_REGISTRY))
				.findFirst().orElse(null);
		final JenkinsParameter tag = parameters.stream().filter(jp -> jp.getName().equals(DOCKER_MODULETAGVALUE))
				.findFirst().orElse(null);
		if (registry != null && username != null && tag != null) {
			microservice.setJenkinsQueueId(result);
			microservice.setDockerImage(registry.getValue().concat("/").concat(username.getValue()).concat("/")
					.concat(microservice.getIdentification().toLowerCase()).concat(":").concat(tag.getValue()));
			microserviceService.save(microservice);
		}
		return result;

	}

	@Override
	public DeployParameters getEnvironments(Microservice microservice) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			final List<String> environments = rancherService
					.getRancherEnvironments(microservice.getRancherConfiguration());

			return DeployParameters.builder().id(microservice.getId()).environments(environments)
					.dockerImageUrl(microservice.getDockerImage() == null ? "" : microservice.getDockerImage())
					.onesaitServerUrl("").build();
		} else
			throw new MicroserviceException("No Rancher Configuration found for this microservice");
	}

	@Override
	public DeployParameters getHosts(Microservice microservice, String environment) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			final List<String> environments = rancherService
					.getRancherEnvironments(microservice.getRancherConfiguration());
			final List<String> workers = rancherService.getRancherWorkers(microservice.getRancherConfiguration(),
					environment);
			return DeployParameters.builder().id(microservice.getId()).workers(workers).environments(environments)
					.dockerImageUrl(microservice.getDockerImage() == null ? "" : microservice.getDockerImage())
					.onesaitServerUrl("").build();
		} else
			throw new MicroserviceException("No Rancher Configuration found for this microservice");
	}

	private String compileXMLTemplate(String xml, String microserviceName, String sources, String docker,
			String gitlabRepo) {
		final HashMap<String, Object> scopes = new HashMap<>();
		scopes.put(MICROSERVICE_NAME, microserviceName.toLowerCase());
		scopes.put(SOURCES_PATH, sources);
		scopes.put(DOCKER_PATH, docker);
		scopes.put(GIT_REPOSITORY, gitlabRepo);
		final Writer writer = new StringWriter();
		final MustacheFactory mf = new DefaultMustacheFactory();
		final Mustache mustache = mf.compile(new StringReader(xml), microserviceName.toLowerCase());
		mustache.execute(writer, scopes);
		return writer.toString();
	}

	@Override
	public String deployMicroservice(Microservice microservice, String environment, String worker,
			String onesaitServerUrl, String dockerImageUrl) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			if (!StringUtils.isEmpty(dockerImageUrl)) {
				microservice.setDockerImage(dockerImageUrl);
			} else if (StringUtils.isEmpty(microservice.getDockerImage()))
				throw new MicroserviceException("Empty parameter docker image url ");

			final String url = rancherService.deployMicroservice(microservice, environment, worker, onesaitServerUrl);
			microserviceService.save(microservice);
			gatewayService.publishMicroserviceToGateway(microservice);
			return url;

		} else
			throw new MicroserviceException("No Rancher Configuration found for this microservice");

	}

	@Override
	public void deleteMicroservice(Microservice microservice) {
		// if (!StringUtils.isEmpty(microservice.getJobUrl()))
		// jenkinsService.deleteJob(microservice.getJenkinsConfiguration().getJenkinsUrl(),
		// microservice.getJenkinsConfiguration().getUsername(),
		// microservice.getJenkinsConfiguration().getToken(), microservice.getJobName(),
		// null);
		if (!StringUtils.isEmpty(microservice.getRancherStack()))
			rancherService.stopStack(microservice.getRancherConfiguration(), microservice.getRancherStack(),
					microservice.getRancherEnv());
		microserviceService.delete(microservice);
	}

	@Override
	public String upgradeMicroservice(Microservice microservice, String dockerImageUrl, Map<String, String> mapEnv) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			if (!StringUtils.isEmpty(dockerImageUrl)) {
				microservice.setDockerImage(dockerImageUrl);
			} else if (StringUtils.isEmpty(microservice.getDockerImage()))
				throw new MicroserviceException("Empty parameter docker image url ");
			else if (StringUtils.isEmpty(microservice.getRancherStack()))
				throw new MicroserviceException("Service is not deployed ");

			final String url = rancherService.upgradeMicroservice(microservice, mapEnv);
			microserviceService.save(microservice);
			gatewayService.publishMicroserviceToGateway(microservice);
			return url;

		} else
			throw new MicroserviceException("No Rancher Configuration found for this microservice");
	}

	@Override
	public Map<String, String> getEnvMap(Microservice microservice) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			if (StringUtils.isEmpty(microservice.getRancherStack()))
				throw new MicroserviceException("Service is not deployed ");
			return rancherService.getDeployedEnvVariables(microservice);
		} else
			throw new MicroserviceException("No Rancher Configuration found for this microservice");
	}

	private boolean validServiceName(String identification) {
		final Pattern p = Pattern.compile("^[a-z_-]+$");
		final Matcher m = p.matcher(identification);
		return m.matches();
	}

	@Override
	public void stopMicroservice(Microservice microservice) {
		if (microservice.getCaas().equals(CaaS.RANCHER) && microservice.getRancherConfiguration() != null) {
			if (StringUtils.isEmpty(microservice.getRancherStack()))
				throw new MicroserviceException("Service is not deployed ");
			rancherService.stopStack(microservice.getRancherConfiguration(), microservice.getRancherStack(),
					microservice.getRancherEnv());
		} else
			throw new MicroserviceException(
					"No Rancher Configuration found for this microservice or it is not deployed yet");
	}

	@Override
	public boolean hasPipelineFinished(Microservice microservice) {
		boolean result = false;
		if (microservice.getJenkinsQueueId() != null) {
			try {
				final BuildInfo info = jenkinsService.buildInfo(microservice.getJenkinsConfiguration(),
						microservice.getJobName(), null, microservice.getJenkinsQueueId());
				if (info != null)
					result = info.result() != null;
			} catch (final Exception e) {
				log.warn("No item found on queue");
				result = true;
			}
		}
		if (result) {
			microservice.setJenkinsQueueId(null);
			microserviceService.save(microservice);
		}
		return result;
	}

}