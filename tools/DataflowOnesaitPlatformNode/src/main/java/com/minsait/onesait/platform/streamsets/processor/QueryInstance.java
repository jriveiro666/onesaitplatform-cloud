/**
 * Copyright minsait by Indra Sistemas, S.A.
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
package com.minsait.onesait.platform.streamsets.processor;

import java.util.List;

public class QueryInstance {
	
	private List<Integer> indexes;
	private String query;

	/**
	 * Constructor
	 * Create a Query instance which has the indexes applicable to the query
	 * @param LinkedList<Integer>, String
	 */
	public QueryInstance(List<Integer> indexes, String query) {
		this.indexes = indexes;
		this.query = query;
	}
	
	/**
	 * Get the indexes of records involved in the query
	 * @return LinkedList<Integer>
	 */
	public List<Integer> getIndexes() {
		return indexes;
	}
	
	/**
	 * Returns the query
	 * @return String
	 */
	public String getQuery() {
		return query;
	}
	
}