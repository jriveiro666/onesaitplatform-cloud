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
package com.minsait.onesait.platform.persistence.external.virtual;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.minsait.onesait.platform.commons.model.ComplexWriteResult;
import com.minsait.onesait.platform.commons.model.MultiDocumentOperationResult;
import com.minsait.onesait.platform.config.model.OntologyVirtualDatasource;
import com.minsait.onesait.platform.config.model.OntologyVirtualDatasource.VirtualDatasourceType;
import com.minsait.onesait.platform.config.repository.OntologyVirtualRepository;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.external.exception.NotSupportedOperationException;
import com.minsait.onesait.platform.persistence.external.exception.SGDBNotSupportedException;
import com.minsait.onesait.platform.persistence.external.virtual.helper.VirtualOntologyHelper;
import com.minsait.onesait.platform.persistence.external.virtual.parser.JSONResultsetExtractor;
import com.minsait.onesait.platform.persistence.external.virtual.parser.JsonRelationalHelper;
import com.minsait.onesait.platform.persistence.models.ErrorResult;

import lombok.extern.slf4j.Slf4j;

@Component("VirtualOntologyOpsDBRepository")
@Lazy
@Slf4j
public class VirtualRelationalOntologyOpsDBRepository implements VirtualOntologyDBRepository {

	private static final String NOT_SUPPORTED_OPERATION = "Operation not supported for Virtual Ontologies";
	private static final String WHERE_STR = " WHERE ";
	private static final String SELECT_FROM = "SELECT * FROM ";
	private static final String OPERATION_NOT_SUPPORTED_FOR_THIS = "Operation not supported for this Virtual Ontology";

	@Autowired
	private OntologyVirtualRepository ontologyVirtualRepository;

	@Autowired
	private VirtualDatasourcesManager virtualDatasourcesManager;

	@Autowired
	@Qualifier("OracleVirtualOntologyHelper")
	private VirtualOntologyHelper oracleVirtualOntologyHelper;

	@Autowired
	@Qualifier("MysqlVirtualOntologyHelper")
	private VirtualOntologyHelper mysqlVirtualOntologyHelper;

	@Autowired
	@Qualifier("MariaDBVirtualOntologyHelper")
	private VirtualOntologyHelper mariaVirtualOntologyHelper;

	@Autowired
	@Qualifier("PostgreSQLVirtualOntologyHelper")
	private VirtualOntologyHelper postgreVirtualOntologyHelper;

	@Autowired
	@Qualifier("SQLServerVirtualOntologyHelper")
	private VirtualOntologyHelper sqlserverVirtualOntologyHelper;

	@Autowired
	@Qualifier("ImpalaVirtualOntologyHelper")
	private VirtualOntologyHelper impalaVirtualOntologyHelper;

	@Autowired
	@Qualifier("HiveVirtualOntologyHelper")
	private VirtualOntologyHelper hiveVirtualOntologyHelper;

	@Autowired
	private JsonRelationalHelper jsonRelationalHelper;

	private VirtualOntologyHelper getOntologyHelper(VirtualDatasourceType type) {
		switch (type) {
		case ORACLE:
			return oracleVirtualOntologyHelper;
		case MYSQL:
			return mysqlVirtualOntologyHelper;
		case MARIADB:
			return mariaVirtualOntologyHelper;
		case POSTGRESQL:
			return postgreVirtualOntologyHelper;
		case SQLSERVER:
			return sqlserverVirtualOntologyHelper;
		case IMPALA:
			return impalaVirtualOntologyHelper;
		case HIVE:
			return hiveVirtualOntologyHelper;
		default:
			throw new SGDBNotSupportedException("Not supported SGDB: " + type);
		}
	}

