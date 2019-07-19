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
package com.minsait.onesait.platform.api.processor.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import com.minsait.onesait.platform.api.audit.aop.ApiManagerAuditable;
import com.minsait.onesait.platform.api.processor.ApiProcessor;
import com.minsait.onesait.platform.api.processor.ScriptProcessorFactory;
import com.minsait.onesait.platform.api.processor.utils.ApiProcessorUtils;
import com.minsait.onesait.platform.api.service.ApiServiceInterface;
import com.minsait.onesait.platform.api.service.impl.ApiServiceImpl.ChainProcessingStatus;
import com.minsait.onesait.platform.commons.model.InsertResult;
import com.minsait.onesait.platform.config.model.Api.ApiType;
import com.minsait.onesait.platform.config.model.ApiOperation;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.model.User;
import com.minsait.onesait.platform.router.service.app.model.NotificationModel;
import com.minsait.onesait.platform.router.service.app.model.OperationModel;
import com.minsait.onesait.platform.router.service.app.model.OperationModel.OperationType;
import com.minsait.onesait.platform.router.service.app.model.OperationModel.QueryType;
import com.minsait.onesait.platform.router.service.app.model.OperationResultModel;
import com.minsait.onesait.platform.router.service.app.service.RouterService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InternalOntologyApiProcessor implements ApiProcessor {

	@Autowired
	private RouterService routerService;

	@Value("${onesaitplatform.apimanager.cacheable:false}")
	private static boolean CACHEABLE;

	@Autowired
	private ScriptProcessorFactory scriptEngine;

	@Override
	@ApiManagerAuditable
	public Map<String, Object> process(Map<String, Object> data) throws Exception {
		final StopWatch watch = new StopWatch();
		try {
			watch.start();
			data = processQuery(data);
			watch.stop();
			log.info("API Process in " + watch.getTotalTimeMillis() + " ms");
			watch.start();
			data = postProcess(data);
			watch.stop();
			log.info("API PostProcess in " + watch.getTotalTimeMillis() + " ms");
			return data;
		} catch (final Exception e) {
			watch.stop();
			log.info("API Error Process in " + watch.getTotalTimeMillis() + " ms");
			throw e;
		}

	}

	@ApiManagerAuditable
	private Map<String, Object> processQuery(Map<String, Object> data) throws Exception {

		String OUTPUT = "";

		final Ontology ontology = (Ontology) data.get(ApiServiceInterface.ONTOLOGY);
		final User user = (User) data.get(ApiServiceInterface.USER);
		final String METHOD = (String) data.get(ApiServiceInterface.METHOD);
		final String BODY = (String) data.get(ApiServiceInterface.BODY);
		String QUERY_TYPE = (String) data.get(ApiServiceInterface.QUERY_TYPE);

		if (QUERY_TYPE.toUpperCase().indexOf("SQL") != -1) {
			QUERY_TYPE = "sql";
		}

		final String QUERY = (String) data.get(ApiServiceInterface.QUERY);
		final String OBJECT_ID = (String) data.get(ApiServiceInterface.OBJECT_ID);
		// String CACHEABLE = (String) data.get(ApiServiceInterface.CACHEABLE);
		String body = BODY;
		OperationType operationType = null;

		if (METHOD.equalsIgnoreCase(ApiOperation.Type.GET.name())) {
			body = QUERY;
			operationType = OperationType.QUERY;
		} else if (METHOD.equalsIgnoreCase(ApiOperation.Type.POST.name())) {
			operationType = OperationType.INSERT;
		} else if (METHOD.equalsIgnoreCase(ApiOperation.Type.PUT.name())) {
			operationType = OperationType.UPDATE;
		} else if (METHOD.equalsIgnoreCase(ApiOperation.Type.DELETE.name())) {
			operationType = OperationType.DELETE;
		} else {
			operationType = OperationType.QUERY;
		}

		final OperationModel model = OperationModel
				.builder(ontology.getIdentification(), OperationType.valueOf(operationType.name()), user.getUserId(),
						OperationModel.Source.APIMANAGER)
				.body(body).queryType(QueryType.valueOf(QUERY_TYPE.toUpperCase())).objectId(OBJECT_ID)
				.deviceTemplate("").cacheable(CACHEABLE).build();

		final NotificationModel modelNotification = new NotificationModel();
		modelNotification.setOperationModel(model);
		final OperationResultModel result = routerService.query(modelNotification);
		
		if (result != null) {
			if ("ERROR".equals(result.getResult())) {
				data.put(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
				data.put(ApiServiceInterface.STATUS, ChainProcessingStatus.STOP);
				data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR);
				final String messageError = ApiProcessorUtils.generateErrorMessage(
						"ERROR Output from Router Processing", "Stopped Execution, Error from Router",
						result.getMessage());
				data.put(ApiServiceInterface.REASON, messageError);
			} else {
				OUTPUT = result.getResult();
				
				if (StringUtils.isEmpty(OUTPUT)&& !METHOD.equalsIgnoreCase(ApiOperation.Type.GET.name())) {
					data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.NO_CONTENT);
					OUTPUT = "{\"RESULT\" : \"NO RESOURCE FOUND WITH ID: " + OBJECT_ID + "\"}";
				}else if(operationType==OperationType.INSERT) {
					final JSONObject obj = new JSONObject(OUTPUT);
					if(obj.has(InsertResult.DATA_PROPERTY)) {
						OUTPUT=obj.get(InsertResult.DATA_PROPERTY).toString();
					}
				}
				
				data.put(ApiServiceInterface.OUTPUT, OUTPUT);
			}
		} else {
			data.put(ApiServiceInterface.STATUS, ChainProcessingStatus.STOP);
			final String messageError = ApiProcessorUtils.generateErrorMessage("ERROR Output from Router Processing",
					"Stopped Execution", "Null Result From Router");
			data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR);
			data.put(ApiServiceInterface.REASON, messageError);
		}
		return data;

	}

	private Map<String, Object> postProcess(Map<String, Object> data) throws Exception {

		final ApiOperation apiOperation = ((ApiOperation) data.get(ApiServiceInterface.API_OPERATION));
		if (apiOperation != null) {
			String postProcessScript = apiOperation.getPostProcess();
			if (postProcessScript != null && !"".equals(postProcessScript)) {
                final User user = (User) data.get(ApiServiceInterface.USER);
                postProcessScript = postProcessScript.replace(ApiServiceInterface.CONTEXT_USER, user.getUserId());
				try {
					final Object result = scriptEngine.invokeScript(postProcessScript,
							data.get(ApiServiceInterface.OUTPUT));
					data.put(ApiServiceInterface.OUTPUT, result);
				} catch (final ScriptException e) {
					log.error("Execution logic for postprocess error", e);

					data.put(ApiServiceInterface.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
					data.put(ApiServiceInterface.STATUS, ChainProcessingStatus.STOP);
					data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR);

					final String messageError = ApiProcessorUtils.generateErrorMessage(
							"ERROR from Scripting Post Process", "Execution logic for Postprocess error",
							e.getCause().getMessage());
					data.put(ApiServiceInterface.REASON, messageError);

				} catch (final Exception e) {
					log.error("Execution logic for postprocess error", e);

					data.put(ApiServiceInterface.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
					data.put(ApiServiceInterface.STATUS, ChainProcessingStatus.STOP);
					data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR);

					final String messageError = ApiProcessorUtils.generateErrorMessage(
							"ERROR from Scripting Post Process", "Exception detected", e.getCause().getMessage());
					data.put(ApiServiceInterface.REASON, messageError);

				}
			}
		}
		return data;

	}

	@Override
	public List<ApiType> getApiProcessorTypes() {
		return Arrays.asList(ApiType.IOT, ApiType.INTERNAL_ONTOLOGY);
	}

}
