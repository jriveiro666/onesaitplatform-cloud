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
package com.minsait.onesait.platform.streamsets.destination;

import java.util.List;
import java.util.stream.Collectors;

import com.minsait.onesait.platform.streamsets.destination.beans.OntologyProcessInstance;
import com.minsait.onesait.platform.streamsets.destination.beans.OntologyProcessInstanceChooserValues;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesTime;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesTimeChooserValues;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.FieldSelectorModel;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.ValueChooserModel;

@StageDef(version = 1, label = "OnesaitPlatform Destination", description = "Insert data as a OnesaitPlatform Ontology Instance", icon = "onesait_vector_insert.png", producesEvents = true, recordsByRef = true, onlineHelpRefUrl = "")
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class OnesaitplatformDTarget extends OnesaitplatformTarget {

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "http", label = "Protocol", description = "Protocol of the IoT Broker", displayPosition = 10, group = "SOURCE")
	public String protocol;

	@Override
	public String getProtocol() {
		return protocol;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "iotbrokerservice", label = "Host", description = "Host of the IoT Broker", displayPosition = 20, group = "SOURCE")
	public String host;

	@Override
	public String getHost() {
		return host;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "19000", label = "Port", description = "Port of the IoT Broker", displayPosition = 30, group = "SOURCE", min = 1, max = 65535)
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

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "token", label = "Token", description = "Access token of the IoT client", displayPosition = 40, group = "SOURCE")
	public String token;

	@Override
	public String getToken() {
		return token;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, label = "IoT Client", defaultValue = "IoTClient", description = "IoT client related to the token", displayPosition = 50, group = "SOURCE")
	public String device;

	@Override
	public String getDevice() {
		return device;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, label = "IoT Client ID", defaultValue = "IoTClientID", description = "IoT Client Instance, if empty will be random UUID", displayPosition = 60, group = "SOURCE")
	public String deviceId;

	@Override
	public String getDeviceId() {
		return deviceId;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "ontology", description = "Ontology to connect to", label = "Ontology", displayPosition = 70, group = "SOURCE")
	public String ontology;

	@Override
	public String getOntology() {
		return ontology.trim();
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, defaultValue = "NOROOTNODE", label = "Root node type", displayPosition = 80, group = "SOURCE")
	@ValueChooserModel(OntologyProcessInstanceChooserValues.class)
	public OntologyProcessInstance ontologyProcessInstance;

	@Override
	public OntologyProcessInstance getOntologyProcessInstance() {
		return ontologyProcessInstance;
	}
	
	@Override
	public String getRootNode() {
		switch (ontologyProcessInstance) {
			case CUSTOMNAME:
				return customRootNode.trim();
			case ONTOLOGYNAME:
				return ontology.trim();
			default:
				return null;
		}
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "", label = "Custom root node name", displayPosition = 90, group = "SOURCE", dependsOn = "ontologyProcessInstance", triggeredByValue = { "CUSTOMNAME" } )
	public String customRootNode;

	@Override
	public String getCustomRootNode() {
		return customRootNode;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "2", label = "Bulk", description = "Bulk", displayPosition = 1, group = "DESTINATION", min = 1)
	public Integer bulk;

	@Override
	public Integer getBulk() {
		return bulk;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "1", label = "ThreadPool", description = "Number of threads to execute", displayPosition = 2, group = "DESTINATION", min = 1)
	public Integer thread;

	@Override
	public Integer getThread() {
		return thread;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Timeseries ontology", description = "Check it when your destination ontology is onesait platform standard mongo timeseries type, update-insert base instance will be use instead bulk insert", displayPosition = 3, group = "DESTINATION")
	public Boolean timeseriesOntology;

	@Override
	public Boolean getTimeseriesOntology() {
		return timeseriesOntology;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "TRUE", label = "Timeseries Multiupdate", description = "Check it when you want to activate timeseries multiupdate. This operation group your data by your update fields in order to reduce the number of updates with many $set and $inc operations", displayPosition = 4, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")
	public Boolean timeseriesMultiupdate;

	@Override
	public Boolean getTimeseriesMultiupdate() {
		return timeseriesMultiupdate;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, defaultValue = "TENMINUTES", label = "Type ontology", description = "Time type interval of timeseries ontology", displayPosition = 5, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")
	@ValueChooserModel(TimeseriesTimeChooserValues.class)
	public TimeseriesTime timeseriesTimeOntology;

	@Override
	public TimeseriesTime getTimeseriesTimeOntology() {
		return timeseriesTimeOntology;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "timestamp", label = "Timeseries time field of ontology", description = "This field must be of timestamp json date format: yyyy-MM-dd'T'HH:mm:ss.SSS", displayPosition = 6, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")

	public String timeseriesFieldOntology;

	@Override
	public String getTimeseriesFieldOntology() {
		return timeseriesFieldOntology;
	}
	
	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "false", label = "Overwrite ", description = "If active, the insert won't check if the document to write has null values and will overwrite what alread has, but the sumatory pre-calc and count pre-calc will be disabled.", displayPosition = 7, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")
	public boolean ignoreNulls;

	@Override
	public boolean getIgnoreNulls() {
		return ignoreNulls;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, defaultValue = "v", label = "Value timeseries estructure field", displayPosition = 8, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true" )
	public String valueTimeseriesField;

	@Override
	public String getValueTimeseriesField() {
		return valueTimeseriesField;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Enable sumatory pre-calc", displayPosition = 9, group = "DESTINATION", dependsOn = "ignoreNulls", triggeredByValue = "false")
	public Boolean precalcSumTimeseries;

	@Override
	public Boolean getPrecalcSumTimeseries() {
		return precalcSumTimeseries;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, defaultValue = "s", label = "Sumatory pre-calc field", description = "This field must be of numeric type", displayPosition = 10, group = "DESTINATION", dependsOn = "precalcSumTimeseries", triggeredByValue = "true")
	public String precalcSumTimeseriesField;

	@Override
	public String getPrecalcSumTimeseriesField() {
		return precalcSumTimeseriesField;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.BOOLEAN, defaultValue = "FALSE", label = "Enable count pre-calc", displayPosition = 11, group = "DESTINATION", dependsOn = "ignoreNulls", triggeredByValue = "false")
	public Boolean precalcCountTimeseries;

	@Override
	public Boolean getPrecalcCountTimeseries() {
		return precalcCountTimeseries;
	}

	@ConfigDef(required = false, type = ConfigDef.Type.STRING, defaultValue = "c", label = "Count pre-calc field", displayPosition = 12, group = "DESTINATION", dependsOn = "precalcCountTimeseries", triggeredByValue = "true")
	public String precalcCountTimeseriesField;

	@Override
	public String getPrecalcCountTimeseriesField() {
		return precalcCountTimeseriesField;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, defaultValue = "", label = "Update fields list", description = "Fields used to perform the filter of update operation plus timestamp field. This fields must be unique index of ontology with timestamp field in order to speed up the process and avoid posible duplicates", displayPosition = 13, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")
	@FieldSelectorModel
	public List<String> updateFields;

	@Override
	public List<String> getUpdateFields() {
		return updateFields.stream().map( field -> field.startsWith("/") ? field.substring(1) : field ).collect(Collectors.toList());
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "value", label = "Origin timeseries value field", displayPosition = 14, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")

	public String originTimeseriesValueField;

	@Override
	public String getOriginTimeseriesValueField() {
		return originTimeseriesValueField;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.STRING, defaultValue = "values", label = "Destination ontology timeseries value field", displayPosition = 15, group = "DESTINATION", dependsOn = "timeseriesOntology", triggeredByValue = "true")
	public String destinationTimeseriesValueField;

	@Override
	public String getDestinationTimeseriesValueField() {
		return destinationTimeseriesValueField;
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

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "80", min = 0, max = 65535, label = "Port", description = "Port of the proxy", displayPosition = 30, group = "PROXY", dependsOn = "useProxy", triggeredByValue = { "true" })
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
