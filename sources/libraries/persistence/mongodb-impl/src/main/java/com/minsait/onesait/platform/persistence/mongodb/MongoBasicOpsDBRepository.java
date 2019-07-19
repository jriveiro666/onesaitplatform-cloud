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
package com.minsait.onesait.platform.persistence.mongodb;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.commons.model.BulkWriteResult;
import com.minsait.onesait.platform.commons.model.ComplexWriteResult;
import com.minsait.onesait.platform.commons.model.ComplexWriteResultType;
import com.minsait.onesait.platform.commons.model.MultiDocumentOperationResult;
import com.minsait.onesait.platform.commons.model.TimeSeriesResult;
import com.minsait.onesait.platform.config.repository.OntologyTimeSeriesRepository;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.exceptions.QueryNativeFormatException;
import com.minsait.onesait.platform.persistence.interfaces.BasicOpsDBRepository;
import com.minsait.onesait.platform.persistence.mongodb.quasar.connector.QuasarMongoDBbHttpConnector;
import com.minsait.onesait.platform.persistence.mongodb.template.MongoDbTemplate;
import com.minsait.onesait.platform.persistence.mongodb.timeseries.MongoDBTimeSeriesProcessor;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoIterable;
import com.mongodb.util.JSON;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component("MongoBasicOpsDBRepository")
@Scope("prototype")
@Lazy
@Slf4j
public class MongoBasicOpsDBRepository implements BasicOpsDBRepository {
	@Autowired
	private UtilMongoDB util;

	@Autowired
	private MongoDbTemplate mongoDbConnector;

	@Autowired
	private QuasarMongoDBbHttpConnector quasarMongoConnector;

	@Autowired
	private OntologyTimeSeriesRepository ontologyTimeSeriesRepository;

	@Autowired
	private MongoDBTimeSeriesProcessor timeSeriesProcessor;

	private static final String QUASAR_QUERY_ERROR = "Error executing query in Quasar: ";

	@Value("${onesaitplatform.database.mongodb.database:onesaitplatform_rtdb}")
	@Getter
	@Setter
	private String database;

	@Value("${onesaitplatform.database.mongodb.queries.executionTimeout:30000}")
	@Getter
	@Setter
	private long queryExecutionTimeout;

	@Value("${onesaitplatform.database.mongodb.queries.defaultLimit:1000}")
	private int queryDefaultLimit;

	@Autowired
	private IntegrationResourcesService resourcesServices;

	protected ObjectMapper objectMapper;

	@PostConstruct
	public void init() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	private int getMaxRegisters() {
		return ((Integer) resourcesServices.getGlobalConfiguration().getEnv().getDatabase().get("queries-limit"))
				.intValue();
	}

