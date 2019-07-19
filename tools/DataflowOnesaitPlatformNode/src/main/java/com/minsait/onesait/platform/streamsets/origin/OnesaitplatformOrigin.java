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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperationsREST;
import com.minsait.onesait.platform.streamsets.connection.ProxyConfig;
import com.minsait.onesait.platform.streamsets.format.FieldParser;
import com.minsait.onesait.platform.streamsets.origin.OnesaitplatformDOrigin.QueryTypeEnum;
import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseSource;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OnesaitplatformOrigin extends BaseSource {

	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformOrigin.class);

	public abstract String getProtocol();
	public abstract String getHost();
	public abstract Integer getPort();
	public abstract boolean getAvoidSSL();
	public abstract String getToken();
	public abstract String getDevice();
	public abstract String getDeviceId();
	public abstract String getOntology();
	
	public abstract String getQuery();
	public abstract Integer getBatch();
	public abstract QueryTypeEnum getQueryType();
	public abstract Integer getDelay();
	public abstract Long getStartOffset();
	public abstract boolean getStopOnComplete();
	
	public abstract boolean getUseProxy();
	public abstract String getProxyHost();
	public abstract int getProxyPort();
	public abstract String getProxyUser();
	public abstract String getProxyUserPassword();	

	private boolean firstBatch;
	private long noMoreDataRecordCount = 0;
	private long lastRecords = 0;
	
	private ProxyConfig proxy;

	private DeviceOperations deviceOperations;

	/** {@inheritDoc} */
	@SuppressWarnings("deprecation")
	@Override
	protected List<ConfigIssue> init() {
		// Validate configuration values and open any required resources.
		List<ConfigIssue> issues = super.init();

		if (getProtocol().equals("invalidValue") || getProtocol() == null) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Protocol required"));
		}
		if (getHost().equals("invalidValue") || getHost() == null) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Host required"));
		}
		if (getPort() <= 0 || getPort() == null) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Port required"));
		}
		if (getToken().equals("invalidValue") || getToken() == null) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Token required"));
		}
		if (getDevice().equals("invalidValue") || getDevice() == null) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Device required"));
		}
		if (getOntology().equals("invalidValue") || getOntology() == null) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Ontology required"));
		}
		if (getQuery() == null || getQuery().isEmpty() || (getQuery().contains("${batch}") && !getQuery().contains("${offset}"))
				|| (!getQuery().contains("${batch}") && getQuery().contains("${offset}"))) {
			issues.add(getContext().createConfigIssue(Groups.QUERY.name(), "Query", Errors.ERROR_00,
					"Variables offset and batch must be used together."));
		}
		if (getBatch() <= 0 || getBatch() == null) {
			issues.add(getContext().createConfigIssue(Groups.QUERY.name(), "Query", Errors.ERROR_00,
					"Batch must be greater than 0"));
		}
		if (getDelay() < 0 || getDelay() == null) {
			issues.add(getContext().createConfigIssue(Groups.QUERY.name(), "Query", Errors.ERROR_00,
					"Delay must be equal or greater than 0"));
		}
		if (getStartOffset() < 0 || getStartOffset() == null) {
			issues.add(getContext().createConfigIssue(Groups.QUERY.name(), "Query", Errors.ERROR_00,
					"Offset must be equal or greater than 0"));
		}
		
		this.proxy = getUseProxy() ? new ProxyConfig(getProxyHost(), getProxyPort(), getProxyUser(), getProxyUserPassword()) : null;

		// If issues is not empty, the UI will inform the user of each
		// configuration issue in the list.
		if (issues.isEmpty()) {
			try {
				this.deviceOperations = new DeviceOperationsREST(getProtocol(), getHost(), getPort(), getToken(), getDevice(), getDeviceId(), null,
						null, null, this.getAvoidSSL(), getContext().getOnErrorRecord(), false, getUseProxy(), this.proxy);
			} catch (Exception e) {
				logger.error("Error init rest operation ", e);
			}
		}
		return issues;
	}

	@Override
	public void destroy() {
		try {
			this.deviceOperations.leave();
		} catch (Exception e) {
			logger.error("Error on finish: " + e.getMessage(), e);
			this.getContext().reportError(Errors.ERROR_06, "Error on finish: " + e.getMessage());
		} finally {
			this.getContext().finishPipeline();
			super.destroy();
		}
	}

	@Override
	public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
		long nextSourceOffset = 0;
		long resultsCount = 0;
		
		String finalBatch = this.getContext().isPreview() ? ""+maxBatchSize : getBatch().toString();

		try {
			// If the offset is null we set the offset from the form
			nextSourceOffset = lastSourceOffset == null ? getStartOffset() : Long.parseLong(lastSourceOffset);

			// If there are variables in the query, we replace them with the data
			String modQuery = getQuery();
			if (getQuery().indexOf("${batch}") != -1 && getQuery().indexOf("${offset}") != -1) {
				modQuery = modQuery.replace("${batch}", finalBatch);
				modQuery = modQuery.replace("${offset}", "" + nextSourceOffset);
			}

			// Delay between queries
			if (getDelay().intValue() > 0 && !firstBatch) Thread.sleep(getDelay());

			if (firstBatch) {
				// If it's the first batch, trigger the event
				initEvent();
				firstBatch = false;
			}

			String message = deviceOperations.query(getOntology(), modQuery, getQueryType().name());

			JsonArray jsnarray = (new JsonParser()).parse(message).getAsJsonArray();
			resultsCount = jsnarray.size();

			for (JsonElement jsnobject : jsnarray) {
				Record record = getContext().createRecord(getOntology() + nextSourceOffset);
				try {
					Map<String, Field> rootmap = FieldParser.ParseJsonObject(jsnobject.getAsJsonObject());
					if (rootmap.isEmpty()) throw new Exception("No data in the document (empty). Document: "+jsnobject.toString());
					record.set(Field.create(Field.Type.LIST_MAP, rootmap));
					batchMaker.addRecord(record);
				} catch (Exception e) {
					if (getContext().getOnErrorRecord() != OnRecordError.DISCARD) 
						this.getContext().reportError(Errors.ERROR_21, e.getMessage());
				} finally {
					nextSourceOffset++;
					this.noMoreDataRecordCount++;
				}
			}

			if (resultsCount == 0) {
				if (this.lastRecords != resultsCount && this.noMoreDataRecordCount > 0)
					generateNoMoreDataEvent();

				// Stops pipeline if user selects option on configuration
				if (getStopOnComplete()) {
					logger.info("[User configuration] Stopping pipeline due to no new results.");
					this.destroy();
				}
			}

		} catch (JsonParseException e) {
			if (getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
				logger.error("ERROR: Parsing query json ", e);
				this.getContext().reportError(Errors.ERROR_21, "Parsing query json: " + e.getMessage());
			}
		} catch (Exception e) {
			if (getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
				logger.error("Error doing query: " + e.getMessage(), e);
				this.getContext().reportError(Errors.ERROR_20, "Error doing query: " + e.getMessage());
			}
		}

		this.lastRecords = resultsCount;

		logger.debug("[ORIGIN] Procesed " + resultsCount + " records, of a total of " + nextSourceOffset);
		return String.valueOf(nextSourceOffset);
	}

	private void generateNoMoreDataEvent() {
		OnesaitOriginEvents.NO_MORE_DATA.create(getContext()).with("record-count", noMoreDataRecordCount)
				.createAndSend();
		noMoreDataRecordCount = 0;
	}

	private void initEvent() {
		OnesaitOriginEvents.INIT.create(this.getContext()).with("init-pipeline", this.getInfo().getName())
				.createAndSend();
	}

}
