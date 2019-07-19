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
package com.minsait.onesait.platform.controlpanel.rest.management.dashboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minsait.onesait.platform.config.model.Category;
import com.minsait.onesait.platform.config.model.CategoryRelation;
import com.minsait.onesait.platform.config.model.Dashboard;
import com.minsait.onesait.platform.config.model.DashboardConf;
import com.minsait.onesait.platform.config.model.DashboardUserAccess;
import com.minsait.onesait.platform.config.model.Subcategory;
import com.minsait.onesait.platform.config.repository.DashboardConfRepository;
import com.minsait.onesait.platform.config.services.category.CategoryService;
import com.minsait.onesait.platform.config.services.categoryrelation.CategoryRelationService;
import com.minsait.onesait.platform.config.services.dashboard.DashboardService;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardCreateDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardExportDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardOrder;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardSimplifiedDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardUserAccessDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.CommandDTO;
import com.minsait.onesait.platform.config.services.exceptions.GadgetDatasourceServiceException;
import com.minsait.onesait.platform.config.services.subcategory.SubcategoryService;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@CrossOrigin(origins = "*")
@Api(value = "Dashboard Management", tags = { "Dashoard management service" })
@RequestMapping("api/dashboards")
@ApiResponses({ @ApiResponse(code = 400, message = "Bad request"),
		@ApiResponse(code = 500, message = "Internal server error"), @ApiResponse(code = 403, message = "Forbidden") })
public class DashboardManagementRestController {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private SubcategoryService subCategoryService;

	@Autowired
	private CategoryRelationService categoryRelationService;

	@Autowired
	private DashboardConfRepository dashboardConfRepository;

	@Autowired
	private AppWebUtils utils;

	@Value("${onesaitplatform.dashboardengine.url}")
	private String url;

	@Value("${onesaitplatform.dashboardengine.url.view}")
	private String viewUrl;

