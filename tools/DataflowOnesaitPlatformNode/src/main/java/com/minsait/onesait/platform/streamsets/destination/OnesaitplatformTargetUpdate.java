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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperationsREST;
import com.minsait.onesait.platform.streamsets.connection.ProxyConfig;
import com.minsait.onesait.platform.streamsets.format.FieldParser;
import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseTarget;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.impl.Utils;

/**
 * This target is an example and does not actually write to any destination.
 */
public abstract class OnesaitplatformTargetUpdate extends BaseTarget {
	
	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformTargetUpdate.class);

	/**
	 * Gives access to the UI configuration of the stage provided by the
	 * {@link OnesaitplatformDTarget} class.
	 */
	
	// ------- Connection host
	
	public abstract String getProtocol();
	public abstract String getHost();
	public abstract Integer getPort();
	public abstract boolean getAvoidSSL();
	public abstract String getToken();
	public abstract String getDevice();
	public abstract String getDeviceId();
	public abstract String getOntology();
	
	// ------- Bulk and threads
	
	public abstract Integer getBulk();
	public abstract Integer getThread();
	
	// ------- Fields and columns
	
	public abstract List<OnesaitFilterColumnMapping> getFilterFields();
	public abstract List<OnesaitUpdateColumnMapping> getUpdateFields();
	
	// ------- Proxy
	
	public abstract boolean getUseProxy();
	public abstract String getProxyHost();
	public abstract int getProxyPort();
	public abstract String getProxyUser();
	public abstract String getProxyUserPassword();	
	
	// ------- Props
	
	private List<DeviceOperations> deviceOperations;
	private ExecutorService executor;
	private ProxyConfig proxy;

	/** {@inheritDoc} */
	@SuppressWarnings("deprecation")
	@Override
	protected List<ConfigIssue> init() {
		// Validate configuration values and open any required resources.
		List<ConfigIssue> issues = super.init();
		
		if (getProtocol().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Protocol required"));
		}
		if (getHost().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Host required"));
		}
		if (getPort() < 1) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Port required"));
		}
		if (getToken().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Token required"));
		}
		if (getDevice().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Device required"));
		}
		if (getOntology().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00,
					"Ontology required"));
		}
		if (getBulk() < 1){
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00,
					"Bulk must be greater than 0"));
		}
		if (getThread() < 1) {
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00,
					"ThreadPool must be greater than 0"));
		}
		if(getFilterFields().size() < 1){
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00,
					"Filter fields must be greater than 0"));
		}
		if(getUpdateFields().size() < 1){
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00,
					"Updated fields must be greater than 0"));
		}
		
		this.deviceOperations = new ArrayList<>();
		
		this.proxy = getUseProxy() ? new ProxyConfig(getProxyHost(), getProxyPort(), getProxyUser(), getProxyUserPassword()) : null;
		
		for (int i=0 ; i< getThread(); i++){
			try{
				this.deviceOperations.add(new DeviceOperationsREST(getProtocol(), getHost(), getPort(), 
						getToken(), getDevice(), getDeviceId(), null, null, null, this.getAvoidSSL(), 
						getContext().getOnErrorRecord(), false, getUseProxy(), this.proxy));
			}catch (Exception e) {
				logger.error("[UPDATE] error init rest operation ", e);
			}
		}
		
		logger.info("[UPDATE] ---------------------- init --------------------");
		logger.info("[UPDATE] The number of device operation is " + this.deviceOperations.size());
		logger.info("[UPDATE] ------------------------------------------------");
		logger.info("[UPDATE] Filters: "+getFilterFields().size()+" - Update fields: "+getUpdateFields().size());
		logger.info("[UPDATE] ------------------------------------------------");
		
		this.executor = Executors.newFixedThreadPool(getThread());
		
		// If issues is not empty, the UI will inform the user of each
		// configuration issue in the list.
		
		return issues;
	}

	/** {@inheritDoc} */
	@Override
	public void destroy() {
		// Clean up any open resources.
		super.destroy();
		for (DeviceOperations deviceOperation : this.deviceOperations){
			try {
				deviceOperation.leave();
			} catch (Exception e) {
				logger.error("[UPDATE] Error leaving ", e);
			}
		}
		
		try {
			if(executor != null) executor.shutdown();
		} catch (Exception e){
			logger.error("[UPDATE] Error shutting down executor ", e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void write(Batch batch) throws StageException {	
		Iterator<Record> batchIterator = batch.getRecords();
		List<InstancesStt> recordsPerBulk = separateRecordsInBulkMessages(batchIterator);
		if (!recordsPerBulk.isEmpty() && !recordsPerBulk.get(0).getUpdateableRest().isEmpty()) {
			Map<Integer, List<InstancesStt>> threadData = separateBulkMessagesForThreads(recordsPerBulk);
			this.executeThreads(threadData);
		} 
	}
	
	private List<InstancesStt> separateRecordsInBulkMessages (Iterator<Record> batchIterator) {
		List<InstancesStt> recordsPerBulk = new ArrayList<>();
		int bulkSize = getBulk();
		int index = 0;
		int itemsInBulk = 0;
		
		recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>()));
		generateInstancesFromRecords(batchIterator, bulkSize, index, itemsInBulk, recordsPerBulk);
				
		return recordsPerBulk;
	}
	
	private void generateInstancesFromRecords(Iterator<Record> batchIterator, int bulkSize, int index, int itemsInBulk, List<InstancesStt> recordsPerBulk) {
		while (batchIterator.hasNext()) {
			InstancesStt recordsInBulkPartition = recordsPerBulk.get(index);
			Record record = batchIterator.next();
			
			try {
				String formatedSQL = formatSQL(getOntology(), getFilterFields(), getUpdateFields(), record);
				recordsInBulkPartition.getUpdateableRest().add(formatedSQL);
				recordsInBulkPartition.getOriginalValues().add(new ArrayList<Record>(Arrays.asList(record)));
			} catch (Exception e) {
				if(getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("[UPDATE] Error while forming the SQL sentence: ", e);
					this.getContext().toError(record, Errors.ERROR_33, e.getMessage());
				}
			}
			
			itemsInBulk++;
			
			if (itemsInBulk >= bulkSize){
				index++;
				recordsPerBulk.add( new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>() ) ); 
				itemsInBulk = 0;
			}
		}
	}
	
	private String formatSQL(String ontology, List<OnesaitFilterColumnMapping> filters,  List<OnesaitUpdateColumnMapping> updateFields, Record record) throws Exception {
		String sql = "UPDATE "+ontology+" SET ";
		String set = "";
		String where = " WHERE ";
		String value;
		
		// WHERE
		for (OnesaitFilterColumnMapping cMap : filters) {
			String field = cMap.GetField();
			String cName = cMap.GetColumnName();
			
			Object val = FieldParser.ParseFieldValue(record.get(field));
			if(val == null) value = "NULL";
			else if(val instanceof String)
				value = "'"+val.toString().replaceAll("((?<![\\\\])['\"])", "\\\\'")+"'"; // Replaces all single or double quotes not preceded by \
			else 
				value = val.toString();
			
			where += cName+cMap.GetOperator()+value+cMap.GetLogicalOperator();
		}
		if(filters.size() > 0) where = where.substring(0, where.length() - 4);
		else throw new Exception("There are no filters in WHERE clause.");
		
		for (OnesaitUpdateColumnMapping cMap : updateFields) {
			String field = cMap.GetField();
			String cName = cMap.GetColumnName();
			
			Object val = FieldParser.ParseFieldValue(record.get(field));
			if(val == null) value = "NULL";
			else if(val instanceof String)
				value = "'"+val.toString().replaceAll("((?<![\\\\])['\"])", "\\\\'")+"'"; // Replaces all single or double quotes not preceded by \
			else 
				value = val.toString();

			set += cName+"="+value+", ";
		}
		
		if(updateFields.size() > 0) set = set.substring(0, set.length() - 2);
		else throw new Exception("There are no update fields in SET clause.");
				
		// RETURN
		return sql+set+where;
	}
	
	private Map<Integer, List<InstancesStt>> separateBulkMessagesForThreads (List<InstancesStt> recordsPerBulk) {
		Map<Integer, List<InstancesStt>> threadData = new HashMap<>();
		int threadId = 0;
		for (InstancesStt recordBulk : recordsPerBulk){
			
			if (threadData.get(threadId) == null)
				threadData.put(threadId, new ArrayList<InstancesStt>());
			
			threadData.get(threadId).add(recordBulk);
			threadId++;
			
			if (threadId == deviceOperations.size())
				threadId = 0;
		}	
		return threadData;
	}
	
	private void executeThreads (Map<Integer, List<InstancesStt>> treadData) throws StageException {
		logger.trace("[UPDATE] Number of threads to execute is " + getThread()+ "  --  Workers to create: "+treadData.size());
		Set<Future<List<ErrorManager>>> set = new HashSet<>();

		// Instead of using getThread, its better to use thread data  
		// because it may have less ThreadId than getthreads and
		// it may cause null bulkdata in the worker
		for (Map.Entry<Integer, List<InstancesStt>> entry : treadData.entrySet()) {
			Callable<List<ErrorManager>> worker = new OnesaitplatformWorker(deviceOperations.get(entry.getKey()), getOntology(), entry.getValue(), this.getContext());
			set.add(executor.submit(worker));
		}
				
		for (Future<List<ErrorManager>> future : set) {
			try{
				List<ErrorManager> errors = future.get();
				for(ErrorManager error : errors) {
					try {
						error.getException();
					} catch (OnRecordErrorException e) {
						switch (getContext().getOnErrorRecord()) {
							case DISCARD:
								break;
							case TO_ERROR:
								getContext().toError(e.getRecord(), e.getMessage());
								break;
							case STOP_PIPELINE:
								throw new StageException(Errors.ERROR_01, e.toString());
							default:
								throw new IllegalStateException(Utils.format("Error unknown inserting '{}'",
										getContext().getOnErrorRecord(), e));
						}
					}
				}
			} catch (IllegalStateException e){
				logger.error("[UPDATE] IllegalStateException - getOnErrorRecord() not defined", e);
			} catch (StageException e){
				logger.error("[UPDATE] StageException: Error writting in thread target ", e);
				throw e;
			} catch (InterruptedException e){
				logger.error("[UPDATE] InterruptedException - Error executing thread ", e);
			} catch (ExecutionException e){
				logger.error("[UPDATE] ExecutionException - Error executing thread ", e);
			} catch (Exception e){
				logger.error("[UPDATE] Exception -  Error writting in thread target ", e);
			}
		 }
	}
	
}