	@Override
	public String insert(String ontology, String schema, String instance) throws DBPersistenceException {
		log.debug("insertInstance", ontology, instance);
		try {
			if (ontologyTimeSeriesRepository.isTimeSeries(ontology)) {
				List<TimeSeriesResult> result = timeSeriesProcessor.processTimeSerie(ontology, instance);
				return objectMapper.writeValueAsString(result);
			} else {
				final ObjectId objectId = mongoDbConnector.insert(database, ontology, util.prepareQuotes(instance));
				return objectId.toString();
			}
		} catch (final javax.persistence.PersistenceException | JsonProcessingException e) {
			log.error("Error inserting Instance", e);
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public ComplexWriteResult insertBulk(String ontology, String schema, List<String> instances, boolean order,
			boolean includeIds) throws DBPersistenceException {
		final ArrayList<String> dataToInsert = new ArrayList<>(instances.size());

		for (final String document : instances) {
			dataToInsert.add(util.prepareQuotes(document));
		}

		ComplexWriteResult result = new ComplexWriteResult();
		try {
			if (ontologyTimeSeriesRepository.isTimeSeries(ontology)) {
				List<TimeSeriesResult> data = timeSeriesProcessor.processTimeSerie(ontology, instances.get(0));

				result.setType(ComplexWriteResultType.TIME_SERIES);
				result.setData(data);
			} else {
				List<BulkWriteResult> data = mongoDbConnector.bulkInsert(database, ontology, dataToInsert, order,
						includeIds);

				result.setType(ComplexWriteResultType.BULK);
				result.setData(data);
			}
		} catch (final javax.persistence.PersistenceException e) {
			log.error("insertBulk", e);
			throw new DBPersistenceException(e);
		}

		return result;

	}

	@Override
	public MultiDocumentOperationResult deleteNative(String collection, String query, boolean includeIds)
			throws DBPersistenceException {
		log.debug("deleteNative", collection, query);
		return mongoDbConnector.remove(database, collection, query, includeIds);
	}

	@Override
	public MultiDocumentOperationResult delete(String collection, boolean includeIds) throws DBPersistenceException {
		return deleteNative(collection, "{}", includeIds);
	}

	@Override
	public MultiDocumentOperationResult deleteNativeById(String collection, String objectId)
			throws DBPersistenceException {
		log.debug("deleteNativeById", collection, objectId);
		return deleteNative(collection, "{\"_id\": { \"$oid\" : \"" + objectId + "\" }}", false);
	}

	@Override
	public MultiDocumentOperationResult updateNative(String collection, String updateQuery, boolean includeIds)
			throws DBPersistenceException {
		String query = null;
		String dataToUpdate = null;

		try {
			if (updateQuery.indexOf(".update(") != -1) {
				updateQuery = util.getQueryContent(updateQuery);
			}

			final int endOfQuery = endOfQuery(updateQuery);
			// query = updateQuery.substring(0, updateQuery.indexOf("},") + 1);
			query = updateQuery.substring(0, endOfQuery + 1);

			// dataToUpdate = updateQuery.substring(updateQuery.indexOf("},") + 2,
			// updateQuery.length());
			dataToUpdate = updateQuery.substring(endOfQuery + 1, updateQuery.length());
			dataToUpdate = dataToUpdate.substring(dataToUpdate.indexOf(',') + 1, dataToUpdate.length());

			return updateNative(collection, query, dataToUpdate, includeIds);
		} catch (final DBPersistenceException e) {
			log.error("update", e);
			throw e;
		} catch (final Exception e) {
			log.error("update", e);
			throw new DBPersistenceException("Error on updating native", e);
		}
	}

	private int endOfQuery(String query) {
		int i = -1;
		boolean found = false;
		int openBracketCount = 0;
		int closeBracketCount = 0;
		while (i < query.length() && !found) {
			i++;
			final char c = query.charAt(i);
			if (c == '{') {
				openBracketCount++;
			} else if (c == '}') {
				closeBracketCount++;
			}
			found = openBracketCount > 0 && openBracketCount == closeBracketCount;

		}
		if (!found) {
			throw new InvalidParameterException("Bad formed updated query");
		}
		return i;
	}

	@Override
	public MultiDocumentOperationResult updateNative(String collection, String query, String dataToUpdate,
			boolean includeIds) throws DBPersistenceException {
		log.debug("update", collection, query, dataToUpdate);
		return mongoDbConnector.update(database, collection, query, dataToUpdate, true, includeIds);
		//
		// FIXME: Update contextData
		/*
		 * List<String> ids = new ArrayList<String>(); BasicDBObject updatedInstance =
		 * (BasicDBObject) JSON.parse(util.prepareQuotes(data)); for (BasicDBObject
		 * document : cursor) { BasicDBObject contextData = (BasicDBObject)
		 * document.get("contextData"); BasicDBObject updatedDocument = new
		 * BasicDBObject(); updatedDocument.append("contextData", contextData); for
		 * (String key : updatedInstance.keySet()) { updatedDocument.append(key,
		 * updatedInstance.get(key)); } mongoDbConnector.replace(database, collection,
		 * document, updatedDocument);
		 * ids.add(util.getObjectIdString(document.getObjectId("_id"))); } return ids;
		 */
	}

	@Override
	public MultiDocumentOperationResult updateNativeByObjectIdAndBodyData(String collection, String objectId,
			String bodyData) {
		final String updateQuery = "db." + collection + ".update({\"_id\": {\"$oid\" : \"" + objectId + "\" }}, {$set:"
				+ bodyData + " })";
		return updateNative(collection, updateQuery, false);
	}

	/*
	 * @Override public List<String> updateNative(String ontology, String statement)
	 * throws DBPersistenceException { log.debug("update", statement); String
	 * statementAux = statement; String data = ""; String query = ""; try { if
	 * (statementAux == null || statementAux.length() == 0) throw new
	 * DBPersistenceException("Statement null: " + statement);
	 *
	 * if (statementAux.startsWith("{")) { statementAux = statementAux.substring(1);
	 * } if (statementAux.endsWith("}")) { statementAux = statementAux.substring(0,
	 * statementAux.length() - 1); } if (!statementAux.endsWith(";")) { statementAux
	 * = statementAux.concat(";"); } if
	 * (!statementAux.toLowerCase().startsWith("db.")) {
	 * log.warn("updateByNativeQuery", "Expected MongoDB update statement"); throw
	 * new DBPersistenceException("Expected MongoDB update statement"); } if
	 * (statementAux.contains("db.")) { statementAux = statementAux.replace("db.",
	 * ""); } if (statementAux.contains("update(")) { statementAux =
	 * statementAux.substring(statementAux.indexOf("(") + 1,
	 * statementAux.lastIndexOf(")")); statementAux = statementAux.trim();
	 *
	 * int anidamiento = 0; int indiceInicioObjeto = 0; List<String> objetos = new
	 * ArrayList<String>(); for (int i = 0; i < statementAux.length(); i++) { if
	 * (statementAux.charAt(i) == '{') { anidamiento++; } else if
	 * (statementAux.charAt(i) == '}') { anidamiento--; } if
	 * ((statementAux.charAt(i) == ',' || i == statementAux.length() - 1) &&
	 * anidamiento == 0) { if (statementAux.charAt(i) == ',') { objetos.add(new
	 * String(statementAux.substring(indiceInicioObjeto, i))); } else {
	 * objetos.add(new String(statementAux.substring(indiceInicioObjeto))); }
	 * indiceInicioObjeto = i + 1; } }
	 *
	 * if (objetos.size() >= 2) { query = objetos.get(0); data = objetos.get(1);
	 *
	 * } else { log.warn("update",
	 * "Expected {$set:{[field:value]}} ||  {$inc:{[field:value]}}"); throw new
	 * DBPersistenceException("Expected {$set:{[field:value]}} ||  {$inc:{[field:value]}}"
	 * ); } } /* Native updates specify the document to be replaced; SQL-LIKE ones
	 * specify an update operator. We should remove this inconsistency. String
	 * updateOperation = util.prepareQuotes(data); String result =
	 * getUpdateStatement(data, ontology);return
	 *
	 * updateNative(ontology, query, updateOperation); }catch(
	 *
	 * Exception e) { log.error("update", e, statement); throw new
	 * DBPersistenceException("Necesary indicate a valid value " + e.getMessage());
	 * } }
	 */

	private String getUpdateStatement(String data, String collection) throws Exception {
		if (data.contains("$set")) {
			final Map<String, Object> mapa = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {
			});
			final String setStatement = (String) mapa.get("$set");
			return setStatement;
		} else {
			// FIX: in these cases, we replace one document with another one.
			// The $set makes the new
			// driver raise an exception.
			// return buildStatementForUpdateNativeFromSSAPResourceData(data);
			return data;
		}
	}

	@Override
	public String queryNativeAsJson(String collection, String query) throws DBPersistenceException {
		return queryNativeAsJson(collection, query, 0, getMaxRegisters());
	}

	@Override
	public String queryNativeAsJson(String ontology, String query, int offset, int limit)
			throws DBPersistenceException {

		final String preparedQuery = query.replace(" ", "");
		if (preparedQuery.contains("db." + ontology + ".count(")) {
			log.debug("countNativeAsJson", query, ontology);
			return "[ {\"value\": " + Long.toString(countNative(ontology, query)) + "}]";
		} else {
			log.debug("queryNativeAsJson", query, ontology);

			try {
				return JSON.serialize(queryNativeMongo(ontology, query, offset, limit));
			} catch (final javax.persistence.PersistenceException e) {
				log.error("find", e, query, ontology);
				throw new DBPersistenceException(e);
			}
		}
	}

	@Override
	public List<String> queryNative(String ontology, String query) throws DBPersistenceException {
		return queryNative(ontology, query, 0, getMaxRegisters());
	}

	/**
	 * this method converts a query in shell language to Java representation
	 * .limit() .sort() projections skip
	 *
	 */
	@Override
	public List<String> queryNative(String ontology, String query, int offset, int limit)
			throws DBPersistenceException {
		log.debug("queryNative", query, ontology);
		final List<String> result = new ArrayList<>();
		try {
			final MongoIterable<BasicDBObject> cursor = queryNativeMongo(ontology, query, offset, limit);
			for (final BasicDBObject obj : cursor)
				result.add(obj.toJson());
			return result;
		} catch (final DBPersistenceException e) {
			log.error("find", e, query, ontology);
			throw e;
		} catch (final Exception e) {
			log.error("find", e, query, ontology);
			throw new DBPersistenceException(e);
		}
	}

	private MongoIterable<BasicDBObject> queryNativeMongo(String ontology, String query, int offset, int limit) {
		log.debug("queryNativeMongo", query, ontology);
		try {
			log.info("executing query:" + query);
			final MongoQueryAndParams mc = new MongoQueryAndParams();
			if (limit == 0)
				limit = queryDefaultLimit;
			mc.parseQuery(query, limit, offset);
			final MongoIterable<BasicDBObject> cursor = mongoDbConnector.find(database, ontology, mc,
					queryExecutionTimeout);
			return cursor;
		} catch (final QueryNativeFormatException e) {
			log.error("Error: ", e, query, ontology);
			throw new QueryNativeFormatException(e);
		} catch (final DBPersistenceException e) {
			log.error("find", e, query, ontology);
			throw e;
		} catch (final Exception e) {
			log.error("find", e, query, ontology);
			throw new DBPersistenceException("Error querying native", e);
		}
	}

	@Override
	public String findById(String collection, String objectId) throws DBPersistenceException {
		try {
			final BasicDBObject o = mongoDbConnector.findById(database, collection, objectId);
			if (o != null)
				return o.toJson();
			return null;
		} catch (final Exception e) {
			log.error("findById", e, objectId);
			throw new DBPersistenceException("Error finding by id", e);
		}
	}

	@Override
	public String findAllAsJson(String ontology) throws DBPersistenceException {
		return JSON.serialize(findAll(ontology));
	}

	@Override
	public String findAllAsJson(String ontology, int limit) throws DBPersistenceException {
		return JSON.serialize(findAll(ontology));
	}

	@Override
	public List<String> findAll(String collection) throws DBPersistenceException {
		return findAll(collection, getMaxRegisters());
	}

	@Override
	public List<String> findAll(String collection, int limit) throws DBPersistenceException {
		final List<String> result = new ArrayList<>();
		log.debug("findAll", collection, limit);
		for (final BasicDBObject obj : mongoDbConnector.findAll(database, collection, 0, limit, queryExecutionTimeout))
			result.add(obj.toJson());
		return result;
	}

	@Override
	public long count(String collectionName) throws DBPersistenceException {
		return mongoDbConnector.count(database, collectionName, "{}");
	}

	@Override
	public long countNative(String collectionName, String query) throws DBPersistenceException {
		return mongoDbConnector.count(database, collectionName, query);
	}

	@Override
	public String querySQLAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			return quasarMongoConnector.queryAsJson(ontology, query, 0, getMaxRegisters());
		} catch (final DBPersistenceException e) {
			log.error("DBPersistenceException:" + e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			log.error(QUASAR_QUERY_ERROR + query, e);
			throw new DBPersistenceException(QUASAR_QUERY_ERROR + query, e);
		}
	}

