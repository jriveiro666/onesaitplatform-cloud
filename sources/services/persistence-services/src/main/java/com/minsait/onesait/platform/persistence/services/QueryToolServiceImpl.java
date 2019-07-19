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
package com.minsait.onesait.platform.persistence.services;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.model.Ontology.RtdbDatasource;
import com.minsait.onesait.platform.config.services.ontology.OntologyService;
import com.minsait.onesait.platform.config.services.templates.PlatformQuery;
import com.minsait.onesait.platform.config.services.templates.QueryTemplateService;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.exceptions.QueryNativeFormatException;
import com.minsait.onesait.platform.persistence.factory.QueryAsTextDBRepositoryFactory;
import com.minsait.onesait.platform.persistence.mongodb.quasar.connector.QuasarMongoDBbHttpConnector;
import com.minsait.onesait.platform.persistence.mongodb.tools.sql.Sql2NativeTool;
import com.minsait.onesait.platform.persistence.services.util.QueryParsers;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class QueryToolServiceImpl implements QueryToolService {

	@PersistenceContext(unitName = "onesaitPlatform")
	private EntityManager entityManager;

	@Autowired
	private OntologyService ontologyService;

	@Autowired
	private QueryTemplateService queryTemplateService;

	@Autowired
	private QueryAsTextDBRepositoryFactory queryAsTextDBRepositoryFactory;

	@Autowired
	private QuasarMongoDBbHttpConnector quasarConnector;

	private void hasUserPermission(String user, String ontology, String query) throws DBPersistenceException {
		if (!ontologyService.hasUserPermissionForQuery(user, ontology)) {
			throw new DBPersistenceException("User:" + user + " has nos permission to query ontology " + ontology);
		}
		if (query.toLowerCase().indexOf("update") != -1 || query.toLowerCase().indexOf("remove") != -1
				|| query.toLowerCase().indexOf("delete") != -1 || query.toLowerCase().indexOf("createindex") != -1

		) {
			if (!ontologyService.hasUserPermissionForInsert(user, ontology)) {
				throw new DBPersistenceException(
						"User:" + user + " has nos permission to update,insert or remove on ontology " + ontology);
			}
		}
	}

	private void hasClientPlatformPermisionForQuery(String clientPlatform, String ontology)
			throws DBPersistenceException {
		if (!ontologyService.hasClientPlatformPermisionForQuery(clientPlatform, ontology)) {
			throw new DBPersistenceException(
					"Client Platform:" + clientPlatform + " has nos permission to query ontology " + ontology);
		}
	}

	@Override
	public String queryNativeAsJson(String user, String ontology, String query, int offset, int limit)
			throws DBPersistenceException {
		try {
			hasUserPermission(user, ontology, query);

			return queryAsTextDBRepositoryFactory.getInstance(ontology, user).queryNativeAsJson(ontology, query, offset,
					limit);
		} catch (final Exception e) {
			log.error("Error queryNativeAsJson:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public String queryNativeAsJson(String user, String ontology, String query)
			throws QueryNativeFormatException, DBPersistenceException {
		try {
			hasUserPermission(user, ontology, query);
			return queryAsTextDBRepositoryFactory.getInstance(ontology, user).queryNativeAsJson(ontology, query);
		} catch (final QueryNativeFormatException e) {
			throw e;
		} catch (final Exception e) {
			log.error("Error queryNativeAsJson:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	private String querySQLAsJson(String user, String ontology, String query, int offset, boolean checkTemplates)
			throws DBPersistenceException {
		try {
			hasUserPermission(user, ontology, query);

			query = QueryParsers.parseFunctionNow(query);
			if (checkTemplates) {
				final PlatformQuery newQuery = queryTemplateService.getTranslatedQuery(ontology, query);
				if (newQuery != null) {

					switch (newQuery.getType()) {
					case SQL:
						return querySQLAsJson(user, ontology, newQuery.getQuery(), offset, false);
					case NATIVE:
						return queryNativeAsJson(user, ontology, newQuery.getQuery());
					default:
						throw new IllegalStateException("Only SQL or NATIVE queries are supported");
					}
				}
			}

			return queryAsTextDBRepositoryFactory.getInstance(ontology, user).querySQLAsJson(ontology, query, offset);

		} catch (final QueryNativeFormatException e) {
			log.error("Error querySQLAsJson:" + e.getMessage());
			throw e;
		} catch (final Exception e) {
			log.error("Error querySQLAsJson:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public String querySQLAsJson(String user, String ontology, String query, int offset) throws DBPersistenceException {
		return querySQLAsJson(user, ontology, query, offset, true);
	}

	@Override
	public String queryNativeAsJsonForPlatformClient(String clientPlatform, String ontology, String query, int offset,
			int limit) throws DBPersistenceException {

		try {
			hasClientPlatformPermisionForQuery(clientPlatform, ontology);
			return queryAsTextDBRepositoryFactory.getInstanceClientPlatform(ontology, clientPlatform)
					.queryNativeAsJson(ontology, query, offset, limit);
		} catch (final Exception e) {
			log.error("Error queryNativeAsJsonForPlatformClient:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	private String querySQLAsJsonForPlatformClient(String clientPlatform, String ontology, String query, int offset,
			boolean checkTemplates) {
		try {
			hasClientPlatformPermisionForQuery(clientPlatform, ontology);
			if (checkTemplates) {
				final PlatformQuery newQuery = queryTemplateService.getTranslatedQuery(ontology, query);
				if (newQuery != null) {

					switch (newQuery.getType()) {
					case SQL:
						return querySQLAsJsonForPlatformClient(clientPlatform, ontology, newQuery.getQuery(), offset,
								false);
					case NATIVE:
						return queryNativeAsJsonForPlatformClient(clientPlatform, ontology, newQuery.getQuery(), offset,
								0);
					default:
						throw new IllegalStateException("Only SQL or NATIVE queries are supported");
					}
				}
			}

			query = QueryParsers.parseFunctionNow(query);
			return queryAsTextDBRepositoryFactory.getInstanceClientPlatform(ontology, clientPlatform)
					.querySQLAsJson(ontology, query, offset);
		} catch (final Exception e) {
			log.error("Error querySQLAsJsonForPlatformClient:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public String querySQLAsJsonForPlatformClient(String clientPlatform, String ontology, String query, int offset)
			throws DBPersistenceException {
		return querySQLAsJsonForPlatformClient(clientPlatform, ontology, query, offset, true);
	}

	@Override
	public String compileSQLQueryAsJson(String user, Ontology ontology, String query, int offset)
			throws DBPersistenceException {
		hasUserPermission(user, ontology.getIdentification(), query);
		if (ontology != null && ontology.getRtdbDatasource().equals(RtdbDatasource.MONGO)) {
			if (query.trim().toLowerCase().startsWith("update") || query.trim().toLowerCase().startsWith("delete")) {
				final ObjectMapper mapper = new ObjectMapper();
				final ObjectNode result = mapper.createObjectNode();
				result.put("sqlQuery", query);
				try {
					final String nativeQuery = Sql2NativeTool.translateSql(query);
					result.put("nativeQuery", nativeQuery);
					return mapper.writeValueAsString(result);
				} catch (final Exception e) {
					throw new DBPersistenceException("SQL Syntax Error");
				}

			} else
				return quasarConnector.compileQueryAsJson(ontology.getIdentification(), query, offset);
		} else
			throw new DBPersistenceException("You can only compile SQL with Mongo Ontologies");

	}

	@Override
	@Transactional
	public List<String> getTables() {
		final List<String> table_list = new LinkedList<>();
		final org.hibernate.Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			final DatabaseMetaData metaData = connection.getMetaData();
			final ResultSet rs = metaData.getTables(null, null, "%", null);
			while (rs.next()) {
				table_list.add(rs.getString(3));
			}
		});

		return table_list;
	}

	@Override
	@Transactional
	public Map<String, String> getTableColumns(String tableName) {
		final Map<String, String> table_list = new TreeMap<>();
		final org.hibernate.Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			final DatabaseMetaData metaData = connection.getMetaData();
			final ResultSet rs = metaData.getColumns(null, null, tableName, "%");
			while (rs.next()) {
				table_list.put(rs.getString(4), rs.getString(6));
			}
		});

		return table_list;
	}

	@Override
	@Transactional
	public List<String> querySQLtoConfigDB(String query) {
		final List<String> queryResult = new LinkedList<>();
		final org.hibernate.Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			final PreparedStatement statement = connection.prepareStatement(query);
			final ResultSet rs = statement.executeQuery();
			final int ncol = rs.getMetaData().getColumnCount();
			int i = 0;
			String row = "";
			while (rs.next()) {
				row = "{";
				i = 0;
				while (i < ncol) {
					row = row + "\"" + rs.getMetaData().getColumnName(i + 1) + "\": \"" + rs.getString(i + 1) + "\",";
					i++;
					if (i == ncol) {
						row = row.substring(0, row.length() - 1);
					}
				}
				queryResult.add(row + "}");
			}
		});
		return queryResult;
	}

}
