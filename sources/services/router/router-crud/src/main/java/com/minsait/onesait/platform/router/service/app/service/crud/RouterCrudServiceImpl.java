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
package com.minsait.onesait.platform.router.service.app.service.crud;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.minsait.onesait.platform.commons.model.BulkWriteResult;
import com.minsait.onesait.platform.commons.model.ComplexWriteResult;
import com.minsait.onesait.platform.commons.model.ComplexWriteResultType;
import com.minsait.onesait.platform.commons.model.DBResult;
import com.minsait.onesait.platform.commons.model.InsertResult;
import com.minsait.onesait.platform.commons.model.MultiDocumentOperationResult;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.model.Ontology.RtdbDatasource;
import com.minsait.onesait.platform.config.repository.OntologyRepository;
import com.minsait.onesait.platform.config.services.ontologydata.DataSchemaValidationException;
import com.minsait.onesait.platform.config.services.ontologydata.OntologyDataJsonProblemException;
import com.minsait.onesait.platform.config.services.ontologydata.OntologyDataService;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.external.virtual.VirtualRelationalOntologyOpsDBRepository;
import com.minsait.onesait.platform.persistence.services.BasicOpsPersistenceServiceFacade;
import com.minsait.onesait.platform.persistence.services.OntologyReferencesValidation;
import com.minsait.onesait.platform.persistence.services.QueryToolService;
import com.minsait.onesait.platform.router.audit.aop.Auditable;
import com.minsait.onesait.platform.router.service.app.model.OperationModel;
import com.minsait.onesait.platform.router.service.app.model.OperationModel.QueryType;
import com.minsait.onesait.platform.router.service.app.model.OperationResultModel;
import com.minsait.onesait.platform.router.service.app.service.RouterCrudService;
import com.minsait.onesait.platform.router.service.app.service.RouterCrudServiceException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RouterCrudServiceImpl implements RouterCrudService {

	@Autowired
	private QueryToolService queryToolService;

	@Autowired
	private BasicOpsPersistenceServiceFacade basicOpsService;

	@Autowired
	private RouterCrudCachedOperationsService routerCrudCachedOperationsService;

	@Autowired
	private OntologyDataService ontologyDataService;

	@Autowired
	private OntologyRepository ontologyRepository;

	@Autowired
	private VirtualRelationalOntologyOpsDBRepository virtualRepo;

	@Autowired
	private OntologyReferencesValidation referencesValidation;

	@Autowired
	private ObjectMapper mapper;

	private final static String ERROR_STR = "ERROR";
	private final static String INSERT_STR = "INSERT";
	private final static String INSERT_ERROR = "Error inserting data";

	@Override
	@Auditable
	public OperationResultModel insert(OperationModel operationModel) throws RouterCrudServiceException {
		log.info("insert:" + operationModel.toString());
		final OperationResultModel result = new OperationResultModel();
		final String METHOD = operationModel.getOperationType().name();
		final String BODY = operationModel.getBody();
		final String ontologyName = operationModel.getOntologyName();
		final String OBJECT_ID = operationModel.getObjectId();
		final String USER = operationModel.getUser();

		String OUTPUT = "";
		result.setMessage("OK");
		result.setStatus(true);
		RtdbDatasource rtdbDatasource = null;
		try {
			final Ontology ontology = ontologyRepository.findByIdentification(ontologyName);

			rtdbDatasource = ontology.getRtdbDatasource();

			final List<String> processedData = ontologyDataService.preProcessInsertData(operationModel);

			try {
				referencesValidation.validate(operationModel);

			} catch (final Exception e) {
				log.error("Could not validate references {}", e.getMessage());
				if (e instanceof OntologyDataJsonProblemException)
					throw e;
			}

			if (METHOD.equalsIgnoreCase("POST")
					|| METHOD.equalsIgnoreCase(OperationModel.OperationType.INSERT.name())) {

				if (rtdbDatasource.equals(RtdbDatasource.VIRTUAL)) {
					ComplexWriteResult data = virtualRepo.insertBulk(ontologyName, ontology.getJsonSchema(),
							processedData, true, true);

					final List<? extends DBResult> results = data.getData();

					if (results.size() > 1) {
						OUTPUT = String.valueOf(results.size());
					} else if (results.size() > 0) {
						OUTPUT = ((BulkWriteResult) results.get(0)).getId();
					}
				} else {

					final ComplexWriteResult data = basicOpsService.insertBulk(ontologyName, ontology.getJsonSchema(),
							processedData, true, true);

					final InsertResult insertResult = new InsertResult();

					List<? extends DBResult> results = data.getData();

					if (data.getType() == ComplexWriteResultType.BULK) {

						insertResult.setType(ComplexWriteResultType.BULK);

						final MultiDocumentOperationResult insertResultData = new MultiDocumentOperationResult();

						if (results.size() > 1) {
							final List<String> lIds = new ArrayList<>();
							for (final DBResult inserted : results) {
								if (inserted.isOk()) {
									lIds.add(((BulkWriteResult) inserted).getId());
								} else {
									throw new Exception(inserted.getErrorMessage());
								}
							}

							insertResultData.setCount(lIds.size());
							insertResultData.setIds(lIds);

							insertResult.setData(insertResultData);

							OUTPUT = insertResult.toString();
						} else if (results.size() > 0) {// Single message Insert
							final List<String> lIds = new ArrayList<>();
							if (results.get(0).isOk()) {
								lIds.add(((BulkWriteResult) results.get(0)).getId());
							} else {
								throw new Exception(results.get(0).getErrorMessage());
							}

							insertResultData.setCount(1);
							insertResultData.setIds(lIds);

							insertResult.setData(insertResultData);

							OUTPUT = insertResult.toString();
						}
					} else if (data.getType() == ComplexWriteResultType.TIME_SERIES) {
						insertResult.setType(ComplexWriteResultType.TIME_SERIES);
						insertResult.setData(results);
						OUTPUT = insertResult.toString();
					}
				}
			}
		} catch (final DataSchemaValidationException e) {
			log.error("Error validating Schema of the Ontology", e);
			result.setResult(ERROR_STR);
			result.setStatus(false);
			result.setMessage("Error validating schema of the ontology:" + e.getMessage());
			result.setErrorCode("ErrorValidationSchema");
			result.setOperation("INSERT_STR");
			throw new RouterCrudServiceException(INSERT_ERROR, e, result);
		} catch (final OntologyDataJsonProblemException e) {
			log.error("Error validating ontology references", e);
			result.setResult(ERROR_STR);
			result.setStatus(false);
			result.setMessage("Error validating ontology references:" + e.getMessage());
			result.setErrorCode("ErrorValidationReferences");
			result.setOperation(INSERT_STR);
			throw new RouterCrudServiceException(INSERT_ERROR, e, result);
		} catch (final DBPersistenceException e) {
			log.error("insert", e);
			result.setResult(ERROR_STR);
			result.setStatus(false);
			result.setMessage(e.getMessage());
			result.setErrorCode("");
			result.setOperation(INSERT_STR);
			throw new RouterCrudServiceException(INSERT_ERROR, e, result);
		} catch (final Exception e) {
			log.error("insert", e);
			result.setResult(ERROR_STR);
			result.setStatus(false);
			result.setMessage(e.getMessage());
			result.setErrorCode("");
			result.setOperation(INSERT_STR);
			throw new RouterCrudServiceException(e, result);
		}
		result.setResult(OUTPUT);
		result.setOperation(METHOD);
		return result;
	}

	@Override
	@Auditable
	public OperationResultModel update(OperationModel operationModel) {
		log.info("update:" + operationModel.toString());
		final OperationResultModel result = new OperationResultModel();

		final String METHOD = operationModel.getOperationType().name();
		final String BODY = operationModel.getBody();
		final String QUERY_TYPE = operationModel.getQueryType().name();
		final String ontologyName = operationModel.getOntologyName();
		final String OBJECT_ID = operationModel.getObjectId();
		final String USER = operationModel.getUser();
		final boolean INCLUDEIDs = operationModel.isIncludeIds();
		final String CLIENTPLATFORM = operationModel.getDeviceTemplate();

		String OUTPUT = "";
		result.setMessage("OK");
		result.setStatus(true);
		try {
			final RtdbDatasource rtdbDatasource = ontologyRepository.findByIdentification(ontologyName)
					.getRtdbDatasource();
			if (METHOD.equalsIgnoreCase("PUT") || METHOD.equalsIgnoreCase(OperationModel.OperationType.UPDATE.name())) {
				if (rtdbDatasource.equals(RtdbDatasource.VIRTUAL)) {
					OUTPUT = String.valueOf(virtualRepo.updateNative(ontologyName, BODY, INCLUDEIDs));
				} else {
					if (OBJECT_ID != null && OBJECT_ID.length() > 0) {
						basicOpsService.updateNativeByObjectIdAndBodyData(ontologyName, OBJECT_ID, BODY);
						final String query = getQueryForOid(ontologyName, OBJECT_ID);
						OUTPUT = (!NullString(CLIENTPLATFORM))
								? queryToolService.querySQLAsJsonForPlatformClient(CLIENTPLATFORM, ontologyName, query,
										0)
								: queryToolService.querySQLAsJson(USER, ontologyName, query, 0);
						try {
							if (!StringUtils.isEmpty(OUTPUT)) {
								final ArrayNode updatedResource = (ArrayNode) mapper.readTree(OUTPUT);
								if (updatedResource.size() > 0)
									OUTPUT = mapper.writeValueAsString(updatedResource.get(0));
							}

						} catch (final Exception e) {
							log.error("Could not extract updated resource from array");
						}

					}

					else {
						OUTPUT = basicOpsService.updateNative(ontologyName, BODY, INCLUDEIDs).toString();
					}
				}
			}
		} catch (final Exception e) {
			log.error("update", e);
			result.setResult(OUTPUT);
			result.setStatus(false);
			result.setMessage(e.getMessage());
		}
		result.setResult(OUTPUT);
		result.setOperation(METHOD);
		return result;
	}

	@Override
	@Auditable
	public OperationResultModel delete(OperationModel operationModel) {
		log.info("delete:" + operationModel.toString());
		final OperationResultModel result = new OperationResultModel();
		final String METHOD = operationModel.getOperationType().name();
		final String BODY = operationModel.getBody();
		final String QUERY_TYPE = operationModel.getQueryType().name();
		final String ontologyName = operationModel.getOntologyName();
		final String OBJECT_ID = operationModel.getObjectId();
		final String USER = operationModel.getUser();
		final boolean INCLUDEIDs = operationModel.isIncludeIds();

		String OUTPUT = "";
		result.setMessage("OK");
		result.setStatus(true);
		try {
			final RtdbDatasource rtdbDatasource = ontologyRepository.findByIdentification(ontologyName)
					.getRtdbDatasource();
			if (METHOD.equalsIgnoreCase("DELETE")
					|| METHOD.equalsIgnoreCase(OperationModel.OperationType.DELETE.name())) {

				if (rtdbDatasource.equals(RtdbDatasource.VIRTUAL)) {
					OUTPUT = String.valueOf(virtualRepo.deleteNative(ontologyName, BODY, INCLUDEIDs));
				} else {
					if (OBJECT_ID != null && OBJECT_ID.length() > 0) {
						OUTPUT = basicOpsService.deleteNativeById(ontologyName, OBJECT_ID).toString();
					} else {
						OUTPUT = basicOpsService.deleteNative(ontologyName, BODY, INCLUDEIDs).toString();
					}
				}
			}
		} catch (final Exception e) {
			log.error("delete", e);
			result.setResult(OUTPUT);
			result.setStatus(false);
			result.setMessage(e.getMessage());
		}
		result.setResult(OUTPUT);
		result.setOperation(METHOD);
		return result;
	}

	@Override
	@Auditable
	public OperationResultModel query(OperationModel operationModel) {
		log.info("query:" + operationModel.toString());
		OperationResultModel result = null;
		final boolean cacheable = operationModel.isCacheable();
		if (cacheable) {
			log.info("QueryCache " + operationModel.toString());
			result = routerCrudCachedOperationsService.queryCache(operationModel);

		} else {
			log.info("QueryNoCache" + operationModel.toString());
			result = queryNoCache(operationModel);
		}
		return result;

	}

	public OperationResultModel queryNoCache(OperationModel operationModel) {
		log.info("queryNoCache:" + operationModel.toString());
		final OperationResultModel result = new OperationResultModel();
		final String METHOD = operationModel.getOperationType().name();
		final String BODY = operationModel.getBody();
		final String QUERY_TYPE = operationModel.getQueryType().name();
		final String ontologyName = operationModel.getOntologyName();
		final String OBJECT_ID = operationModel.getObjectId();
		final String USER = operationModel.getUser();
		final String CLIENTPLATFORM = operationModel.getDeviceTemplate();

		String OUTPUT = "";
		result.setMessage("OK");
		result.setStatus(true);
		try {
			final RtdbDatasource rtdbDatasource = ontologyRepository.findByIdentification(ontologyName)
					.getRtdbDatasource();
			if (METHOD.equalsIgnoreCase("GET") || METHOD.equalsIgnoreCase(OperationModel.OperationType.QUERY.name())) {

				if (QUERY_TYPE != null) {
					if (QUERY_TYPE.equalsIgnoreCase(QueryType.SQL.name())) {
						// OUTPUT = queryToolService.querySQLAsJson(ontologyName, QUERY, 0);
						OUTPUT = (!NullString(CLIENTPLATFORM))
								? queryToolService.querySQLAsJsonForPlatformClient(CLIENTPLATFORM, ontologyName, BODY,
										0)
								: queryToolService.querySQLAsJson(USER, ontologyName, BODY, 0);
					} else if (QUERY_TYPE.equalsIgnoreCase(QueryType.NATIVE.name())) {
						if (rtdbDatasource.equals(RtdbDatasource.VIRTUAL)) {
							OUTPUT = virtualRepo.queryNativeAsJson(ontologyName, BODY);
						} else {
							// OUTPUT = queryToolService.queryNativeAsJson(ontologyName, QUERY, 0,0);
							OUTPUT = (!NullString(CLIENTPLATFORM))
									? queryToolService.queryNativeAsJsonForPlatformClient(CLIENTPLATFORM, ontologyName,
											BODY, 0, 0)
									: queryToolService.queryNativeAsJson(USER, ontologyName, BODY, 0, 0);
						}
					} else {
						OUTPUT = basicOpsService.findById(ontologyName, OBJECT_ID);
					}
				} else {
					OUTPUT = basicOpsService.findById(ontologyName, OBJECT_ID);
				}
			}
		} catch (final Exception e) {
			log.error("queryNoCache", e);
			result.setResult(OUTPUT);
			result.setStatus(false);
			result.setMessage(e.getMessage());
		}

		result.setResult(OUTPUT);
		result.setOperation(METHOD);
		return result;
	}

	@Override
	// @Auditable
	public OperationResultModel execute(OperationModel operationModel) {
		log.info("execute:" + operationModel.toString());
		final String METHOD = operationModel.getOperationType().name();
		OperationResultModel result = new OperationResultModel();
		try {
			if (METHOD.equalsIgnoreCase("GET") || METHOD.equalsIgnoreCase(OperationModel.OperationType.QUERY.name())) {
				result = query(operationModel);
			}

			if (METHOD.equalsIgnoreCase("POST")
					|| METHOD.equalsIgnoreCase(OperationModel.OperationType.INSERT.name())) {
				result = insert(operationModel);
			}
			if (METHOD.equalsIgnoreCase("PUT") || METHOD.equalsIgnoreCase(OperationModel.OperationType.UPDATE.name())) {
				result = update(operationModel);
			}
			if (METHOD.equalsIgnoreCase("DELETE")
					|| METHOD.equalsIgnoreCase(OperationModel.OperationType.DELETE.name())) {
				result = delete(operationModel);
			}
		} catch (final Exception e) {
			log.error("execute", e);
		}
		return result;
	}

	public QueryToolService getQueryToolService() {
		return queryToolService;
	}

	public void setQueryToolService(QueryToolService queryToolService) {
		this.queryToolService = queryToolService;
	}

	public static boolean NullString(String l) {
		if (l == null)
			return true;
		else if (l != null && l.equalsIgnoreCase(""))
			return true;
		else
			return false;
	}

	@Override
	public OperationResultModel insertWithNoAudit(OperationModel model) throws RouterCrudServiceException {
		return insert(model);
	}

	private String getQueryForOid(String ontology, String oid) {
		return "select c,_id from ".concat(ontology).concat(" as c where _id=OID(\"").concat(oid).concat("\")");
	}

}