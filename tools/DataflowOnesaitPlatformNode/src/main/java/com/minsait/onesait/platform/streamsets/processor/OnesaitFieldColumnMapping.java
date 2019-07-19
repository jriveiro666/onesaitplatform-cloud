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
package com.minsait.onesait.platform.streamsets.processor;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.FieldSelectorModel;
import com.streamsets.pipeline.api.Label;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

public class OnesaitFieldColumnMapping {

	/**
	 * Parameter-less constructor required.
	 */
	public OnesaitFieldColumnMapping() {
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, label = "Record field", description = "The field in the record to receive the value. It should start with / and point to a primitive value.", displayPosition = 10)
	@FieldSelectorModel(singleValued = true)
	public String field;

	public String GetField() {
		return field.trim().startsWith("/") ? field.trim() : "/" + field.trim();
	}
	
	public enum OperatorType implements Label {
		EQUALS("="),
		LESSTHAN("<"),
		MORETHAN(">"),
		LESSEQTHAN("<="),
		MOREEQTHAN(">="),
		DIFFERENT("<>");

		private final String label;

		OperatorType(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class OperatorSelector extends BaseEnumChooserValues<OperatorType> {
		public OperatorSelector() {
			super(OperatorType.class);
		}
	}
	
	@ConfigDef(type = ConfigDef.Type.MODEL, required = true, defaultValue = "EQUALS", label = "Operator", description = "SQL operator", displayPosition = 15)
	@ValueChooserModel(OperatorSelector.class)
	public OperatorType operator;

	public String GetOperator() {
		return operator.getLabel();
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Column name", description = "The data source property path.", displayPosition = 20)
	public String columnName;

	public String GetColumnName() {
		return columnName.trim();
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Optional path", description = "If the returned document from the ontology lacks of root paths, the path of the value may be different from the column name field and the record may not match. This path can be set on this field separated by dots (.), otherwise leave blank.", displayPosition = 30)
	public String searchPath;

	public String GetSearchPath() {
		return searchPath.trim();
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Is timestamp?", description = "Wraps the value in TIMESTAMP() to perform the query.", displayPosition = 40 )
	public boolean isTimeStamp;
	
	public boolean isTimeStamp() {
		return isTimeStamp;
	}
}
