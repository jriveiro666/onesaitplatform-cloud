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
package com.minsait.onesait.platform.persistence.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.minsait.onesait.platform.commons.model.BulkWriteResult;
import com.minsait.onesait.platform.commons.model.ComplexWriteResult;
import com.minsait.onesait.platform.commons.model.DBResult;
import com.minsait.onesait.platform.commons.model.MultiDocumentOperationResult;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESCountService;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESDataService;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESDeleteService;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESInsertService;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESNativeService;
import com.minsait.onesait.platform.persistence.elasticsearch.api.ESUpdateService;
import com.minsait.onesait.platform.persistence.elasticsearch.sql.connector.ElasticSearchSQLDbHttpImpl;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.interfaces.BasicOpsDBRepository;
import com.minsait.onesait.platform.persistence.models.ErrorResult;
import com.minsait.onesait.platform.resources.service.IntegrationResourcesService;

import lombok.extern.slf4j.Slf4j;

@Component("ElasticSearchBasicOpsDBRepository")
@Scope("prototype")
@Lazy
@Slf4j
public class ElasticSearchBasicOpsDBRepository implements BasicOpsDBRepository {

	private static final String NOT_IMPLEMENTED = "Not implemented";

	@Autowired
	private IntegrationResourcesService resourcesServices;

	@Autowired
	private ESCountService eSCountService;
	@Autowired
	private ESDataService eSDataService;
	@Autowired
	private ESDeleteService eSDeleteService;
	@Autowired
	private ESInsertService eSInsertService;
	@Autowired
	private ESUpdateService eSUpdateService;
	@Autowired
	private ElasticSearchSQLDbHttpImpl elasticSearchSQLDbHttpConnector;

	@Autowired
	private ESNativeService eSNativeService;

