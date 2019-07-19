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
package com.minsait.onesait.platform.controlpanel.rest.management.microservices;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cdancy.jenkins.rest.domain.job.JobInfo;
import com.minsait.onesait.platform.config.components.RancherConfiguration;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Microservice.CaaS;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;
import com.minsait.onesait.platform.controlpanel.rest.deployment.CaasPlatform;
import com.minsait.onesait.platform.controlpanel.rest.management.microservices.model.GitlabProject;
import com.minsait.onesait.platform.controlpanel.rest.management.microservices.model.MicroserviceDeployment;
import com.minsait.onesait.platform.controlpanel.rest.management.model.ErrorValidationResponse;
import com.minsait.onesait.platform.controlpanel.services.jenkins.JenkinsException;
import com.minsait.onesait.platform.controlpanel.services.jenkins.JenkinsService;
import com.minsait.onesait.platform.controlpanel.services.jenkins.model.JenkinsBuild;
import com.minsait.onesait.platform.controlpanel.services.jenkins.model.JenkinsPipeline;
import com.minsait.onesait.platform.controlpanel.services.project.GitlabRestService;
import com.minsait.onesait.platform.controlpanel.services.project.RancherService;
import com.minsait.onesait.platform.controlpanel.services.project.exceptions.GitlabException;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Api(value = "Microservices", tags = { "Microservices REST API" })
@RestController
@ApiResponses({ @ApiResponse(code = 400, message = "Bad request"),
		@ApiResponse(code = 500, message = "Internal server error"), @ApiResponse(code = 403, message = "Forbidden") })
@Slf4j
@PreAuthorize("hasRole('ADMINISTRATOR') OR hasRole('DEVELOPER')")
@RequestMapping("api/microservices")
public class MicroservicesRestController {

	@Autowired
	private GitlabRestService gitlabService;
	@Autowired
	private JenkinsService jenkinsService;
	@Autowired
	private AppWebUtils utils;
	@Autowired
	private RancherService rancherService;
	@Autowired
	private ConfigurationService configurationService;

