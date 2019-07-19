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
package com.minsait.onesait.platform.streamsets.destination.ontology;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.util.Asserts;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.minsait.onesait.platform.streamsets.destination.beans.OntologyProcessInstance;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesConfig;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;

public class OnesaitplatformOntology {
	
	private static String setValueSecondTemplate = "\"%1$s.%2$s.%3$d.%2$s.%4$d.%2$s.%5$d\":";
	private static String setValueSecondTemplate_null = "\"%1$s.%2$s.%3$d.%2$s.%4$d.%2$s.%5$d\":{$type: 10}";
	private static String setValueMinuteTemplate = "\"%1$s.%2$s.%3$d.%2$s.%4$d\":";
	private static String setValueMinuteTemplate_null = "\"%1$s.%2$s.%3$d.%2$s.%4$d\":{$type: 10}";

	private static String setCountMinuteTemplate = "\"%1$s.%2$s.%3$d.%2$s.%4$d.%5$s\":%6$d";
	private static String setCountHourTemplate = "\"%1$s.%2$s.%3$d.%4$s\":%5$d";
	private static String setCountDayTemplate = "\"%1$s.%2$s\":%3$d";
	
	private static String setSumMinuteTemplate = "\"%1$s.%2$s.%3$d.%2$s.%4$d.%5$s\":";
	private static String setSumHourTemplate = "\"%1$s.%2$s.%3$d.%4$s\":";
	private static String setSumDayTemplate = "\"%1$s.%2$s\":";
	
	public static String constructOntologyInstance(Record record, OntologyProcessInstance ontologyProcessInstance, String ontology, String customRootNode) throws Exception {
		
		JSONObject json=null;
		
		switch(ontologyProcessInstance) {
			case NOROOTNODE:
				json = (JSONObject) constructBody(record.get());
				break;
			case CUSTOMNAME:
				json = new JSONObject();
				json.put(customRootNode, constructBody(record.get()));
				break;
			case ONTOLOGYNAME:
				json = new JSONObject();
				json.put(ontology, constructBody(record.get()));
				break;
		}
		return json.toJSONString();
	}
	
	public static JSONObject constructOntologyJsonIstance(Record record) throws Exception {
		return (JSONObject) constructBody(record.get());
	}
	
	private static void constructUpdateUntilSet(StringBuilder stb, StringBuilder stbNulls, String rootNode, String ontology, String customRootNode, TimeseriesConfig tsConfig, Map<String, Field> fieldvalue, DateTime dt, boolean ignoreNulls) throws Exception {
		stb.append("db.");
		stb.append(ontology);
		stb.append(".update({");
		
		for (String updField : tsConfig.getUpdateFields()) {
			try {
				stb.append("\"" + rootNode + updField + "\": " + constructBodyFieldQuery(fieldvalue.get(updField)) + ",");
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Update field "+updField+" does not exist in record.");
			}
		}
		stb.append("\"" + rootNode + tsConfig.getTsTimeField() + "\":{\"$date\":\"" + formatToIsoDateDay(dt) + "\"}");
		// Add null checks if overwrite is not active
		if(stbNulls.length() > 0 && !ignoreNulls) stb.append(","+stbNulls);
		stb.append("},");
	}
	
