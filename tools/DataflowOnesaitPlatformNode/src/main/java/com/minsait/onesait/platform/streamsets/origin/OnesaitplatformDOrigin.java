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
package com.minsait.onesait.platform.streamsets.origin;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigDef.Mode;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ElConstant;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

@StageDef(version = 1,  label = "OnesaitPlatform Origin", description = "Obtains data from Onesait Platform", execution = ExecutionMode.STANDALONE, icon = "onesait_vector_from.png", producesEvents = true, recordsByRef = true, resetOffset = true, onlineHelpRefUrl = "" )
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class OnesaitplatformDOrigin extends OnesaitplatformOrigin {

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Protocol", defaultValue = "http", description = "Protocol to listen on", group = "SOURCE", displayPosition = 10)
	public String protocol;
	
	@Override
	public String getProtocol() {
		return protocol;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Host", defaultValue = "iotbrokerservice", description = "Host to connect to", group = "SOURCE", displayPosition = 20)
	public String host;
	
	@Override
	public String getHost() {
		return host;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, label = "Port", defaultValue = "19000", description = "Port to use", group = "SOURCE", displayPosition = 30)
	public Integer port;
	
	@Override
	public Integer getPort() {
		return port;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, label = "Avoid SSL validation", defaultValue = "false", description = "Avoids SSL certificate validation ** USE ONLY IF YOU TRUST THE SOURCE **", group = "SOURCE", displayPosition = 35)
	public boolean avoidSSL;
	
	@Override
	public boolean getAvoidSSL() {
		return avoidSSL;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Token", defaultValue = "token", description = "Token used by Device", group = "SOURCE", displayPosition = 40)
	public String token;
	
	@Override
	public String getToken() {
		return token;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "IoT Client", defaultValue = "IoTClient", description = "IoT Client to connect", group = "SOURCE", displayPosition = 50)
	public String device;
	
	@Override
	public String getDevice() {
		return device;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, label = "IoT Client Id", defaultValue = "IoTClientID", description = "IoT Client Instance, if empty will be random UUID", group = "SOURCE", displayPosition = 60)
	public String deviceId;
	
	@Override
	public String getDeviceId() {
		return deviceId;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "Ontology", defaultValue = "ontology", description = "Ontology from obtain data", group = "SOURCE", displayPosition = 70)
	public String ontology;
	
	@Override
	public String getOntology() {
		return ontology;
	}

	public static class OriginDefs {

		@ElConstant(name = "batch", description = "Sets batch")
		public static final String BATCH = "${batch}";

		@ElConstant(name = "offset", description = "Sets offset")
		public static final String OFFSET = "${offset}";

	}

	@ConfigDef(required = true, type = ConfigDef.Type.TEXT, mode = Mode.SQL, label = "Query", elDefs = {
			OriginDefs.class }, defaultValue = "select * from <ontology> offset ${offset} limit ${batch}", description = "Query to obtain data. To use batch iteration, the query must contain ${batch} and ${offset} variables. Eg. select * from ontology offset ${offset} limit ${batch}.", group = "QUERY", displayPosition = 10)
	public String query;
	
	@Override
	public String getQuery() {
		return query;
	}

	// Query type selector
	public enum QueryTypeEnum {
		SQL, NATIVE
	}

	public static class QueryTypeEnumValueChooser extends BaseEnumChooserValues<QueryTypeEnum> {
		public QueryTypeEnumValueChooser() {
			super(QueryTypeEnum.class);
		}
	}

	@ConfigDef(label = "Query type", defaultValue = "SQL", type = ConfigDef.Type.MODEL, required = true, group = "QUERY", description = "Query type SQL or NATIVE", displayPosition = 20)
	@ValueChooserModel(QueryTypeEnumValueChooser.class)
	public QueryTypeEnum qTypeEnum;
	
	@Override
	public QueryTypeEnum getQueryType() {
		return qTypeEnum;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, label = "Batch number", defaultValue = "20", min = 1, description = "Sets the batch number of every query. The query must contain the variables ${batch} in the limit clause, and ${offset} in the offset clause, in order to produce an interator.", group = "QUERY", displayPosition = 30 )
	public Integer batch;
	
	@Override
	public Integer getBatch() {
		return batch;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, label = "Starting offset", defaultValue = "0", min = 0, description = "Sets the starting offset. Set to 0 to start querying from the first position.", group = "QUERY", displayPosition = 40)
	public Long startOffset;
	
	@Override
	public Long getStartOffset() {
		return startOffset;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, label = "Delay (ms)", min = 0, defaultValue = "0", description = "Sets the time span between queries in milliseconds", group = "QUERY", displayPosition = 50)
	public Integer delay;
	
	@Override
	public Integer getDelay() {
		return delay;
	}

	@ConfigDef(type = ConfigDef.Type.BOOLEAN, label = "Stop on complete", defaultValue = "false", description = "Stops the pipeline after finishes retreiving data", group = "QUERY", displayPosition = 60, required = true)
	public boolean stopOnComplete;
	
	@Override
	public boolean getStopOnComplete() {
		return stopOnComplete;
	}
	
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
