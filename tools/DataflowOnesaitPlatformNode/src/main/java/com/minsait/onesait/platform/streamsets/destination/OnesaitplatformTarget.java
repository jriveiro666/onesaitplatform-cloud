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
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperationsREST;
import com.minsait.onesait.platform.streamsets.connection.ProxyConfig;
import com.minsait.onesait.platform.streamsets.destination.beans.OntologyProcessInstance;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesConfig;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesTime;
import com.minsait.onesait.platform.streamsets.destination.ontology.OnesaitplatformOntology;
import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseTarget;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.impl.Utils;

/**
 * This target is an example and does not actually write to any destination.
 */
public abstract class OnesaitplatformTarget extends BaseTarget {
	
	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformTarget.class);

	public abstract String getProtocol();
	public abstract String getHost();
	public abstract Integer getPort();
	public abstract boolean getAvoidSSL();
	public abstract boolean getIgnoreNulls();
	public abstract String getToken();
	public abstract String getDevice();
	public abstract String getDeviceId();
	public abstract String getOntology();
	
	public abstract OntologyProcessInstance getOntologyProcessInstance();
	public abstract String getCustomRootNode();
	
	public abstract String getRootNode();
	
	public abstract Integer getBulk();
	public abstract Integer getThread();
	
	public abstract Boolean getTimeseriesOntology();
	public abstract Boolean getTimeseriesMultiupdate();
	public abstract String getTimeseriesFieldOntology();
	
	public abstract TimeseriesTime getTimeseriesTimeOntology();
	public abstract String getValueTimeseriesField();
	
	public abstract Boolean getPrecalcSumTimeseries();
	public abstract Boolean getPrecalcCountTimeseries();
	public abstract String getPrecalcSumTimeseriesField();
	public abstract String getPrecalcCountTimeseriesField();
	
	public abstract List<String> getUpdateFields();
	public abstract String getOriginTimeseriesValueField();
	public abstract String getDestinationTimeseriesValueField();
	
	public abstract boolean getUseProxy();
	public abstract String getProxyHost();
	public abstract int getProxyPort();
	public abstract String getProxyUser();
	public abstract String getProxyUserPassword();
	
	private ProxyConfig proxy;
	
	private List<DeviceOperations> deviceOperations;
	private ExecutorService executor;
	private TimeseriesConfig tsConfig;

	@SuppressWarnings("deprecation")
	@Override
	protected List<ConfigIssue> init() {
		// Validate configuration values and open any required resources.
		List<ConfigIssue> issues = super.init();
		
		if (getProtocol().equals("invalidValue")) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Protocol required"));
		
		if (getHost().equals("invalidValue")) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Host required"));
		
		if (getPort() < 1) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Port required"));
		
		if (getToken().equals("invalidValue")) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Token required"));
		
		if (getDevice().equals("invalidValue")) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Device required"));
		
		if (getOntology().equals("invalidValue")) 
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Ontology required"));
		
		if (getBulk() < 1)
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00, "Bulk must be greater than 0"));
		
		if (getThread() < 1) 
			issues.add(getContext().createConfigIssue(Groups.DESTINATION.name(), "Destination", Errors.ERROR_00, "ThreadPool must be greater than 0"));
		
		this.proxy = getUseProxy() ? new ProxyConfig(getProxyHost(), getProxyPort(), getProxyUser(), getProxyUserPassword()) : null;
		
		this.deviceOperations = new ArrayList<>();
		for (int i=0 ; i< getThread(); i++){
			try{
				if(getTimeseriesOntology()) {
					this.tsConfig = new TimeseriesConfig(getTimeseriesTimeOntology(), getTimeseriesFieldOntology(), getValueTimeseriesField(), getPrecalcSumTimeseries()?getPrecalcSumTimeseriesField():null, getPrecalcCountTimeseries()?getPrecalcCountTimeseriesField():null, getUpdateFields(), getOriginTimeseriesValueField(), getDestinationTimeseriesValueField(), getTimeseriesMultiupdate());
				}
				else {
					this.tsConfig = null;
				}
				this.deviceOperations.add(new DeviceOperationsREST(getProtocol(), getHost(), getPort(), getToken(), getDevice(), getDeviceId(), this.tsConfig, this.getCustomRootNode(), this.getOntologyProcessInstance(), this.getAvoidSSL(), getContext().getOnErrorRecord(), getIgnoreNulls(), getUseProxy(), this.proxy));
			}catch (Exception e) {
				logger.error("[INSERT] Error init rest operation ", e);
			}
		}
		
		logger.info("[INSERT] ---------------------- init --------------------------");
		logger.info("[INSERT] Number of device operation is " + this.deviceOperations.size());
		logger.info("[INSERT] ------------------------------------------------");
		
		// If issues is not empty, the UI will inform the user of each
		// configuration issue in the list.
		
		executor = Executors.newFixedThreadPool(getThread());
		
		return issues;
	}

	@Override
	public void destroy() {
		for (DeviceOperations deviceOperation : this.deviceOperations){
			try {
				deviceOperation.leave();
			} catch (Exception e) {
				logger.error("[INSERT] Error leave ", e);
			}
		}
		
		try {
			if(executor != null) {
				executor.shutdown();
			} else {
				logger.error("[INSERT] Found executor in null state");
			}
		}catch (Exception e){
			logger.error("[INSERT] Error shutting down executor ", e);
		}
		
		// Clean up any open resources.
		super.destroy();
	}

	@Override
	public void write(Batch batch) throws StageException {	
		Iterator<Record> batchIterator = batch.getRecords();
		if(batchIterator.hasNext()) {
			List<InstancesStt> recordsPerBulk = separateRecordsInBulkMessages(batchIterator);
			if (!recordsPerBulk.isEmpty() && !recordsPerBulk.get(0).getInsertableRest().isEmpty()) {
				Map<Integer, List<InstancesStt>> treadData = separateBulkMessagesForThreads(recordsPerBulk);
				executeThreads(treadData);
			} 
		} else logger.debug("[INSERT][DEBUG] Batch has no records");
	}
	
	private List<InstancesStt> separateRecordsInBulkMessages (Iterator<Record> batchIterator) {
		List<InstancesStt> recordsPerBulk = new ArrayList<>();
		int bulkSize = getBulk();
		int index = 0;
		int itemsInBulk = 0;
		
		if(!getTimeseriesOntology()) {
			recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>() ));
			generateInstancesFromRecords(batchIterator,bulkSize,index, itemsInBulk,recordsPerBulk);
		} else {
			recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>() ) );
			if(!getTimeseriesMultiupdate()) {
				generateUpdatesFromRecords(batchIterator,bulkSize,index, itemsInBulk,recordsPerBulk);
			}
			else {
				generateMultiUpdatesFromRecords(batchIterator,bulkSize,index, itemsInBulk,recordsPerBulk);
			}
		}		
		return recordsPerBulk;
	}
	
	private void generateInstancesFromRecords(Iterator<Record> batchIterator, int bulkSize, int index, int itemsInBulk, List<InstancesStt> recordsPerBulk) {
		while (batchIterator.hasNext()) {
			InstancesStt recordsInBulkPartition = recordsPerBulk.get(index);
			Record record = batchIterator.next();
			
			try {
				recordsInBulkPartition.getInsertableRest().add(OnesaitplatformOntology.constructOntologyInstance(record, getOntologyProcessInstance(), getOntology(), getCustomRootNode()));
				List<Record> lrd = new ArrayList<Record>();
				lrd.add(record);
				recordsInBulkPartition.getOriginalValues().add(lrd);
			} catch (Exception e) {
				if(getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("[INSERT] Error constructing ontology instance ", e);
					getContext().toError(record, e.getMessage());
					
					OnesaitDestinationEvents.DOCUMENT_NOT_MODIFIED.create(getContext())
					.with("document-not-inserted", record.get().getValueAsMap())
					.createAndSend();
				}
			}
			
			itemsInBulk++;
			
			if (itemsInBulk >= bulkSize){
				index++;
				recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>()) ); 
				itemsInBulk = 0;
			}
			
		}
	}
	
	private void generateUpdatesFromRecords(Iterator<Record> batchIterator, int bulkSize, int index, int itemsInBulk, List<InstancesStt> recordsPerBulk) {
		while (batchIterator.hasNext()) {
			InstancesStt recordsInBulkPartition = recordsPerBulk.get(index);
			Record record = batchIterator.next();
			
			try {
				recordsInBulkPartition.getInsertableRest().add(OnesaitplatformOntology.constructUpdate(record, getOntologyProcessInstance(), getOntology(), getCustomRootNode(), this.tsConfig, getIgnoreNulls()));
				List<Record> lrd = new ArrayList<Record>();
				lrd.add(record);
				recordsInBulkPartition.getOriginalValues().add(lrd);
			} catch (Exception e) {
				if(getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("[INSERT] Error constructing update ", e);
					getContext().toError(record, e.getMessage());
					
					OnesaitDestinationEvents.DOCUMENT_NOT_MODIFIED.create(getContext())
						.with("document-not-inserted", record.get().getValueAsMap())
						.createAndSend();
				}
			}
			
			itemsInBulk++;
			
			if (itemsInBulk >= bulkSize){
				index++;
				recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>()) ); 
				itemsInBulk = 0;
			}
			
		}
	}
	
	private static DateTimeFormatter isoDateDayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'00:00:00.000'Z'");
	
	private static String formatToIsoDateDay(DateTime date) {
		return isoDateDayFormatter.print(date);
	}
	
	private List<Object> generateKeyOfGroup(Record record, TimeseriesConfig tsConfig) {
		List<Object> l = new ArrayList<Object>();
		
		Map<String, Field> fieldvalueNew = record.get().getValueAsMap();
		
		DateTime dtNew = new DateTime(fieldvalueNew.get(tsConfig.getTsTimeField()).getValueAsDatetime());
		l.add(formatToIsoDateDay(dtNew));
		
		Iterator<String> updIterator = tsConfig.getUpdateFields().iterator();
		while(updIterator.hasNext()) {
			l.add(fieldvalueNew.get(updIterator.next()));
		}		
		return l;
	}
	
	private void generateMultiUpdatesFromRecords(Iterator<Record> batchIterator, int bulkSize, int index, int itemsInBulk, List<InstancesStt> recordsPerBulk) {
		Map<List<Object>,List<Record>> mapRecordAgg = new HashMap<List<Object>,List<Record>>();
		InstancesStt recordsInBulkPartition = recordsPerBulk.get(index);
				
		while (batchIterator.hasNext()) {
			Record record = batchIterator.next();			
			List<Object> lkey = generateKeyOfGroup(record, tsConfig);
			
			if(mapRecordAgg.containsKey(lkey)) {
				mapRecordAgg.get(lkey).add(record);
			}
			else {
				List<Record> lrd = new ArrayList<Record>();
				lrd.add(record);
				mapRecordAgg.put(lkey,lrd);
			}
		}
				
		for (List<Record> lrd : mapRecordAgg.values()) {
			try {
				recordsInBulkPartition.getInsertableRest().add(OnesaitplatformOntology.constructMultiUpdate(lrd, getOntologyProcessInstance(), getOntology(), getCustomRootNode(), this.tsConfig, getIgnoreNulls()));
				recordsInBulkPartition.getOriginalValues().add(lrd);
			} catch (Exception e) {
				if(getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("[INSERT] Error constructing multi update ", e);
					lrd.stream().forEach(r -> getContext().toError(r, e.getMessage()) );
					
					List<Field> fields = lrd.stream().map(field -> field.get()).collect(Collectors.toList()); 
					OnesaitDestinationEvents.DOCUMENT_NOT_MODIFIED.create(getContext())
						.withFieldList("document-not-inserted", fields)
						.createAndSend();
				}
			}
			itemsInBulk++;
			
			if (itemsInBulk >= bulkSize){
				index++;
				recordsPerBulk.add(new InstancesStt(new ArrayList<String>(), new ArrayList<List<Record>>(), new ArrayList<String>()) );
				recordsInBulkPartition = recordsPerBulk.get(index);
				itemsInBulk = 0;
			}
		}
	}
	
	private Map<Integer, List<InstancesStt>> separateBulkMessagesForThreads (List<InstancesStt> recordsPerBulk) {
		Map<Integer, List<InstancesStt>> treadData = new HashMap<>();
		int threadId = 0;
		for (InstancesStt recordBulk : recordsPerBulk){
			
			if (treadData.get(threadId) == null)
				treadData.put(threadId, new ArrayList<InstancesStt>());
			
			treadData.get(threadId).add(recordBulk);
			threadId++;
			
			if (threadId == deviceOperations.size()) threadId = 0;			
		}	
		return treadData;
	}
	
	private void executeThreads (Map<Integer, List<InstancesStt>> treadData) throws StageException {
		logger.debug("[INSERT][DEBUG] Number of threads to execute is " + getThread()+ "  --  Workers to create: "+treadData.size());
		Set<Future<List<ErrorManager>>> set = new HashSet<>();		
		
		// Instead of using getThread, its better to use thread data  
		// because it may have less ThreadId than getThread and
		// it may cause null bulkdata in the worker
		for (Map.Entry<Integer, List<InstancesStt>> entry : treadData.entrySet()) {
			Callable<List<ErrorManager>> worker = new OnesaitplatformWorker(deviceOperations.get(entry.getKey()), getOntology(), entry.getValue(), this.getContext());
			set.add(executor.submit(worker));
		}
				
		for (Future<List<ErrorManager>> future : set) {
			try{
				List<ErrorManager> errors = future.get();
				if(errors.size() > 0) {
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
									throw new IllegalStateException(Utils.format("Error unknown inserting '{}'", getContext().getOnErrorRecord(), e));
							}
						}
					}
				}
			} catch (IllegalStateException e){
				logger.error("[INSERT] IllegalStateException - getOnErrorRecord() not defined", e);
			} catch (StageException e){
				logger.error("[INSERT] StageException: Error writting in thread target ", e);
				throw e;
			} catch (InterruptedException e){
				logger.error("[INSERT] InterruptedException - Error executing thread ", e);
			} catch (ExecutionException e){
				logger.error("[INSERT] ExecutionException - Error executing thread ", e);
			} catch (Exception e){
				logger.error("[INSERT] Exception -  Error writting in thread target ", e);
			}
		 }
	}

}
