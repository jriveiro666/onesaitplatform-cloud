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
package com.minsait.onesait.platform.controlpanel.controller.consolemenu;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.minsait.onesait.platform.config.model.ConsoleMenu;
import com.minsait.onesait.platform.config.repository.ConsoleMenuRepository;
import com.minsait.onesait.platform.config.services.menu.MenuServiceImpl;
import com.minsait.onesait.platform.controlpanel.controller.rollback.RollbackController;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/consolemenu")
@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
@Slf4j
public class consoleMenuController {
	
	@Autowired
	private ConsoleMenuRepository consoleMenuRepository;
	@Autowired
	private MenuServiceImpl menuService;
	@Autowired
	private RollbackController rollbackController;
	@Autowired
	private AppWebUtils utils;
	
	@GetMapping(value = "/list", produces = "text/html")
	@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
	public String list (Model model) {

		model.addAttribute("menus", consoleMenuRepository.findAll());

		return "consolemenu/list";
	}
	
	@GetMapping(value = "/show/{id}", produces = "text/html")
	public String show (Model model, @PathVariable("id") String id) {
		
		model.addAttribute("option", "show");
		model.addAttribute("menu", consoleMenuRepository.findById(id).getJson());
		
		return "consolemenu/show";
	}
	
	@GetMapping(value = "/edit/{id}", produces = "text/html")
	public String edit (Model model, @PathVariable("id") String id) {
		
		model.addAttribute("option", "edit");
		model.addAttribute("menu", consoleMenuRepository.findById(id).getJson());
		model.addAttribute("idCm",id);
		
		return "consolemenu/show";
	}
	