	@Override
	public String insert(String ontology, String schema, String instance) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(schema, "Schema can't be null or empty");
			Assert.hasLength(instance, "Instance can't be null or empty");
			ontology = ontology.toLowerCase();
			log.info(String.format("ElasticSearchBasicOpsDBRepository : Loading content: %s into elasticsearch  %s",
					instance, ontology));
			List<? extends DBResult> output = null;
			final List<String> instances = new ArrayList<String>();
			instances.add(instance);
			output = eSInsertService.load(ontology, ontology, instances, schema).getData();
			return ((BulkWriteResult) output.get(0)).getId();
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error inserting instance :" + instance + " into :" + ontology);
		}
	}

	@Override
	public ComplexWriteResult insertBulk(String ontology, String schema, List<String> instances, boolean order,
			boolean includeIds) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(schema, "Schema can't be null or empty");
			Assert.notEmpty(instances, "Instances can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSInsertService.load(ontology, ontology, instances, schema);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error inserting instances :" + instances + " into :" + ontology);
		}
	}

	@Override
	@Deprecated
	public MultiDocumentOperationResult updateNative(String ontology, String updateStmt, boolean includeIds)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(updateStmt, "Statement can't be null or empty");
			ontology = ontology.toLowerCase();
			log.info(String.format("ElasticSearchBasicOpsDBRepository :Update Native"));
			SearchResponse output = null;
			output = eSNativeService.updateByQuery(ontology, ontology, updateStmt);
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			result.setCount(output.getHits().totalHits);
			return result;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in operation ES updateNative");
		}
	}

	@Override
	@Deprecated
	public MultiDocumentOperationResult updateNative(String collection, String query, String data, boolean includeIds)
			throws DBPersistenceException {
		try {
			Assert.hasLength(collection, "Collection can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			Assert.hasLength(data, "Data can't be null or empty");
			collection = collection.toLowerCase();
			SearchResponse output = null;
			output = eSNativeService.updateByQueryAndFilter(collection, collection, data, query);
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			result.setCount(output.getHits().totalHits);
			return result;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in operation ES updateNative");
		}
	}

	@Override
	public MultiDocumentOperationResult deleteNative(String ontology, String query, boolean includeIds)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSDeleteService.deleteByQuery(ontology, ontology, query, includeIds);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in operation ES delete native");
		}
	}

	@Override
	public List<String> queryNative(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSDataService.findQueryData(query, ontology);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query native");
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
			ontology = ontology.toLowerCase();
			return eSDataService.findAllByType(ontology, query, offset, limit);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query native");
		}
	}

	@Override
	public String queryNativeAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSDataService.findQueryDataAsJson(query, ontology);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query native as json");
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
			ontology = ontology.toLowerCase();
			final String output = eSDataService.findAllByTypeAsJson(ontology, offset, limit);
			return output;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query native as json");
		}
	}

	@Override
	public String findById(String ontology, String objectId) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(objectId, "ID can't be null or empty");
			ontology = ontology.toLowerCase();
			final String getResponse = eSDataService.findByIndex(ontology, ontology, objectId);
			return getResponse;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in finding by ID");
		}
	}

	@Override
	public String querySQLAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			ontology = ontology.toLowerCase();
			return elasticSearchSQLDbHttpConnector.queryAsJson(query,
					((Integer) resourcesServices.getGlobalConfiguration().getEnv().getDatabase().get("queries-limit"))
							.intValue());
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query SQL as json");
		}
	}

	// TODO IMPLEMENT
	@Override
	public String querySQLAsTable(String ontology, String query) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			ontology = ontology.toLowerCase();
			throw new DBPersistenceException("Error in query SQL as table",
					new NotImplementedException(NOT_IMPLEMENTED));
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query SQL as table");
		}
	}

	@Override
	public String querySQLAsJson(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			ontology = ontology.toLowerCase();
			return elasticSearchSQLDbHttpConnector.queryAsJson(query, offset);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query SQL as json");
		}
	}

	// TODO IMPLEMENT
	@Override
	public String querySQLAsTable(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(query, "Query can't be null or empty");
			Assert.isTrue(offset >= 0, "Offset must be greater or equals to 0");
			ontology = ontology.toLowerCase();
			throw new DBPersistenceException("Error in query SQL as table",
					new NotImplementedException(NOT_IMPLEMENTED));
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in query SQL as table");
		}
	}

	@Override
	public String findAllAsJson(String ontology) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			ontology = ontology.toLowerCase();
			final String output = eSDataService.findAllByTypeAsJson(ontology, 200);
			return output;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in finding all as json");
		}
	}

	@Override
	public String findAllAsJson(String ontology, int limit) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.isTrue(limit >= 0, "Limit must be greater or equals to 0");
			ontology = ontology.toLowerCase();
			final String output = eSDataService.findAllByTypeAsJson(ontology, limit);
			return output;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in finding all as json");
		}
	}

	@Override
	public List<String> findAll(String ontology) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			ontology = ontology.toLowerCase();
			final List<String> output = eSDataService.findAllByType(ontology);
			return output;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in finding all");
		}
	}

	@Override
	public List<String> findAll(String ontology, int limit) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.isTrue(limit >= 0, "Limit must be greater or equals to 0");
			ontology = ontology.toLowerCase();
			final List<String> output = eSDataService.findAllByType(ontology, limit);
			return output;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in finding all");
		}
	}

	@Override
	public long count(String ontology) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSCountService.getMatchAllQueryCountByType(ontology, ontology);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in count");
		}
	}

	@Override
	public MultiDocumentOperationResult delete(String ontology, boolean includeIds) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			ontology = ontology.toLowerCase();
			final long count = count(ontology);
			final boolean all = eSDeleteService.deleteAll(ontology, ontology);

			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			if (all) {
				result.setCount(1);
			} else {
				result.setCount(-1);
			}
			return result;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in delete native");
		}
	}

	@Override
	public long countNative(String ontology, String jsonQueryString) throws DBPersistenceException {
		try {
			Assert.hasLength(ontology, "Ontology can't be null or empty");
			Assert.hasLength(jsonQueryString, "json can't be null or empty");
			ontology = ontology.toLowerCase();
			return eSCountService.getQueryCount(jsonQueryString, ontology);
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in count native");
		}
	}

	@Override
	public MultiDocumentOperationResult deleteNativeById(String ontologyName, String objectId)
			throws DBPersistenceException {
		try {
			Assert.hasLength(ontologyName, "Ontology can't be null or empty");
			Assert.hasLength(objectId, "ID can't be null or empty");
			ontologyName = ontologyName.toLowerCase();
			final boolean all = eSDeleteService.deleteById(ontologyName, ontologyName, objectId);
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			if (all) {
				result.setCount(1);
			} else {
				result.setCount(-1);
			}
			return result;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in delete native by id");
		}
	}

	@Override
	public MultiDocumentOperationResult updateNativeByObjectIdAndBodyData(String ontologyName, String objectId,
			String body) throws DBPersistenceException {
		try {
			Assert.hasLength(ontologyName, "Ontology can't be null or empty");
			Assert.hasLength(objectId, "ID can't be null or empty");
			Assert.hasLength(body, "Body can't be null or empty");
			ontologyName = ontologyName.toLowerCase();
			final boolean response = eSUpdateService.updateIndex(ontologyName, ontologyName, objectId, body);
			MultiDocumentOperationResult result = new MultiDocumentOperationResult();
			if (response == true) {
				result.setCount(1);
			} else {
				result.setCount(-1);
			}
			return result;
		} catch (final DBPersistenceException e) {
			throw e;
		} catch (final Exception e) {
			throw new DBPersistenceException(e, new ErrorResult(ErrorResult.PersistenceType.ELASTIC, e.getMessage()),
					"Error in update native");
		}

	}
}
