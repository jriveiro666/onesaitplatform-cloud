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
/*******************************************************************************
 * © Indra Sistemas, S.A.
 * 2013 - 2014  SPAIN
 * 
 * All rights reserved
 ******************************************************************************/
package com.minsait.onesait.platform.streamsets.processor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.minsait.onesait.platform.streamsets.RecordEL;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigDef.Mode;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Label;
import com.streamsets.pipeline.api.ListBeanModel;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

@StageDef(version = 1, label = "OnesaitPlatform Lookup", description = "Look ups for data as a OnesaitPlatform Ontology Instance", icon = "onesait_vector_processor.png", producesEvents = true, recordsByRef = true, onlineHelpRefUrl = "")
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class OnesaitplatformDProcessor extends OnesaitplatformProcessor {

	// SOURCE

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "http", label = "Protocol", description = "Protocol of the IoT Broker", displayPosition = 10, group = "SOURCE")
	public String protocol;

	@Override
	public String getProtocol() {
		return protocol;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "iotbrokerservice", label = "Host", description = "Host of the IoT Broker", displayPosition = 20, group = "SOURCE")
	public String host;

	/** {@inheritDoc} */
	@Override
	public String getHost() {
		return host;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "19000", min = 0, label = "Port", description = "Port of the IoT Broker", displayPosition = 30, group = "SOURCE")
	public Integer port;

	/** {@inheritDoc} */
	@Override
	public Integer getPort() {
		return port;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, label = "Avoid SSL validation", defaultValue = "false", description = "Avoids SSL certificate validation ** USE ONLY IF YOU TRUST THE SOURCE **", group = "SOURCE", displayPosition = 35)
	public boolean avoidSSL;

	/** {@inheritDoc} */
	@Override
	public boolean getAvoidSSL() {
		return avoidSSL;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "token", label = "Token", description = "Access token of the IoT client", displayPosition = 40, group = "SOURCE")
	public String token;

	/** {@inheritDoc} */
	@Override
	public String getToken() {
		return token;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "IoT Client", defaultValue = "IoTClient", description = "IoT client related to the token", displayPosition = 50, group = "SOURCE")
	public String device;

	/** {@inheritDoc} */
	@Override
	public String getDevice() {
		return device;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, label = "IoT Client ID", defaultValue = "IoTClientID", description = "IoT Client Instance, if empty will be random UUID", displayPosition = 60, group = "SOURCE")
	public String deviceId;

	/** {@inheritDoc} */
	@Override
	public String getDeviceId() {
		return deviceId;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "ontology", description = "Ontology to connect to", label = "Ontology", displayPosition = 70, group = "SOURCE")
	public String ontology;

	/** {@inheritDoc} */
	@Override
	public String getOntology() {
		return ontology;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "10000", description = "Maximum characters to create the get query using the matching option. (Generated SQL)", label = "Max. GET characters", displayPosition = 80, group = "SOURCE")
	public Integer maxCharacters;

	/** {@inheritDoc} */
	@Override
	public Integer getMaxCharacters() {
		return maxCharacters;
	}

	// /SOURCE

	// CONFIGURATION

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "100", min = 1, label = "Bulk", description = "The total number of records are divided in sub batches to process them by the threads.", displayPosition = 10, group = "CONFIGURATION")
	public Integer bulk;

	/** {@inheritDoc} */
	@Override
	public Integer getBulk() {
		return bulk;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "1", min = 1, label = "ThreadPool", description = "Sub batches are processed asynchronously by the threads created. Every thread can process several sub batches.", displayPosition = 20, group = "CONFIGURATION")
	public Integer thread;

	/** {@inheritDoc} */
	@Override
	public Integer getThread() {
		return thread;
	}

	// ------- /Field filtering

	// For SQL

	public enum CallTypeEnum implements Label {
		FIELDMATCHING("Field matching (Generates performant SQL calls.)"),
		SQL("SQL (One call per record. It could generate excesive load on the host.)");

		private final String label;

		CallTypeEnum(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class CallTypeEnumValueChooser extends BaseEnumChooserValues<CallTypeEnum> {
		public CallTypeEnumValueChooser() {
			super(CallTypeEnum.class);
		}
	}

	@ConfigDef(label = "Query type", defaultValue = "FIELDMATCHING", type = ConfigDef.Type.MODEL, required = true, group = "QUERY", description = "Query type.", displayPosition = 25)
	@ValueChooserModel(CallTypeEnumValueChooser.class)
	public CallTypeEnum callType;

	@Override
	public boolean IsSQL() {
		switch (callType) {
		case SQL:
			return true;
		default:
			return false;
		}
	}

	@ConfigDef(required = true, type = ConfigDef.Type.TEXT, mode = Mode.SQL, label = "Query", defaultValue = "SELECT * FROM <ontology> WHERE ...", description = "Query to obtain data.", group = "QUERY", elDefs = {
			RecordEL.class }, evaluation = ConfigDef.Evaluation.EXPLICIT, displayPosition = 30, dependsOn = "callType", triggeredByValue = "SQL")
	public String query;

	@Override
	public String GetQuery() {
		return query;
	}

	@ConfigDef(required = true, dependsOn = "callType", triggeredByValue = "FIELDMATCHING", type = ConfigDef.Type.MODEL, label = "SELECT", description = "Columns to select in the query. Leave empty to select all the document. Equivalent to SELECT clause in SQL.", displayPosition = 50, group = "QUERY")
	@ListBeanModel
	public List<OnesaitColumnSelector> columnSelector;

	/** {@inheritDoc} */
	@Override
	public List<String> getColumnSelector() {
		return columnSelector.stream().filter(col -> !col.GetColumnName().isEmpty()).map(col -> col.GetColumnName()).distinct().collect(Collectors.toList());
	}

	@ConfigDef(required = true, dependsOn = "callType", triggeredByValue = "FIELDMATCHING", type = ConfigDef.Type.MODEL, label = "WHERE", defaultValue = "", description = "Mapping from record fields to data source columns. Equivalent to the WHERE clause in SQL.", displayPosition = 55, group = "QUERY")
	@ListBeanModel
	public List<OnesaitFieldColumnMapping> columnMappings;

	/** {@inheritDoc} */
	@Override
	public List<OnesaitFieldColumnMapping> getColumnMappings() {
		return columnMappings.stream().filter(col -> !col.GetColumnName().isEmpty() && !col.GetField().isEmpty()).collect(Collectors.toList());
	}

	// Multiple values found policy
	public enum MultipleValuesFoundEnum implements Label {

		FIRST("Use the first record"), CREATELIST("Create list inside record"),
		GENERATEMULTIPLE("Generate multiple records");

		private final String label;

		MultipleValuesFoundEnum(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class MultipleValuesFoundEnumValueChooser extends BaseEnumChooserValues<MultipleValuesFoundEnum> {
		public MultipleValuesFoundEnumValueChooser() {
			super(MultipleValuesFoundEnum.class);
		}
	}

	@ConfigDef(label = "Multiple value behavior", defaultValue = "FIRST", type = ConfigDef.Type.MODEL, required = true, group = "CONFIGURATION", description = "Sets the action to perform if multiple values are found. If the path to put the data on can be merged (lists or maps), the fields will merge prevailing the data fetched, otherwise the field will be overwritten.", displayPosition = 60)
	@ValueChooserModel(MultipleValuesFoundEnumValueChooser.class)
	public MultipleValuesFoundEnum multipleValues;

	/** {@inheritDoc} */
	@Override
	public MultipleValuesFoundEnum getMultipleValues() {
		return multipleValues;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Set data on path", defaultValue = "/", description = "Where to put the data retreived. Path described by / symbol. Eg. /data/lookup ", displayPosition = 65, group = "CONFIGURATION")
	public String dataPosition;

	public String GetDataPosition() {
		return dataPosition.trim().isEmpty() ? "/" : dataPosition.trim();
	}

	public enum NoValueTypeEnum implements Label {

		ERROR("Send to error."), UNCHANGED("Left the record unchanged."), NULLS("Create fields provided in the SELECT option with null values.");

		private final String label;

		NoValueTypeEnum(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class NoValueTypeEnumValueChooser extends BaseEnumChooserValues<NoValueTypeEnum> {
		public NoValueTypeEnumValueChooser() {
			super(NoValueTypeEnum.class);
		}
	}

	@ConfigDef(label = "No value policy", defaultValue = "ERROR", type = ConfigDef.Type.MODEL, required = true, group = "CONFIGURATION", description = "Sets the action to perform if no value is matched.", displayPosition = 70)
	@ValueChooserModel(NoValueTypeEnumValueChooser.class)
	public NoValueTypeEnum noValuePolicy;

	/** {@inheritDoc} */
	@Override
	public NoValueTypeEnum getNoValuePolicy() {
		return noValuePolicy;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Retry on missing values", description = "Performs new look ups if the value is missing, otherwise the next record won't search for matching values in the cache and source.", displayPosition = 75, group = "CONFIGURATION")
	public boolean retryMissing;

	/** {@inheritDoc} */
	@Override
	public boolean getRetry() {
		return retryMissing;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "60", min = 1, label = "Retry after", description = "Retry and lookup again after the selected value.", displayPosition = 80, group = "CONFIGURATION", dependsOn = "retryMissing", triggeredByValue = "true")
	public Integer timeValueMissing;

	/** {@inheritDoc} */
	@Override
	public Integer getTimeValueMissing() {
		return timeValueMissing;
	}

	@ConfigDef(label = "Time unit", defaultValue = "MINUTES", type = ConfigDef.Type.MODEL, required = true, group = "CONFIGURATION", description = "Sets the time unit", displayPosition = 85, dependsOn = "retryMissing", triggeredByValue = "true")
	@ValueChooserModel(TimeUnitEnumValueChooser.class)
	public TimeUnitEnum timeUnitMissing;
	
	@Override
	public TimeUnit getTimeUnitMissing() {
		return getTime(timeUnitMissing);
	}
	
	// /CONFIGURATION

	// CACHE

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "TRUE", label = "Use local cache", description = "Check it to activate local cache.", displayPosition = 10, group = "CACHE")
	public Boolean localCache;

	/** {@inheritDoc} */
	@Override
	public Boolean getLocalCache() {
		return localCache;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Limit cache entries", description = "Check it to limit cache entries", displayPosition = 20, group = "CACHE", dependsOn = "localCache", triggeredByValue = "true")
	public Boolean limitCache;

	/** {@inheritDoc} */
	@Override
	public Boolean getLimitCache() {
		return limitCache;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "1000", min = 1, label = "Max. entries", description = "Maximum number of entries of the local cache", displayPosition = 30, group = "CACHE", dependsOn = "limitCache", triggeredByValue = "true")
	public Integer entriesNumber;

	/** {@inheritDoc} */
	@Override
	public Integer getEntriesNumber() {
		return entriesNumber;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "TRUE", label = "Cache expiration", description = "Sets expiration to the local cache entries.", displayPosition = 40, group = "CACHE", dependsOn = "localCache", triggeredByValue = "true")
	public Boolean cacheExpiration;

	/** {@inheritDoc} */
	@Override
	public Boolean getCacheExpiration() {
		return cacheExpiration;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "60", min = 1, label = "Expiration time", description = "Expiration time value, select unit below", displayPosition = 50, group = "CACHE", dependsOn = "cacheExpiration", triggeredByValue = "true")
	public Integer timeValue;

	/** {@inheritDoc} */
	@Override
	public Integer getTimeValue() {
		return timeValue;
	}

	// Expiration time unit
	public enum TimeUnitEnum implements Label {

		SECONDS("Seconds"),	MINUTES("Minutes"), HOURS("Hours"), DAYS("Days");

		private final String label;

		TimeUnitEnum(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class TimeUnitEnumValueChooser extends BaseEnumChooserValues<TimeUnitEnum> {
		public TimeUnitEnumValueChooser() {
			super(TimeUnitEnum.class);
		}
	}

	@ConfigDef(label = "Time unit", defaultValue = "MINUTES", type = ConfigDef.Type.MODEL, required = true, group = "CACHE", description = "Sets the time unit", displayPosition = 60, dependsOn = "cacheExpiration", triggeredByValue = "true")
	@ValueChooserModel(TimeUnitEnumValueChooser.class)
	public TimeUnitEnum timeUnit;

	@Override
	public TimeUnit getTimeUnit() {
		return getTime(timeUnit);
	}
	
	private TimeUnit getTime(TimeUnitEnum tu) {
		switch (tu) {
			case SECONDS:
				return TimeUnit.SECONDS;
			case HOURS:
				return TimeUnit.HOURS;
			case DAYS:
				return TimeUnit.DAYS;
			case MINUTES:
			default:
				return TimeUnit.MINUTES;
		}
	}

	public enum ExpirationTypeEnum implements Label {

		ACCESS("After last access to the entry"), WRITE("After last write to the entry");

		private final String label;

		ExpirationTypeEnum(String label) {
			this.label = label;
		}

		@Override
		public String getLabel() {
			return label;
		}
	}

	public static class ExpirationTypeEnumValueChooser extends BaseEnumChooserValues<ExpirationTypeEnum> {
		public ExpirationTypeEnumValueChooser() {
			super(ExpirationTypeEnum.class);
		}
	}

	@ConfigDef(label = "Expiration policy", defaultValue = "ACCESS", type = ConfigDef.Type.MODEL, required = true, group = "CACHE", description = "Sets the expiration policy of cache entries", displayPosition = 70, dependsOn = "cacheExpiration", triggeredByValue = "true")
	@ValueChooserModel(ExpirationTypeEnumValueChooser.class)
	public ExpirationTypeEnum expirationPolicy;

	/** {@inheritDoc} */
	@Override
	public ExpirationTypeEnum getExpirationPolicy() {

		return expirationPolicy;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "false", label = "Initial cache population", description = "Populates the cache with the data from the ontology.", displayPosition = 80, group = "CACHE", dependsOn = "localCache", triggeredByValue = {"true" })
	public boolean initialLoad;

	public boolean getInitialLoad() {
		return this.initialLoad;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "2000", min = 100, label = "Registries per request", description = "Number of documents to retrieve from the ontology in the initial load per call.", displayPosition = 90, group = "CACHE", dependsOn = "initialLoad", triggeredByValue = {"true" })
	public int maxRecordInitialLoad;

	public int getMaxRecordInitialLoad() {
		return this.maxRecordInitialLoad;
	}

	// /CACHE
	
	
	// Proxy
	
	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "false", label = "Use proxy", description = "Use proxy", displayPosition = 10, group = "PROXY")
	public boolean useProxy;

	public boolean getUseProxy() {
		return useProxy;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "", label = "Host", description = "Host of the proxy", displayPosition = 20, group = "PROXY", dependsOn = "useProxy", triggeredByValue = { "true" })
	public String proxyHost;

	@Override
	public String getProxyHost() {
		return proxyHost;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "80", min = 0, label = "Port", description = "Port of the proxy", displayPosition = 30, group = "PROXY", dependsOn = "useProxy", triggeredByValue = { "true" })
	public int proxyPort;

	@Override
	public int getProxyPort() {
		return proxyPort;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.CREDENTIAL, defaultValue = "", label = "User", description = "User of the proxy", displayPosition = 40, group = "PROXY", dependsOn = "useProxy", triggeredByValue = { "true" })
	public String proxyUser;

	@Override
	public String getProxyUser() {
		return proxyUser;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.CREDENTIAL, defaultValue = "", label = "Password", description = "User password of the proxy", displayPosition = 50, group = "PROXY", dependsOn = "useProxy", triggeredByValue = { "true" })
	public String proxyUserPassword;

	@Override
	public String getProxyUserPassword() {
		return proxyUserPassword;
	}
	
	// /Proxy

}