	private final static String PATH = "/dashboard";

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardDTO[].class))
	@ApiOperation(value = "Get dashboards")
	@GetMapping
	public ResponseEntity<DashboardDTO[]> getAll(
			@RequestParam(value = "orderType", required = false) DashboardOrder order) {
		order = order == null ? DashboardOrder.identificationAsc : order;
		final List<Dashboard> dashboards = dashboardService.getByUserIdOrdered(utils.getUserId(), order);
		final DashboardDTO[] dashboardsDTO = new DashboardDTO[dashboards.size()];
		int i = 0;
		for (final Dashboard dashboard : dashboards) {
			final CategoryRelation categoryRelationship = categoryRelationService.getByIdType(dashboard.getId());
			String categoryIdentification = null;
			String subCategoryIdentification = null;
			if (categoryRelationship != null) {
				final Category category = categoryService
						.getCategoryByIdentification(categoryRelationship.getCategory());
				final Subcategory subcategory = subCategoryService
						.getSubcategoryById(categoryRelationship.getSubcategory());
				categoryIdentification = category.getIdentification();
				subCategoryIdentification = subcategory.getIdentification();
			}

			final int ngadgets = dashboardService.getNumGadgets(dashboard);

			final List<DashboardUserAccess> dashaccesses = dashboardService.getDashboardUserAccesses(dashboard.getId());
			final List<DashboardUserAccessDTO> dashAuths = dashAuthstoDTO(dashaccesses);

			final DashboardDTO dashboardDTO = DashboardDTO.builder().id(dashboard.getId())
					.identification(dashboard.getIdentification()).description(dashboard.getDescription())
					.user(dashboard.getUser().getUserId()).url(url + dashboard.getId()).isPublic(dashboard.isPublic())
					.category(categoryIdentification).subcategory(subCategoryIdentification).nGadgets(ngadgets)
					.headerlibs(dashboard.getHeaderlibs()).createdAt(dashboard.getCreatedAt().toString())
					.modifiedAt(dashboard.getUpdatedAt().toString()).viewUrl(viewUrl + dashboard.getId())
					.dashboardAuths(dashAuths).build();

			dashboardsDTO[i] = dashboardDTO;
			i++;
		}

		return new ResponseEntity<>(dashboardsDTO, HttpStatus.OK);
	}

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardDTO.class))
	@ApiOperation(value = "Get dashboard by id")
	@GetMapping("/{id}")
	public ResponseEntity<DashboardDTO> getDashboardById(
			@ApiParam(value = "dashboard id", required = true) @PathVariable("id") String dashboardId) {
		Dashboard dashboard = dashboardService.getDashboardById(dashboardId, utils.getUserId());
		if (dashboard == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		CategoryRelation categoryRelationship = categoryRelationService.getByIdType(dashboard.getId());
		String categoryIdentification = null;
		String subCategoryIdentification = null;
		if (categoryRelationship != null) {
			Category category = categoryService.getCategoryByIdentification(categoryRelationship.getCategory());
			Subcategory subcategory = subCategoryService.getSubcategoryById(categoryRelationship.getSubcategory());
			categoryIdentification = category.getIdentification();
			subCategoryIdentification = subcategory.getIdentification();
		}

		final int ngadgets = dashboardService.getNumGadgets(dashboard);

		final List<DashboardUserAccess> dashaccesses = dashboardService.getDashboardUserAccesses(dashboard.getId());
		final List<DashboardUserAccessDTO> dashAuths = dashAuthstoDTO(dashaccesses);

		DashboardDTO dashboardDTO = DashboardDTO.builder().identification(dashboard.getIdentification())
				.id(dashboard.getId()).description(dashboard.getDescription()).user(dashboard.getUser().getUserId())
				.url(url + dashboard.getId()).isPublic(dashboard.isPublic()).category(categoryIdentification)
				.subcategory(subCategoryIdentification).nGadgets(ngadgets).headerlibs(dashboard.getHeaderlibs())
				.createdAt(dashboard.getCreatedAt().toString()).modifiedAt(dashboard.getUpdatedAt().toString())
				.viewUrl(viewUrl + dashboard.getId()).dashboardAuths(dashAuths).build();

		return new ResponseEntity<>(dashboardDTO, HttpStatus.OK);
	}

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardDTO.class))
	@ApiOperation(value = "Get dashboard by identification")
	@GetMapping(PATH + "/{identification}")
	public ResponseEntity<DashboardDTO> getDashboardByIdentification(
			@ApiParam(value = "dashboard identification", required = true) @PathVariable("identification") String identification) {
		Dashboard dashboard = dashboardService.getDashboardByIdentification(identification, utils.getUserId());
		if (dashboard == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		CategoryRelation categoryRelationship = categoryRelationService.getByIdType(dashboard.getId());
		String categoryIdentification = null;
		String subCategoryIdentification = null;
		if (categoryRelationship != null) {
			Category category = categoryService.getCategoryByIdentification(categoryRelationship.getCategory());
			Subcategory subcategory = subCategoryService.getSubcategoryById(categoryRelationship.getSubcategory());
			categoryIdentification = category.getIdentification();
			subCategoryIdentification = subcategory.getIdentification();
		}

		final int ngadgets = dashboardService.getNumGadgets(dashboard);

		final List<DashboardUserAccess> dashaccesses = dashboardService.getDashboardUserAccesses(dashboard.getId());
		final List<DashboardUserAccessDTO> dashAuths = dashAuthstoDTO(dashaccesses);

		DashboardDTO dashboardDTO = DashboardDTO.builder().identification(dashboard.getIdentification())
				.id(dashboard.getId()).description(dashboard.getDescription()).user(dashboard.getUser().getUserId())
				.url(url + dashboard.getId()).isPublic(dashboard.isPublic()).category(categoryIdentification)
				.subcategory(subCategoryIdentification).nGadgets(ngadgets).headerlibs(dashboard.getHeaderlibs())
				.createdAt(dashboard.getCreatedAt().toString()).modifiedAt(dashboard.getUpdatedAt().toString())
				.viewUrl(viewUrl + dashboard.getId()).dashboardAuths(dashAuths).build();

		return new ResponseEntity<>(dashboardDTO, HttpStatus.OK);
	}

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardDTO.class))
	@ApiOperation(value = "Create new dashboard")
	@PostMapping
	public ResponseEntity<?> create(
			@ApiParam(value = "CommandDTO", required = true) @Valid @RequestBody CommandDTO commandDTO, Errors errors) {
		try {

			final DashboardCreateDTO dashboard = new DashboardCreateDTO();
			dashboard.setIdentification(commandDTO.getInformation().getDashboard());
			String description = "";
			if (commandDTO.getInformation().getDashboardDescription() != null) {
				description = commandDTO.getInformation().getDashboardDescription();
			}
			dashboard.setDescription(description);
			dashboard.setPublicAccess(Boolean.FALSE);
			final List<DashboardConf> listStyles = dashboardConfRepository.findAll();
			String initialStyleId = null;
			String initialIdentification = null;
			if (commandDTO.getInformation().getDashboardStyle() == null) {
				initialIdentification = "notitle";
			} else {
				initialIdentification = commandDTO.getInformation().getDashboardStyle();
			}
			for (final Iterator iterator = listStyles.iterator(); iterator.hasNext();) {
				final DashboardConf dashboardCon = (DashboardConf) iterator.next();
				if (dashboardCon.getIdentification().equals(initialIdentification)) {
					initialStyleId = dashboardCon.getId();
					dashboard.setHeaderlibs(dashboardCon.getHeaderlibs());
					break;
				}
			}
			dashboard.setDashboardConfId(initialStyleId);
			final String dashboardId = dashboardService.createNewDashboard(dashboard, utils.getUserId());
			final Dashboard dashboardCreated = dashboardService.getDashboardById(dashboardId, utils.getUserId());

			final DashboardDTO dashboardDTO = DashboardDTO.builder().id(dashboardCreated.getId())
					.identification(dashboardCreated.getIdentification()).user(dashboardCreated.getUser().getUserId())
					.url(url + dashboardCreated.getId()).description(dashboardCreated.getDescription())
					.isPublic(dashboardCreated.isPublic()).category(null).subcategory(null)
					.createdAt(dashboardCreated.getCreatedAt().toString()).nGadgets(0)
					.headerlibs(dashboard.getHeaderlibs()).modifiedAt(dashboardCreated.getUpdatedAt().toString())
					.viewUrl(viewUrl + dashboard.getId()).build();
			return new ResponseEntity<>(dashboardDTO, HttpStatus.OK);

		} catch (final GadgetDatasourceServiceException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@ApiOperation(value = "Delete dashboard by id")
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(
			@ApiParam(value = "dashboard id", example = "developer", required = true) @PathVariable("id") String DashboardId) {
		try {
			final Dashboard dashboard = dashboardService.getDashboardById(DashboardId, utils.getUserId());
			if (dashboard != null)
				dashboardService.deleteDashboard(dashboard.getId(), utils.getUserId());
			else
				return new ResponseEntity<>("\"Dashboard not found\"", HttpStatus.NOT_FOUND);
			return new ResponseEntity<>("\"Dashboard deleted successfully\"", HttpStatus.OK);
		} catch (final GadgetDatasourceServiceException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@ApiOperation(value = "Update dashboard")
	@PutMapping("/{id}")
	public ResponseEntity<Object> update(
			@ApiParam(value = "dashboard id", required = true) @PathVariable("id") String dashboardID,
			@ApiParam(value = "Dashboard simplified", required = true) @Valid @RequestBody DashboardSimplifiedDTO dashboardSimplifiedDTO) {
		try {
			dashboardService.updateDashboardSimplified(dashboardID, dashboardSimplifiedDTO, utils.getUserId());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (final GadgetDatasourceServiceException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public List<DashboardUserAccessDTO> dashAuthstoDTO(List<DashboardUserAccess> dashaccesses) {
		final ArrayList<DashboardUserAccessDTO> dashAuths = new ArrayList<DashboardUserAccessDTO>();
		for (DashboardUserAccess dashua : dashaccesses) {
			DashboardUserAccessDTO dashAccDTO = new DashboardUserAccessDTO();
			dashAccDTO.setUserId(dashua.getUser().getUserId());
			dashAccDTO.setDashboardId(dashua.getDashboard().getIdentification());
			dashAccDTO.setAccessType(dashua.getDashboardUserAccessType().getName());
			dashAuths.add(dashAccDTO);
		}
		return dashAuths;
	}

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardExportDTO.class))
	@ApiOperation(value = "Export dashboard by id")
	@GetMapping("/export/{id}")
	public ResponseEntity<?> exportNotebook(
			@ApiParam(value = "dashboard id", required = true) @PathVariable("id") String dashboardId) {
		Dashboard dashboard = dashboardService.getDashboardById(dashboardId, utils.getUserId());
		if (dashboard == null) {
			return new ResponseEntity<>("\"Dashboard not found\"", HttpStatus.NOT_FOUND);
		}

		CategoryRelation categoryRelationship = categoryRelationService.getByIdType(dashboard.getId());
		String categoryIdentification = null;
		String subCategoryIdentification = null;
		if (categoryRelationship != null) {
			Category category = categoryService.getCategoryByIdentification(categoryRelationship.getCategory());
			Subcategory subcategory = subCategoryService.getSubcategoryById(categoryRelationship.getSubcategory());
			categoryIdentification = category.getIdentification();
			subCategoryIdentification = subcategory.getIdentification();
		}

		final int ngadgets = dashboardService.getNumGadgets(dashboard);

		final List<DashboardUserAccess> dashaccesses = dashboardService.getDashboardUserAccesses(dashboard.getId());
		final List<DashboardUserAccessDTO> dashAuths = dashAuthstoDTO(dashaccesses);

		DashboardExportDTO dashboardDTO = DashboardExportDTO.builder().identification(dashboard.getIdentification())
				.user(dashboard.getUser().getUserId()).subcategory(subCategoryIdentification).nGadgets(ngadgets)
				.headerlibs(dashboard.getHeaderlibs()).createdAt(dashboard.getCreatedAt())
				.description(dashboard.getDescription()).modifiedAt(dashboard.getUpdatedAt()).dashboardAuths(dashAuths)
				.model(dashboard.getModel()).build();

		final DashboardExportDTO dashWGadgets = dashboardService.addGadgets(dashboardDTO);

		return new ResponseEntity<>(dashWGadgets, HttpStatus.OK);
	}

	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = DashboardDTO.class))
	@ApiOperation(value = "Import dashboard")
	@PostMapping("/import")
	public ResponseEntity<?> importDashboard(
			@ApiParam(value = "DashboardDTO", required = true) @Valid @RequestBody DashboardExportDTO dashboardimportDTO,
			Errors errors) {

		String dashboardId = "";
		dashboardId = dashboardService.importDashboard(dashboardimportDTO, utils.getUserId());
		if (!dashboardId.equals(""))
			return new ResponseEntity<>("\"Dashboard " + dashboardId + " imported successfully\"", HttpStatus.OK);
		else
			return new ResponseEntity<>("\"Dashboard already exists\"", HttpStatus.FORBIDDEN);
	}
	
	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = byte[].class))
	@ApiOperation(value = "Generate image of dashboard")
	@GetMapping(PATH + "/generateDashboardImage/{identification}")
	public ResponseEntity<byte[]> generateDashboardImage(
			@ApiParam(value = "Dashboard ID", required = true) @PathVariable("identification") String id,
			@ApiParam(value = "Wait time (ms) for rendering dashboard", required = true) @RequestParam("waittime") int waittime,
			@ApiParam(value = "Render Height", required = true) @RequestParam("height") int height,
			@ApiParam(value = "Render Width", required = true) @RequestParam("width") int width,
			@ApiParam(value = "Fullpage", required = false, defaultValue = "false") @RequestParam("fullpage") Boolean fullpage)
			{
		Dashboard dashboard = dashboardService.getDashboardById(id, utils.getUserId());
		if (dashboard == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		else if(!dashboardService.hasUserViewPermission(id,utils.getUserId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		
		return dashboardService.generateImgFromDashboardId(id,waittime,height,width,(fullpage==null?false:fullpage),utils.getCurrentUserOauthToken());
	}
	
	@ApiResponses(@ApiResponse(code = 200, message = "OK", response = byte[].class))
	@ApiOperation(value = "Generate PDF of dashboard")
	@GetMapping(PATH + "/generatePDFImage/{identification}")
	public ResponseEntity<byte[]> generatePDFImage(
			@ApiParam(value = "Dashboard ID", required = true) @PathVariable("identification") String id,
			@ApiParam(value = "Wait time (ms) for rendering dashboard", required = true) @RequestParam("waittime") int waittime,
			@ApiParam(value = "Render Height", required = true) @RequestParam("height") int height,
			@ApiParam(value = "Render Width", required = true) @RequestParam("width") int width)
			{
		Dashboard dashboard = dashboardService.getDashboardById(id, utils.getUserId());
		if (dashboard == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		else if(!dashboardService.hasUserViewPermission(id,utils.getUserId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		
		return dashboardService.generatePDFFromDashboardId(id,waittime,height,width,utils.getCurrentUserOauthToken());
	}
	
}
