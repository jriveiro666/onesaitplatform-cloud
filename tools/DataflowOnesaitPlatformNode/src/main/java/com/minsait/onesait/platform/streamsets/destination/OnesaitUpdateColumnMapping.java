/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.minsait.onesait.platform.streamsets.destination;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.FieldSelectorModel;

public class OnesaitUpdateColumnMapping {

	/**
	 * Parameter-less constructor required.
	 */
	public OnesaitUpdateColumnMapping() {
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, label = "Record field", description = "The field in the record to receive the value. It should start with / and point to a primitive value.", displayPosition = 10)
	@FieldSelectorModel(singleValued = true)
	public String field;

	public String GetField() {
		return field.trim().startsWith("/") ? field.trim() : "/" + field.trim();
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Column name", description = "The data source column name.", displayPosition = 20)
	public String columnName;

	public String GetColumnName() {
		return columnName.trim();
	}

}
