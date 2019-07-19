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
package com.minsait.onesait.platform.persistence.hadoop.missing;

import static com.minsait.onesait.platform.persistence.hadoop.common.HadoopMessages.NOT_SUPPORTED;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.minsait.onesait.platform.commons.model.DescribeColumnData;
import com.minsait.onesait.platform.commons.rtdbmaintainer.dto.ExportData;
import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.interfaces.ManageDBRepository;

@Repository
public class DefaultManageDBRepository implements ManageDBRepository {

	@Override
	public Map<String, Boolean> getStatusDatabase() throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public String createTable4Ontology(String ontology, String schema, Map<String,String> config) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public List<String> getListOfTables() throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public List<String> getListOfTables4Ontology(String ontology) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void removeTable4Ontology(String ontology) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void createIndex(String ontology, String attribute) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void createIndex(String ontology, String nameIndex, String attribute) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void createIndex(String sentence) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void dropIndex(String ontology, String indexName) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public List<String> getListIndexes(String ontology) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public String getIndexes(String ontology) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public void validateIndexes(String ontology, String schema) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public ExportData exportToJson(String ontology, long startDateMillis, String path) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public long deleteAfterExport(String ontology, String query) {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public List<DescribeColumnData> describeTable(String name) {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public Map<String, String> getAdditionalDBConfig(String ontology) throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

	@Override
	public String updateTable4Ontology(String identification, String jsonSchema, Map<String, String> config)
			throws DBPersistenceException {
		throw new DBPersistenceException(NOT_SUPPORTED);
	}

}
