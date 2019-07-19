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
package com.minsait.onesait.platform.controlpanel.controller.gis.layer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minsait.onesait.platform.commons.model.InsertResult;
import com.minsait.onesait.platform.config.model.ApiOperation;
import com.minsait.onesait.platform.config.model.Layer;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.services.gis.layer.LayerService;
import com.minsait.onesait.platform.config.services.ontology.OntologyService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;
import com.minsait.onesait.platform.router.service.app.model.NotificationModel;
import com.minsait.onesait.platform.router.service.app.model.OperationModel;
import com.minsait.onesait.platform.router.service.app.model.OperationModel.OperationType;
import com.minsait.onesait.platform.router.service.app.model.OperationModel.QueryType;
import com.minsait.onesait.platform.router.service.app.model.OperationResultModel;
import com.minsait.onesait.platform.router.service.app.service.RouterService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/layers")
@Slf4j
public class LayerController {

	private static final String RASTER = "raster";

	@Autowired
	LayerService layerService;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private AppWebUtils utils;

	@Autowired
	private UserService userService;

	@Autowired
	private RouterService routerService;

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/list", produces = "text/html")
	public String list(Model model, HttpServletRequest request) {

		final List<Layer> layers = layerService.findAllLayers(utils.getUserId());
		model.addAttribute("layers", layers);
		return "layers/list";
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/create")
	public String create(Model model) {
		return "layers/create";
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/createiot")
	public String createIoT(Model model) {
		List<Ontology> ontologies = ontologyService.getOntologiesWithDescriptionAndIdentification(utils.getUserId(),
				null, null);
		model.addAttribute("ontologies", ontologies);
		model.addAttribute("layer", new LayerDTO());
		return "layers/createiot";
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/createexternal")
	public String createExternal(Model model) {
		model.addAttribute("layer", new LayerDTO());
		return "layers/createexternal";
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/show/{id}")
	public String show(Model model, @PathVariable("id") String id, RedirectAttributes redirect) {
		Layer layer = layerService.findById(id, utils.getUserId());
		if (layer != null && layer.getOntology() != null) {
			LayerDTO layerDto = new LayerDTO();
			layerDto.setRefreshTime(layer.getRefreshTime());
			layerDto.setDescription(layer.getDescription());
			layerDto.setGeometryField(layer.getGeometryField());
			layerDto.setGeometryType(layer.getGeometryType());
			layerDto.setIdentification(layer.getIdentification());
			layerDto.setIsPublic(layer.isPublic());
			layerDto.setOntology(layer.getOntology().getIdentification());
			layerDto.setId(layer.getId());
			layerDto.setIsHeatMap(layer.isHeatMap());
			layerDto.setIsFilter(layer.isFilter());

			if (layer.isHeatMap()) {

				layerDto.setWeightField(layer.getWeightField());
				layerDto.setHeatMapMax(layer.getHeatMapMax().toString());
				layerDto.setHeatMapMin(layer.getHeatMapMin().toString());
				layerDto.setHeatMapRadius(layer.getHeatMapRadius().toString());
				layerDto.setGeometryType("Point");
			} else {

				layerDto.setInnerColor(layer.getInnerColor());
				layerDto.setOuterColor(layer.getOuterColor());
				layerDto.setOuterThin(layer.getOuterThin());
				layerDto.setSize(layer.getSize());
				layerDto.setInfoBox(layer.getInfoBox());
			}

			if (layer.isFilter()) {
				layerDto.setFilters(layer.getFilters());
			}

			if (layer.getQuery() != null && layer.getQuery() != "") {
				layerDto.setQuery(layer.getQuery());
				layerDto.setQueryParams(layer.getQueryParams());
			}

			model.addAttribute("layer", layerDto);
			return "layers/showiot";
		} else if (layer != null && layer.getOntology() == null) {
			LayerDTO layerDto = new LayerDTO();
			layerDto.setDescription(layer.getDescription());
			layerDto.setIdentification(layer.getIdentification());
			layerDto.setIsPublic(layer.isPublic());
			layerDto.setId(layer.getId());
			layerDto.setExternalType(layer.getExternalType());
			layerDto.setUrl(layer.getUrl());
			layerDto.setLayerTypeWms(layer.getLayerTypeWms());
			layerDto.setWest(layer.getWest() != null ? layer.getWest().toString() : null);
			layerDto.setSouth(layer.getSouth() != null ? layer.getSouth().toString() : null);
			layerDto.setNorth(layer.getNorth() != null ? layer.getNorth().toString() : null);
			layerDto.setEast(layer.getEast() != null ? layer.getEast().toString() : null);
			model.addAttribute("layer", layerDto);
			return "layers/showexternal";
		} else {
			utils.addRedirectMessage("ontology.notfound.error", redirect);
			return "redirect:/layers/list";
		}

	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/update/{id}")
	public String update(Model model, @PathVariable("id") String id) {
		Layer layer = layerService.findById(id, utils.getUserId());

		if (!utils.getUserId().equals(layer.getUser().getUserId()) && !utils.getRole().equals("ROLE_ADMINISTRATOR")) {
			log.error("User has not permission");
			return "error/403";
		}

		if (layer.getOntology() != null) {
			List<Ontology> ontologies = ontologyService.getOntologiesWithDescriptionAndIdentification(utils.getUserId(),
					null, null);

			LayerDTO layerDto = new LayerDTO();
			layerDto.setRefreshTime(layer.getRefreshTime());
			layerDto.setDescription(layer.getDescription());
			layerDto.setGeometryField(layer.getGeometryField());
			layerDto.setGeometryType(layer.getGeometryType());
			layerDto.setIdentification(layer.getIdentification());
			layerDto.setIsPublic(layer.isPublic());
			layerDto.setOntology(layer.getOntology().getIdentification());
			layerDto.setId(layer.getId());
			layerDto.setIsHeatMap(layer.isHeatMap());
			layerDto.setIsFilter(layer.isFilter());

			if (layer.isHeatMap()) {

				layerDto.setWeightField(layer.getWeightField());
				layerDto.setHeatMapMax(layer.getHeatMapMax().toString());
				layerDto.setHeatMapMin(layer.getHeatMapMin().toString());
				layerDto.setHeatMapRadius(layer.getHeatMapRadius().toString());
				layerDto.setGeometryType("Point");

			} else {

				layerDto.setInnerColor(layer.getInnerColor());
				layerDto.setOuterColor(layer.getOuterColor());
				layerDto.setOuterThin(layer.getOuterThin());
				layerDto.setSize(layer.getSize());
				layerDto.setInfoBox(layer.getInfoBox());
			}
			if (layer.isFilter()) {
				layerDto.setFilters(layer.getFilters());
			}

			if (layer.getQuery() != null && layer.getQuery() != "") {
				layerDto.setQuery(layer.getQuery());
				layerDto.setQueryParams(layer.getQueryParams());
			}

			model.addAttribute("ontologies", ontologies);
			model.addAttribute("layer", layerDto);
			return "layers/createiot";
		} else {
			LayerDTO layerDto = new LayerDTO();
			layerDto.setRefreshTime(layer.getRefreshTime());
			layerDto.setDescription(layer.getDescription());
			layerDto.setIdentification(layer.getIdentification());
			layerDto.setIsPublic(layer.isPublic());
			layerDto.setId(layer.getId());
			layerDto.setExternalType(layer.getExternalType());
			layerDto.setUrl(layer.getUrl());
			layerDto.setLayerTypeWms(layer.getLayerTypeWms());
			layerDto.setWest(layer.getWest() != null ? layer.getWest().toString() : null);
			layerDto.setSouth(layer.getSouth() != null ? layer.getSouth().toString() : null);
			layerDto.setNorth(layer.getNorth() != null ? layer.getNorth().toString() : null);
			layerDto.setEast(layer.getEast() != null ? layer.getEast().toString() : null);
			model.addAttribute("layer", layerDto);
			return "layers/createexternal";
		}

	}

	@PostMapping("/getNamesForAutocomplete")
	public @ResponseBody List<String> getNamesForAutocomplete() {
		return layerService.getAllIdentificationsByUser(utils.getUserId());
	}

	@PostMapping(value = "/getOntologyGeometryFields")
	public ResponseEntity<Map<String, String>> getOntologyGeometryFields(@RequestParam String ontologyIdentification)
			throws JsonProcessingException, IOException {

		try {
			return new ResponseEntity<Map<String, String>>(
					layerService.getOntologyGeometryFields(ontologyIdentification, utils.getUserId()), HttpStatus.OK);
		} catch (final RuntimeException e) {
			return new ResponseEntity<Map<String, String>>(HttpStatus.BAD_REQUEST);
		}

	}

	@PostMapping(value = "/getOntologyFields")
	public ResponseEntity<Map<String, String>> getOntologyFields(@RequestParam String ontologyIdentification)
			throws JsonProcessingException, IOException {

		try {
			return new ResponseEntity<Map<String, String>>(
					ontologyService.getOntologyFields(ontologyIdentification, utils.getUserId()), HttpStatus.OK);
		} catch (final RuntimeException e) {
			return new ResponseEntity<Map<String, String>>(HttpStatus.BAD_REQUEST);
		}

	}

	@PostMapping(value = "/getQueryFields")
	public ResponseEntity<List<String>> getQueryFields(@RequestParam String query, @RequestParam String ontology)
			throws JsonProcessingException, IOException {
		try {
			List<String> fields = layerService.getQueryFields(query, ontology, utils.getUserId());
			return new ResponseEntity<List<String>>(fields, HttpStatus.OK);
		} catch (final RuntimeException e) {
			return new ResponseEntity<List<String>>(HttpStatus.BAD_REQUEST);
		}

	}

	@PostMapping(value = { "/createiot", "/createexternal" })
	@Transactional
	public ResponseEntity<?> createLayer(org.springframework.ui.Model model, @Valid LayerDTO layerDto,
			BindingResult bindingResult, RedirectAttributes redirect, HttpServletRequest httpServletRequest) {
		final Map<String, String> response = new HashMap<>();
		if (bindingResult.hasErrors()) {
			response.put("status", "error");
			response.put("cause", utils.getMessage("ontology.validation.error", "validation error"));
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
		}

		User user = userService.getUser(utils.getUserId());

		if (layerDto.getOntology() != null) {
			Ontology ontology = ontologyService.getOntologyByIdentification(layerDto.getOntology(), utils.getUserId());
			String infoBox = httpServletRequest.getParameter("infoBox");
			Boolean isPublic = Boolean.valueOf(httpServletRequest.getParameter("isPublic"));
			Boolean isHeatMap = Boolean.valueOf(httpServletRequest.getParameter("isHeatMap"));
			Boolean isFilter = Boolean.valueOf(httpServletRequest.getParameter("isFilter"));
			Boolean isQuery = Boolean.valueOf(httpServletRequest.getParameter("isQuery"));
			if (ontology != null) {
				Layer layer = new Layer();
				layer.setRefreshTime(layerDto.getRefreshTime());
				layer.setDescription(layerDto.getDescription());
				layer.setGeometryField(layerDto.getGeometryField());
				layer.setIdentification(layerDto.getIdentification());
				layer.setGeometryType(layerDto.getGeometryType());
				layer.setPublic(isPublic);
				layer.setUser(user);
				layer.setOntology(ontology);
				layer.setFilter(isFilter);

				if (isHeatMap) {
					layer.setGeometryType(RASTER);
					layer.setHeatMap(isHeatMap);
					layer.setWeightField(layerDto.getWeightField());
					layer.setHeatMapMax(Integer.valueOf(layerDto.getHeatMapMax()));
					layer.setHeatMapMin(Integer.valueOf(layerDto.getHeatMapMin()));
					layer.setHeatMapRadius(Integer.valueOf(layerDto.getHeatMapRadius()));
				} else {
					layer.setInnerColor(layerDto.getInnerColor());
					layer.setOuterColor(layerDto.getOuterColor());
					layer.setOuterThin(layerDto.getOuterThin());
					layer.setSize(layerDto.getSize());
					layer.setInfoBox(infoBox);
				}
				if (isFilter) {
					String filters = httpServletRequest.getParameter("filters");
					layer.setFilters(filters);
				}

				if (isQuery) {
					String queryParams = httpServletRequest.getParameter("queryParams");
					layer.setQuery(layerDto.getQuery());
					layer.setQueryParams(queryParams);
				}

				layerService.create(layer);

				response.put("redirect", "/controlpanel/layers/list");
				response.put("status", "ok");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.CREATED);
			} else {
				log.error("Ontology {} not found for the user {} to create the layer {}", layerDto.getOntology(),
						user.getFullName(), layerDto.getIdentification());
				response.put("cause", "Ontology not found to create the layer");
				response.put("status", "error");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.NOT_FOUND);
			}
		} else {
			Boolean isPublic = Boolean.valueOf(httpServletRequest.getParameter("isPublic"));
			Layer layer = new Layer();
			layer.setDescription(layerDto.getDescription());
			layer.setIdentification(layerDto.getIdentification());
			layer.setPublic(isPublic);
			layer.setUser(user);
			layer.setExternalType(layerDto.getExternalType());
			layer.setUrl(layerDto.getUrl());
			layer.setLayerTypeWms(layerDto.getLayerTypeWms());
			if (layerDto.getExternalType().equalsIgnoreCase("svg_image")) {
				layer.setEast(Double.parseDouble(layerDto.getEast()));
				layer.setWest(Double.parseDouble(layerDto.getWest()));
				layer.setSouth(Double.parseDouble(layerDto.getSouth()));
				layer.setNorth(Double.parseDouble(layerDto.getNorth()));
			}

			layerService.create(layer);

			response.put("redirect", "/controlpanel/layers/list");
			response.put("status", "ok");
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.CREATED);
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@PutMapping(value = "/update/{id}")
	@Transactional
	public ResponseEntity<?> updateLayer(org.springframework.ui.Model model, @Valid LayerDTO layerDto,
			@PathVariable("id") String id, BindingResult bindingResult, RedirectAttributes redirect,
			HttpServletRequest httpServletRequest) {
		final Map<String, String> response = new HashMap<>();
		if (bindingResult.hasErrors()) {
			response.put("status", "error");
			response.put("cause", utils.getMessage("ontology.validation.error", "validation error"));
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
		}

		Layer layer = layerService.findById(id, utils.getUserId());
		User user = userService.getUser(utils.getUserId());

		if (!user.getUserId().equals(layer.getUser().getUserId()) && !utils.getRole().equals("ROLE_ADMINISTRATOR")) {
			log.error("User has not permission");
			response.put("status", "error");
			response.put("cause", "User has not permission");
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.BAD_REQUEST);
		}

		if (layer.getOntology() != null) {

			Ontology ontology = ontologyService.getOntologyByIdentification(layerDto.getOntology(), utils.getUserId());
			String infoBox = httpServletRequest.getParameter("infoBox");
			Boolean isPublic = Boolean.valueOf(httpServletRequest.getParameter("isPublic"));
			Boolean isHeatMap = Boolean.valueOf(httpServletRequest.getParameter("isHeatMap"));
			Boolean isFilter = Boolean.valueOf(httpServletRequest.getParameter("isFilter"));
			Boolean isQuery = Boolean.valueOf(httpServletRequest.getParameter("isQuery"));
			if (ontology != null) {
				layer.setRefreshTime(layerDto.getRefreshTime());
				layer.setDescription(layerDto.getDescription());
				layer.setGeometryField(layerDto.getGeometryField());
				layer.setGeometryType(layerDto.getGeometryType());
				layer.setPublic(isPublic);
				layer.setOntology(ontology);
				layer.setHeatMap(isHeatMap);
				layer.setFilter(isFilter);

				if (isHeatMap) {
					layer.setGeometryType(RASTER);
					layer.setWeightField(layerDto.getWeightField());
					layer.setHeatMapMax(Integer.valueOf(layerDto.getHeatMapMax()));
					layer.setHeatMapMin(Integer.valueOf(layerDto.getHeatMapMin()));
					layer.setHeatMapRadius(Integer.valueOf(layerDto.getHeatMapRadius()));
					layer.setInnerColor(null);
					layer.setOuterColor(null);
					layer.setOuterThin(null);
					layer.setSize(null);
					layer.setInfoBox(null);
				} else {
					layer.setInnerColor(layerDto.getInnerColor());
					layer.setOuterColor(layerDto.getOuterColor());
					layer.setOuterThin(layerDto.getOuterThin());
					layer.setSize(layerDto.getSize());
					layer.setInfoBox(infoBox);
					layer.setWeightField(null);
					layer.setHeatMapMax(null);
					layer.setHeatMapMin(null);
				}

				if (isFilter) {
					String filters = httpServletRequest.getParameter("filters");
					layer.setFilters(filters);
				} else {
					layer.setFilters(null);
				}

				if (isQuery) {
					String queryParams = httpServletRequest.getParameter("queryParams");
					layer.setQuery(layerDto.getQuery());
					layer.setQueryParams(queryParams);
				} else {
					layer.setQuery(null);
					layer.setQueryParams(null);
				}

				layerService.create(layer);

				response.put("redirect", "/controlpanel/layers/list");
				response.put("status", "ok");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.CREATED);
			} else {
				log.error("Ontology {} not found for the user {} to create the layer {}", layerDto.getOntology(),
						user.getFullName(), layerDto.getIdentification());
				response.put("cause", "Ontology not found to create the layer");
				response.put("status", "error");
				return new ResponseEntity<Map<String, String>>(response, HttpStatus.NOT_FOUND);
			}
		} else {
			Boolean isPublic = Boolean.valueOf(httpServletRequest.getParameter("isPublic"));
			layer.setDescription(layerDto.getDescription());
			layer.setIdentification(layerDto.getIdentification());
			layer.setPublic(isPublic);
			layer.setExternalType(layerDto.getExternalType());
			layer.setUrl(layerDto.getUrl());
			layer.setLayerTypeWms(layerDto.getLayerTypeWms());
			if (layerDto.getExternalType().equalsIgnoreCase("svg_image")) {
				layer.setEast(Double.parseDouble(layerDto.getEast()));
				layer.setWest(Double.parseDouble(layerDto.getWest()));
				layer.setSouth(Double.parseDouble(layerDto.getSouth()));
				layer.setNorth(Double.parseDouble(layerDto.getNorth()));
			}

			layerService.create(layer);

			response.put("redirect", "/controlpanel/layers/list");
			response.put("status", "ok");
			return new ResponseEntity<Map<String, String>>(response, HttpStatus.CREATED);
		}

	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@DeleteMapping("/{id}")
	public String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirect) {

		final Layer layer = layerService.findById(id, utils.getUserId());
		if (layer != null) {
			try {

				layerService.deleteLayer(layer, utils.getUserId());

			} catch (final Exception e) {
				utils.addRedirectMessageWithParam("ontology.delete.error", e.getMessage(), redirect);
				log.error("Error deleting layer. ", e);
				return "redirect:/layers/update/" + id;
			}
			return "redirect:/layers/list";
		} else {
			return "redirect:/layers/list";
		}
	}

	@GetMapping("/isLayerInUse/{layer}")
	public @ResponseBody Boolean isLayerInUse(@PathVariable("layer") String layer) {
		return this.layerService.isLayerInUse(layer);
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST','ROLE_DEVELOPER')")
	@GetMapping(value = "/crud/{id}")
	public String crud(Model model, @PathVariable("id") String id, RedirectAttributes redirect) {
		Layer layer = layerService.findById(id, utils.getUserId());
		if (layer != null) {
			model.addAttribute("layer", layer);
			model.addAttribute("ontologyName", layer.getOntology().getIdentification());
			model.addAttribute("schema", layer.getOntology().getJsonSchema());
			return "layers/crud";
		} else {
			utils.addRedirectMessage("ontology.notfound.error", redirect);
			return "redirect:/layers/list";
		}

	}

	@PostMapping(value = { "/crud/insert" }, produces = "text/plain")
	public @ResponseBody String insert(String ontologyID, String body) {

		try {
			final String result = processQuery("", ontologyID, ApiOperation.Type.POST.name(), body, "");
			return result;
		} catch (final Exception e) {

			return "{\"exception\":\"true\"}";
		}
	}

	@PostMapping(value = { "/crud/update" }, produces = "text/plain")
	public @ResponseBody String update(String ontologyID, String body, String oid) {

		try {
			final String result = processQuery("", ontologyID, ApiOperation.Type.PUT.name(), body, oid);
			return result;
		} catch (final Exception e) {

			return "{\"exception\":\"true\"}";
		}
	}

	@PostMapping(value = { "/crud/deleteById" }, produces = "text/plain")
	public @ResponseBody String deleteById(String ontologyID, String oid) {

		try {
			final String result = processQuery("", ontologyID, ApiOperation.Type.DELETE.name(), "", oid);
			return result;
		} catch (final Exception e) {

			return "{\"error\":\"true\"}";
		}
	}

	public String processQuery(String query, String ontologyID, String method, String body, String objectId)
			throws Exception {

		final User user = userService.getUser(utils.getUserId());
		OperationType operationType = null;
		if (method.equalsIgnoreCase(ApiOperation.Type.GET.name())) {
			body = query;
			operationType = OperationType.QUERY;
		} else if (method.equalsIgnoreCase(ApiOperation.Type.POST.name())) {
			operationType = OperationType.INSERT;
		} else if (method.equalsIgnoreCase(ApiOperation.Type.PUT.name())) {
			operationType = OperationType.UPDATE;
		} else if (method.equalsIgnoreCase(ApiOperation.Type.DELETE.name())) {
			operationType = OperationType.DELETE;
		} else {
			operationType = OperationType.QUERY;
		}

		final OperationModel model = OperationModel
				.builder(ontologyID, OperationType.valueOf(operationType.name()), user.getUserId(),
						OperationModel.Source.INTERNAL_ROUTER)
				.body(body).queryType(QueryType.SQL).objectId(objectId).deviceTemplate("").build();
		final NotificationModel modelNotification = new NotificationModel();

		modelNotification.setOperationModel(model);

		final OperationResultModel result = routerService.query(modelNotification);

		if (result != null) {
			if ("ERROR".equals(result.getResult())) {
				String ret = "{\"error\":\"" + result.getMessage() + "\"}";
				return ret;
			}

			String OUTPUT = result.getResult();

			if (operationType == OperationType.INSERT) {
				final JSONObject obj = new JSONObject(OUTPUT);
				if (obj.has(InsertResult.DATA_PROPERTY)) {
					OUTPUT = obj.getJSONObject(InsertResult.DATA_PROPERTY).toString();
				}
			}
			return OUTPUT;
		} else {
			return null;
		}

	}

	@PostMapping(value = { "/hasOntRoot" })
	public ResponseEntity<Boolean> hasOntologyRoot(@RequestParam("ontologyID") String ontologyID) {
		String root = null;
		try {
			String schema = ontologyService.getOntologyByIdentification(ontologyID, utils.getUserId()).getJsonSchema();
			JSONObject jsonschema = new JSONObject(schema);
			Iterator<String> iterator = jsonschema.keys();
			while (iterator.hasNext()) {
				String prop = iterator.next();
				try {
					Iterator<String> iteratorAux = jsonschema.getJSONObject(prop).keys();
					while (iteratorAux.hasNext()) {
						String p = iteratorAux.next();
						if (jsonschema.getJSONObject(prop).getJSONObject(p).has("$ref")) {
							root = p;
							break;
						}
					}
				} catch (Exception e) {
				}
			}
		} catch (JSONException e) {
			log.info("Error: ", e.getMessage());
			return new ResponseEntity<Boolean>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (root == null) {
			return new ResponseEntity<Boolean>(false, HttpStatus.OK);
		}
		return new ResponseEntity<Boolean>(true, HttpStatus.OK);
	}

}
