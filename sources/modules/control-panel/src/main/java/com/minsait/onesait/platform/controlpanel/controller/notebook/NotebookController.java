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
package com.minsait.onesait.platform.controlpanel.controller.notebook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.minsait.onesait.platform.config.model.Notebook;
import com.minsait.onesait.platform.config.model.NotebookUserAccess;
import com.minsait.onesait.platform.config.model.NotebookUserAccessType;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.repository.NotebookRepository;
import com.minsait.onesait.platform.config.repository.NotebookUserAccessRepository;
import com.minsait.onesait.platform.config.repository.NotebookUserAccessTypeRepository;
import com.minsait.onesait.platform.config.repository.UserRepository;
import com.minsait.onesait.platform.config.services.exceptions.NotebookServiceException;
import com.minsait.onesait.platform.config.services.notebook.NotebookService;
import com.minsait.onesait.platform.config.services.user.UserService;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import lombok.extern.slf4j.Slf4j;

@RequestMapping("/notebooks")
@Controller
@Slf4j
public class NotebookController {

	@Autowired
	private NotebookService notebookService;

	@Autowired
	private NotebookUserAccessRepository notebookUserAccessRepository;

	@Autowired
	private AppWebUtils utils;

	@Autowired
	private UserService userService;

	@Autowired
	ServletContext context;

