package com.minsait.onesait.platform.streamsets.destination;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.FieldSelectorModel;
import com.streamsets.pipeline.api.Label;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

public class OnesaitFilterColumnMapping {

	public OnesaitFilterColumnMapping() {
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, label = "Record field", description = "The field in the record to receive the value. It should start with / and point to a primitive value.", displayPosition = 10)
	@FieldSelectorModel(singleValued = true)
	public String field;

	public String GetField() {
		return field.trim().startsWith("/") ? field.trim() : "/" + field.trim();
	}

	public enum OperatorType implements Label {
		EQUALS("="), LESSTHAN("<"), MORETHAN(">"), LESSEQTHAN("<="), MOREEQTHAN(">="), DIFFERENT("<>");

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

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Column name", description = "The data source column name.", displayPosition = 20)
	public String columnName;

	public String GetColumnName() {
		return columnName.trim();
	}

	public enum LogicalOperators implements Label {
		AND(" AND "), OR(" OR ");

		private final String label;

		LogicalOperators(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class LogicalSelector extends BaseEnumChooserValues<LogicalOperators> {
		public LogicalSelector() {
			super(LogicalOperators.class);
		}
	}

	@ConfigDef(type = ConfigDef.Type.MODEL, required = true, defaultValue = "AND", label = "Logical Op.", description = "Logical operator. The last one will be omitted.", displayPosition = 25)
	@ValueChooserModel(LogicalSelector.class)
	public LogicalOperators logicalOperator;

	public String GetLogicalOperator() {
		return logicalOperator.getLabel();
	}

}
