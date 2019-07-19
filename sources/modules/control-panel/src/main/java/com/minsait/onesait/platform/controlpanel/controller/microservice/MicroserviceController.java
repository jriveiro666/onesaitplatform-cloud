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
package com.minsait.onesait.platform.controlpanel.controller.microservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.config.model.Microservice;
import com.minsait.onesait.platform.config.model.Microservice.CaaS;
import com.minsait.onesait.platform.config.model.Microservice.TemplateType;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.services.configuration.ConfigurationService;
import com.minsait.onesait.platform.config.services.exceptions.MicroserviceException;
import com.minsait.onesait.platform.config.services.microservice.MicroserviceService;
import com.minsait.onesait.platform.config.services.microservice.dto.DeployParameters;
import com.minsait.onesait.platform.config.services.microservice.dto.JenkinsParameter;
import com.minsait.onesait.platform.config.services.microservice.dto.MicroserviceDTO;
import com.minsait.onesait.platform.config.services.ontology.OntologyService;
import com.minsait.onesait.platform.config.services.project.ProjectService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.services.gateway.CloudGatewayService;
import com.minsait.onesait.platform.controlpanel.services.microservice.MicroserviceBusinessService;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("microservices")
@PreAuthorize("hasRole('ADMINISTRATOR') OR hasRole('DEVELOPER')")
@Slf4j
public class MicroserviceController {

	@Autowired
	private MicroserviceService microserviceService;
	@Autowired
	private MicroserviceBusinessService microserviceBusinessService;
	@Autowired
	private AppWebUtils utils;
	@Autowired
	private UserService userService;
	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private OntologyService ontologyService;
	@Autowired
	private ProjectService projectService;

	@Autowired
	private CloudGatewayService cloudGatewayService;

	@GetMapping("list")
	public String list(Model model) {
		if (utils.isAdministrator())
			model.addAttribute("microservices", microserviceService.getAllMicroservices());
		else
			model.addAttribute("microservices",
					microserviceService.getMicroservices(userService.getUser(utils.getUserId())));

		return "microservice/list";
	}