	@Transactional
	@RequestMapping(value = "/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ConsoleMenu> updateConsoleMenu(@RequestParam String menuId,
			@RequestParam String menuJson, HttpServletRequest request) throws IOException, ProcessingException {
		
		try {
			final JsonNode menuJsonNode = JsonLoader.fromString(menuJson);
			final JsonNode jsonSchema = JsonLoader.fromString("{\r\n" + 
					"  \"definitions\": {},\r\n" + 
					"  \"type\": \"object\",\r\n" + 
					"  \"title\": \"The Root Schema\",\r\n" + 
					"  \"required\": [\r\n" + 
					"    \"menu\",\r\n" + 
					"    \"rol\",\r\n" + 
					"    \"noSession\",\r\n" + 
					"    \"navigation\"\r\n" + 
					"  ],\r\n" + 
					"  \"properties\": {\r\n" + 
					"    \"menu\": {\r\n" + 
					"      \"type\": \"string\",\r\n" + 
					"      \"title\": \"The Menu Schema\",\r\n" + 
					"      \"default\": \"\",\r\n" + 
					"      \"pattern\": \"^(.*)$\"\r\n" + 
					"    },\r\n" + 
					"    \"rol\": {\r\n" + 
					"      \"type\": \"string\",\r\n" + 
					"      \"title\": \"The Rol Schema\",\r\n" + 
					"      \"default\": \"\",\r\n" + 
					"      \"pattern\": \"^(.*)$\"\r\n" + 
					"    },\r\n" + 
					"    \"noSession\": {\r\n" + 
					"      \"type\": \"string\",\r\n" + 
					"      \"title\": \"The Nosession Schema\",\r\n" + 
					"      \"default\": \"\",\r\n" + 
					"      \"pattern\": \"^(.*)$\"\r\n" + 
					"    },\r\n" + 
					"    \"navigation\": {\r\n" + 
					"      \"type\": \"array\",\r\n" + 
					"      \"title\": \"The Navigation Schema\",\r\n" + 
					"      \"items\": {\r\n" + 
					"        \"type\": \"object\",\r\n" + 
					"        \"title\": \"The Items Schema\",\r\n" + 
					"        \"required\": [\r\n" + 
					"          \"title\",\r\n" + 
					"          \"icon\",\r\n" + 
					"          \"url\",\r\n" + 
					"          \"submenu\"\r\n" + 
					"        ],\r\n" + 
					"        \"properties\": {\r\n" + 
					"          \"title\": {\r\n" + 
					"            \"type\": \"object\",\r\n" + 
					"            \"title\": \"The Title Schema\",\r\n" + 
					"            \"required\": [\r\n" + 
					"              \"EN\",\r\n" + 
					"              \"ES\"\r\n" + 
					"            ],\r\n" + 
					"            \"properties\": {\r\n" + 
					"              \"EN\": {\r\n" + 
					"                \"type\": \"string\",\r\n" + 
					"                \"title\": \"The En Schema\",\r\n" + 
					"                \"default\": \"\",\r\n" + 
					"                \"pattern\": \"^(.*)$\"\r\n" + 
					"              },\r\n" + 
					"              \"ES\": {\r\n" + 
					"                \"type\": \"string\",\r\n" + 
					"                \"title\": \"The Es Schema\",\r\n" + 
					"                \"default\": \"\",\r\n" + 
					"                \"pattern\": \"^(.*)$\"\r\n" + 
					"              }\r\n" + 
					"            }\r\n" + 
					"          },\r\n" + 
					"          \"icon\": {\r\n" + 
					"            \"type\": \"string\",\r\n" + 
					"            \"title\": \"The Icon Schema\",\r\n" + 
					"            \"default\": \"\",\r\n" + 
					"            \"pattern\": \"^(.*)$\"\r\n" + 
					"          },\r\n" + 
					"          \"url\": {\r\n" + 
					"            \"type\": \"string\",\r\n" + 
					"            \"title\": \"The Url Schema\",\r\n" + 
					"            \"default\": \"\",\r\n" + 
					"            \"pattern\": \"^(.*)$\"\r\n" + 
					"          },\r\n" + 
					"          \"submenu\": {\r\n" + 
					"            \"type\": \"array\",\r\n" + 
					"            \"title\": \"The Submenu Schema\",\r\n" + 
					"            \"items\": {\r\n" + 
					"              \"type\": \"object\",\r\n" + 
					"              \"title\": \"The Items Schema\",\r\n" + 
					"              \"required\": [\r\n" + 
					"                \"title\",\r\n" + 
					"                \"icon\",\r\n" + 
					"                \"url\"\r\n" + 
					"              ],\r\n" + 
					"              \"properties\": {\r\n" + 
					"                \"title\": {\r\n" + 
					"                  \"type\": \"object\",\r\n" + 
					"                  \"title\": \"The Title Schema\",\r\n" + 
					"                  \"required\": [\r\n" + 
					"                    \"EN\",\r\n" + 
					"                    \"ES\"\r\n" + 
					"                  ],\r\n" + 
					"                  \"properties\": {\r\n" + 
					"                    \"EN\": {\r\n" + 
					"                      \"type\": \"string\",\r\n" + 
					"                      \"title\": \"The En Schema\",\r\n" + 
					"                      \"default\": \"\",\r\n" + 
					"                      \"pattern\": \"^(.*)$\"\r\n" + 
					"                    },\r\n" + 
					"                    \"ES\": {\r\n" + 
					"                      \"type\": \"string\",\r\n" + 
					"                      \"title\": \"The Es Schema\",\r\n" + 
					"                      \"default\": \"\",\r\n" + 
					"                      \"pattern\": \"^(.*)$\"\r\n" + 
					"                    }\r\n" + 
					"                  }\r\n" + 
					"                },\r\n" + 
					"                \"icon\": {\r\n" + 
					"                  \"type\": \"string\",\r\n" + 
					"                  \"title\": \"The Icon Schema\",\r\n" + 
					"                  \"default\": \"\",\r\n" + 
					"                  \"pattern\": \"^(.*)$\"\r\n" + 
					"                },\r\n" + 
					"                \"url\": {\r\n" + 
					"                  \"type\": \"string\",\r\n" + 
					"                  \"title\": \"The Url Schema\",\r\n" + 
					"                  \"default\": \"\",\r\n" + 
					"                  \"pattern\": \"^(.*)$\"\r\n" + 
					"                }\r\n" + 
					"              }\r\n" + 
					"            }\r\n" + 
					"          }\r\n" + 
					"        }\r\n" + 
					"      }\r\n" + 
					"    }\r\n" + 
					"  }\r\n" + 
					"}");
			final JsonSchemaFactory factoryJson = JsonSchemaFactory.byDefault();
			final JsonSchema schema = factoryJson.getJsonSchema(jsonSchema);
			ProcessingReport report = schema.validate(menuJsonNode);
			if (report != null && !report.isSuccess()) {
				return new ResponseEntity<ConsoleMenu>(HttpStatus.BAD_REQUEST);
			}
		}catch (final RuntimeException e) {
			log.error("Error validating Json structure: ", e.getMessage());
			return new ResponseEntity<ConsoleMenu>(HttpStatus.BAD_REQUEST);
		}

		try {
			menuService.updateMenu(menuId, menuJson);

			ConsoleMenu menu = consoleMenuRepository.findById(menuId);

			if (menu.getRoleType().getId().equals(utils.getRole())) {
				utils.setSessionAttribute(request, "menu", menu.getJson());}
			
			return new ResponseEntity<ConsoleMenu>(menu, HttpStatus.CREATED);

		} catch (final RuntimeException e) {
			log.error("Error updating console menu: ", e.getMessage());
			return new ResponseEntity<ConsoleMenu>(HttpStatus.BAD_REQUEST);
		}

	}

	@Transactional
	@RequestMapping(value = "/rollback/", method = RequestMethod.POST)
	public String rollbackMenu(@RequestParam String menuId, HttpServletRequest request){
		
		ConsoleMenu menu = consoleMenuRepository.findById(menuId);
		
		ConsoleMenu OriginalMenu = (ConsoleMenu) rollbackController.getRollback(menuId);
		String originalMenuJson = OriginalMenu.getJson();
		
		menu.setJson(originalMenuJson);		
		consoleMenuRepository.save(menu);
		
		if (menu.getRoleType().getId().equals(utils.getRole())) {
			utils.setSessionAttribute(request, "menu", menu.getJson());}
		
		return "consolemenu/list";
	}
	
}