	private static void constructUpdateSetAndPrecalcs(StringBuilder stb, StringBuilder stbNulls, List<Record> records, String fixedValuePath, int secondStepRef, TimeseriesConfig tsConfig, Map<Integer, Object> precalcSum, Map<Integer,Integer> precalcCount, boolean ignoreNulls) {
		//Day precalcs
		if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
			precalcSum.put(10000, 0);
		}
		if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
			precalcCount.put(10000, 0);
		}
		if(secondStepRef != -1) {
			Iterator<Record> it = records.iterator();
			while(it.hasNext()) {
				Map<String, Field> fieldvalue = it.next().get().getValueAsMap();
				
				Field timestamp = fieldvalue.get(tsConfig.getTsTimeField());
				DateTime dt = new DateTime(timestamp.getValueAsDatetime());
				
				int hour = dt.getHourOfDay();
				int minute = dt.getMinuteOfHour();
				int second = dt.getSecondOfMinute();
				int minuteStep = minute/tsConfig.getMinuteStep();
				int secondStep = -1;
				
				if(secondStepRef!=-1) {
					secondStep = second/secondStepRef;
				}
				Field vnumber = fieldvalue.get(tsConfig.getOriginTimeseriesValueField());
				if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
					int hourKey = hour*100;
					int minuteKey = hour*100 + minuteStep*minute + 1;
					Object vsh = precalcSum.get(hourKey);
					Object vsm = precalcSum.get(minuteKey);
					if(vsh!=null) {
						precalcSum.put(hourKey, sumValues(vnumber, vsh));
					}
					else {
						precalcSum.put(hourKey, vnumber.getValue());
					}
					if(vsm!=null) {
						precalcSum.put(minuteKey, sumValues(vnumber, vsm));
					}
					else {
						precalcSum.put(minuteKey, vnumber.getValue());
					}
					precalcSum.put(10000, sumValues(vnumber, precalcSum.get(10000)) );
				}
				if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
					int hourKey = hour*100;
					int minuteKey = hour*100 + minuteStep*minute + 1;
					Integer vch = precalcCount.get(hourKey);
					Integer vcm = precalcCount.get(minuteKey);
					if(vch!=null) {
						precalcCount.put(hourKey, vch+1);
					}
					else {
						precalcCount.put(hourKey, 1);
					}
					if(vcm!=null) {
						precalcCount.put(minuteKey, vcm+1);
					}
					else {
						precalcCount.put(minuteKey, 1);
					}
					precalcCount.put(10000,precalcCount.get(10000) + 1);
				}
				
				stb.append(String.format(setValueSecondTemplate, fixedValuePath, tsConfig.getValueTimeseriesField(), hour, minuteStep, secondStep));
				stb.append(getFieldObjectValue(vnumber));
				if(!ignoreNulls) stbNulls.append(String.format(setValueSecondTemplate_null, fixedValuePath, tsConfig.getValueTimeseriesField(), hour, minuteStep, secondStep));
				
				if(it.hasNext()) {
					stb.append(",");
					if(!ignoreNulls) stbNulls.append(",");
				}
			}
		}
		else {
			Iterator<Record> it = records.iterator();
			while(it.hasNext()) {
				Map<String, Field> fieldvalue = it.next().get().getValueAsMap();
				
				Field timestamp = fieldvalue.get(tsConfig.getTsTimeField());
				DateTime dt = new DateTime(timestamp.getValueAsDatetime());
				
				int hour = dt.getHourOfDay();
				int minute = dt.getMinuteOfHour();
				int minuteStep = minute/tsConfig.getMinuteStep();
		        
				Field vnumber = fieldvalue.get(tsConfig.getOriginTimeseriesValueField());
				if(tsConfig.getPrecalcSumTimeseriesField() !=null) {					
					int hourKey = hour*100;
					Object vsh = precalcSum.get(hourKey);
					if(vsh!=null) {
						precalcSum.put(hourKey,  sumValues(vnumber, vsh) );
					}
					else {
						precalcSum.put(hourKey, vnumber.getValue());
					}
					precalcSum.put(10000, sumValues(vnumber, precalcSum.get(10000)) );
				}
				
				if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
					int hourKey = hour*100;
					Integer vch = precalcCount.get(hourKey);
					if(vch!=null) {
						precalcCount.put(hourKey, vch+1);
					}
					else {
						precalcCount.put(hourKey, 1);
					}
					precalcCount.put(10000,precalcCount.get(10000) + 1);
				}
				stb.append(String.format(setValueMinuteTemplate, fixedValuePath, tsConfig.getValueTimeseriesField(), hour, minuteStep));
				stb.append(getFieldObjectValue(vnumber));
				
				if(!ignoreNulls) stbNulls.append(String.format(setValueMinuteTemplate_null, fixedValuePath, tsConfig.getValueTimeseriesField(), hour, minuteStep));

				if(it.hasNext()) {
					stb.append(",");
					if(!ignoreNulls) stbNulls.append(",");
				}
			}
		}
	}
	
	private static Object sumValues(Field field, Object sum) {
		switch (field.getType()) {
			case LONG:
			case INTEGER:
				return Long.sum(field.getValueAsLong(), Long.parseLong(sum.toString()));
			case DECIMAL:
				return field.getValueAsDecimal().add(new BigDecimal(sum.toString()));
			case FLOAT:
			case DOUBLE:
			default:
				return Double.sum(field.getValueAsDouble(), Double.parseDouble(sum.toString()) );
		}
	}
	
	private static void constructUpdateIncWithPrecals(StringBuilder stb, String fixedValuePath, TimeseriesConfig tsConfig, Map<Integer, Object> precalcSum, Map<Integer,Integer> precalcCount) {
		if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
			Iterator<Integer> ipc = precalcCount.keySet().iterator();
			while(ipc.hasNext()) {
				Integer pck = ipc.next();
				Integer pcv = precalcCount.get(pck);
				int partHour = pck/100;
				int partMinute = (pck % 100 == 0 ? -1 : (pck%100-1));
				//hour-minute $inc
				
				if(partMinute!=-1) {
					stb.append(String.format(setCountMinuteTemplate,fixedValuePath,tsConfig.getValueTimeseriesField(),partHour,partMinute,tsConfig.getPrecalcCountTimeseriesField(),pcv));
				}
				else if(pck == 10000) {//Day inc
					stb.append(String.format(setCountDayTemplate,fixedValuePath,tsConfig.getPrecalcCountTimeseriesField(),pcv));
				}//hour $inc
				else{
					stb.append(String.format(setCountHourTemplate,fixedValuePath,tsConfig.getValueTimeseriesField(),partHour,tsConfig.getPrecalcCountTimeseriesField(),pcv));
				}
				
				if(ipc.hasNext() || tsConfig.getPrecalcSumTimeseriesField() !=null) {
					stb.append(",");
				}
			}
		}
		if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
			Iterator<Integer> ipc = precalcSum.keySet().iterator();
			while(ipc.hasNext()) {
				Integer psk = ipc.next();
				Object psv = precalcSum.get(psk);
				int partHour = psk/100;
				int partMinute = (psk % 100 == 0 ? -1 : (psk%100-1));
				//hour-minute $inc
				
				if(partMinute!=-1) {
					stb.append(String.format(setSumMinuteTemplate,fixedValuePath,tsConfig.getValueTimeseriesField(),partHour,partMinute,tsConfig.getPrecalcSumTimeseriesField()));						
					stb.append(getFieldObjectValue(psv));
				} else if(psk == 10000) {//Day inc
					stb.append(String.format(setSumDayTemplate,fixedValuePath,tsConfig.getPrecalcSumTimeseriesField()));
					stb.append(getFieldObjectValue(psv));
				} else {
					stb.append(String.format(setSumHourTemplate,fixedValuePath,tsConfig.getValueTimeseriesField(),partHour,tsConfig.getPrecalcSumTimeseriesField()));
					stb.append(getFieldObjectValue(psv));
				}
				
				if(ipc.hasNext()) {
					stb.append(",");
				}
			}
		}
	}
	
	private static String getFieldObjectValue(Object obj) {
		switch (obj.getClass().getSimpleName()) {
			case "Short":
			case "Integer":
			case "Long":
				return String.format("%d", obj);
			case "Float":				
			case "Double":
				return String.format("%f", obj);						
			case "BigDecimal":
				return String.format("%s", ((BigDecimal) obj).toPlainString() );
			case "Field":
				return getFieldObjectValue(constructBodyField((Field)obj));
			default:
				return String.format("%s", obj.toString() );
		}
	}
	
	public static String constructMultiUpdate(List<Record> records, OntologyProcessInstance ontologyProcessInstance, String ontology, String customRootNode, TimeseriesConfig tsConfig, boolean ignoreNulls) throws Exception {
		Map<String, Field> fieldvalueRef = records.get(0).get().getValueAsMap();
		Field timestamp = fieldvalueRef.get(tsConfig.getTsTimeField());
		DateTime dt = new DateTime(timestamp.getValueAsDatetime());
		
		int hour = dt.getHourOfDay();
		int minute = dt.getMinuteOfHour();
		int second = dt.getSecondOfMinute();
		int minuteStep = minute/tsConfig.getMinuteStep();
		int secondStepRef = tsConfig.getSecondStep();
		int secondStep = -1;
		
		if(secondStepRef!=-1) {
			secondStep = second/secondStepRef;
		}
		
		String rootNode;
		
		switch(ontologyProcessInstance) {
			case NOROOTNODE:
				rootNode="";
				break;
			case CUSTOMNAME:
				rootNode=customRootNode + ".";
				break;
			case ONTOLOGYNAME:
				rootNode=ontology + ".";
				break;
			default:
				rootNode="";
		}
		
		// New string builders
		StringBuilder sUsaPrecalcs 		= new StringBuilder();
		StringBuilder sUsaPrecalcsNulls = new StringBuilder();
				
		// precalcSum, precalcCount to accumulate partials sums, counts in different levels of timeseries, the key is hour*100 or hour*100 + minutes + 1 in order to difference hour 0 sum, count a hour 0 min 0 sum 
		Map<Integer, Object> precalcSum = new HashMap<Integer, Object>();
		Map<Integer,Integer> precalcCount = new HashMap<Integer,Integer>();
		String fixedValuePath = rootNode + tsConfig.getDestinationTimeseriesValueField();
		
		// Constructs update values
		constructUpdateSetAndPrecalcs(sUsaPrecalcs, sUsaPrecalcsNulls, records, fixedValuePath, secondStepRef, tsConfig, precalcSum, precalcCount, ignoreNulls);
		
		// Final string builder
		StringBuilder stb = new StringBuilder();
		
		// Using iterations of the constructUpdateSetAndPrecalcs() method 
		// to construct sUsaPrecalcsNulls and use it in constructUpdateUntilSet()
		constructUpdateUntilSet(stb, sUsaPrecalcsNulls, rootNode, ontology, rootNode, tsConfig, fieldvalueRef, dt, ignoreNulls);
		
		// Finally we construct the final string
		stb.append("{$set:{");
		stb.append(sUsaPrecalcs);
		stb.append("}");
		
		if(!ignoreNulls)
			if(tsConfig.getPrecalcCountTimeseriesField() != null || tsConfig.getPrecalcSumTimeseriesField() != null) {
				stb.append(",$inc:{");
				
				constructUpdateIncWithPrecals(stb, fixedValuePath, tsConfig, precalcSum, precalcCount);
				
				stb.append("}");
			}
		
		stb.append("})");
		return stb.toString();
	}
	
	public static String constructUpdate(Record record, OntologyProcessInstance ontologyProcessInstance, String ontology, String customRootNode, TimeseriesConfig tsConfig, boolean ignoreNulls) throws Exception {
		Map<String, Field> fieldvalue = record.get().getValueAsMap();
		Field timestamp = fieldvalue.get(tsConfig.getTsTimeField());
		DateTime dt = new DateTime(timestamp.getValueAsDatetime());
		
		int hour = dt.getHourOfDay();
		int minute = dt.getMinuteOfHour();
		int second = dt.getSecondOfMinute();
		int minuteStep = minute/tsConfig.getMinuteStep();
		int secondStepRef = tsConfig.getSecondStep();
		int secondStep = -1;
		
		if(secondStepRef != -1) {
			secondStep = second/secondStepRef;
		}
		
		String rootNode;
		
		switch(ontologyProcessInstance) {
			case NOROOTNODE:
				rootNode="";
				break;
			case CUSTOMNAME:
				rootNode=customRootNode + ".";
				break;
			case ONTOLOGYNAME:
				rootNode=ontology + ".";
				break;
			default:
				rootNode = "";
		}
		
		
		// New string builders
		StringBuilder stbNull = new StringBuilder();
		StringBuilder stbField = new StringBuilder();
		
		if(secondStepRef != -1) {
			stbField.append("\"" +rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + minuteStep + "." + tsConfig.getValueTimeseriesField() + "." + secondStep + "\":" + constructBodyFieldQuery(fieldvalue.get(tsConfig.getOriginTimeseriesValueField())));
			if(!ignoreNulls) stbNull.append("\"" +rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + minuteStep + "." + tsConfig.getValueTimeseriesField() + "." + secondStep + "\":{$type:10}");
		}
		else {
			stbField.append("\"" +rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + "." + minuteStep + "\":" + constructBodyFieldQuery(fieldvalue.get(tsConfig.getOriginTimeseriesValueField())));
			if(!ignoreNulls) stbNull.append("\"" +rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + "." + minuteStep + "\":{$type:10}");
		}
		
		// Final string builder
		StringBuilder stb = new StringBuilder();
		constructUpdateUntilSet(stb, stbNull, rootNode, ontology, rootNode, tsConfig, fieldvalue, dt, ignoreNulls);
		stb.append("{$set:{");
		stb.append(stbField);
		stb.append("}");
		
		if(!ignoreNulls)
			if(tsConfig.getPrecalcCountTimeseriesField() != null || tsConfig.getPrecalcSumTimeseriesField() != null) {
				stb.append(",$inc:{");
				if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
					stb.append("\"" + rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getPrecalcCountTimeseriesField() + "\":1");
					if(secondStepRef != -1) {
						stb.append(",\"" + rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + "." + minute + "." + tsConfig.getValueTimeseriesField() + "." + tsConfig.getPrecalcCountTimeseriesField() + "\":1");
					}
					if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
						stb.append(",");
					}
				}
				if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
					stb.append("\"" + rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getPrecalcSumTimeseriesField() + "\": " + constructBodyFieldQuery(fieldvalue.get(tsConfig.getOriginTimeseriesValueField())));
					if(secondStepRef != -1) {
						stb.append(",\"" + rootNode + tsConfig.getDestinationTimeseriesValueField() + "." + tsConfig.getValueTimeseriesField() + "." + hour + "." + tsConfig.getValueTimeseriesField() + "." + minute + "." + tsConfig.getValueTimeseriesField() + "." + tsConfig.getPrecalcSumTimeseriesField() + "\":" + constructBodyFieldQuery(fieldvalue.get(tsConfig.getOriginTimeseriesValueField())));
					}
				}
				stb.append("}");
			}
		stb.append("})");		
		return stb.toString();
	}

	private static Object constructBody(Map<String, Field> subcampos) throws Exception{
		JSONObject json=new JSONObject();
		for (String subcampoName : subcampos.keySet()){
			Field subcampo = subcampos.get(subcampoName);
			if (subcampo.getType()!=Field.Type.MAP && subcampo.getType()!=Field.Type.LIST_MAP && subcampo.getType()!=Field.Type.LIST){
				//si es 'primitivo'
				json.put(subcampoName,constructBodyField(subcampo));
			}else {
				json.put(subcampoName, constructBody(subcampos.get(subcampoName)));
			}
		}	
		return json;
	}
	
	private static Object constructBody(Field campo) throws Exception{
		
		JSONObject json=new JSONObject();
		JSONArray jsonarray=new JSONArray();
		if (campo.getType()==Field.Type.MAP || campo.getType()==Field.Type.LIST_MAP){
			Map<String, Field>subcampos = campo.getValueAsMap();
			for (String subcampoName : subcampos.keySet()){
				Field subcampo = subcampos.get(subcampoName);
				if (subcampo.getValue() == null) {
					json.put(subcampoName, null);
				} else {
					switch (subcampo.getType()) {
					case MAP:
					case LIST:
					case LIST_MAP:
						json.put(subcampoName, constructBody(subcampos.get(subcampoName)));
						break;
					default:
						json.put(subcampoName,constructBodyField(subcampo));
						break;
					}
				}
			}	
			return json;
		}else if (campo.getType()==Field.Type.LIST){
			List<Field> subcampos = campo.getValueAsList();
			for (Field field : subcampos){
				if (field.getValue() == null) {
					jsonarray.add(null);
				} else {
					switch (field.getType()) {
					case MAP:
					case LIST:
					case LIST_MAP:
						jsonarray.add(constructBody(field));
						break;
					default:
						jsonarray.add(constructBodyField(field));
						break;
					}
				}
			}	
			return jsonarray;
		}else {
			return constructBodyField(campo);
		}
		
	}
	
	private static Object constructBodyFieldQuery(Field field) throws IllegalArgumentException{
		if(field == null) throw new IllegalArgumentException("Field can't be null");
		switch (field.getType()) {
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case DECIMAL:
			case SHORT:
			case BOOLEAN:
			case DATE:
			case DATETIME:
				return constructBodyField(field);
			default:
				return "\"" + field.getValueAsString() + "\"";
		}
	}

	private static Object constructBodyField(Field field){
		if(field == null) throw new IllegalArgumentException("Field can't be null");
		switch (field.getType()) {
			case INTEGER:
				return field.getValueAsInteger();
			case LONG:
				return field.getValueAsLong();
			case FLOAT:
				return field.getValueAsFloat();
			case DOUBLE:
				return field.getValueAsDouble();
			case BOOLEAN:
				return field.getValueAsBoolean();
			case DECIMAL:
				return field.getValueAsDecimal();
			case SHORT:
				return field.getValueAsShort();
			case DATE:
			case DATETIME:
				JSONObject json=new JSONObject();
				json.put("$date", formatToIsoDate(new DateTime(field.getValueAsDate())));
				return json;
			default:
				return (field.getValueAsString());
		}
	}
	
	public static String instanceToBaseInsert(Record record, String ontology, String customRootNode, TimeseriesConfig tsConfig, OntologyProcessInstance ontologyProcessInstance)  throws Exception {
		JSONObject json=null;
		Field baseRecord = record.get();
		Map<String, Field> mapBaseRecord = (Map<String, Field>) baseRecord.getValue();
		
		Field dateField = mapBaseRecord.get(tsConfig.getTsTimeField());
		
		Map<String, Field> mapBaseRecordInsert = new HashMap<String, Field>();
		for(String field : mapBaseRecord.keySet()) {
			if(!field.equals(tsConfig.getOriginTimeseriesValueField()) && !field.equals(tsConfig.getTsTimeField())) {
				mapBaseRecordInsert.put(field, mapBaseRecord.get(field));
			}
		}
		
		JSONObject jobj = (JSONObject) constructBody(mapBaseRecordInsert);
		
		JSONObject jsonDate=new JSONObject();
		
		jsonDate.put("$date", formatToIsoDateDay(new DateTime(dateField.getValueAsDate())));
		
		jobj.put(tsConfig.getTsTimeField(), jsonDate);
		
		generateTimeseriesValuesArrays(jobj,tsConfig);
		
		if(customRootNode != null) {
			json = new JSONObject();
			json.put(customRootNode, jobj);
		}
		else {
			json = (JSONObject) jobj;
		}
		return json.toJSONString();
	}
	
	private static void generateTimeseriesValuesArrays(JSONObject jobj, TimeseriesConfig tsConfig) {
		JSONObject dayValue = new JSONObject();
        JSONArray hoursValueArray = new JSONArray();
        for (int i = 0; i < 24; i++) {
        	JSONObject hourValue = new JSONObject();
            int hourSum = 0;
            int hourCount = 0;
            JSONArray minutesValueArray = new JSONArray();
            
            int minutesSteps = 60 / tsConfig.getMinuteStep();
            
            for (int j = 0; j < minutesSteps; j++) {
               
                if(tsConfig.getSecondStep()!=-1) {
                	JSONObject minuteJSONValue = new JSONObject();
                	int secondsSteps = 60 / tsConfig.getSecondStep();
                	
	                JSONArray secondsValueArray = new JSONArray();
	                for (int k = 0; k < secondsSteps; k++) {
	                    secondsValueArray.add(null);
	                }
	                minuteJSONValue.put(tsConfig.getValueTimeseriesField(), secondsValueArray);
                
                
	                if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
	                	minuteJSONValue.put(tsConfig.getPrecalcCountTimeseriesField(), 0);
	                }
	                if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
	                	minuteJSONValue.put(tsConfig.getPrecalcSumTimeseriesField(), 0);
	                }
	                minutesValueArray.add(minuteJSONValue);
                }
                
                else {
                	minutesValueArray.add(null);
                }

            }
            
            hourValue.put(tsConfig.getValueTimeseriesField(), minutesValueArray);
            if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
            	hourValue.put(tsConfig.getPrecalcCountTimeseriesField(), 0);
            }
            if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
            	hourValue.put(tsConfig.getPrecalcSumTimeseriesField(), 0);
            }
            hoursValueArray.add(hourValue);
        }
        
        dayValue.put(tsConfig.getValueTimeseriesField(), hoursValueArray);
        if(tsConfig.getPrecalcCountTimeseriesField() !=null) {
        	dayValue.put(tsConfig.getPrecalcCountTimeseriesField(), 0);
        }
        if(tsConfig.getPrecalcSumTimeseriesField() !=null) {
        	dayValue.put(tsConfig.getPrecalcSumTimeseriesField(), 0);
        }
        jobj.put(tsConfig.getDestinationTimeseriesValueField(), dayValue);
	}
	
	private static DateTimeFormatter isoDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static DateTimeFormatter isoDateDayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'00:00:00.000'Z'");

	private static String formatToIsoDate(DateTime date) {
		return isoDateFormatter.print(date);
	}
	
	private static String formatToIsoDateDay(DateTime date) {
		return isoDateDayFormatter.print(date);
	}
	
}