	@Autowired
	private NotebookRepository notebookRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotebookUserAccessTypeRepository notebookUserAccessTypeRepository;

	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/createNotebook", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> createNotebook(@RequestParam("name") String name) {
		try {
			return new ResponseEntity<String>(notebookService.createEmptyNotebook(name, utils.getUserId()).getIdzep(),HttpStatus.OK);
		} 
		catch (final NotebookServiceException e) {
			switch(e.getError()) {
				case DUPLICATE_NOTEBOOK_NAME:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch (final Exception e) {
			log.error("Cannot create notebook: ", e);
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/cloneNotebook", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> cloneNotebook(@RequestParam("name") String name, @RequestParam("idzep") String idzep) {
		try {
			return new ResponseEntity<String>(notebookService.cloneNotebook(name, idzep, utils.getUserId()).getIdzep(),HttpStatus.OK);
		}
		catch (final NotebookServiceException e) {
			switch(e.getError()) {
				case DUPLICATE_NOTEBOOK_NAME:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
				case PERMISSION_DENIED:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.UNAUTHORIZED);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch (final Exception e) {
			log.error("Cannot clone notebook: ", e);
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/renameNotebook", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> renameNotebook(@RequestParam("name") String name, @RequestParam("idzep") String idzep) {
		try {
			notebookService.renameNotebook(name, idzep, utils.getUserId());
			return new ResponseEntity<String>(HttpStatus.OK);
		} 
		catch (final NotebookServiceException e) {
			switch(e.getError()) {
				case DUPLICATE_NOTEBOOK_NAME:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
				case PERMISSION_DENIED:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.UNAUTHORIZED);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch (final Exception e) {
			log.error("Cannot rename notebook: ", e);
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/importNotebook", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> importNotebook(@RequestParam("name") String name, @RequestParam("data") String data) {
		try {
			return new ResponseEntity<String>(notebookService.importNotebook(name, data, utils.getUserId()).getIdzep(),HttpStatus.OK);
		} 
		catch (final NotebookServiceException e) {
			switch(e.getError()) {
				case DUPLICATE_NOTEBOOK_NAME:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch (final Exception e) {
			log.error("Cannot import notebook: ", e);
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/importNotebookFromJupyter", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> importNotebookFromJupyter(@RequestParam("name") String name, @RequestParam("data") String data) {
		try {
			return new ResponseEntity<String>(notebookService.importNotebookFromJupyter(name, data, utils.getUserId()).getIdzep(),HttpStatus.OK);
		} 
		catch (final NotebookServiceException e) {
			switch(e.getError()) {
				case DUPLICATE_NOTEBOOK_NAME:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
				case INVALID_FORMAT_NOTEBOOK:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.BAD_REQUEST);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch (final Exception e) {
			log.error("Cannot import notebook: ", e);
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/exportNotebook/{id}", method = RequestMethod.GET, produces = "text/html")
	@ResponseBody
	public ResponseEntity<byte[]> exportNotebook(@PathVariable("id") String id, Model uiModel) {
		return notebookService.exportNotebook(id, utils.getUserId());
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
	public String removeNotebook(@PathVariable("id") String id, Model uiModel, RedirectAttributes ra) {
		try {
			notebookService.removeNotebook(id, utils.getUserId());
			uiModel.asMap().clear();
		} catch (final RuntimeException e) {
			utils.addRedirectException(e, ra);
		}

		return "redirect:/notebooks/list";
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/list", produces = "text/html")
	public String list(Model uiModel) {

		uiModel.addAttribute("lnt", notebookService.getNotebooks(utils.getUserId()));
		uiModel.addAttribute("user", utils.getUserId());
		uiModel.addAttribute("userRole", utils.getRole());

		return "notebooks/list";
	}

	@PostMapping("/public")
	@ResponseBody
	public String changePublic(@RequestParam("id") Notebook notebookId) {
		if (notebookService.hasUserPermissionInNotebook(notebookId, utils.getUserId())) {
			notebookService.changePublic(notebookId);
			return "ok";
		} else {
			return "ko";
		}
	}

	@RequestMapping(value = "/share/{id}", produces = "text/html")
	public String share(Model model, @PathVariable("id") Notebook id) {
		String user = utils.getUserId();
		if (id.getUser().toString().equals(user) || utils.getRole().equals("ROLE_ADMINISTRATOR")) {
			final List<User> users = userService.getAllActiveUsers();

			model.addAttribute("users", users);
			// model.addAttribute("int", notebookUserAccessRepository.findAllByOrderById());
			model.addAttribute("int", notebookUserAccessRepository.findByNotebook(id));
			model.addAttribute("notebookid", id.getId());

			return "notebooks/share";
		} else {
			return "error/403";
		}
	}

	@Transactional
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/auth", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<NotebookUserAccess> createAuthorization(@RequestParam String accesstype,
			@RequestParam String notebook, @RequestParam String user) {

		try {
			notebookService.createUserAccess(notebook, user, accesstype);

			final User userObject = userRepository.findByUserId(user);
			final Notebook notebookObject = notebookRepository.findById(notebook);
			final NotebookUserAccessType notebookUserAccessTypeObject = notebookUserAccessTypeRepository
					.findById(accesstype);
			final NotebookUserAccess notebookUserAccess = notebookUserAccessRepository
					.findByNotebookAndUserAndAccess(notebookObject, userObject, notebookUserAccessTypeObject);
			return new ResponseEntity<NotebookUserAccess>(notebookUserAccess, HttpStatus.CREATED);

		} catch (final RuntimeException e) {
			return new ResponseEntity<NotebookUserAccess>(HttpStatus.BAD_REQUEST);
		}

	}

	@RequestMapping(value = "/auth/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<String> deleteAuthorization(@RequestParam String id) {

		try {
			notebookService.deleteUserAccess(id);
			return new ResponseEntity<String>("{\"status\" : \"ok\"}", HttpStatus.OK);
		} catch (final RuntimeException e) {
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/app/api/security/ticket", method = RequestMethod.GET)
	@ResponseBody
	public String loginAppOrGetWSToken() {
		if (utils.isAdministrator()) {
			return notebookService.loginOrGetWSTokenAdmin();
		} else {
			return notebookService.loginOrGetWSToken();
		}

	}

	@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
	@RequestMapping(value = { "/app/api/interpreter/**", "/app/api/configurations/**", "/app/api/credential/**",
			"/app/api/version" }, method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> adminAppRest(Model uiModel, HttpServletRequest request)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.GET, "");
	}

	@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
	@RequestMapping(value = { "/app/api/interpreter/**",
			"/app/api/helium/**" }, method = RequestMethod.PUT, headers = "Accept=application/json")
	@ResponseBody
	public ResponseEntity<String> adminAppRestPutJSON(Model uiModel, HttpServletRequest request,
			@RequestBody(required = false) String body)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.valueOf(request.getMethod()), body);
	}

	@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
	@RequestMapping(value = { "/app/api/interpreter/**",
			"/app/api/helium/**" }, method = RequestMethod.POST, headers = "Accept=application/json")
	@ResponseBody
	public ResponseEntity<String> adminAppRestPostJSON(Model uiModel, HttpServletRequest request,
			@RequestBody(required = false) String body)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.valueOf(request.getMethod()), body);
	}

	@PreAuthorize("hasRole('ROLE_ADMINISTRATOR')")
	@RequestMapping(value = { "/app/api/interpreter/**",
			"/app/api/helium/**" }, method = RequestMethod.DELETE, headers = "Accept=application/json")
	@ResponseBody
	public ResponseEntity<String> adminAppRestDeleteJSON(Model uiModel, HttpServletRequest request,
			@RequestBody(required = false) String body)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.valueOf(request.getMethod()), body);
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = { "/app/api/notebook/**", "/app/api/helium/**" }, method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> analyAppRest(Model uiModel, HttpServletRequest request)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.GET, "");
	}

	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = "/app/")
	public String indexAppRedirectNoPath(Model uiModel, HttpServletRequest request) {
		return "notebooks/index";
	}
	
	@RequestMapping(value = "/nameByIdZep/{id}", produces = "text/html")
	@ResponseBody
	public ResponseEntity<String> nameByIdZep(Model model, @PathVariable("id") String id) {
		try {
			return new ResponseEntity<String>(notebookService.notebookNameByIdZep(id, utils.getUserId()),HttpStatus.OK);
		}
		catch(NotebookServiceException e) {
			switch(e.getError()) {
				case PERMISSION_DENIED:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.UNAUTHORIZED);
				default:
					return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		catch(Exception e) {
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_ADMINISTRATOR','ROLE_DATASCIENTIST')")
	@RequestMapping(value = { "/app/api/interpreter/setting/restart/**"
			 }, method = RequestMethod.PUT, headers = "Accept=application/json")
	@ResponseBody
	public ResponseEntity<String> analyAppRestPutJSON(Model uiModel, HttpServletRequest request,
			@RequestBody(required = false) String body)
			throws ClientProtocolException, URISyntaxException, IOException {
		return notebookService.sendHttp(request, HttpMethod.valueOf(request.getMethod()), body);
	}

}