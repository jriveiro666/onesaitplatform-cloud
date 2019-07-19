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

//import com.minsait.onesait.platform.streamsets.processor.OnesaitUpdateColumnMapping;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.ListBeanModel;
import com.streamsets.pipeline.api.StageDef;

@StageDef(version = 1, label = "OnesaitPlatform Update Destination", description = "Updates data as a OnesaitPlatform Ontology Instance", icon = "onesait_vector_update_2.png", producesEvents = true, recordsByRef = true, onlineHelpRefUrl = "")
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class OnesaitplatformDTargetUpdate extends OnesaitplatformTargetUpdate {

	// ------- Connection host

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

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "19000", label = "Port", description = "Port of the IoT Broker", displayPosition = 30, group = "SOURCE")
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

	// ------- /Connection host

	// ------- Bulk and threads

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "2", label = "Bulk", description = "Bulk", displayPosition = 1, group = "DESTINATION")
	public Integer bulk;

	/** {@inheritDoc} */
	@Override
	public Integer getBulk() {
		return bulk;
	}

	@ConfigDef(required = true, type = ConfigDef.Type.NUMBER, defaultValue = "1", label = "ThreadPool", description = "Number of threads to execute", displayPosition = 2, group = "DESTINATION")
	public Integer thread;

	/** {@inheritDoc} */
	@Override
	public Integer getThread() {
		return thread;
	}

	// ------- /Bulk and threads

	// ------- Field filtering

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, label = "Filtering fields", description = "Fields used to perform the filter of update operation. This fields must be unique index of ontology with timestamp field in order to speed up the process and avoid posible duplicates.", displayPosition = 30, group = "DESTINATION")
	@ListBeanModel
	public List<OnesaitFilterColumnMapping> filterFields;

	/** {@inheritDoc} */
	@Override
	public List<OnesaitFilterColumnMapping> getFilterFields() {
		return filterFields.stream().filter(filter -> !filter.GetColumnName().isEmpty() && !filter.GetField().isEmpty()).collect(Collectors.toList());
	}

	@ConfigDef(required = true, type = ConfigDef.Type.MODEL, label = "Update fields", description = "Fields used to perform the update operatio.", displayPosition = 40, group = "DESTINATION")
	@ListBeanModel
	public List<OnesaitUpdateColumnMapping> updateFields;

	/** {@inheritDoc} */
	@Override
	public List<OnesaitUpdateColumnMapping> getUpdateFields() {
		return updateFields.stream().filter(field -> !field.GetColumnName().isEmpty() && !field.GetField().isEmpty()).collect(Collectors.toList());
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
