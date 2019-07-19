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
package com.minsait.onesait.platform.persistence.interfaces;

import com.minsait.onesait.platform.persistence.exceptions.DBPersistenceException;

public interface QueryAsTextDBRepository {

	String queryNativeAsJson(String ontology, String query, int offset, int limit) throws DBPersistenceException;

	String queryNativeAsJson(String ontology, String query) throws DBPersistenceException;

	String querySQLAsJson(String ontology, String query, int offset) throws DBPersistenceException;

}