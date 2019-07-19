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
package com.minsait.onesait.platform.config.services.notebook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.minsait.onesait.platform.config.model.Notebook;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.config.services.exceptions.NotebookServiceException;

public interface NotebookService {

	public Notebook saveDBNotebook(String name, String idzep, User user);

	public Notebook createEmptyNotebook(String name, String userId)
			throws NotebookServiceException;

	public Notebook importNotebook(String name, String data, String userId)
			throws NotebookServiceException;
	
	public Notebook importNotebookFromJupyter(String name, String data, String userId)
			throws NotebookServiceException;

	public Notebook cloneNotebook(String name, String idzep, String userId)
			throws NotebookServiceException;

	public ResponseEntity<byte[]> exportNotebook(String id, String ususerIder);

	public void removeNotebook(String id, String userId);

	public String loginOrGetWSToken();

	public String loginOrGetWSTokenAdmin();

	public ResponseEntity<String> sendHttp(HttpServletRequest requestServlet, HttpMethod httpMethod, String body)
			throws URISyntaxException, ClientProtocolException, IOException;

	public ResponseEntity<String> sendHttp(String url, HttpMethod httpMethod, String body)
			throws URISyntaxException, ClientProtocolException, IOException;

	public ResponseEntity<String> sendHttp(String url, HttpMethod httpMethod, String body, HttpHeaders headers)
			throws URISyntaxException, ClientProtocolException, IOException;

	public Notebook getNotebook(String identification, String userId);

	public List<Notebook> getNotebooks(String userId);

	public boolean hasUserPermissionForNotebook(String zeppelinId, String userId);

	public ResponseEntity<String> runParagraph(String zeppelinId, String paragraphId, String bodyParams)
			throws ClientProtocolException, URISyntaxException, IOException;

	public ResponseEntity<String> runAllParagraphs(String zeppelinId)
			throws ClientProtocolException, URISyntaxException, IOException;

	public ResponseEntity<String> getParagraphResult(String zeppelinId, String paragraphId)
			throws ClientProtocolException, URISyntaxException, IOException;

	ResponseEntity<String> getAllParagraphStatus(String zeppelinId)
			throws ClientProtocolException, URISyntaxException, IOException;

	public String cloneNotebookOnlyZeppelin(String nameClone, String notebookZepId, String userId);
	
	public boolean hasUserPermissionInNotebook(Notebook nt, String userId);
	
	void createUserAccess(String notebookId, String userId, String accessType);
	
	void deleteUserAccess(String notebookUserAccessId);
	
	void changePublic(Notebook notebookId);
	
	public void renameNotebook(String name, String idzep, String userId)
			throws NotebookServiceException;

	public String notebookNameByIdZep(String idzep, String userId)
			throws NotebookServiceException;

	public boolean hasUserPermissionCreateNotebook(String userId);

	public void removeNotebookByIdZep(String idZep, String user);
	
	public ResponseEntity<String> restartInterpreter(String interpreterName, String notebookId, User user)
			throws ClientProtocolException, URISyntaxException, IOException;

	public ResponseEntity<String> restartInterpreter(String interpreterName, String body) 
			throws ClientProtocolException, URISyntaxException, IOException;

	public ResponseEntity<String> restartAllInterpretersNotebook(String notebookId, String body, User user) 
			throws ClientProtocolException, URISyntaxException, IOException;
}