	@ApiOperation("Creates Microservice")
	@PostMapping
	public ResponseEntity<?> create(@ApiParam(value = "Microservice Name", required = true) String microserviceName,
			@ApiParam(value = "Template Type", required = true) TemplateType template) {

		final Microservice microservice = new Microservice();
		microservice.setIdentification(microserviceName);
		microservice.setTemplateType(template);
		microservice.setCaas(CaaS.RANCHER);

		// TO-DO Decide how to implement via REST
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ApiOperation("Create Jenkins Pipeline")
	@PostMapping("/jenkins")
	@ApiResponse(code = 200, response = String.class, message = "The URL of the Jenkins Pipeline")
	public ResponseEntity<?> createJenkinsPipeline(@ApiParam("Pipeline info") @Valid JenkinsPipeline pipeline,
			@ApiParam("XML Pipeline File") MultipartFile file, Errors errors) {
		if (errors.hasErrors())
			return ErrorValidationResponse.generateValidationErrorResponse(errors);
		if (!file.getContentType().equalsIgnoreCase("text/xml"))
			return new ResponseEntity<>("Invalid Config XML file, must be text/xml", HttpStatus.BAD_REQUEST);

		try (InputStream is = file.getInputStream()) {
			jenkinsService.createJob(pipeline.getJenkinsUrl(), pipeline.getUsername(), pipeline.getToken(),
					pipeline.getJobName(), null, IOUtils.toString(is));
			final JobInfo job = jenkinsService.getJobInfo(pipeline.getJenkinsUrl(), pipeline.getUsername(),
					pipeline.getToken(), pipeline.getJobName(), null);
			return new ResponseEntity<>(job.url(), HttpStatus.OK);
		} catch (final JenkinsException e) {
			log.error("Could not create pipeline {}", e.getMessage());
			return new ResponseEntity<>("Could not create Pipeline " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (final IOException e1) {
			return new ResponseEntity<>("Invalid Config XML file content", HttpStatus.BAD_REQUEST);
		}

	}

	@ApiOperation("Gets Jenkins Parameters")
	@PostMapping("/jenkins/parameters")
	@ApiResponse(code = 200, response = String.class, message = "The URL of the Jenkins Pipeline")
	public ResponseEntity<?> getJenkinsParameters(@ApiParam("Pipeline info") @Valid JenkinsPipeline pipeline,
			Errors errors) {
		if (errors.hasErrors())
			return ErrorValidationResponse.generateValidationErrorResponse(errors);

		try {
			final Map<String, String> parameters = jenkinsService.getParametersFromJob(pipeline.getJenkinsUrl(),
					pipeline.getUsername(), pipeline.getToken(), pipeline.getJobName());
			return new ResponseEntity<>(parameters, HttpStatus.OK);
		} catch (final JenkinsException e) {
			log.error("Could not create pipeline {}", e.getMessage());
			return new ResponseEntity<>("Could not create Pipeline " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation("Build Jenkins Pipeline")
	@PostMapping("/jenkins/build")
	@ApiResponse(code = 200, response = String.class, message = "The URL of the Jenkins Pipeline")
	public ResponseEntity<?> buildJenkinsPipeline(
			@ApiParam(value = "Pipeline Build Info", required = true) @Valid @RequestBody JenkinsBuild buildInfo) {

		try {
			final JobInfo job = jenkinsService.getJobInfo(buildInfo.getPipeline().getJenkinsUrl(),
					buildInfo.getPipeline().getUsername(), buildInfo.getPipeline().getToken(),
					buildInfo.getPipeline().getJobName(), null);
			if (job != null) {

				final Map<String, List<String>> params = buildInfo.getParameters().entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(), e -> Arrays.asList(e.getValue())));
				jenkinsService.buildWithParameters(buildInfo.getPipeline().getJenkinsUrl(),
						buildInfo.getPipeline().getUsername(), buildInfo.getPipeline().getToken(),
						buildInfo.getPipeline().getJobName(), null, params);
			}
			return new ResponseEntity<>("Pipeline is being executed, more info at " + job.url(), HttpStatus.OK);
		} catch (final JenkinsException e) {
			log.error("Could not build pipeline {}", e.getMessage());
			return new ResponseEntity<>("Could not create Pipeline " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation("Create Gitlab Project")
	@PostMapping("/gitlab")
	@ApiResponse(code = 200, response = String.class, message = "The URL of the Gitlab project")
	public ResponseEntity<?> createGitlabProject(
			@ApiParam("Gitlab Parameters") @Valid @RequestBody GitlabProject gitlabProject, Errors errors) {
		if (errors.hasErrors())
			return ErrorValidationResponse.generateValidationErrorResponse(errors);

		try {
			final String url = gitlabService.createGitlabProject(gitlabProject.getProjectName(), gitlabProject.getUrl(),
					gitlabProject.getPrivateToken(), gitlabProject.isScaffolding(), null, null, null);
			return new ResponseEntity<>(url, HttpStatus.OK);
		} catch (final GitlabException e) {
			log.error("Could not create project {}", e.getMessage());
			return new ResponseEntity<>("Could not create GitlabProject " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation("Deploys an existing Microservice")
	@PostMapping("/deploy")
	public ResponseEntity<?> deploy(@ApiParam("Deploy Configuration") @RequestBody @Valid MicroserviceDeployment deploy,
			Errors errors) {
		if (errors.hasErrors())
			return ErrorValidationResponse.generateValidationErrorResponse(errors);

		String url = deploy.getDeploymentUrl();

		if (deploy.getCaasPlatform().equals(CaasPlatform.RANCHER)) {

			if (StringUtils.isEmpty(deploy.getDeploymentUrl())) {
				RancherConfiguration config;

				try {
					config = configurationService.getRancherConfiguration("", "default");
					if (config == null)
						throw new RuntimeException();
				} catch (final RuntimeException e) {
					log.error("There's no configuration for Rancher");
					return new ResponseEntity<>(
							"Deployment URL is empty and there's no Rancher configuration available",
							HttpStatus.BAD_REQUEST);
				}
				try {
					rancherService.deployMicroservice(config, deploy.getEnvironment(), deploy.getMicroserviceName(),
							deploy.getDockerImageURL(), deploy.getOnesaitServerName(), deploy.getContextPath(),
							deploy.getPort());
					url = config.getUrl();
				} catch (final Exception e) {
					log.error("Could not deploy microservice", e);
					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}

			} else {
				try {
					rancherService.deployMicroservice(
							RancherConfiguration.builder().url(deploy.getDeploymentUrl())
									.accessKey(deploy.getAccessKey()).secretKey(deploy.getSecretKey()).build(),
							deploy.getEnvironment(), deploy.getMicroserviceName(), deploy.getDockerImageURL(),
							deploy.getOnesaitServerName(), deploy.getContextPath(), deploy.getPort());
				} catch (final Exception e) {
					return new ResponseEntity<>("Could not deploy on Rancher environment " + deploy.getEnvironment()
							+ " reason: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}

		} else {
			return new ResponseEntity<>("Only Rancher is supported ", HttpStatus.NOT_IMPLEMENTED);
		}

		return new ResponseEntity<>("Environment successfully deployed in " + url, HttpStatus.OK);
	}

	@ApiOperation("Creates a Microservice from a Spring Boot template. "
			+ "This operation executes the full flow: publish project to Gitlab repo,"
			+ " create Jenkins Pipeline, build pipeline with parameters (if provided), and finally deploy it to Rancher."
			+ "Only use this operation for testing purposes.")
	@PreAuthorize("hasRole(ADMINISTRATOR)")
	@PostMapping("/full/pipeline")
	public ResponseEntity<?> executeFullFlow() {
		final String url = new String();

		return new ResponseEntity<>("Environment successfully deployed in " + url, HttpStatus.OK);

	}

}
