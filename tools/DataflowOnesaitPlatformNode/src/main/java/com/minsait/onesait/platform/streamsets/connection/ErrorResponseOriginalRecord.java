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
package com.minsait.onesait.platform.streamsets.connection;

import com.minsait.onesait.platform.streamsets.Errors;
import com.streamsets.pipeline.api.Record;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponseOriginalRecord {
	private Record oriRecord;
	private String responseErrorText;
	private Errors error;
	
	public ErrorResponseOriginalRecord(Record oriRecord, String responseErrorText, Errors error) {
		this.oriRecord = oriRecord;
		this.responseErrorText = responseErrorText; 
		this.error = error;
	}
}