	@Override
	public String querySQLAsTable(String ontology, String query) throws DBPersistenceException {
		try {
			return quasarMongoConnector.queryAsTable(query, 0, getMaxRegisters());
		} catch (final DBPersistenceException e) {
			log.error("DBPersistenceException:" + e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			log.error(QUASAR_QUERY_ERROR + query, e);
			throw new DBPersistenceException(QUASAR_QUERY_ERROR + query, e);
		}
	}

	@Override
	public String querySQLAsJson(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			return quasarMongoConnector.queryAsJson(ontology, query, offset, getMaxRegisters());
		} catch (final QueryNativeFormatException e) {
			log.error("QueryNativeFormatException:" + e.getMessage(), e);
			throw e;
		} catch (final DBPersistenceException e) {
			log.error("DBPersistenceException:" + e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			log.error(QUASAR_QUERY_ERROR + query, e);
			throw new DBPersistenceException(QUASAR_QUERY_ERROR + query, e);
		}
	}

	@Override
	public String querySQLAsTable(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			return quasarMongoConnector.queryAsTable(query, offset, getMaxRegisters());
		} catch (final DBPersistenceException e) {
			log.error("DBPersistenceException:" + e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			log.error(QUASAR_QUERY_ERROR + query, e);
			throw new DBPersistenceException(QUASAR_QUERY_ERROR + query, e);
		}
	}

}
