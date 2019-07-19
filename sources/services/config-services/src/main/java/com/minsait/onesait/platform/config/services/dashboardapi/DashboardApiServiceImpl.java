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
package com.minsait.onesait.platform.config.services.dashboardapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.config.model.Gadget;
import com.minsait.onesait.platform.config.model.GadgetDatasource;
import com.minsait.onesait.platform.config.model.GadgetMeasure;
import com.minsait.onesait.platform.config.model.GadgetTemplate;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.CommandDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.DataDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.FilterDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.FiltersDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.GadgetDatasourceDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.GadgetTemplateDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.MeasureDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.ResponseDTO;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.SetupLayout;
import com.minsait.onesait.platform.config.services.dashboardapi.dto.TargetDTO;
import com.minsait.onesait.platform.config.services.exceptions.GadgetDatasourceServiceException;
import com.minsait.onesait.platform.config.services.gadget.GadgetDatasourceService;
import com.minsait.onesait.platform.config.services.gadget.GadgetService;
import com.minsait.onesait.platform.config.services.gadgettemplate.GadgetTemplateService;
import com.minsait.onesait.platform.config.services.ontology.OntologyService;
import com.minsait.onesait.platform.config.services.user.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardApiServiceImpl implements DashboardApiService {

	@Autowired
	private GadgetService gadgetService;

	@Autowired
	private UserService userService;

	@Autowired
	private GadgetDatasourceService gadgetDatasourceService;

	@Autowired
	private GadgetTemplateService gadgetTemplateService;

	@Autowired
	private OntologyService ontologyService;

	private final String SELECT_FROM = "select * from ";
	private final String RTDB = "RTDB";
	private final String QUERY = "query";
	private final String PIE = "pie";
	private final String BAR = "bar";
	private final String LINE = "line";
	private final String MIXED = "mixed";
	private final String RADAR = "radar";
	private final String WORDCLOUD = "wordcloud";
	private final String TABLE = "table";
	private final String MAP = "map";
	private final String LIVEHTML = "livehtml";
	private final int MAXVALUES = 1000;
	private final String[] COLORS = { "rgba(40,146,215, 0.8)", "rgba(119,178,131, 0.8)", "rgba(178,131,119, 0.8)",
			"rgba(178,161,119, 0.8)", "rgba(247,179,121, 0.8)", "rgba(139,165,160, 0.8)", "rgba(254, 246, 240, 0.8)",
			"rgba(207, 206, 229, 0.8)" };

	@Override
	public String createGadget(String json, String userId) {
		ObjectMapper mapper = new ObjectMapper();
		CommandDTO commandDTO;
		try {
			commandDTO = mapper.readValue(json, CommandDTO.class);

			// Validations

			if (commandDTO == null || commandDTO.getInformation() == null) {
				log.error("Cannot create gadget: command Malformed");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
			}
			if ((commandDTO.getInformation().getOntology() == null
					|| commandDTO.getInformation().getOntology().trim().length() == 0)
					&& (commandDTO.getInformation().getDataSource() == null
							|| commandDTO.getInformation().getDataSource().trim().length() == 0)) {
				log.error("Cannot create gadget: ontology or datasource is necessary");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly ontology or datasource is necessary\",\"data\":{}}";
			}
			if (commandDTO.getInformation().getDataSource() != null
					&& commandDTO.getInformation().getDataSource().trim().length() > 0
					&& commandDTO.getInformation().getGadgetType().equals(MAP)) {
				GadgetDatasource datasource = gadgetDatasourceService
						.getDatasourceByIdentification(commandDTO.getInformation().getDataSource());
				if (datasource == null) {
					log.error("Cannot create gadget: valid datasource is necessary");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly valid datasource is necessary\",\"data\":{}}";
				}
			}
			if (commandDTO.getInformation().getDashboard() == null
					|| commandDTO.getInformation().getDashboard().trim().length() == 0) {
				log.error("Cannot create gadget: dashboard is necessary");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly dashboard is necessary\",\"data\":{}}";
			}
			if (commandDTO.getInformation().getGadgetName() == null) {
				log.error("Cannot create gadget: gadgetName is necessary");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly gadgetName is necessary\",\"data\":{}}";
			}

			// if setupLayout == null , create a class instance
			if (commandDTO.getInformation().getSetupLayout() == null) {
				commandDTO.getInformation().setSetupLayout(new SetupLayout());
			}

			if (commandDTO.getInformation().getGadgetType() == null
					|| commandDTO.getInformation().getGadgetType().trim().length() == 0) {
				log.error("Cannot create gadget: gadgetType is necessary");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly gadgetType is necessary\",\"data\":{}}";
			}
			if (commandDTO.getInformation().getGadgetType().equals(PIE)
					|| commandDTO.getInformation().getGadgetType().equals(LINE)
					|| commandDTO.getInformation().getGadgetType().equals(RADAR)
					|| commandDTO.getInformation().getGadgetType().equals(MIXED)
					|| commandDTO.getInformation().getGadgetType().equals(BAR)) {
				// At least one measure is necessary
				if (commandDTO.getInformation().getAxes() == null) {
					log.error("Cannot create gadget: At least one measure is necessary X and Y");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly At least one measure is necessary X and Y\",\"data\":{}}";
				}
				if (commandDTO.getInformation().getAxes().getMeasuresX().size() == 0
						|| commandDTO.getInformation().getAxes().getMeasuresY().size() == 0) {
					log.error("Cannot create gadget: At least one measure is necessary X and Y");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly At least one measure is necessary X and Y\",\"data\":{}}";
				}
			}

			else if (commandDTO.getInformation().getGadgetType().equals(MAP)) {
				if (commandDTO.getInformation().getMapConf() == null
						|| commandDTO.getInformation().getMapConf().getIdentifier() == null
						|| commandDTO.getInformation().getMapConf().getLatitude() == null
						|| commandDTO.getInformation().getMapConf().getLongitude() == null
						|| commandDTO.getInformation().getMapConf().getName() == null) {
					log.error("Cannot create gadget: Information MapConf data is necessary ");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly Information MapConf data is necessary\",\"data\":{}}";
				}
				if (commandDTO.getInformation().getOntology() == null
						|| commandDTO.getInformation().getOntology().trim().length() == 0) {
					log.error("Cannot create gadget: Information MapConf data is necessary ");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly Information MapConf data is necessary\",\"data\":{}}";
				}
			} else if (commandDTO.getInformation().getGadgetType().equals(TABLE)
					|| commandDTO.getInformation().getGadgetType().equals(WORDCLOUD)) {
				if (commandDTO.getInformation().getColumns() == null
						|| commandDTO.getInformation().getColumns().size() == 0) {
					log.error("Cannot create gadget: Information Columns data is necessary ");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly Information Columns data is necessary\",\"data\":{}}";
				}
			}

			if (commandDTO.getInformation().getOntology() != null && (commandDTO.getInformation().getRefresh() == null
					|| commandDTO.getInformation().getRefresh().trim().length() == 0)) {
				log.error("Cannot create gadget: refresh is necessary");
				return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly refresh is necessary\",\"data\":{}}";
			}
			Gadget gadget = null;

			GadgetTemplate gadgetTemplate = gadgetTemplateService
					.getGadgetTemplateByIdentification(commandDTO.getInformation().getGadgetType(), userId);

			if (gadgetTemplate != null) {
				// Create gadget from template
				GadgetDatasource datasource;
				// Use exist datasource
				if (commandDTO.getInformation().getDataSource() != null
						&& commandDTO.getInformation().getDataSource().trim().length() > 0) {
					datasource = gadgetDatasourceService
							.getDatasourceByIdentification(commandDTO.getInformation().getDataSource());
				} else {
					// create new datasource
					datasource = createFromTemplate(commandDTO, userId);
				}
				ResponseDTO responseDTO = new ResponseDTO();
				responseDTO.setGadgetDatasource(mapToGadgetDatasourceDTO(datasource));
				responseDTO.setGadgetTemplate(mapToGadgetTemplateDTO(gadgetTemplate));
				responseDTO.setRequestcode("newGadget");
				responseDTO.setStatus("OK");
				responseDTO.setSetupLayout(commandDTO.getInformation().getSetupLayout());
				responseDTO.setMessage("properly created gadget");
				String id = commandDTO.getInformation().getGadgetName() + "_" + new Date().getTime();
				responseDTO.setId(id);
				responseDTO.setType(LIVEHTML);
				responseDTO.setFilters(createFiltersFromCommand(commandDTO, id));
				return mapper.writeValueAsString(responseDTO);

			} else {

				if (commandDTO.getInformation().getGadgetType().equals(MAP)) {
					gadget = createMap(commandDTO, userId);
					ResponseDTO responseDTO = new ResponseDTO();
					// responseDTO.setGadgetDatasource();
					// responseDTO.setGadgetTemplate(mapToGadgetTemplateDTO(gadgetTemplate));
					responseDTO.setRequestcode("newGadget");
					responseDTO.setStatus("OK");
					responseDTO.setSetupLayout(commandDTO.getInformation().getSetupLayout());
					responseDTO.setMessage("properly created gadget");
					responseDTO.setId(gadget.getId());
					responseDTO.setType(gadget.getType());
					responseDTO.setFilters(createFiltersFromCommand(commandDTO, gadget.getId()));
					return mapper.writeValueAsString(responseDTO);
				} else if (commandDTO.getInformation().getGadgetType().equals(PIE)
						|| commandDTO.getInformation().getGadgetType().equals(LINE)
						|| commandDTO.getInformation().getGadgetType().equals(BAR)
						|| commandDTO.getInformation().getGadgetType().equals(MIXED)
						|| commandDTO.getInformation().getGadgetType().equals(RADAR)
						|| commandDTO.getInformation().getGadgetType().equals(TABLE)
						|| commandDTO.getInformation().getGadgetType().equals(WORDCLOUD)) {
					gadget = createGadgetAndMeasures(commandDTO, userId);
					ResponseDTO responseDTO = new ResponseDTO();
					// responseDTO.setGadgetDatasource();
					// responseDTO.setGadgetTemplate(mapToGadgetTemplateDTO(gadgetTemplate));
					responseDTO.setRequestcode("newGadget");
					responseDTO.setStatus("OK");
					responseDTO.setSetupLayout(commandDTO.getInformation().getSetupLayout());
					responseDTO.setMessage("properly created gadget");
					responseDTO.setId(gadget.getId());
					responseDTO.setType(gadget.getType());
					responseDTO.setFilters(createFiltersFromCommand(commandDTO, gadget.getId()));
					return mapper.writeValueAsString(responseDTO);
				} else {
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly, type not valid\",\"data\":{}}";
				}
			}

		} catch (IOException e1) {
			log.error("Cannot create gadget", e1);
			return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
		} catch (final GadgetDatasourceServiceException e) {
			log.error("Cannot create gadget", e);
			return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
		}
	}

	@Override
	public String updateGadget(String json, String userId) {
		ObjectMapper mapper = new ObjectMapper();
		CommandDTO commandDTO;
		try {
			commandDTO = mapper.readValue(json, CommandDTO.class);

			// Validations

			if (commandDTO == null || commandDTO.getInformation() == null) {
				log.error("Cannot update gadget: command Malformed");
				return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not updated correctly\",\"data\":{}}";
			}

			if (commandDTO.getInformation().getDashboard() == null
					|| commandDTO.getInformation().getDashboard().trim().length() == 0) {
				log.error("Cannot update gadget: dashboard is necessary");
				return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not updated correctly\",\"data\":{}}";
			}

			if (commandDTO.getInformation().getGadgetId() == null) {
				log.error("Cannot update gadget: id is necessary");
				return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not updated correctly\",\"data\":{}}";
			}

			if (commandDTO.getInformation().getGadgetType().equals(PIE)
					|| commandDTO.getInformation().getGadgetType().equals(LINE)
					|| commandDTO.getInformation().getGadgetType().equals(RADAR)
					|| commandDTO.getInformation().getGadgetType().equals(MIXED)
					|| commandDTO.getInformation().getGadgetType().equals(BAR)) {
				// At least one measure is necessary
				if (commandDTO.getInformation().getAxes() == null) {
					log.error("Cannot update gadget: At least one measure is necessary X and Y");
					return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not updated correctly\",\"data\":{}}";
				}
				if (commandDTO.getInformation().getAxes().getMeasuresX().size() == 0
						|| commandDTO.getInformation().getAxes().getMeasuresY().size() == 0) {
					log.error("Cannot update gadget: At least one measure is necessary X and Y");
					return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not updated correctly\",\"data\":{}}";
				}
			} else if (commandDTO.getInformation().getGadgetType().equals(MAP)) {
				if (commandDTO.getInformation().getMapConf() == null
						|| commandDTO.getInformation().getMapConf().getIdentifier() == null
						|| commandDTO.getInformation().getMapConf().getLatitude() == null
						|| commandDTO.getInformation().getMapConf().getLongitude() == null
						|| commandDTO.getInformation().getMapConf().getName() == null) {
					log.error("Cannot create gadget: Information MapConf data is necessary ");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
				}
			} else if (commandDTO.getInformation().getGadgetType().equals(TABLE)
					|| commandDTO.getInformation().getGadgetType().equals(WORDCLOUD)) {
				if (commandDTO.getInformation().getColumns() == null
						|| commandDTO.getInformation().getColumns().size() == 0) {
					log.error("Cannot create gadget: Information Columns data is necessary ");
					return "{\"requestcode\":\"newGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
				}
			}

			Gadget gadget = null;
			GadgetTemplate gadgetTemplate = gadgetTemplateService
					.getGadgetTemplateByIdentification(commandDTO.getInformation().getGadgetType(), userId);

			if (gadgetTemplate != null) {
				List<String> listValues = new ArrayList<String>();
				for (Iterator iterator = commandDTO.getInformation().getAxes().getMeasuresY().iterator(); iterator
						.hasNext();) {
					MeasureDTO measureDTOY = (MeasureDTO) iterator.next();
					listValues.add(measureDTOY.getPath());
				}
				return "{\"requestcode\":\"updateGadget\",\"status\":\"Template\", \"filters\":"
						+ mapper.writeValueAsString(commandDTO.getInformation().getFilters()) + ",\"merge\":"
						+ commandDTO.getInformation().isMerge() + " , \"message\":\"properly created gadget\",\"id\":\""
						+ commandDTO.getInformation().getGadgetId() + "\",\"type\":\""
						+ commandDTO.getInformation().getGadgetType() + "\"}";

			} else {
				Gadget gad = gadgetService.getGadgetById(userId, commandDTO.getInformation().getGadgetId());

				/*
				 * if (gad.getType().equals(TREND)) { gadget = updateTrend(commandDTO, userId);
				 * } else
				 */
				if (commandDTO.getInformation().getGadgetType().equals(PIE)
						|| commandDTO.getInformation().getGadgetType().equals(LINE)
						|| commandDTO.getInformation().getGadgetType().equals(BAR)
						|| commandDTO.getInformation().getGadgetType().equals(MIXED)
						|| commandDTO.getInformation().getGadgetType().equals(RADAR)
						|| commandDTO.getInformation().getGadgetType().equals(TABLE)
						|| commandDTO.getInformation().getGadgetType().equals(WORDCLOUD)) {
					gadget = updateGadgetAndMeasures(commandDTO, userId);
				} else if (gad.getType().equals(MAP)) {
					gadget = updateMap(commandDTO, userId);
				}
				return "{\"requestcode\":\"updateGadget\",\"status\":\"OK\", \"message\":\"properly created gadget\",\"id\":\""
						+ gadget.getId() + "\",\"type\":\"" + gadget.getType() + "\"}";
			}

		} catch (IOException e1) {
			log.error("Cannot update gadget", e1);
			return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
		} catch (final GadgetDatasourceServiceException e) {
			log.error("Cannot update gadget", e);
			return "{\"requestcode\":\"updateGadget\",\"status\":\"ERROR\", \"message\":\"gadget not created correctly\",\"data\":{}}";
		}
	}

	private GadgetDatasource createFromTemplate(CommandDTO commandDTO, String userId) {
		// Creation datasource
		String ontologyIdentification = commandDTO.getInformation().getOntology();
		String query = SELECT_FROM + ontologyIdentification;

		int refresh = Integer.parseInt(commandDTO.getInformation().getRefresh());

		User user = this.userService.getUser(userId);
		GadgetDatasource datasource = new GadgetDatasource();
		long time = new Date().getTime();
		datasource.setDbtype(RTDB);
		datasource.setIdentification(commandDTO.getInformation().getGadgetName() + "_" + time);
		datasource.setMaxvalues(MAXVALUES);
		datasource.setMode(QUERY);

		datasource.setRefresh(refresh);
		datasource.setQuery(query);
		datasource.setOntology(ontologyService.getOntologyByIdentification(ontologyIdentification, userId));
		datasource.setUser(user);

		datasource = this.gadgetDatasourceService.createGadgetDatasource(datasource);

		return datasource;
	}

	private GadgetDatasourceDTO mapToGadgetDatasourceDTO(GadgetDatasource datasource) {
		GadgetDatasourceDTO gdDTO = new GadgetDatasourceDTO();
		gdDTO.setId(datasource.getId());
		gdDTO.setName(datasource.getIdentification());
		gdDTO.setRefresh(datasource.getRefresh());
		gdDTO.setType(datasource.getMode());

		return gdDTO;
	}

	private GadgetTemplateDTO mapToGadgetTemplateDTO(GadgetTemplate template) {
		GadgetTemplateDTO gtDTO = new GadgetTemplateDTO();
		gtDTO.setId(template.getId());
		gtDTO.setIdentification(template.getIdentification());
		gtDTO.setUser(template.getUser().getUserId());
		gtDTO.setDescription(template.getDescription());
		gtDTO.setPublic(template.isPublic());

		return gtDTO;
	}

	private FiltersDTO[] createFiltersFromCommand(CommandDTO commandDTO, String id) throws IOException {

		if (commandDTO.getInformation().getFilters() != null && commandDTO.getInformation().getFilters().length > 0) {
			ArrayList<FiltersDTO> filters = new ArrayList<FiltersDTO>();
			for (FilterDTO filter : commandDTO.getInformation().getFilters()) {
				if (filter.getType().equals("livefilter")) {
					createLiveFilter(id, filter, filters);
				} else if (filter.getType().equals("multiselectfilter")) {
					createMultiSelectFilter(id, filter, filters);
				} else if (filter.getType().equals("textfilter")) {
					createTextFilter(id, filter, filters);
				} else if (filter.getType().equals("numberfilter")) {
					createNumberFilter(id, filter, filters);
				} else {
					throw new IOException("Filter not defined");
				}
			}
			return filters.toArray(new FiltersDTO[0]);

		}
		return null;
	}

	private void createNumberFilter(String id, FilterDTO filter, ArrayList<FiltersDTO> filters) {
		FiltersDTO numberfilter = new FiltersDTO();
		numberfilter.setId(filter.getId());
		numberfilter.setType(filter.getType());
		numberfilter.setField(filter.getField());
		numberfilter.setName(filter.getName());
		numberfilter.setOp(filter.getOp());
		numberfilter.setTypeAction("filter");
		numberfilter.setInitialFilter(filter.isInitialFilter());
		numberfilter.setUseLastValue(true);
		numberfilter.setFilterChaining(false);
		numberfilter.setValue(filter.getValue());
		numberfilter.setHide(filter.isHide());

		TargetDTO targDTO = new TargetDTO(id, filter.getId(), filter.getField());
		TargetDTO[] listTargDTO = new TargetDTO[1];
		listTargDTO[0] = targDTO;
		numberfilter.setTargetList(listTargDTO);
		filters.add(numberfilter);

	}

	private void createTextFilter(String id, FilterDTO filter, ArrayList<FiltersDTO> filters) {

		FiltersDTO textfilter = new FiltersDTO();
		textfilter.setId(filter.getId());
		textfilter.setType(filter.getType());
		textfilter.setField(filter.getField());
		textfilter.setName(filter.getName());
		textfilter.setOp(filter.getOp());
		textfilter.setTypeAction("filter");
		textfilter.setInitialFilter(filter.isInitialFilter());
		textfilter.setUseLastValue(true);
		textfilter.setFilterChaining(false);
		textfilter.setValue(filter.getValue());
		textfilter.setHide(filter.isHide());

		TargetDTO targDTO = new TargetDTO(id, filter.getId(), filter.getField());
		TargetDTO[] listTargDTO = new TargetDTO[1];
		listTargDTO[0] = targDTO;
		textfilter.setTargetList(listTargDTO);
		filters.add(textfilter);
	}

	private void createMultiSelectFilter(String id, FilterDTO filter, ArrayList<FiltersDTO> filters) {

		FiltersDTO multiselectfilter = new FiltersDTO();
		multiselectfilter.setId(filter.getId());
		multiselectfilter.setType(filter.getType());
		multiselectfilter.setField(filter.getField());
		multiselectfilter.setName(filter.getName());
		multiselectfilter.setOp("IN");
		multiselectfilter.setTypeAction("filter");
		multiselectfilter.setInitialFilter(filter.isInitialFilter());
		multiselectfilter.setUseLastValue(false);
		multiselectfilter.setFilterChaining(false);
		multiselectfilter.setValue("start");
		multiselectfilter.setHide(filter.isHide());

		DataDTO datamultiDTO = new DataDTO();
		datamultiDTO.setOptions(filter.getData().getOptions());
		datamultiDTO.setOptionsSelected(filter.getData().getOptionsSelected());
		multiselectfilter.setData(datamultiDTO);
		// setTargetList

		TargetDTO targetDTO = new TargetDTO(id, filter.getField(), filter.getField());
		TargetDTO[] listTargetDTO = new TargetDTO[1];
		listTargetDTO[0] = targetDTO;
		multiselectfilter.setTargetList(listTargetDTO);
		filters.add(multiselectfilter);
	}

	private void createLiveFilter(String id, FilterDTO filter, ArrayList<FiltersDTO> filters) {

		// Create livefilter
		FiltersDTO livefilter = new FiltersDTO();

		livefilter.setId(filter.getId());
		livefilter.setType(filter.getType());
		livefilter.setField("");
		livefilter.setName("");
		livefilter.setOp("");
		livefilter.setTypeAction("filter");
		livefilter.setInitialFilter(filter.isInitialFilter());
		livefilter.setUseLastValue(false);
		livefilter.setFilterChaining(false);
		livefilter.setValue("start");
		livefilter.setHide(filter.isHide());
		if (filter.getData() != null) {
			DataDTO dataDTO = new DataDTO();
			dataDTO.setRealtime(filter.getData().getRealtime());
			dataDTO.setSelectedPeriod(filter.getData().getSelectedPeriod());
			dataDTO.setStartDate(filter.getData().getStartDate());
			dataDTO.setEndDate(filter.getData().getEndDate());
			livefilter.setData(dataDTO);
		} else {
			DataDTO dataDTO = new DataDTO();
			dataDTO.setRealtime("start");
			dataDTO.setSelectedPeriod(8);
			dataDTO.setStartDate("NOW(\"yyyy-MM-dd\'T\'HH:mm:ss\'Z\'\",\"hour\",-8)");
			dataDTO.setEndDate("NOW(\"yyyy-MM-dd\'T\'HH:mm:ss\'Z\'\",\"hour\",0)");
			livefilter.setData(dataDTO);
		}
		// setTargetList
		TargetDTO targetDTO = new TargetDTO(id, filter.getField(), filter.getField());
		TargetDTO[] listTargetDTO = new TargetDTO[1];
		listTargetDTO[0] = targetDTO;
		livefilter.setTargetList(listTargetDTO);
		filters.add(livefilter);

	}

	private Gadget updateGadgetAndMeasures(CommandDTO commandDTO, String userId) {
		User user = this.userService.getUser(userId);
		List<GadgetMeasure> listMeasures = gadgetService.getGadgetMeasuresByGadgetId(userId,
				commandDTO.getInformation().getGadgetId());
		String idDataSource = "";
		for (Iterator iterator = listMeasures.iterator(); iterator.hasNext();) {
			GadgetMeasure gadgetMeasure = (GadgetMeasure) iterator.next();
			idDataSource = gadgetMeasure.getDatasource().getId();
			break;
		}
		Gadget gadget = gadgetService.getGadgetById(userId, commandDTO.getInformation().getGadgetId());
		List<GadgetMeasure> measures = updateGadgetMeasures(commandDTO, user);
		gadgetService.addMeasuresGadget(gadget, idDataSource, measures);
		return gadget;
	}

	private Gadget updateMap(CommandDTO commandDTO, String userId) {
		List<GadgetMeasure> listMeasures = gadgetService.getGadgetMeasuresByGadgetId(userId,
				commandDTO.getInformation().getGadgetId());
		String idDataSource = "";
		for (Iterator iterator = listMeasures.iterator(); iterator.hasNext();) {
			GadgetMeasure gadgetMeasure = (GadgetMeasure) iterator.next();
			idDataSource = gadgetMeasure.getDatasource().getId();
			break;
		}
		GadgetDatasource datasource = this.gadgetDatasourceService.getGadgetDatasourceById(idDataSource);
		String ontologyIdentification = commandDTO.getInformation().getOntology();

		String query = "select c." + commandDTO.getInformation().getMapConf().getIdentifier() + " as identifier,c."
				+ commandDTO.getInformation().getMapConf().getName() + " as name, c."
				+ commandDTO.getInformation().getMapConf().getLatitude() + " as latitude , c."
				+ commandDTO.getInformation().getMapConf().getLongitude() + " as longitude from "
				+ ontologyIdentification + " as c";

		datasource.setQuery(query);

		this.gadgetDatasourceService.updateGadgetDatasource(datasource);
		Gadget gadget = gadgetService.getGadgetById(userId, commandDTO.getInformation().getGadgetId());
		List<GadgetMeasure> measures = updateGadgetCoordinates(commandDTO);
		gadgetService.addMeasuresGadget(gadget, idDataSource, measures);
		return gadget;
	}

	private Gadget createGadgetAndMeasures(CommandDTO commandDTO, String userId) {
		// Creation datasource
		String ontologyIdentification = commandDTO.getInformation().getOntology();
		String query = SELECT_FROM + ontologyIdentification;
		// String identificationDashboard = commandDTO.getInformation().getDashboard();
		String gadgetType = commandDTO.getInformation().getGadgetType();
		int refresh = Integer.parseInt(commandDTO.getInformation().getRefresh());
		User user = this.userService.getUser(userId);
		GadgetDatasource datasource = new GadgetDatasource();
		long time = new Date().getTime();
		if (commandDTO.getInformation().getDataSource() != null
				&& commandDTO.getInformation().getDataSource().trim().length() > 0) {
			datasource = gadgetDatasourceService
					.getDatasourceByIdentification(commandDTO.getInformation().getDataSource());
		} else {
			// create new datasource
			datasource.setDbtype(RTDB);
			datasource.setIdentification(commandDTO.getInformation().getGadgetName() + "_" + time);
			datasource.setMaxvalues(MAXVALUES);
			datasource.setMode(QUERY);
			datasource.setRefresh(refresh);
			datasource.setQuery(query);
			datasource.setOntology(ontologyService.getOntologyByIdentification(ontologyIdentification, userId));
			datasource.setUser(user);
			datasource = this.gadgetDatasourceService.createGadgetDatasource(datasource);
		}
		// Creation gadget
		Gadget gadget = new Gadget();
		gadget.setIdentification(commandDTO.getInformation().getGadgetName() + "_" + time);
		// configuration depending on the type
		String configGadget = "";
		List<GadgetMeasure> measures;
		if (gadgetType.equals(PIE) || gadgetType.equals(LINE) || gadgetType.equals(BAR) || gadgetType.equals(MIXED)
				|| gadgetType.equals(RADAR)) {
			measures = createGadgetAxes(commandDTO, gadgetType, user, gadget, configGadget);
		} else {
			measures = createGadgetColumns(commandDTO, gadgetType, user, gadget, configGadget);
		}
		gadget = gadgetService.createGadget(gadget, datasource, measures);
		return gadget;
	}

	private Gadget createMap(CommandDTO commandDTO, String userId) {
		// Creation datasource
		String ontologyIdentification = commandDTO.getInformation().getOntology();

		String query = "select c." + commandDTO.getInformation().getMapConf().getIdentifier() + " as identifier,c."
				+ commandDTO.getInformation().getMapConf().getName() + " as name, c."
				+ commandDTO.getInformation().getMapConf().getLatitude() + " as latitude , c."
				+ commandDTO.getInformation().getMapConf().getLongitude() + " as longitude from "
				+ ontologyIdentification + " as c ";
		String gadgetType = commandDTO.getInformation().getGadgetType();
		int refresh = Integer.parseInt(commandDTO.getInformation().getRefresh());

		User user = this.userService.getUser(userId);
		GadgetDatasource datasource = new GadgetDatasource();
		long time = new Date().getTime();
		datasource.setDbtype(RTDB);
		datasource.setIdentification(commandDTO.getInformation().getGadgetName() + "_" + time);
		datasource.setMaxvalues(MAXVALUES);
		datasource.setMode(QUERY);
		datasource.setRefresh(refresh);
		datasource.setQuery(query);
		datasource.setOntology(ontologyService.getOntologyByIdentification(ontologyIdentification, userId));
		datasource.setUser(user);
		datasource = this.gadgetDatasourceService.createGadgetDatasource(datasource);

		// Creation gadget

		Gadget gadget = new Gadget();
		gadget.setIdentification(commandDTO.getInformation().getGadgetName() + "_" + time);
		// configuration depending on the type
		String configGadget = "";
		List<GadgetMeasure> measures = createGadgetCoordinates(commandDTO, gadgetType, user, gadget, configGadget);
		gadget = gadgetService.createGadget(gadget, datasource, measures);

		return gadget;
	}

	private List<GadgetMeasure> createGadgetAxes(CommandDTO commandDTO, String gadgetType, User user, Gadget gadget,
			String configGadget) {

		if (gadgetType != null && gadgetType.equals(PIE)) {
			configGadget = "{\"legend\":{\"display\":true,\"fullWidth\":false,\"position\":\"left\",\"labels\":{\"padding\":10,\"fontSize\":11,\"usePointStyle\":false,\"boxWidth\":1}},\"elements\":{\"arc\":{\"borderWidth\":1,\"borderColor\":\"#fff\"}},\"maintainAspectRatio\":false,\"responsive\":true,\"responsiveAnimationDuration\":500,\"circumference\":\"6.283185307179586\",\"rotation\":\"6.283185307179586\",\"charType\":\"doughnut\"}";
		} else if (gadgetType != null && gadgetType.equals(LINE)) {
			configGadget = "{\"legend\":{\"display\":true,\"fullWidth\":false,\"position\":\"top\",\"labels\":{\"padding\":10,\"fontSize\":11,\"usePointStyle\":false,\"boxWidth\":2}},\"scales\":{\"yAxes\":[{\"id\":\"#0\",\"display\":true,\"type\":\"linear\",\"position\":\"left\",\"scaleLabel\":{\"labelString\":\"\",\"display\":true,\"fontFamily\":\"Soho\",\"padding\":4},\"stacked\":false,\"sort\":true,\"ticks\":{\"suggestedMin\":\"0\",\"suggestedMax\":\"1000\",\"maxTicksLimit\":\"10\"},\"gridLines\":{\"display\":false}}],\"xAxes\":[{\"stacked\":false,\"sort\":true,\"ticks\":{\"fontFamily\":\"Soho\"},\"scaleLabel\":{\"display\":true,\"labelString\":\"\",\"fontFamily\":\"Soho\",\"padding\":4},\"hideLabel\":\"2\",\"gridLines\":{\"display\":true,\"borderDash\":[2,4],\"color\":\"#CCC\",\"zeroLineBorderDash\":[2,4],\"zeroLineColor\":\"transparent\"}}]}}";
		} else if (gadgetType != null && gadgetType.equals(BAR)) {
			configGadget = "{\"legend\":{\"display\":true,\"fullWidth\":false,\"position\":\"top\",\"labels\":{\"padding\":10,\"fontSize\":11,\"usePointStyle\":false,\"boxWidth\":2}},\"scales\":{\"yAxes\":[{\"id\":\"#0\",\"display\":true,\"type\":\"linear\",\"position\":\"left\",\"scaleLabel\":{\"labelString\":\"\",\"display\":true,\"fontFamily\":\"Soho\",\"padding\":4},\"stacked\":false,\"sort\":true,\"ticks\":{\"suggestedMin\":\"0\",\"suggestedMax\":\"1000\",\"maxTicksLimit\":\"10\"},\"gridLines\":{\"display\":false}}],\"xAxes\":[{\"stacked\":false,\"sort\":true,\"ticks\":{\"fontFamily\":\"Soho\"},\"scaleLabel\":{\"display\":true,\"labelString\":\"\",\"fontFamily\":\"Soho\",\"padding\":4},\"hideLabel\":\"2\",\"gridLines\":{\"display\":true,\"borderDash\":[2,4],\"color\":\"#CCC\",\"zeroLineBorderDash\":[2,4],\"zeroLineColor\":\"transparent\"}}]}}";
		} else if (gadgetType != null && gadgetType.equals(MIXED)) {
			configGadget = "{\"legend\":{\"display\":false,\"fullWidth\":false,\"labels\":{\"padding\":10,\"fontSize\":11,\"usePointStyle\":false,\"boxWidth\":2}},\"scales\":{\"yAxes\":[{\"id\":\"#0\",\"display\":true,\"type\":\"linear\",\"position\":\"left\",\"scaleLabel\":{\"labelString\":\"\",\"display\":true,\"fontFamily\":\"Soho\",\"padding\":4},\"stacked\":false,\"sort\":true,\"ticks\":{\"suggestedMin\":\"0\",\"suggestedMax\":\"1000\",\"maxTicksLimit\":10},\"gridLines\":{\"display\":false}}],\"xAxes\":[{\"stacked\":false,\"sort\":true,\"ticks\":{\"fontFamily\":\"Soho\"},\"scaleLabel\":{\"display\":true,\"labelString\":\"\",\"fontFamily\":\"Soho\",\"padding\":4},\"hideLabel\":\"2\",\"gridLines\":{\"display\":true,\"borderDash\":[2,4],\"color\":\"#CCC\",\"zeroLineBorderDash\":[2,4],\"zeroLineColor\":\"transparent\"}}]}}";
		} else if (gadgetType != null && gadgetType.equals(RADAR)) {
			configGadget = "{}";
		}

		gadget.setConfig(configGadget);
		gadget.setDescription("");
		gadget.setPublic(Boolean.FALSE);
		gadget.setType(gadgetType);
		gadget.setUser(user);

		// Create measaures for gadget
		List<GadgetMeasure> measures = new ArrayList<GadgetMeasure>();
		int position = 0;
		for (Iterator iterator = commandDTO.getInformation().getAxes().getMeasuresY().iterator(); iterator.hasNext();) {
			MeasureDTO measureDTOY = (MeasureDTO) iterator.next();
			for (Iterator iterator2 = commandDTO.getInformation().getAxes().getMeasuresX().iterator(); iterator2
					.hasNext();) {
				MeasureDTO measureDTOX = (MeasureDTO) iterator2.next();
				GadgetMeasure measure = new GadgetMeasure();

				String config = "{\"fields\": [\"" + measureDTOX.getPath() + "\",\"" + measureDTOY.getPath()
						+ "\"],\"name\":\"" + measureDTOY.getName() + "\",\"config\": {"
						+ generateMeasureConfig(gadgetType, position, measureDTOY) + "}}";

				measure.setConfig(config);
				measures.add(measure);
				position++;
			}

		}
		return measures;
	}

	private String generateMeasureConfig(String gadgetType, int position, MeasureDTO measureDTO) {
		String config = "";
		if (gadgetType != null && gadgetType.equals(PIE)) {
			config = "";
		} else if (gadgetType.equals(LINE)) {
			config = "\"backgroundColor\":\"" + getColor(position) + "\",\"borderColor\":\"" + getColor(position)
					+ "\",\"pointBackgroundColor\":\"" + getColor(position) + "\",\"pointHoverBackgroundColor\":\""
					+ getColor(position)
					+ "\",\"yAxisID\":\"#0\",\"fill\":false,\"steppedLine\":false,\"radius\":\"0\"";
		} else if (gadgetType.equals(BAR)) {
			config = "\"backgroundColor\":\"" + getColor(position) + "\",\"borderColor\":\"" + getColor(position)
					+ "\",\"pointBackgroundColor\":\"" + getColor(position) + "\",\"yAxisID\":\"#0\"";
		} else if (gadgetType.equals(MIXED)) {
			if (measureDTO.getType() != null && measureDTO.getType().equals(LINE)) {
				config = "\"backgroundColor\":\"" + getColor(position) + "\",\"borderColor\":\"" + getColor(position)
						+ "\",\"pointBackgroundColor\":\"" + getColor(position) + "\",\"pointHoverBackgroundColor\":\""
						+ getColor(position)
						+ "\",\"yAxisID\":\"#0\",\"type\":\"line\",\"fill\":false,\"steppedLine\":false,\"radius\":\"2\",\"pointRadius\":\"2\",\"pointHoverRadius\":\"2\"";
			} else if (measureDTO.getType() != null && measureDTO.getType().equals(BAR)) {
				config = "\"backgroundColor\":\"" + getColor(position) + "\",\"borderColor\":\"" + getColor(position)
						+ "\",\"pointBackgroundColor\":\"" + getColor(position) + "\",\"pointHoverBackgroundColor\":\""
						+ getColor(position)
						+ "\",\"yAxisID\":\"#0\",\"type\":\"bar\",\"fill\":false,\"steppedLine\":false,\"radius\":\"1\",\"pointRadius\":\"1\",\"pointHoverRadius\":\"1\"";
			} else {
				config = "\"backgroundColor\":\"" + getColor(position) + "\",\"borderColor\":\"" + getColor(position)
						+ "\",\"pointBackgroundColor\":\"" + getColor(position) + "\",\"pointHoverBackgroundColor\":\""
						+ getColor(position)
						+ "\",\"yAxisID\":\"#0\",\"type\":\"points\",\"fill\":false,\"steppedLine\":false,\"radius\":\"0\",\"pointRadius\":\"0\",\"pointHoverRadius\":\"0\"";
			}
		} else if (gadgetType.equals(RADAR)) {
			config = "";
		}

		return config;
	}

	private String getColor(int position) {
		String col = "";
		if (position < COLORS.length && position >= 0) {
			col = COLORS[position];
		} else {
			Random random = new Random();
			col = "rgba(" + random.nextInt(256) + "," + random.nextInt(256) + "," + random.nextInt(256) + ",0.8)";

		}
		return col;
	}

	private List<GadgetMeasure> createGadgetColumns(CommandDTO commandDTO, String gadgetType, User user, Gadget gadget,
			String configGadget) {

		if (gadgetType != null && gadgetType.equals(WORDCLOUD)) {
			configGadget = "{}";
		} else if (gadgetType != null && gadgetType.equals(TABLE)) {
			configGadget = "{\"tablePagination\":{\"limit\":\"100\",\"page\":1,\"limitOptions\":[5,10,20,50,100],\"style\":{\"backGroundTHead\":\"#ffffff\",\"backGroundTFooter\":\"#ffffff\",\"trHeightHead\":\"40\",\"trHeightBody\":\"40\",\"trHeightFooter\":\"40\",\"textColorTHead\":\"#060e14\",\"textColorBody\":\"#555555\",\"textColorFooter\":\"#555555\"},\"options\":{\"rowSelection\":false,\"multiSelect\":false,\"autoSelect\":false,\"decapitate\":false,\"largeEditDialog\":false,\"boundaryLinks\":true,\"limitSelect\":true,\"pageSelect\":true}}}";
		}

		gadget.setConfig(configGadget);
		gadget.setDescription("");
		gadget.setPublic(Boolean.FALSE);
		gadget.setType(gadgetType);
		gadget.setUser(user);

		// Create measaures for gadget
		List<GadgetMeasure> measures = new ArrayList<GadgetMeasure>();

		int position = 0;
		for (Iterator iterator2 = commandDTO.getInformation().getColumns().iterator(); iterator2.hasNext();) {
			MeasureDTO measureDTO = (MeasureDTO) iterator2.next();
			GadgetMeasure measure = new GadgetMeasure();
			String config = "{}";
			if (gadgetType.equals(WORDCLOUD)) {
				config = "{\"fields\": [\"" + measureDTO.getPath() + "\"],\"name\":\"" + measureDTO.getName()
						+ "\",\"config\": {}}";
			} else if (gadgetType.equals(TABLE)) {
				config = "{\"fields\": [\"" + measureDTO.getPath() + "\"],\"name\":\"" + measureDTO.getName()
						+ "\",\"config\": {\"position\":\"" + position + "\"}}";
			}
			measure.setConfig(config);
			measures.add(measure);
			position++;
		}

		return measures;
	}

	private List<GadgetMeasure> updateGadgetMeasures(CommandDTO commandDTO, User user) {
		// Create measaures for gadget
		List<GadgetMeasure> measures = new ArrayList<GadgetMeasure>();
		if (commandDTO.getInformation().getGadgetType().equals(PIE)
				|| commandDTO.getInformation().getGadgetType().equals(LINE)
				|| commandDTO.getInformation().getGadgetType().equals(BAR)
				|| commandDTO.getInformation().getGadgetType().equals(MIXED)
				|| commandDTO.getInformation().getGadgetType().equals(RADAR)) {

			for (Iterator iterator = commandDTO.getInformation().getAxes().getMeasuresY().iterator(); iterator
					.hasNext();) {
				MeasureDTO measureDTOY = (MeasureDTO) iterator.next();
				for (Iterator iterator2 = commandDTO.getInformation().getAxes().getMeasuresX().iterator(); iterator2
						.hasNext();) {
					MeasureDTO measureDTOX = (MeasureDTO) iterator2.next();
					GadgetMeasure measure = new GadgetMeasure();
					String config = "{\"fields\": [\"" + measureDTOX.getPath() + "\",\"" + measureDTOY.getPath()
							+ "\"],\"name\":\"" + measureDTOY.getName() + "\",\"config\": {"
							+ generateMeasureConfig(commandDTO.getInformation().getGadgetType(), -1, measureDTOY)
							+ "}}";
					measure.setConfig(config);
					measures.add(measure);
				}

			}

		} else {
			// WORDCLOUD,TABLE
			for (Iterator iterator2 = commandDTO.getInformation().getColumns().iterator(); iterator2.hasNext();) {
				MeasureDTO measureDTO = (MeasureDTO) iterator2.next();
				GadgetMeasure measure = new GadgetMeasure();
				String config = "{}";
				if (commandDTO.getInformation().getGadgetType().equals(WORDCLOUD)) {
					config = "{\"fields\": [\"" + measureDTO.getPath() + "\"],\"name\":\"" + measureDTO.getName()
							+ "\",\"config\": {}}";
				} else if (commandDTO.getInformation().getGadgetType().equals(TABLE)) {
					config = "{\"fields\": [\"" + measureDTO.getPath() + "\"],\"name\":\"" + measureDTO.getName()
							+ "\",\"config\": {\"position\":\"" + 0 + "\"}}";
				}
				measure.setConfig(config);
				measures.add(measure);
			}
		}

		return measures;
	}

	private List<GadgetMeasure> createGadgetCoordinates(CommandDTO commandDTO, String gadgetType, User user,
			Gadget gadget, String configGadget) {

		configGadget = "{\"center\":{\"lat\":31.952162238024975,\"lng\":5.625,\"zoom\":2},\"markersFilter\":\"identifier\",\"jsonMarkers\":\"\"}";
		gadget.setConfig(configGadget);
		gadget.setDescription("");
		gadget.setPublic(Boolean.FALSE);
		gadget.setType(gadgetType);
		gadget.setUser(user);

		// Create measaures for gadget
		List<GadgetMeasure> measures = new ArrayList<GadgetMeasure>();
		GadgetMeasure measure = new GadgetMeasure();
		String config = "{\"fields\":[\"latitude\",\"longitude\",\"identifier\",\"name\"],\"name\":\"\",\"config\":{}}";
		measure.setConfig(config);
		measures.add(measure);
		return measures;
	}

	private List<GadgetMeasure> updateGadgetCoordinates(CommandDTO commandDTO) {
		// Create measaures for gadget
		List<GadgetMeasure> measures = new ArrayList<GadgetMeasure>();
		GadgetMeasure measure = new GadgetMeasure();
		String config = "{\"fields\":[\"latitude\",\"longitude\",\"identifier\",\"name\"],\"name\":\"\",\"config\":{}}";
		measure.setConfig(config);
		measures.add(measure);
		return measures;
	}

}