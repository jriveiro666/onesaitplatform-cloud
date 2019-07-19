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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;
import com.minsait.onesait.platform.persistence.interfaces.QueryAsTextDBRepository;

import lombok.extern.slf4j.Slf4j;

@Component("QueryAsTextVirtualDBImpl")
@Scope("prototype")
@Slf4j
public class QueryAsTextVirtualDBImpl implements QueryAsTextDBRepository {

	@Autowired
	private VirtualRelationalOntologyOpsDBRepository virtualRelationalOntologyOps;

	@Override
	public String queryNativeAsJson(String ontology, String query, int offset, int limit)
			throws DBPersistenceException {
		try {
			return this.virtualRelationalOntologyOps.queryNativeAsJson(ontology, query, offset, limit);
		} catch (Exception e) {
			log.error("Error queryNativeAsJson:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public String queryNativeAsJson(String ontology, String query) throws DBPersistenceException {
		try {
			return this.virtualRelationalOntologyOps.queryNativeAsJson(ontology, query);
		} catch (Exception e) {
			log.error("Error queryNativeAsJson:" + e.getMessage(), e);
			throw new DBPersistenceException(e);
		}
	}

	@Override
	public String querySQLAsJson(String ontology, String query, int offset) throws DBPersistenceException {
		try {
			return this.virtualRelationalOntologyOps.querySQLAsJson(ontology, query, offset);
		} catch (Exception e) {
			log.error("Error querySQLAsJson:" + e.getMessage());
			throw new DBPersistenceException(e);
		}
	}

}
