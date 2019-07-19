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
package com.minsait.onesait.platform.streamsets.destination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.connection.ErrorResponseOriginalRecord;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Target.Context;
import com.streamsets.pipeline.api.base.OnRecordErrorException;

public class OnesaitplatformWorker implements Callable<List<ErrorManager>> {
	
	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformWorker.class);

	private DeviceOperations deviceOperations;
	private String ontologyName;
	private List<InstancesStt> bulkData;
	private Context context;
		
	public OnesaitplatformWorker(DeviceOperations deviceOperations, String ontologyName, List<InstancesStt> bulkData, Context context){
		this.deviceOperations = deviceOperations;
		this.ontologyName = ontologyName;
		this.bulkData = bulkData;
		this.context = context;		
	}
	
	 @Override
	 public List<ErrorManager> call() {
		 List<ErrorManager> lem = new ArrayList<ErrorManager>();
		 List<ErrorResponseOriginalRecord> lerrorRecords = new ArrayList<ErrorResponseOriginalRecord>();
		 		 
		 try{
			 for (InstancesStt data : bulkData){
				 
				 if(data.getInsertableRest().size() > 0) {
					 List<ErrorResponseOriginalRecord> errors = this.deviceOperations.insert(data, ontologyName);
					 
					 if(data.getRecordsNotInserted().size() > 0) {
						 this.sendErrorEvent(data.getRecordsNotInserted(), "data-not-inserted");
					 }
					 if(data.getRecordsDupKeys().size() > 0) {
						 this.sendErrorEvent(data.getRecordsDupKeys(), "duplicated-keys");
					 }
					 if(data.getRecordsOkInserted().size() > 0) {
						 this.sendOkEvent(data.getRecordsOkInserted(), "data-inserted");
					 }
					 
					 lerrorRecords.addAll(errors);					 
				 }
				 
				 if(data.getUpdateableRest().size() > 0) {
					 lerrorRecords.addAll(this.deviceOperations.update(data, ontologyName));
					 
					 // If there are any records updated or not, send events
					 if(data.getRecordsNotUpdated().size() > 0) {
						 this.sendErrorEvent(data.getRecordsNotUpdated(), "data-not-updated");
					 }
					 if(data.getRecordsDupKeys().size() > 0) {
						 this.sendErrorEvent(data.getRecordsDupKeys(), "duplicated-keys");
					 }
					 if(data.getRecordsOkUpdated().size() > 0) {
						 this.sendOkEvent(data.getRecordsOkUpdated(), "data-updated");
					 }
					 
				 } 
				 
			 }
			 if(lerrorRecords.size() > 0) {
				 for(ErrorResponseOriginalRecord eror : lerrorRecords) {
					 lem.add(new ErrorManager(new OnRecordErrorException(eror.getOriRecord(), eror.getError(), eror.getResponseErrorText()), eror.getOriRecord()));
				 }
			 }
		 }catch (Exception e){
			 if(context.getOnErrorRecord() != OnRecordError.DISCARD) logger.error("[DESTINATION] Exception in worker: "+e.getMessage());
		 }
		 return lem;
	 }
	 
	 private void sendOkEvent(List<Record> records, String optional) {
		List<Field> fields = records.stream().map(field -> field.get()).collect(Collectors.toList()); 
		OnesaitDestinationEvents.DOCUMENT_MODIFIED.create(this.context)
			.withFieldList(optional, fields)
			.createAndSend();
	 }
	 
	 private void sendErrorEvent(List<Record> records, String optional) {
		List<Field> fields = records.stream().map(field -> field.get()).collect(Collectors.toList()); 
		OnesaitDestinationEvents.DOCUMENT_NOT_MODIFIED.create(this.context)
			.withFieldList(optional, fields)
			.createAndSend();
	}
	
}
