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
package com.minsait.onesait.platform.flowengine.api.rest.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.DigitalTwinTypeDTO;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.FlowEngineInvokeRestApiOperationRequest;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.MailRestDTO;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.RestApiDTO;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.RestApiOperationDTO;
import com.minsait.onesait.platform.flowengine.api.rest.pojo.UserDomainValidationRequest;
import com.minsait.onesait.platform.flowengine.exception.NotAllowedException;
import com.minsait.onesait.platform.flowengine.exception.NotAuthorizedException;
import com.minsait.onesait.platform.flowengine.exception.ResourceNotFoundException;

import javassist.NotFoundException;

public interface FlowEngineNodeService {

	public ResponseEntity<String> deploymentNotification(String json);

	public List<String> getApiRestCategories(String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public List<RestApiDTO> getApiRestByUser(String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public List<RestApiOperationDTO> getApiRestOperationsByUser(String apiName, Integer version, String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public Set<String> getOntologyByUser(String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public List<String> getClientPlatformByUser(String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public String validateUserDomain(UserDomainValidationRequest request)
			throws ResourceNotFoundException, NotAuthorizedException, NotAllowedException;

	public String submitQuery(String ontology, String queryType, String query, String authentication)
			throws ResourceNotFoundException, NotAuthorizedException, JsonProcessingException, NotFoundException;

	public String submitInsert(String ontology, String data, String authentication)
			throws ResourceNotFoundException, NotAuthorizedException, JsonProcessingException, NotFoundException;

	public List<DigitalTwinTypeDTO> getDigitalTwinTypes(String authentication)
			throws ResourceNotFoundException, NotAuthorizedException;

	public ResponseEntity<String> invokeRestApiOperation(FlowEngineInvokeRestApiOperationRequest invokeRequest);

	public void sendMail(MailRestDTO mail);

	public void sendSimpleMail(MailRestDTO mail);
}