	@GetMapping("jenkins/parameters/{id}")
	public @ResponseBody List<JenkinsParameter> parameters(@PathVariable("id") String id) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice != null
				&& microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId()))) {
			return microserviceBusinessService.getJenkinsJobParameters(microservice);
		}
		return new ArrayList<>();
	}

	@GetMapping("data")
	public @ResponseBody List<MicroserviceDTO> listData(Model model) {
		List<Microservice> microservices = new ArrayList<>();
		if (utils.isAdministrator())
			microservices = microserviceService.getAllMicroservices();
		else
			microservices = microserviceService.getMicroservices(userService.getUser(utils.getUserId()));
		// TO-DO when openshift -> isDeployed changes logic
		return microservices.stream()
				.map(m -> MicroserviceDTO.builder().caasUrl(m.getRancherConfiguration().getUrl())
						.caas(m.getCaas().name()).gitlab(m.getGitlabRepository()).id(m.getId())
						.name(m.getIdentification()).isDeployed(m.getRancherStack() != null)
						.deploymentUrl(cloudGatewayService.getDeployedMicroserviceURL(m)).jenkins(m.getJobUrl())
						.owner(m.getUser().getUserId())
						.lastBuild(m.getJenkinsQueueId() == null ? null : String.valueOf(m.getJenkinsQueueId()))
						.build())
				.collect(Collectors.toList());
	}

	@GetMapping("create")
	public String create(Model model) {
		model.addAttribute("microservice", new Microservice());
		model.addAttribute("caas", CaaS.values());
		model.addAttribute("templates", TemplateType.values());
		model.addAttribute("defaultGitlab", configurationService.getDefautlGitlabConfiguration() != null);
		model.addAttribute("defaultCaaS", configurationService.getDefaultRancherConfiguration() != null);
		model.addAttribute("defaultJenkins", configurationService.getDefaultJenkinsConfiguration() != null);
		final Set<Ontology> ontologies = new LinkedHashSet<>(ontologyService.getAllOntologies(utils.getUserId()));
		ontologies.addAll(projectService.getResourcesForUserOfType(utils.getUserId(), Ontology.class));
		model.addAttribute("ontologies", ontologies);
		return "microservice/create";
	}

	@GetMapping("update/{id}")
	public String update(Model model, @PathVariable("id") String id) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return "404";
		model.addAttribute("microservice", microservice);
		return "microservice/create";

	}

	@PutMapping("update/{id}")
	public String update(Model model, Microservice microservice, @PathVariable("id") String id) {
		final Microservice serviceDb = microserviceService.getById(id);
		if (serviceDb == null)
			return "404";
		if (!microserviceService.hasUserPermission(serviceDb, userService.getUser(utils.getUserId())))
			return "403";
		microservice.setId(serviceDb.getId());
		microserviceService.update(microservice);
		return "redirect:/microservices/list";
	}

	@PostMapping("create")
	public String createPost(Model model, @Valid Microservice microservice,
			@RequestParam(value = "createGitlab", required = true, defaultValue = "true") Boolean createGitlab,
			@RequestParam(value = "defaultGitlab", required = false, defaultValue = "true") Boolean defaultGitlab,
			@RequestParam(value = "defaultJenkins", required = false, defaultValue = "true") Boolean defaultJenkins,
			@RequestParam(value = "defaultCaaS", required = false, defaultValue = "true") Boolean defaultCaaS,
			@RequestParam(value = "sourcesPath", required = false, defaultValue = "sources") String sources,
			@RequestParam(value = "dockerPath", required = false, defaultValue = "sources/docker") String docker,
			@RequestParam(value = "ontology", required = false) String ontology, RedirectAttributes ra) {
		try {
			microservice.setUser(userService.getUser(utils.getUserId()));
			microservice = microserviceBusinessService.createMicroservice(microservice, createGitlab, defaultGitlab,
					defaultJenkins, defaultCaaS, sources, docker, null, ontology);

		} catch (final Exception e) {
			log.error("Could not create Microservice", e);
			utils.addRedirectException(e, ra);
		}

		return "redirect:/microservices/list";
	}

	@PostMapping(value = "jenkins/build/{id}", consumes = "application/json")
	public ResponseEntity<?> buildWithParameters(@PathVariable("id") String id,
			@RequestBody List<JenkinsParameter> parameters) {

		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		try {
			final int i = microserviceBusinessService.buildJenkins(microservice, parameters);
			return new ResponseEntity<>(i, HttpStatus.OK);
		} catch (final Exception e) {
			log.error("Could not complete jenkins build", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("jenkins/completed/{id}")
	public ResponseEntity<?> buildWithParameters(@PathVariable("id") String id) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);

		try {
			if (microserviceBusinessService.hasPipelineFinished(microservice))
				return new ResponseEntity<>("y", HttpStatus.OK);
			else
				return new ResponseEntity<>("n", HttpStatus.OK);
		} catch (final Exception e) {
			log.error("Could not complete jenkins build", e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("deploy/{id}/parameters")
	public String deployParameters(Model model, @PathVariable("id") String id,
			@RequestParam(value = "upgrade", required = false, defaultValue = "false") Boolean upgrade,
			@RequestParam(value = "hosts", required = false, defaultValue = "false") Boolean hosts,
			@RequestParam(value = "environment", required = false, defaultValue = "") String environment) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return "404";
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return "403";
		if (microservice.getCaas().equals(CaaS.RANCHER)) {

			if (upgrade) {
				model.addAttribute("currentImageUrl", microservice.getDockerImage());
				model.addAttribute("microserviceId", microservice.getId());
				model.addAttribute("env", microserviceBusinessService.getEnvMap(microservice));
				return "microservice/fragments/upgrade-modal";
			} else if (!hosts) {
				final DeployParameters parameters = microserviceBusinessService.getEnvironments(microservice);
				model.addAttribute("deploymentParameters", parameters);
				return "microservice/fragments/deployment-modal";
			} else {
				final DeployParameters parameters = microserviceBusinessService.getHosts(microservice, environment);
				model.addAttribute("deploymentParameters", parameters);
				return "microservice/fragments/deployment-modal";
			}

		} else
			throw new MicroserviceException("Only Rancher is supported as CaaS");

	}

	@PostMapping(value = "deploy/{id}")
	public ResponseEntity<?> deploy(@PathVariable("id") String id, @RequestParam("environment") String environment,
			@RequestParam("worker") String worker, @RequestParam("onesaitServerUrl") String onesaitServerUrl,
			@RequestParam("dockerImageUrl") String dockerImageUrl) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		if (microservice.getCaas().equals(CaaS.RANCHER)) {
			final String url = microserviceBusinessService.deployMicroservice(microservice, environment, worker,
					onesaitServerUrl, dockerImageUrl);
			return new ResponseEntity<>(url, HttpStatus.OK);
		} else
			return new ResponseEntity<>("Not supported", HttpStatus.BAD_REQUEST);

	}

	@PostMapping(value = "upgrade/{id}")
	public ResponseEntity<?> upgrade(@PathVariable("id") String id,
			@RequestParam("dockerImageUrl") String dockerImageUrl, @RequestParam String env) throws IOException {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		if (microservice.getCaas().equals(CaaS.RANCHER)) {
			Map<String, String> mapEnv = new HashMap<>();
			if (!StringUtils.isEmpty(env)) {
				final ObjectMapper mapper = new ObjectMapper();
				mapEnv = mapper.readValue(env, new TypeReference<Map<String, String>>() {
				});
			}
			final String url = microserviceBusinessService.upgradeMicroservice(microservice, dockerImageUrl, mapEnv);

			return new ResponseEntity<>(url, HttpStatus.OK);
		} else
			return new ResponseEntity<>("Not supported", HttpStatus.BAD_REQUEST);

	}

	@PostMapping("stop/{id}")
	public ResponseEntity<?> stop(@PathVariable("id") String id) {
		final Microservice microservice = microserviceService.getById(id);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);

		try {
			microserviceBusinessService.stopMicroservice(microservice);
		} catch (final Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("{id}")
	public ResponseEntity<String> delete(@PathVariable("id") String id) {
		final Microservice microservice = microserviceService.getById(id);
		if (!microserviceService.hasUserPermission(microservice, userService.getUser(utils.getUserId())))
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		if (microservice == null)
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		microserviceBusinessService.deleteMicroservice(microservice);
		return new ResponseEntity<>("OK", HttpStatus.OK);
	}

}