	private OntologyVirtualDatasource getDatasourceForOntology(String ontology) {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			OntologyVirtualDatasource ontologyDatasource = this.ontologyVirtualRepository
					.findOntologyVirtualDatasourceByOntologyIdentification(ontology);
			Assert.notNull(ontologyDatasource, "Datasource not found for virtual ontology: " + ontology);
			return ontologyDatasource;
		} catch (Exception e) {
			throw new DBPersistenceException("Datasource not found for virtual ontology: " + ontology, e);
		}
	}

	private JdbcTemplate getJdbTemplate(String ontology) {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			String datasourceName = this.getDatasourceForOntology(ontology).getDatasourceName();

			VirtualDataSourceDescriptor datasource = this.virtualDatasourcesManager
					.getDataSourceDescriptor(datasourceName);

			return new JdbcTemplate(datasource.getDatasource());

		} catch (Exception e) {
			throw new DBPersistenceException("JdbcTemplate not found for virtual ontology: " + ontology, e);
		}
	}

	@Override
	public List<String> getTables(String datasourceName) {
		try {
			Assert.hasLength(datasourceName, "Datasource name can't be null or empty");
			VirtualDataSourceDescriptor datasource = this.virtualDatasourcesManager
					.getDataSourceDescriptor(datasourceName);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource.getDatasource());
			VirtualOntologyHelper helper = getOntologyHelper(datasource.getVirtualDatasourceType());
			List<String> lTables = jdbcTemplate.queryForList(helper.getAllTablesStatement(), String.class);
			return lTables;
		} catch (Exception e) {
			log.error("Error listing tables from user in external database", e);
			throw new DBPersistenceException("Error listing tables from user in external database", e);
		}
	}

	@Override
	public String insert(String ontology, String schema, String instance) throws DBPersistenceException {
		try {
			String statement = jsonRelationalHelper.getInsertStatement(ontology, instance);
			JdbcTemplate jdbcTemplate = getJdbTemplate(ontology);
			jdbcTemplate.update(statement);
			String objectId = this.ontologyVirtualRepository
					.findOntologyVirtualObjectIdByOntologyIdentification(ontology);
			Assert.hasLength(objectId, "Virtual ontology not found");
			JSONObject json = new JSONObject(instance);
			return json.get(objectId).toString();
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error inserting data into virtual ontology");
		}
	}

	@Override
	public ComplexWriteResult insertBulk(String ontology, String schema, List<String> instances, boolean order,
			boolean includeIds) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(schema, "Schema can't be null or empty");
			Assert.notEmpty(instances, "Instances can't be null or empty");

			List<ErrorResult> errorList = new ArrayList<ErrorResult>();
			try {
				for (String instance : instances)
					this.insert(ontology, schema, instance);
			} catch (DBPersistenceException e) {
				errorList.addAll(((DBPersistenceException) e).getErrorsResult());
			}

			if (errorList.size() > 0) {
				throw new DBPersistenceException(errorList, "Error inserting bulk data into virtual ontology");
			}else {
				return new ComplexWriteResult();
			}

		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error inserting bulk data on virtual ontology");
		}
	}

	@Override
	public MultiDocumentOperationResult updateNative(String ontology, String updateStmt, boolean includeIds)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(updateStmt, "Statement can't be null or empty");
			JdbcTemplate jdbcTemplate = getJdbTemplate(ontology);
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			result.setCount(jdbcTemplate.update(updateStmt.replaceAll(";", "")));
			return result;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error updating data on virtual ontology");
		}
	}

	@Override
	public MultiDocumentOperationResult updateNative(String collection, String query, String data, boolean includeIds)
			throws DBPersistenceException {
		throw new DBPersistenceException("Not implemented method",
				new NotImplementedException(NOT_SUPPORTED_OPERATION));
	}

	@Override
	public MultiDocumentOperationResult deleteNative(String collection, String query, boolean includeIds)
			throws DBPersistenceException {
		try {
			Assert.hasLength(collection, "Collection can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			JdbcTemplate jdbcTemplate = getJdbTemplate(collection);
			int updated = jdbcTemplate.update(query.replaceAll(";", ""));
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			result.setCount(updated);
			return result;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error removing data on virtual ontology");
		}
	}

	@Override
	public List<String> queryNative(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			OntologyVirtualDatasource datasource = getDatasourceForOntology(ontology);
			VirtualOntologyHelper helper = getOntologyHelper(datasource.getSgdb());
			query = helper.addLimit(query, datasource.getQueryLimit());
			JdbcTemplate jdbcTemplate = getJdbTemplate(ontology);
			return jdbcTemplate.query(query.replaceAll(";", ""), new JSONResultsetExtractor(query));
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying data on virtual ontology");
		}
	}

	@Override
	public List<String> queryNative(String ontology, String query, int offset, int limit)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			Assert.isTrue(offset >= 0, "Offset must be greater or equals to 0");
			Assert.isTrue(limit >= 1, "Limit must be greater or equals to 1");
			OntologyVirtualDatasource datasource = getDatasourceForOntology(ontology);
			VirtualOntologyHelper helper = getOntologyHelper(datasource.getSgdb());
			query = helper.addLimit(query, Math.min(limit, datasource.getQueryLimit()), offset);
			JdbcTemplate jdbcTemplate = getJdbTemplate(ontology);
			return jdbcTemplate.query(query.replaceAll(";", ""), new JSONResultsetExtractor(query));
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying data on virtual ontology");
		}
	}

	@Override
	public String queryNativeAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			List<String> result = this.queryNative(ontology, query);
			JSONArray jsonResult = new JSONArray();
			for (String instance : result) {
				JSONObject obj = new JSONObject(instance);
				jsonResult.put(obj);
			}
			return jsonResult.toString();
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying on virtual ontology");
		}
	}

	@Override
	public String queryNativeAsJson(String ontology, String query, int offset, int limit)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			Assert.isTrue(offset >= 0, "Offset must be greater or equals to 0");
			Assert.isTrue(limit >= 1, "Limit must be greater or equals to 1");
			List<String> result = this.queryNative(ontology, query, offset, limit);
			JSONArray jsonResult = new JSONArray();
			for (String instance : result) {
				JSONObject obj = new JSONObject(instance);
				jsonResult.put(obj);
			}
			return jsonResult.toString();
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying on virtual ontology");
		}
	}

	@Override
	public String findById(String ontology, String objectId) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(objectId, "Query can't be null or empty");
			String objId = this.ontologyVirtualRepository.findOntologyVirtualObjectIdByOntologyIdentification(ontology);
			Assert.hasLength(objectId, "Virtual ontology not found");
			return this.queryNativeAsJson(ontology, SELECT_FROM + ontology + WHERE_STR + objId + " = " + objectId);
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error finding by id on virtual ontology");
		}
	}

	@Override
	public String querySQLAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			List<String> result = this.queryNative(ontology, query);
			JSONArray jsonResult = new JSONArray();
			for (String instance : result) {
				JSONObject obj = new JSONObject(instance);
				jsonResult.put(obj);
			}
			return jsonResult.toString();
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying on virtual ontology");
		}
	}

	@Override
	public String querySQLAsTable(String ontology, String query) throws DBPersistenceException {
		throw new DBPersistenceException("Not implemented method",
				new NotImplementedException(NOT_SUPPORTED_OPERATION));
	}

	@Override
	public String querySQLAsJson(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			Assert.isTrue(offset >= 0, "Offset must be greater or equals to 0");
			List<String> result = this.queryNative(ontology, query, offset,
					getDatasourceForOntology(ontology).getQueryLimit());
			JSONArray jsonResult = new JSONArray();
			for (String instance : result) {
				JSONObject obj = new JSONObject(instance);
				jsonResult.put(obj);
			}
			return jsonResult.toString();
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error querying on virtual ontology");
		}
	}

	@Override
	public String querySQLAsTable(String ontology, String query, int offset) throws DBPersistenceException {
		throw new DBPersistenceException("Not implemented method",
				new NotImplementedException(NOT_SUPPORTED_OPERATION));
	}

	@Override
	public String findAllAsJson(String ontology) throws DBPersistenceException {
		return this.queryNativeAsJson(ontology, SELECT_FROM + ontology);
	}

	@Override
	public String findAllAsJson(String ontology, int limit) throws DBPersistenceException {
		return this.queryNativeAsJson(ontology, SELECT_FROM + ontology, 0, limit);
	}

	@Override
	public List<String> findAll(String ontology) throws DBPersistenceException {
		return this.queryNative(ontology, SELECT_FROM + ontology);
	}

	@Override
	public List<String> findAll(String ontology, int limit) throws DBPersistenceException {
		return this.queryNative(ontology, SELECT_FROM + ontology);
	}

	@Override
	public long count(String ontology) throws DBPersistenceException {
		JSONArray result = new JSONArray(this.queryNativeAsJson(ontology, "SELECT COUNT(*) FROM " + ontology));
		Iterator<String> itr = result.getJSONObject(0).keys();
		String key = itr.next();
		return result.getJSONObject(0).getLong(key);
	}

	@Override
	public MultiDocumentOperationResult delete(String ontology, boolean includeIds) throws DBPersistenceException {
		return this.deleteNative(ontology, "DELETE FROM " + ontology, includeIds);
	}

	@Override
	public long countNative(String collectionName, String query) throws DBPersistenceException {
		JSONArray result = new JSONArray(this.queryNativeAsJson(collectionName, query));
		Iterator<String> itr = result.getJSONObject(0).keys();
		String key = itr.next();
		return result.getJSONObject(0).getLong(key);
	}

	@Override
	public MultiDocumentOperationResult deleteNativeById(String ontologyName, String objectId)
			throws DBPersistenceException {
		String objId;
		try {
			Assert.hasLength(ontologyName, "Ontology name can't be null or empty");
			Assert.hasLength(objectId, "ObjectID can't be null or empty");
			objId = this.ontologyVirtualRepository.findOntologyVirtualObjectIdByOntologyIdentification(ontologyName);
			Assert.hasLength(objId, "Virtual ontology not found");
			return this.deleteNative(ontologyName, "DELETE FROM " + ontologyName + WHERE_STR + objId + " = " + objectId,
					false);
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error removing data on virtual ontology");
		}
	}

	@Override
	public MultiDocumentOperationResult updateNativeByObjectIdAndBodyData(String ontologyName, String objectId,
			String body) throws DBPersistenceException {
		String objId;
		try {
			Assert.hasLength(ontologyName, "Ontology name can't be null or empty");
			Assert.hasLength(objectId, "ObjectID can't be null or empty");
			Assert.hasLength(body, "Body can't be null or empty");
			objId = this.ontologyVirtualRepository.findOntologyVirtualObjectIdByOntologyIdentification(ontologyName);
			Assert.hasLength(objId, "Virtual ontology not found");
			return this.updateNative(ontologyName,
					"UPDATE " + ontologyName + " SET " + body + WHERE_STR + objId + " = " + objectId, false);
		} catch (DBPersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error updating data on virtual ontology");
		}
	}

	@Override
	public List<String> getInstanceFromTable(String datasource, String query) {
		try {
			Assert.hasLength(datasource, "Datasource can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			query = getStatementFromJson(query);

			VirtualDataSourceDescriptor sourceDescriptor = this.virtualDatasourcesManager
					.getDataSourceDescriptor(datasource);

			if (log.isDebugEnabled())
				log.debug("Query requested: {}", query);
			query = getOntologyHelper(sourceDescriptor.getVirtualDatasourceType()).addLimit(query,
					sourceDescriptor.getQueryLimit());

			JdbcTemplate jdbcTemplate = new JdbcTemplate(sourceDescriptor.getDatasource());

			return jdbcTemplate.query(query.replaceAll(";", ""), new JSONResultsetExtractor(query));
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error getting instance data on virtual ontology");
		}
	}

	@Override
	public List<String> getTableMetadata(String datasource, String collection) {
		try {
			Assert.hasLength(datasource, "Datasource can't be null or empty");
			Assert.hasLength(collection, "Collection can't be null or empty");
			JSONObject json = new JSONObject();
			List<String> list = new LinkedList<String>();

			DatabaseMetaData dbmeta = this.virtualDatasourcesManager.getDataSourceDescriptor(datasource).getDatasource()
					.getConnection().getMetaData();
			ResultSet rs = dbmeta.getColumns(null, null, collection, null);
			while (rs.next())
				json.put(rs.getString("COLUMN_NAME"),
						this.extractResultSetRowField(rs, rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE")));
			list.add(json.toString());
			return list;
		} catch (Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.VIRTUAL, e.getMessage()),
					"Error getting meta data on virtual ontology");
		}
	}

	private Object extractResultSetRowField(ResultSet rs, String columnName, int rowType)
			throws JSONException, SQLException {
		switch (rowType) {
		case java.sql.Types.BOOLEAN:
			return true;
		case java.sql.Types.NUMERIC:
		case java.sql.Types.BIGINT:
		case java.sql.Types.INTEGER:
		case java.sql.Types.SMALLINT:
		case java.sql.Types.TINYINT:
		case java.sql.Types.DOUBLE:
		case java.sql.Types.FLOAT:
		case java.sql.Types.TIMESTAMP:
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
		case java.sql.Types.DECIMAL:
		case java.sql.Types.BIT:
		case java.sql.Types.BINARY:
		case java.sql.Types.ROWID:
			return 1;
		case java.sql.Types.VARCHAR:
		case java.sql.Types.NVARCHAR:
		case java.sql.Types.DATE:
		case java.sql.Types.TIME:
		case java.sql.Types.TIME_WITH_TIMEZONE:
		case java.sql.Types.CLOB:
		case java.sql.Types.BLOB:
		default:
			return "String";
		}
	}

	private String getStatementFromJson(String statement) {
		if (statement.trim().startsWith("{")) {
			statement = statement.substring(statement.indexOf("{") + 1);
		}
		if (statement.trim().endsWith("}")) {
			statement = statement.substring(0, statement.lastIndexOf("}"));
		}
		return statement;
	}

	@Override
	public String executeQuery(String ontology, String query) {
		if (query.toUpperCase().startsWith("SELECT")) {
			return this.queryNativeAsJson(ontology, query);
		} else if (query.toUpperCase().startsWith("UPDATE")) {
			return String.valueOf(this.updateNative(ontology, query, false));
		} else if (query.toUpperCase().startsWith("DELETE")) {
			return String.valueOf(this.deleteNative(ontology, query, false));
		} else if (query.toUpperCase().startsWith("SELECT COUNT")) {
			return String.valueOf(this.countNative(ontology, query));
		} else {
			throw new NotSupportedOperationException("Operation not supported for Virtual Ontology");
		}
	}

}
