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
package com.minsait.onesait.platform.config.services.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.config.model.Category;
import com.minsait.onesait.platform.config.model.CategoryRelation;
import com.minsait.onesait.platform.config.model.Dashboard;
import com.minsait.onesait.platform.config.model.DashboardConf;
import com.minsait.onesait.platform.config.model.DashboardUserAccess;
import com.minsait.onesait.platform.config.model.DashboardUserAccessType;
import com.minsait.onesait.platform.config.model.Gadget;
import com.minsait.onesait.platform.config.model.GadgetDatasource;
import com.minsait.onesait.platform.config.model.GadgetMeasure;
import com.minsait.onesait.platform.config.model.ProjectResourceAccess.ResourceAccessType;
import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.Subcategory;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.repository.CategoryRelationRepository;
import com.minsait.onesait.platform.config.repository.CategoryRepository;
import com.minsait.onesait.platform.config.repository.DashboardConfRepository;
import com.minsait.onesait.platform.config.repository.DashboardRepository;
import com.minsait.onesait.platform.config.repository.DashboardUserAccessRepository;
import com.minsait.onesait.platform.config.repository.DashboardUserAccessTypeRepository;
import com.minsait.onesait.platform.config.repository.GadgetDatasourceRepository;
import com.minsait.onesait.platform.config.repository.GadgetMeasureRepository;
import com.minsait.onesait.platform.config.repository.GadgetRepository;
import com.minsait.onesait.platform.config.repository.OntologyRepository;
import com.minsait.onesait.platform.config.repository.SubcategoryRepository;
import com.minsait.onesait.platform.config.repository.UserRepository;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardAccessDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardCreateDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardExportDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardOrder;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardSimplifiedDTO;
import com.minsait.onesait.platform.config.services.dashboard.dto.DashboardUserAccessDTO;
import com.minsait.onesait.platform.config.services.exceptions.DashboardServiceException;
import com.minsait.onesait.platform.config.services.exceptions.OPResourceServiceException;
import com.minsait.onesait.platform.config.services.gadget.GadgetService;
import com.minsait.onesait.platform.config.services.gadget.dto.GadgetDTO;
import com.minsait.onesait.platform.config.services.gadget.dto.GadgetDatasourceDTO;
import com.minsait.onesait.platform.config.services.gadget.dto.GadgetMeasureDTO;
import com.minsait.onesait.platform.config.services.gadget.dto.OntologyDTO;
import com.minsait.onesait.platform.config.services.opresource.OPResourceService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

	@Autowired
	private DashboardRepository dashboardRepository;
	@Autowired
	private DashboardUserAccessRepository dashboardUserAccessRepository;
	@Autowired
	private DashboardUserAccessTypeRepository dashboardUserAccessTypeRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private SubcategoryRepository subcategoryRepository;
	@Autowired
	private CategoryRelationRepository categoryRelationRepository;
	@Autowired
	private OPResourceService resourceService;
	@Autowired
	private DashboardConfRepository dashboardConfRepository;
	@Autowired
	private GadgetRepository gadgetRepository;
	@Autowired
	private GadgetDatasourceRepository gadgetDatasourceRepository;
	@Autowired
	private GadgetMeasureRepository gadgetMeasureRepository;
	@Autowired
	private OntologyRepository ontologyRepository;
	@Autowired
	private GadgetService gadgetService;

	protected ObjectMapper objectMapper;

	@Value("${onesaitplatform.dashboardengine.url.view:http://localhost:8087/controlpanel/dashboards/viewiframe/}")
	private String prefixURLView;

	@Value("${onesaitplatform.dashboard.export.url:http://dashboardexport:26000}")
	private String dashboardexporturl;

	private static final String toImg = "%s/imgfromurl";
	private static final String toPDF = "%s/pdffromurl";

	@PostConstruct
	public void init() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	// private static final String INITIAL_MODEL = "{\"header\":{\"title\":\"My new
	// onesait platform
	// Dashboard\",\"enable\":true,\"height\":72,\"logo\":{\"height\":48},\"backgroundColor\":\"#FFFFFF\",\"textColor\":\"#060E14\",\"iconColor\":\"#060E14\",\"pageColor\":\"#2e6c99\"},\"navigation\":{\"showBreadcrumbIcon\":true,\"showBreadcrumb\":true},\"pages\":[{\"title\":\"New
	// Page\",\"icon\":\"apps\",\"background\":{\"file\":[]},\"layers\":[{\"gridboard\":[{}],\"title\":\"baseLayer\",\"$$hashKey\":\"object:23\"}],\"selectedlayer\":0,\"combinelayers\":false,\"$$hashKey\":\"object:4\"}],\"gridOptions\":{\"gridType\":\"fit\",\"compactType\":\"none\",\"margin\":3,\"outerMargin\":true,\"mobileBreakpoint\":640,\"minCols\":20,\"maxCols\":100,\"minRows\":20,\"maxRows\":100,\"maxItemCols\":5000,\"minItemCols\":1,\"maxItemRows\":5000,\"minItemRows\":1,\"maxItemArea\":25000,\"minItemArea\":1,\"defaultItemCols\":4,\"defaultItemRows\":4,\"fixedColWidth\":250,\"fixedRowHeight\":250,\"enableEmptyCellClick\":false,\"enableEmptyCellContextMenu\":false,\"enableEmptyCellDrop\":true,\"enableEmptyCellDrag\":false,\"emptyCellDragMaxCols\":5000,\"emptyCellDragMaxRows\":5000,\"draggable\":{\"delayStart\":100,\"enabled\":true,\"ignoreContent\":true,\"dragHandleClass\":\"drag-handler\"},\"resizable\":{\"delayStart\":0,\"enabled\":true},\"swap\":false,\"pushItems\":true,\"disablePushOnDrag\":false,\"disablePushOnResize\":false,\"pushDirections\":{\"north\":true,\"east\":true,\"south\":true,\"west\":true},\"pushResizeItems\":false,\"displayGrid\":\"none\",\"disableWindowResize\":false,\"disableWarnings\":false,\"scrollToNewItems\":true,\"api\":{}},\"interactionHash\":{\"1\":[]}}";
	private static final String ANONYMOUSUSER = "anonymousUser";
	private static final String AUTH_PARSE_EXCEPT = "Authorizations parse Exception";
	private static final String DASH_NOT_EXIST = "Dashboard does not exist in the database";
	private static final String CATEGORY_SUBCATEGORY_NOTFOUND = "Category and subcategory not found";
	private static final String CATEGORY_SUBCATEGORY_EXIST = "Category and subcategory already exist for this dashboard";
	private static final String DASH_CREATE_AUTH_EXCEPT = "You do not have authorization to create dashboards";

	@Override
	public List<DashboardDTO> findDashboardWithIdentificationAndDescription(String identification, String description,
			String userId) {
		List<Dashboard> dashboards;
		final User sessionUser = userRepository.findByUserId(userId);

		description = description == null ? "" : description;
		identification = identification == null ? "" : identification;

		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			dashboards = dashboardRepository.findByIdentificationContainingAndDescriptionContaining(identification,
					description);
		} else {
			dashboards = dashboardRepository
					.findByUserAndPermissionsANDIdentificationContainingAndDescriptionContaining(sessionUser,
							identification, description);
		}

		final List<DashboardDTO> dashboardsDTO = dashboards.stream().map(temp -> {
			final DashboardDTO obj = new DashboardDTO();
			obj.setCreatedAt(temp.getCreatedAt());
			obj.setDescription(temp.getDescription());
			obj.setId(temp.getId());
			obj.setIdentification(temp.getIdentification());
			if (null != temp.getImage()) {
				obj.setHasImage(Boolean.TRUE);
			} else {
				obj.setHasImage(Boolean.FALSE);
			}
			obj.setPublic(temp.isPublic());
			obj.setUpdatedAt(temp.getUpdatedAt());
			obj.setUser(temp.getUser());
			obj.setUserAccessType(getUserTypePermissionForDashboard(temp, sessionUser));
			obj.setType(temp.getType());
			return obj;
		}).collect(Collectors.toList());

		return dashboardsDTO;
	}

	@Override
	public List<String> getAllIdentifications() {
		final List<Dashboard> dashboards = dashboardRepository.findAllByOrderByIdentificationAsc();
		final List<String> identifications = new ArrayList<String>();
		for (final Dashboard dashboard : dashboards) {
			identifications.add(dashboard.getIdentification());

		}
		return identifications;
	}

	@Transactional
	@Override
	public void deleteDashboard(String dashboardId, String userId) {
		final Dashboard dashboard = dashboardRepository.findById(dashboardId);
		if (dashboard != null && hasUserEditPermission(dashboardId, userId)) {
			if (resourceService.isResourceSharedInAnyProject(dashboard))
				throw new OPResourceServiceException(
						"This Dashboard is shared within a Project, revoke access from project prior to deleting");
			final CategoryRelation categoryRelation = categoryRelationRepository.findByTypeId(dashboard.getId());
			if (categoryRelation != null) {

				categoryRelationRepository.delete(categoryRelation);
			}
			dashboardUserAccessRepository.deleteByDashboard(dashboard);
			dashboardRepository.delete(dashboard);
		} else
			throw new DashboardServiceException("Cannot delete dashboard that does not exist");

	}

	@Transactional
	@Override
	public String deleteDashboardAccess(String dashboardId, String userId) {

		final Dashboard d = dashboardRepository.findById(dashboardId);
		if (resourceService.isResourceSharedInAnyProject(d))
			throw new OPResourceServiceException(
					"This Dashboard is shared within a Project, revoke access from project prior to deleting");
		dashboardUserAccessRepository.deleteByDashboard(d);
		return d.getId();

	}

	@Override
	public boolean hasUserPermission(String id, String userId) {
		final User user = userRepository.findByUserId(userId);
		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return true;
		} else {
			return dashboardRepository.findById(id).getUser().getUserId().equals(userId);
		}
	}

	@Override
	public boolean hasUserEditPermission(String id, String userId) {
		final User user = userRepository.findByUserId(userId);
		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return true;
		} else {
			final boolean propietary = dashboardRepository.findById(id).getUser().getUserId().equals(userId);
			if (propietary) {
				return true;
			}
			final DashboardUserAccess userAuthorization = dashboardUserAccessRepository
					.findByDashboardAndUser(dashboardRepository.findById(id), user);

			if (userAuthorization != null) {
				switch (DashboardUserAccessType.Type
						.valueOf(userAuthorization.getDashboardUserAccessType().getName())) {
				case EDIT:
					return true;
				case VIEW:
				default:
					return false;
				}
			} else {
				return resourceService.hasAccess(userId, id, ResourceAccessType.MANAGE);
			}

		}
	}

	@Override
	public boolean hasUserViewPermission(String id, String userId) {
		final User user = userRepository.findByUserId(userId);

		if (dashboardRepository.findById(id).isPublic()) {
			return true;
		} else if (userId.equals(ANONYMOUSUSER) || user == null) {
			return dashboardRepository.findById(id).isPublic();
		} else if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return true;
		} else {
			final boolean propietary = dashboardRepository.findById(id).getUser().getUserId().equals(userId);
			if (propietary) {
				return true;
			}
			final DashboardUserAccess userAuthorization = dashboardUserAccessRepository
					.findByDashboardAndUser(dashboardRepository.findById(id), user);

			if (userAuthorization != null) {
				switch (DashboardUserAccessType.Type
						.valueOf(userAuthorization.getDashboardUserAccessType().getName())) {
				case EDIT:
					return true;
				case VIEW:
					return true;
				default:
					return false;
				}
			} else {
				return resourceService.hasAccess(userId, id, ResourceAccessType.VIEW);
			}

		}
	}

	public String getUserTypePermissionForDashboard(Dashboard dashboard, User user) {

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return DashboardUserAccessType.Type.EDIT.toString();
		} else {

			if (dashboard.getUser().getUserId().equals(user.getUserId())) {
				return DashboardUserAccessType.Type.EDIT.toString();
			}
			final DashboardUserAccess userAuthorization = dashboardUserAccessRepository
					.findByDashboardAndUser(dashboard, user);

			if (userAuthorization != null) {
				switch (DashboardUserAccessType.Type
						.valueOf(userAuthorization.getDashboardUserAccessType().getName())) {
				case EDIT:
					return DashboardUserAccessType.Type.EDIT.toString();
				case VIEW:
					return DashboardUserAccessType.Type.VIEW.toString();
				default:
					return DashboardUserAccessType.Type.VIEW.toString();
				}
			} else {
				if (resourceService.getResourceAccess(user.getUserId(), dashboard.getId()) != null) {
					switch (resourceService.getResourceAccess(user.getUserId(), dashboard.getId())) {
					case MANAGE:
						return DashboardUserAccessType.Type.EDIT.toString();
					case VIEW:
					default:
						return DashboardUserAccessType.Type.VIEW.toString();
					}
				} else
					return DashboardUserAccessType.Type.VIEW.toString();

			}

		}
	}

	@Override
	public void saveDashboard(String id, Dashboard dashboard, String userId) {
		if (hasUserEditPermission(id, userId)) {
			final Dashboard dashboardEnt = dashboardRepository.findById(dashboard.getId());
			dashboardEnt.setCustomcss(dashboard.getCustomcss());
			dashboardEnt.setCustomjs(dashboard.getCustomjs());
			dashboardEnt.setDescription(dashboard.getDescription());
			dashboardEnt.setJsoni18n(dashboard.getJsoni18n());
			dashboardEnt.setModel(dashboard.getModel());
			dashboardEnt.setPublic(dashboard.isPublic());
			dashboardEnt.setHeaderlibs(dashboard.getHeaderlibs());
			dashboardEnt.setType(dashboard.getType());
			dashboardRepository.save(dashboardEnt);
		} else
			throw new DashboardServiceException("Cannot update Dashboard that does not exist or don't have permission");
	}

	@Override
	public void saveDashboardModel(String id, String model, String userId) {
		if (hasUserEditPermission(id, userId)) {
			final Dashboard dashboardEnt = dashboardRepository.findById(id);
			dashboardEnt.setModel(model);

			dashboardRepository.save(dashboardEnt);
		} else
			throw new DashboardServiceException("Cannot update Dashboard that does not exist or don't have permission");
	}

	@Override
	public Dashboard getDashboardById(String id, String userId) {
		return dashboardRepository.findById(id);
	}

	@Override
	public Dashboard getDashboardByIdentification(String identification, String userId) {
		if (!dashboardRepository.findByIdentification(identification).isEmpty())
			return dashboardRepository.findByIdentification(identification).get(0);
		else
			return null;
	}

	@Override
	public Dashboard getDashboardEditById(String id, String userId) {
		if (hasUserEditPermission(id, userId)) {
			return dashboardRepository.findById(id);
		}
		throw new DashboardServiceException("Cannot view Dashboard that does not exist or don't have permission");
	}

	@Override
	public String getCredentialsString(String userId) {
		final User user = userRepository.findByUserId(userId);
		return userId;
	}

	@Override
	public boolean dashboardExists(String identification) {
		if (dashboardRepository.findByIdentification(identification).size() != 0)
			return true;
		else
			return false;
	}

	@Override
	public boolean dashboardExistsById(String id) {
		Dashboard dash = dashboardRepository.findById(id);
		if (dash != null && dash.getId().length() != 0)
			return true;
		return false;
	}

	@Override
	public String cloneDashboard(Dashboard originalDashboard, String identification, User user) {
		Dashboard cloneDashboard = new Dashboard();

		try {

			cloneDashboard.setIdentification(identification);
			cloneDashboard.setUser(user);
			cloneDashboard.setCustomcss(originalDashboard.getCustomcss());
			cloneDashboard.setCustomjs(originalDashboard.getCustomjs());
			cloneDashboard.setDescription(originalDashboard.getDescription());
			cloneDashboard.setHeaderlibs(originalDashboard.getHeaderlibs());
			cloneDashboard.setImage(originalDashboard.getImage());
			cloneDashboard.setPublic(originalDashboard.isPublic());
			cloneDashboard.setJsoni18n(originalDashboard.getJsoni18n());
			cloneDashboard.setModel(originalDashboard.getModel());
			cloneDashboard.setType(originalDashboard.getType());

			dashboardRepository.save(cloneDashboard);

			return cloneDashboard.getId();
		} catch (final Exception e) {

			log.error(e.getMessage());
			return null;
		}
	}

	@Override
	public String createNewDashboard(DashboardCreateDTO dashboard, String userId) {

		User sessionUser = userRepository.findByUserId(userId);
		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_USER.toString())
				|| sessionUser.getRole().getId().equals(Role.Type.ROLE_DATAVIEWER.toString())) {
			throw new DashboardServiceException(DASH_CREATE_AUTH_EXCEPT);
		}

		if (!dashboardExists(dashboard.getIdentification())) {

			log.debug("Dashboard no exist, creating...");
			final Dashboard d = new Dashboard();
			d.setCustomcss("");
			d.setCustomjs("");
			d.setJsoni18n("");
			try {
				if (null != dashboard.getImage() && !dashboard.getImage().isEmpty()) {
					d.setImage(dashboard.getImage().getBytes());
				} else {
					d.setImage(null);
				}
			} catch (final IOException e) {

				log.error(e.getMessage());

			}
			d.setDescription(dashboard.getDescription());
			d.setIdentification(dashboard.getIdentification());
			d.setPublic(dashboard.getPublicAccess());
			d.setUser(userRepository.findByUserId(userId));
			d.setHeaderlibs(dashboard.getHeaderlibs());
			d.setType(dashboard.getType());
			String model = null;
			if (dashboard.getDashboardConfId() == null) {
				List<DashboardConf> dashConfList = dashboardConfRepository.findByIdentification("default");
				for (Iterator<DashboardConf> iterator = dashConfList.iterator(); iterator.hasNext();) {
					DashboardConf dashConf = iterator.next();
					model = dashConf.getModel();
					break;
				}
			} else {
				DashboardConf dashConf = dashboardConfRepository.findById(dashboard.getDashboardConfId());
				model = dashConf.getModel();
			}
			d.setModel(model);

			final Dashboard dAux = dashboardRepository.save(d);

			if (dashboard.getCategory() != null && dashboard.getSubcategory() != null
					&& !dashboard.getCategory().isEmpty() && !dashboard.getSubcategory().isEmpty()) {

				Category category = categoryRepository.findByIdentification(dashboard.getCategory()).get(0);
				Subcategory subcategory = subcategoryRepository.findByIdentification(dashboard.getSubcategory()).get(0);

				if (category != null && subcategory != null) {
					CategoryRelation categoryRelationAux = categoryRelationRepository.findByTypeId(d.getId());

					if (categoryRelationAux != null) {
						log.error("Category {} and Subcategory {} already exist in this dashboard",
								category.getIdentification(), subcategory.getIdentification());
						throw new DashboardServiceException(CATEGORY_SUBCATEGORY_EXIST);
					}

					final CategoryRelation categoryRelation = new CategoryRelation();
					categoryRelation.setCategory(category.getId());
					categoryRelation.setSubcategory(subcategory.getId());
					categoryRelation.setType(CategoryRelation.Type.DASHBOARD);
					categoryRelation.setTypeId(dAux.getId());

					categoryRelationRepository.save(categoryRelation);
				} else {
					log.error("Category {} and Subcategory {} not found", category.getIdentification(),
							subcategory.getIdentification());
					throw new DashboardServiceException(CATEGORY_SUBCATEGORY_NOTFOUND);
				}

			}

			final ObjectMapper objectMapper = new ObjectMapper();

			try {
				if (dashboard.getAuthorizations() != null) {
					final List<DashboardAccessDTO> access = objectMapper.readValue(dashboard.getAuthorizations(),
							objectMapper.getTypeFactory().constructCollectionType(List.class,
									DashboardAccessDTO.class));
					for (final Iterator<DashboardAccessDTO> iterator = access.iterator(); iterator.hasNext();) {
						final DashboardAccessDTO dashboardAccessDTO = iterator.next();
						final DashboardUserAccess dua = new DashboardUserAccess();
						dua.setDashboard(d);
						final List<DashboardUserAccessType> managedTypes = dashboardUserAccessTypeRepository
								.findByName(dashboardAccessDTO.getAccesstypes());
						final DashboardUserAccessType managedType = managedTypes != null && managedTypes.size() > 0
								? managedTypes.get(0)
								: null;
						dua.setDashboardUserAccessType(managedType);
						dua.setUser(userRepository.findByUserId(dashboardAccessDTO.getUsers()));
						dashboardUserAccessRepository.save(dua);
					}
				}

			} catch (final JsonParseException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			} catch (final JsonMappingException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			} catch (final IOException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			}

			return d.getId();
		} else
			throw new DashboardServiceException("Dashboard already exists in Database");
	}

	@Override
	public List<DashboardUserAccess> getDashboardUserAccesses(String dashboardId) {
		final Dashboard dashboard = dashboardRepository.findById(dashboardId);
		final List<DashboardUserAccess> authorizations = dashboardUserAccessRepository.findByDashboard(dashboard);
		return authorizations;
	}

	@Transactional
	@Override
	public String cleanDashboardAccess(DashboardCreateDTO dashboard, String userId) {
		if (!dashboardExistsById(dashboard.getId())) {
			throw new DashboardServiceException(DASH_NOT_EXIST);
		} else {

			final Dashboard d = dashboardRepository.findById(dashboard.getId());
			dashboardUserAccessRepository.deleteByDashboard(d);
			return d.getId();

		}
	}

	@Transactional
	@Override
	public String saveUpdateAccess(DashboardCreateDTO dashboard, String userId) {
		if (!dashboardExistsById(dashboard.getId())) {
			throw new DashboardServiceException(DASH_NOT_EXIST);
		} else {

			final Dashboard d = dashboardRepository.findById(dashboard.getId());
			final ObjectMapper objectMapper = new ObjectMapper();

			try {
				if (dashboard.getAuthorizations() != null) {
					final List<DashboardAccessDTO> access = objectMapper.readValue(dashboard.getAuthorizations(),
							objectMapper.getTypeFactory().constructCollectionType(List.class,
									DashboardAccessDTO.class));
					for (final Iterator iterator = access.iterator(); iterator.hasNext();) {
						final DashboardAccessDTO dashboardAccessDTO = (DashboardAccessDTO) iterator.next();
						final DashboardUserAccess dua = new DashboardUserAccess();
						dua.setDashboard(dashboardRepository.findById(dashboard.getId()));
						final List<DashboardUserAccessType> managedTypes = dashboardUserAccessTypeRepository
								.findByName(dashboardAccessDTO.getAccesstypes());
						final DashboardUserAccessType managedType = managedTypes != null && managedTypes.size() > 0
								? managedTypes.get(0)
								: null;
						dua.setDashboardUserAccessType(managedType);
						dua.setUser(userRepository.findByUserId(dashboardAccessDTO.getUsers()));
						dashboardUserAccessRepository.save(dua);
					}
				}
				return d.getId();

			} catch (final JsonParseException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			} catch (final JsonMappingException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			} catch (final IOException e) {
				throw new DashboardServiceException(AUTH_PARSE_EXCEPT);
			}

		}
	}

	@Transactional
	@Override
	public String updatePublicDashboard(DashboardCreateDTO dashboard, String userId) {
		if (!dashboardExistsById(dashboard.getId())) {
			throw new DashboardServiceException(DASH_NOT_EXIST);
		} else {
			final Dashboard d = dashboardRepository.findById(dashboard.getId());
			d.setPublic(dashboard.getPublicAccess());
			d.setDescription(dashboard.getDescription());
			d.setHeaderlibs(dashboard.getHeaderlibs());
			d.setIdentification(dashboard.getIdentification());
			try {
				if (dashboard.getImage() != null && !dashboard.getImage().isEmpty()) {
					d.setImage(dashboard.getImage().getBytes());
				} else {
					d.setImage(null);
				}
			} catch (final IOException e) {
				log.error(e.getMessage());
			}
			final Dashboard dAux = dashboardRepository.save(d);

			if (dashboard.getCategory() != null && dashboard.getSubcategory() != null
					&& !dashboard.getCategory().isEmpty() && !dashboard.getSubcategory().isEmpty()) {

				CategoryRelation categoryRelation = categoryRelationRepository.findByTypeId(d.getId());

				if (categoryRelation == null) {
					categoryRelation = new CategoryRelation();
				}

				categoryRelation
						.setCategory(categoryRepository.findByIdentification(dashboard.getCategory()).get(0).getId());
				categoryRelation.setSubcategory(
						subcategoryRepository.findByIdentification(dashboard.getSubcategory()).get(0).getId());
				categoryRelation.setType(CategoryRelation.Type.DASHBOARD);
				categoryRelation.setTypeId(dAux.getId());

				categoryRelationRepository.save(categoryRelation);

			}

			return d.getId();
		}
	}

	@Override
	public byte[] getImgBytes(String id) {
		final Dashboard d = dashboardRepository.findById(id);

		final byte[] buffer = d.getImage();

		return buffer;
	}

	@Override
	public List<Dashboard> getByUserId(String userId) {
		final User sessionUser = userRepository.findByUserId(userId);
		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return dashboardRepository.findAllByOrderByIdentificationAsc();
		} else {
			return dashboardRepository.findByUserOrderByIdentificationAsc(sessionUser);
		}
	}

	@Transactional
	@Override
	public void updateDashboardSimplified(String identification, DashboardSimplifiedDTO dashboard, String userId) {
		if (hasUserEditPermission(identification, userId)) {
			final Dashboard dashboardEnt = dashboardRepository.findById(identification);
			dashboardEnt.setDescription(dashboard.getDescription());
			dashboardEnt.setIdentification(dashboard.getIdentification());
			dashboardRepository.save(dashboardEnt);
		} else {
			throw new DashboardServiceException("Cannot update Dashboard that does not exist or don't have permission");
		}
	}

	@Override
	public List<Dashboard> getByUserIdOrdered(String userId, DashboardOrder order) {
		final User sessionUser = userRepository.findByUserId(userId);
		if (sessionUser.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			return dashboardRepository.findAllByOrderByIdentificationAsc();
		}
		switch (order) {
		case createdAtAsc:
			return dashboardRepository.findByUserOrderByCreatedAtAsc(sessionUser);
		case createdAtDesc:
			return dashboardRepository.findByUserOrderByCreatedAtDesc(sessionUser);
		case modifiedAtAsc:
			return dashboardRepository.findByUserOrderByUpdatedAtAsc(sessionUser);
		case modifiedAtDesc:
			return dashboardRepository.findByUserOrderByUpdatedAtDesc(sessionUser);
		case identificationDesc:
			return dashboardRepository.findByUserOrderByIdentificationDesc(sessionUser);
		default:
		case identificationAsc:
			return dashboardRepository.findByUserOrderByIdentificationAsc(sessionUser);
		}

	}

	@Override
	public int getNumGadgets(Dashboard dashboard) {
		String model = dashboard.getModel();
		int ngadgets = 0;
		try {
			final Map<String, Object> obj = objectMapper.readValue(model, new TypeReference<Map<String, Object>>() {
			});
			if (obj.containsKey("pages")) {
				final ArrayList<Object> pages = (ArrayList<Object>) obj.get("pages");
				int npages = pages.size();
				for (int i = 0; i < npages; i++) {
					final Map<String, Object> page = (Map<String, Object>) pages.get(i);
					final ArrayList<Object> layers = (ArrayList<Object>) page.get("layers");
					final Map<String, Object> layer = (Map<String, Object>) layers.get(0);
					final ArrayList<Object> gridboard = (ArrayList<Object>) layer.get("gridboard");
					ngadgets += gridboard.size();
				}
				ngadgets -= 1;
			}
		} catch (final JsonParseException e) {
			log.error("Json parse exception", e);
		} catch (JsonMappingException e) {
			log.error("Json mapping exception", e);
		} catch (IOException e) {
			log.error("IO exception", e);
		}

		return ngadgets;
	}

	@Override
	public DashboardExportDTO addGadgets(DashboardExportDTO dashboard) {
		ArrayList<String> listGadgetsID = new ArrayList<String>();
		ArrayList<String> listDatasourcesID = new ArrayList<String>();
		ArrayList<String> listGadgetMeasuresID = new ArrayList<String>();
		try {
			final Map<String, Object> obj = objectMapper.readValue(dashboard.getModel(),
					new TypeReference<Map<String, Object>>() {
					});
			if (obj.containsKey("pages")) {
				final ArrayList<Object> pages = (ArrayList<Object>) obj.get("pages");
				int npages = pages.size();
				for (int i = 0; i < npages; i++) {
					final Map<String, Object> page = (Map<String, Object>) pages.get(i);
					final ArrayList<Object> layers = (ArrayList<Object>) page.get("layers");
					final Map<String, Object> layer = (Map<String, Object>) layers.get(0);
					final ArrayList<Object> gridboard = (ArrayList<Object>) layer.get("gridboard");
					int ngadgets = gridboard.size();
					for (int j = 0; j < ngadgets; j++) {
						final Map<String, Object> gadget = (Map<String, Object>) gridboard.get(j);
						if (gadget.containsKey("datasource")) {
							final Map<String, Object> datasource = (Map<String, Object>) gadget.get("datasource");
							final String datasourceId = (String) datasource.get("id");
							listDatasourcesID.add(datasourceId);
						} else if (gadget.containsKey("id")) {
							final String gadgetId = (String) gadget.get("id");
							listGadgetsID.add(gadgetId);
							List<GadgetMeasure> gadgetMeasures = gadgetMeasureRepository
									.findByGadget(gadgetRepository.findById(gadgetId));
							if (gadgetMeasures.size() > 0) {
								for (GadgetMeasure gadgetMeasure : gadgetMeasures) {
									listGadgetMeasuresID.add(gadgetMeasure.getId());
								}
								String datasource = gadgetMeasureRepository
										.findByGadget(gadgetRepository.findById(gadgetId)).get(0).getDatasource()
										.getId();
								listDatasourcesID.add(datasource);
							}
						}

					}
				}
			}
		} catch (final JsonParseException e) {
			log.error("Json parse exception", e);
		} catch (JsonMappingException e) {
			log.error("Json mapping exception", e);
		} catch (IOException e) {
			log.error("IO exception", e);
		}

		final ArrayList<GadgetDTO> gadgetsDTO = new ArrayList<GadgetDTO>();
		for (String gadgetId : listGadgetsID) {
			if (gadgetToDTO(gadgetId) != null)
				gadgetsDTO.add(gadgetToDTO(gadgetId));
		}
		final ArrayList<GadgetDatasourceDTO> datasourcesDTO = new ArrayList<GadgetDatasourceDTO>();
		for (String gadgetDsId : listDatasourcesID) {
			if (gadgetDatasourceToDTO(gadgetDsId) != null)
				datasourcesDTO.add(gadgetDatasourceToDTO(gadgetDsId));
		}
		final ArrayList<GadgetMeasureDTO> gadgetMeasuresDTO = new ArrayList<GadgetMeasureDTO>();
		for (String gadgetMeasureId : listGadgetMeasuresID) {
			if (gadgetMeasureToDTO(gadgetMeasureId) != null)
				gadgetMeasuresDTO.add(gadgetMeasureToDTO(gadgetMeasureId));
		}

		dashboard.setGadgets(gadgetsDTO);
		dashboard.setGadgetDatasources(datasourcesDTO);
		dashboard.setGadgetMeasures(gadgetMeasuresDTO);

		return dashboard;
	}

	@Override
	public String importDashboard(DashboardExportDTO dashboardimportDTO, String userId) {
		Dashboard dashboard = new Dashboard();
		if (!dashboardExists(dashboardimportDTO.getIdentification())) {
			dashboard.setIdentification(dashboardimportDTO.getIdentification());
			String description = "";
			if (dashboardimportDTO.getDescription() != null) {
				description = dashboardimportDTO.getDescription();
			}
			dashboard.setDescription(description);
			dashboard.setPublic(dashboardimportDTO.isPublic());
			dashboard.setHeaderlibs(dashboardimportDTO.getHeaderlibs());
			dashboard.setCreatedAt(dashboardimportDTO.getCreatedAt());
			dashboard.setUpdatedAt(dashboardimportDTO.getModifiedAt());
			dashboard.setUser(userRepository.findByUserId(userId));
			dashboard.setCustomcss("");
			dashboard.setCustomjs("");
			dashboard.setJsoni18n("");
			dashboard.setModel(dashboardimportDTO.getModel());

			final Dashboard dAux = dashboardRepository.save(dashboard);

			if (dashboardimportDTO.getCategory() != null && dashboardimportDTO.getSubcategory() != null
					&& !dashboardimportDTO.getCategory().isEmpty() && !dashboardimportDTO.getSubcategory().isEmpty()) {

				Category category = categoryRepository.findByIdentification(dashboardimportDTO.getCategory()).get(0);
				Subcategory subcategory = subcategoryRepository
						.findByIdentification(dashboardimportDTO.getSubcategory()).get(0);

				if (category != null && subcategory != null) {
					CategoryRelation categoryRelationAux = categoryRelationRepository.findByTypeId(dashboard.getId());

					if (categoryRelationAux != null) {
						log.error("Category {} and Subcategory {} already exist in this dashboard",
								category.getIdentification(), subcategory.getIdentification());
						throw new DashboardServiceException(CATEGORY_SUBCATEGORY_EXIST);
					}

					final CategoryRelation categoryRelation = new CategoryRelation();
					categoryRelation.setCategory(category.getId());
					categoryRelation.setSubcategory(subcategory.getId());
					categoryRelation.setType(CategoryRelation.Type.DASHBOARD);
					categoryRelation.setTypeId(dAux.getId());

					categoryRelationRepository.save(categoryRelation);
				} else {
					log.error("Category {} and Subcategory {} not found", category.getIdentification(),
							subcategory.getIdentification());
					throw new DashboardServiceException(CATEGORY_SUBCATEGORY_NOTFOUND);
				}

			}

			// include DASH_AUTHS
			for (DashboardUserAccessDTO dashboardUADTO : dashboardimportDTO.getDashboardAuths()) {
				DashboardUserAccess dashboardUA = new DashboardUserAccess();
				dashboardUA.setDashboard(dashboardRepository.findById(dashboard.getId()));
				final List<DashboardUserAccessType> managedTypes = dashboardUserAccessTypeRepository
						.findByName(dashboardUADTO.getAccessType());
				final DashboardUserAccessType managedType = managedTypes != null && managedTypes.size() > 0
						? managedTypes.get(0)
						: null;
				dashboardUA.setDashboardUserAccessType(managedType);
				dashboardUA.setUser(userRepository.findByUserId(dashboardUADTO.getUserId()));

				dashboardUserAccessRepository.save(dashboardUA);
			}

			// include GADGETS
			for (GadgetDTO gadgetDTO : dashboardimportDTO.getGadgets()) {
				Gadget gadget = new Gadget();
				if (gadgetRepository.findById(gadgetDTO.getId()) == null) {
					gadget.setId(gadgetDTO.getId());
					gadget.setConfig(gadgetDTO.getConfig());
					gadget.setDescription(gadgetDTO.getDescription());
					gadget.setIdentification(gadgetDTO.getIdentification());
					gadget.setPublic(gadgetDTO.isPublic());
					gadget.setType(gadgetDTO.getType());
					gadget.setUser(userRepository.findByUserId(userId));

					gadgetRepository.save(gadget);
				}
			}

			// include GADGET_DATASOURCES
			for (GadgetDatasourceDTO gadgetDSDTO : dashboardimportDTO.getGadgetDatasources()) {
				GadgetDatasource gadgetDS = new GadgetDatasource();
				if (gadgetDatasourceRepository.findById(gadgetDSDTO.getId()) == null) {
					gadgetDS.setId(gadgetDSDTO.getId());
					gadgetDS.setConfig(gadgetDSDTO.getConfig());
					gadgetDS.setDbtype(gadgetDSDTO.getDbtype());
					gadgetDS.setDescription(gadgetDSDTO.getDescription());
					gadgetDS.setIdentification(gadgetDSDTO.getIdentification());
					gadgetDS.setMaxvalues(gadgetDSDTO.getMaxvalues());
					gadgetDS.setMode(gadgetDSDTO.getMode());
					gadgetDS.setQuery(gadgetDSDTO.getQuery());
					gadgetDS.setRefresh(gadgetDSDTO.getRefresh());
					OntologyDTO oDTO = gadgetDSDTO.getOntology();
					if (oDTO.getIdentification() != null
							&& ontologyRepository.findByIdentification(oDTO.getIdentification()) != null)
						gadgetDS.setOntology(ontologyRepository.findByIdentification(oDTO.getIdentification()));
					else
						gadgetDS.setOntology(null);
					gadgetDS.setUser(userRepository.findByUserId(userId));
					gadgetDatasourceRepository.save(gadgetDS);
				}
			}

			// include GADGET_MEASURES
			for (GadgetMeasureDTO gadgetMeasureDTO : dashboardimportDTO.getGadgetMeasures()) {
				GadgetMeasure gadgetMeasure = new GadgetMeasure();
				if (gadgetMeasureRepository.findById(gadgetMeasureDTO.getId()).isEmpty()) {
					gadgetMeasure.setId(gadgetMeasureDTO.getId());
					gadgetMeasure.setConfig(gadgetMeasureDTO.getConfig());
					gadgetMeasure.setGadget(gadgetRepository.findById(gadgetMeasureDTO.getGadget().getId()));
					gadgetMeasure.setDatasource(
							gadgetDatasourceRepository.findById(gadgetMeasureDTO.getDatasource().getId()));

					gadgetMeasureRepository.save(gadgetMeasure);
				}
			}

			final String dashboardId = dashboardRepository.findByIdentification(dashboard.getIdentification()).get(0)
					.getIdentification();
			return dashboardId;
		} else {
			return "";
		}

	}

	@Override
	public String getElementsAssociated(String dashboardId) {
		JSONArray elements = new JSONArray();
		JSONObject element = new JSONObject();
		Dashboard dashboard = dashboardRepository.findById(dashboardId);
		List<String> added = new ArrayList<String>();

		try {
			final Map<String, Object> obj = objectMapper.readValue(dashboard.getModel(),
					new TypeReference<Map<String, Object>>() {
					});
			if (obj.containsKey("pages")) {
				final ArrayList<Object> pages = (ArrayList<Object>) obj.get("pages");
				int npages = pages.size();
				for (int i = 0; i < npages; i++) {
					final Map<String, Object> page = (Map<String, Object>) pages.get(i);
					final ArrayList<Object> layers = (ArrayList<Object>) page.get("layers");
					final Map<String, Object> layer = (Map<String, Object>) layers.get(0);
					final ArrayList<Object> gridboard = (ArrayList<Object>) layer.get("gridboard");
					int ngadgets = gridboard.size();
					for (int j = 0; j < ngadgets; j++) {
						final Map<String, Object> gadget = (Map<String, Object>) gridboard.get(j);
						if (gadget.containsKey("datasource")) {
							final Map<String, Object> datasource = (Map<String, Object>) gadget.get("datasource");
							final String datasourceId = (String) datasource.get("id");
							GadgetDatasource datasourceObj = gadgetDatasourceRepository.findById(datasourceId);
							element = new JSONObject();
							element.put("id",datasourceObj.getId());
							element.put("identification",datasourceObj.getIdentification());
							element.put("type",datasourceObj.getClass().getSimpleName());
							added.add(datasourceObj.getId());
							elements.put(element);
						} else if (gadget.containsKey("id") && !added.contains(gadget.get("id").toString()) &&
								gadgetRepository.findById(gadget.get("id").toString()) != null) {
							final Gadget gadgetObj = gadgetRepository.findById(gadget.get("id").toString());
							element = new JSONObject();
							element.put("id",gadgetObj.getId());
							element.put("identification",gadgetObj.getIdentification());
							element.put("type",gadgetObj.getClass().getSimpleName());
							added.add(gadgetObj.getId());
							elements.put(element);
							List<GadgetMeasure> gadgetMeasures = gadgetMeasureRepository
									.findByGadget(gadgetRepository.findById(gadgetObj.getId()));
							if (gadgetMeasures.size() > 0) {
								GadgetDatasource datasourceObj = gadgetMeasureRepository
										.findByGadget(gadgetRepository.findById(gadgetObj.getId())).get(0).getDatasource();
								if (!added.contains(datasourceObj.getId())) {
									element = new JSONObject();
									element.put("id",datasourceObj.getId());
									element.put("identification",datasourceObj.getIdentification());
									element.put("type",datasourceObj.getClass().getSimpleName());
									added.add(datasourceObj.getId());
									elements.put(element);
								}
								if (!added.contains(datasourceObj.getOntology().getId())) {
									element = new JSONObject();
									element.put("id",datasourceObj.getOntology().getId());
									element.put("identification",datasourceObj.getOntology().getIdentification());
									element.put("type",datasourceObj.getOntology().getClass().getSimpleName());
									added.add(datasourceObj.getOntology().getId());
									elements.put(element);
								}
							}
						}
					}
				}
			}
		} catch (final JsonParseException e) {
			log.error("Json parse exception", e);
		} catch (JsonMappingException e) {
			log.error("Json mapping exception", e);
		} catch (IOException e) {
			log.error("IO exception", e);
		}
		return elements.toString();
	}

	private GadgetDTO gadgetToDTO(String gadgetId) {
		Gadget gadget = gadgetRepository.findById(gadgetId);
		GadgetDTO gDto = new GadgetDTO();
		if (gadget != null) {
			gDto.setId(gadget.getId());
			gDto.setConfig(gadget.getConfig());
			gDto.setIdentification(gadget.getIdentification());
			gDto.setPublic(gadget.isPublic());
			gDto.setType(gadget.getType());
			gDto.setDescription(gadget.getDescription());
			return gDto;
		}
		return null;
	}

	private GadgetDatasourceDTO gadgetDatasourceToDTO(String gadgetDSId) {
		GadgetDatasource gadgetds = gadgetDatasourceRepository.findById(gadgetDSId);
		GadgetDatasourceDTO gDto = new GadgetDatasourceDTO();
		if (gadgetds != null) {
			gDto.setId(gadgetds.getId());
			gDto.setConfig(gadgetds.getConfig());
			gDto.setDescription(gadgetds.getDescription());
			gDto.setIdentification(gadgetds.getIdentification());
			gDto.setDbtype(gadgetds.getDbtype());
			gDto.setMaxvalues(gadgetds.getMaxvalues());
			gDto.setQuery(gadgetds.getQuery());
			gDto.setMode(gadgetds.getMode());
			gDto.setRefresh(gadgetds.getRefresh());
			final OntologyDTO oDTO = new OntologyDTO();
			if (gadgetds.getOntology() != null) {
				oDTO.setIdentification(gadgetds.getOntology().getIdentification());
				oDTO.setDescription(gadgetds.getOntology().getDescription());
				oDTO.setUser(gadgetds.getOntology().getUser().getUserId());
			}
			gDto.setOntology(oDTO);
			return gDto;
		}

		return null;
	}

	private GadgetMeasureDTO gadgetMeasureToDTO(String gadgetMeasuresId) {
		GadgetMeasure gadgetMeasure = gadgetMeasureRepository.findOne(gadgetMeasuresId);
		GadgetMeasureDTO gDto = new GadgetMeasureDTO();
		if (gadgetMeasure != null) {
			gDto.setId(gadgetMeasure.getId());
			gDto.setConfig(gadgetMeasure.getConfig());
			gDto.setGadget(gadgetToDTO(gadgetMeasure.getGadget().getId()));
			gDto.setDatasource(gadgetDatasourceToDTO(gadgetMeasure.getDatasource().getId()));
			return gDto;
		}
		return null;
	}

	@Override
	public ResponseEntity<byte[]> generateImgFromDashboardId(String id, int waittime, int height, int width,
			boolean fullpage, String oauthtoken) {

		final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.IMAGE_PNG_VALUE);

		HttpEntity<?> entity = new HttpEntity<>(headers);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(String.format(toImg, dashboardexporturl))
				.queryParam("waittime", waittime).queryParam("url", prefixURLView + id).queryParam("fullpage", fullpage)
				.queryParam("width", width).queryParam("height", height).queryParam("oauthtoken", oauthtoken);

		ResponseEntity<byte[]> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity,
				byte[].class);

		return response;
	}

	@Override
	public ResponseEntity<byte[]> generatePDFFromDashboardId(String id, int waittime, int height, int width,
			String oauthtoken) {
		final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Access-Control-Allow-Methods", "GET");
		headers.add("Access-Control-Allow-Headers", "Content-Type");
		headers.add("Content-Disposition", "filename=" + id + ".pdf");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		HttpEntity<?> entity = new HttpEntity<>(headers);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(String.format(toPDF, dashboardexporturl))
				.queryParam("waittime", waittime).queryParam("url", prefixURLView + id).queryParam("width", width)
				.queryParam("height", height).queryParam("oauthtoken", oauthtoken);

		ResponseEntity<byte[]> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity,
				byte[].class);

		return response;
	}
}