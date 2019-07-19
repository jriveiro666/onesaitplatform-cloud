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
import java.util.ArrayList;
import java.util.Map;

import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.base.SingleLaneProcessor.SingleLaneBatchMaker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkInstance {
	private List<String> queries;
	private List<Map<String, String>> queryData;
	private List<Record> originalRecords;
	private List<Record> modifiedRecords;
	private List<Record> recordsFound;
	private List<Record> recordsNotFound;
	private List<OnRecordErrorException> recordsError;
	
	private SingleLaneBatchMaker batchMaker;

	public BulkInstance(List<String> queries, List<Record> originalRecords, List<Map<String, String>> queryData, SingleLaneBatchMaker batchMaker) {
		this.originalRecords = originalRecords;
		this.queries = queries;
		this.queryData = queryData;
		this.batchMaker = batchMaker;

		// Records list for events
		this.recordsNotFound = new ArrayList<Record>();
		this.recordsFound = new ArrayList<Record>();
		
		// Output lists
		this.modifiedRecords = new ArrayList<Record>();
		this.recordsError = new ArrayList<OnRecordErrorException>();
	}

}
