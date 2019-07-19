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
package com.minsait.onesait.platform.controlpanel.rest.management.notebook;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minsait.onesait.platform.config.model.Notebook;
import com.minsait.onesait.platform.config.model.Role;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.repository.NotebookRepository;
import com.minsait.onesait.platform.config.repository.UserRepository;
import com.minsait.onesait.platform.config.services.notebook.NotebookService;
import com.minsait.onesait.platform.controlpanel.rest.NotebookOpsRestServices;
import com.minsait.onesait.platform.controlpanel.utils.AppWebUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(value = "Notebook Ops", tags = { "Notebook Ops service" })
@RestController
@RequestMapping("api/notebooks")
public class NotebookManagementController extends NotebookOpsRestServices {

	@Autowired
	private NotebookService notebookService;
	@Autowired
	private NotebookRepository notebookRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AppWebUtils utils;

	@ApiOperation(value = "Runs paragraph synchronously")
	@PostMapping(value = "/run/notebook/{notebookZepId}/paragraph/{paragraphId}")
	public ResponseEntity<?> runParagraph(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId,
			@ApiParam(value = "Paragraph Id", required = true) @PathVariable(name = "paragraphId") String paragraphId,
			@ApiParam(value = "Input parameters") @RequestBody(required = false) String parameters) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.runParagraph(notebookZepId, paragraphId, parameters != null ? parameters : "");
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Runs all paragraphs synchronously")
	@PostMapping(value = "/run/notebook/{notebookZepId}")
	public ResponseEntity<?> runAllParagraphs(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.runAllParagraphs(notebookZepId);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Get the results of a paragraph")
	@GetMapping(value = "/result/notebook/{notebookZepId}/paragraph/{paragraphId}")
	public ResponseEntity<?> getParagraphResult(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId,
			@ApiParam(value = "Paragraph Id", required = true) @PathVariable(name = "paragraphId") String paragraphId) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.getParagraphResult(notebookZepId, paragraphId);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Get the status of all paragraphs")
	@GetMapping(value = "/run/notebook/{notebookZepId}")
	public ResponseEntity<?> getAllParagraphStatus(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.getAllParagraphStatus(notebookZepId);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Clone a notebook only zeppelin")
	@GetMapping(value = "/run/notebook/{notebookZepId}/{nameClone}")
	public ResponseEntity<?> cloneNotebook(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId,
			@ApiParam(value = "Name for the clone", required = true) @PathVariable("nameClone") String nameClone) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				final String id = notebookService.cloneNotebookOnlyZeppelin(nameClone, notebookZepId, userId);
				return new ResponseEntity<>(id, HttpStatus.OK);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Clone a notebook and save db")
	@GetMapping(value = "/clone/notebook/{notebookZepId}/{nameClone}")
	public ResponseEntity<?> cloneNotebookSaveDB(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId,
			@ApiParam(value = "Name for the clone", required = true) @PathVariable("nameClone") String nameClone) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				final Notebook result = notebookService.cloneNotebook(nameClone, notebookZepId, userId);
				return new ResponseEntity<>(result.getIdzep(), HttpStatus.OK);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Create a notebook onesait")
	@PostMapping(value = "/create/notebook/{nameCreate}")
	public ResponseEntity<?> createEmptyNotebook(
			@ApiParam(value = "Name for the create", required = true) @PathVariable("nameCreate") String nameCreate) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionCreateNotebook(userId);

		if (authorized) {
			try {
				final String id = notebookService.createEmptyNotebook(nameCreate, userId).getIdzep();
				return new ResponseEntity<>(id, HttpStatus.OK);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

	}

	@ApiOperation(value = "Delete notebook")
	@DeleteMapping(value = "/delete/notebook/{notebookZepId}")
	public ResponseEntity<?> deleteNotebook(
			@ApiParam(value = "Id notebook for the delete", required = true) @PathVariable("notebookZepId") String notebookZepId) {

		final String userId = utils.getUserId();

		try {
			notebookService.removeNotebookByIdZep(notebookZepId, userId);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (final Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation(value = "Get a paragraph information")
	@GetMapping(value = "/api/notebook/{notebookZepId}/paragraph/{paragraphId}")
	public ResponseEntity<?> getParagraphInfo(
			@ApiParam(value = "Notebook Zeppelin Id", required = true) @PathVariable("notebookZepId") String notebookZepId,
			@ApiParam(value = "Paragraph Id", required = true) @PathVariable(name = "paragraphId") String paragraphId) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.getParagraphResult(notebookZepId, paragraphId);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "List notebooks")
	@GetMapping(value = "/list/")
	public ResponseEntity<?> listNotebook() {
		List<Notebook> notebooks;
		final JSONObject notebooksInfo = new JSONObject();
		final String userId = utils.getUserId();
		final User user = userRepository.findByUserId(userId);

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			notebooks = notebookRepository.findAll();
		} else {
			notebooks = notebookRepository.findByUser(user);
		}
		try {
			if (!notebooks.isEmpty()) {
				notebooks.stream().forEach(n -> notebooksInfo.put(n.getIdzep(), n.getIdentification()));
				return new ResponseEntity<>(notebooksInfo.toString(), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

		} catch (final Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation(value = "Export notebook")
	@GetMapping(value = "/export/{notebookZepId}")
	public ResponseEntity<?> exportNotebook(
			@ApiParam(value = "Id notebook for the export", required = true) @PathVariable("notebookZepId") String notebookZepId) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookZepId, userId);

		if (authorized) {
			try {
				return notebookService.exportNotebook(notebookZepId, userId);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Export all notebooks")
	@GetMapping(value = "/export/")
	public ResponseEntity<?> exportAllNotebooks() {
		List<Notebook> notebooks;
		final JSONObject notebooksInfo = new JSONObject();
		final String userId = utils.getUserId();
		final User user = userRepository.findByUserId(userId);

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			notebooks = notebookRepository.findAll();
		} else {
			notebooks = notebookRepository.findByUser(user);
		}
		try {
			if (!notebooks.isEmpty()) {
				for (int i = 0; i < notebooks.size(); i++) {
					final Notebook n = notebooks.get(i);
					final String notebookIdzep = n.getIdzep();
					final ResponseEntity<byte[]> data = notebookService.exportNotebook(n.getIdzep(), userId);
					final byte[] bytes = data.getBody();
					final String base64String = Base64.encodeBase64String(bytes);
					notebooksInfo.put(notebookIdzep, base64String);
				}
				return new ResponseEntity<>(notebooksInfo.toString(), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

		} catch (final Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@ApiOperation(value = "Import notebook")
	@PostMapping(value = "/import/{notebookName}")
	public ResponseEntity<?> importNotebook(
			@ApiParam(value = "Notebook Zeppelin Name", required = true) @PathVariable("notebookName") String notebookName,
			@ApiParam(value = "Input parameters") @RequestBody(required = true) String data) {

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionCreateNotebook(userId);

		if (authorized) {
			try {
				final Notebook note = notebookService.importNotebook(notebookName, data, userId);
				final JSONObject notebookJSONObject = new JSONObject();
				notebookJSONObject.put("id", note.getIdzep());
				return new ResponseEntity<>(notebookJSONObject.toString(), HttpStatus.OK);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}

	@ApiOperation(value = "Import several notebooks")
	@PostMapping(value = "/import/")
	public ResponseEntity<?> importSeveralNotebooks(
			@ApiParam(value = "Input parameters") @RequestBody(required = true) String data) {
		// input format : {"id_json_0": {"json_1}, "id_json_1": {"json_1"}, ...}

		final String userId = utils.getUserId();
		final boolean authorized = notebookService.hasUserPermissionCreateNotebook(userId);

		if (authorized) {
			try {
				final JSONObject notebookJSONObject = new JSONObject();
				final JSONObject jsonObject = new JSONObject(data);
				final Iterator keys = jsonObject.keys();
				int counter = 0;
				while (keys.hasNext()) {
					final String currentKey = (String) keys.next();
					final JSONObject currentNotebook = jsonObject.getJSONObject(currentKey);
					final String notebookName = currentNotebook.getString("name");
					try {
						final Notebook note = notebookService.importNotebook(notebookName, currentNotebook.toString(),
								userId);
						notebookJSONObject.append("id", note.getIdzep());
					} catch (final Exception e) {
						notebookJSONObject.append("errors", currentKey);
					}
					counter++;
				}

				if (counter == 0) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
				return new ResponseEntity<>(notebookJSONObject.toString(), HttpStatus.OK);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

	}
	
	@ApiOperation(value = "Restart global interpreter")
	@GetMapping(value = "/restart/{interpreterName}")
	public ResponseEntity<?> restartInterpreter(
			@ApiParam(value = "Interpreter Name", required = true) @PathVariable(name = "interpreterName") String interpreterName) {
		
		final String userId = utils.getUserId();
		final User user = userRepository.findByUserId(userId);

		if (user.getRole().getId().equals(Role.Type.ROLE_ADMINISTRATOR.toString())) {
			try {
				return notebookService.restartInterpreter(interpreterName, "");
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
				
	}
	
	@ApiOperation(value = "Restart interpreter of a notebook")
	@GetMapping(value = "/restart/{interpreterName}/notebook/{notebookId}")
	public ResponseEntity<?> restartInterpreter(
			@ApiParam(value = "Interpreter Name", required = true) @PathVariable(name = "interpreterName") String interpreterName,
			@ApiParam(value = "Notebook Id", required = true) @PathVariable(name = "notebookId") String notebookId) {
		
		final String userId = utils.getUserId();
		final User user = userRepository.findByUserId(userId);
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookId, userId);

		if (authorized) {
			try {
				JSONObject body = new JSONObject();
				body.put("noteId", notebookId);
				
				return notebookService.restartInterpreter(interpreterName, body.toString(), user);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);			
	}
	
	@ApiOperation(value = "Restart all interpreters of a notebook")
	@GetMapping(value = "/restart/notebook/{notebookId}")
	public ResponseEntity<?> restartInterpreters(
			@ApiParam(value = "Notebook Id", required = true) @PathVariable(name = "notebookId") String notebookId) {
		
		final String userId = utils.getUserId();
		final User user = userRepository.findByUserId(userId);
		final boolean authorized = notebookService.hasUserPermissionForNotebook(notebookId, userId);

		if (authorized) {
			try {
				JSONObject body = new JSONObject();
				body.put("noteId", notebookId);
				
				return notebookService.restartAllInterpretersNotebook(notebookId, body.toString(), user);
			} catch (final Exception e) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);			
	}

}
