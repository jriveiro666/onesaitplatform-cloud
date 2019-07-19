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
package com.minsait.onesait.platform.streamsets.processor;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.format.FieldParser;
import com.minsait.onesait.platform.streamsets.processor.OnesaitplatformDProcessor.MultipleValuesFoundEnum;
import com.minsait.onesait.platform.streamsets.processor.OnesaitplatformDProcessor.NoValueTypeEnum;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Processor.Context;
import com.streamsets.pipeline.api.base.OnRecordErrorException;

public class OnesaitplatformWorker implements Callable<List<BulkInstance>> {

	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformWorker.class);

	private DeviceOperations deviceOperations;
	private String ontologyName;
	private List<BulkInstance> bulks;
	private Context context;
	private Boolean useCache;
	private Cache<String, List<Field>> entriesCache;
	private Cache<String, Integer> missingValues;
	private List<String> selectFields;
	private List<OnesaitFieldColumnMapping> matchingFields;
	private MultipleValuesFoundEnum multipleValues;
	private NoValueTypeEnum noValueBehavior;
	private String dataPosition;
	private Boolean retry;
	protected final Integer maxCharacters;
	private boolean isSQL;

	public OnesaitplatformWorker(DeviceOperations deviceOperations, String ontologyName,
			List<BulkInstance> bulkData, Context context, Boolean useCache,
			Cache<String, List<Field>> entriesCache, List<String> selectFields,
			List<OnesaitFieldColumnMapping> matchingFields, MultipleValuesFoundEnum multipleValues,
			NoValueTypeEnum noValueBehavior, Integer maxCharacters, Boolean retry, Cache<String, Integer> missingValues,
			String dataPosition, boolean isSQL) {

		this.deviceOperations = deviceOperations;
		this.ontologyName = ontologyName;
		this.bulks = bulkData;
		this.context = context;
		this.useCache = useCache;
		this.entriesCache = entriesCache;
		this.matchingFields = matchingFields;
		this.selectFields = selectFields;
		this.multipleValues = multipleValues;
		this.noValueBehavior = noValueBehavior;
		this.retry = retry;
		this.maxCharacters = maxCharacters;
		this.missingValues = missingValues;
		this.dataPosition = dataPosition;
		this.isSQL = isSQL;
	}

	/**
	 * Processes the bulk messages and adds them to the BulkInstance in
	 * modifiedRecords. It adds errors to the BulkInstance if applicable.
	 */
	@Override
	public List<BulkInstance> call() {
		try {
			List<Integer> fetchAllList;
			for (BulkInstance bulkData : this.bulks) {
				fetchAllList = new ArrayList<Integer>();
				
				for (int i = 0; i < bulkData.getQueries().size(); i++) {
					String query = bulkData.getQueries().get(i);
					Record record = bulkData.getOriginalRecords().get(i);
					
					if(this.retry && this.missingValues.getIfPresent(query) != null) {
						this.ComputeRecordsNotFound(bulkData, i);
					} else {
						if(this.useCache) {
							this.ComputeCacheRecords(bulkData, record, fetchAllList, i, query);
						} else {
							fetchAllList.add(i);
						}
					}
				}
				
				if (fetchAllList.size() > 0) 
					if (this.isSQL) 
						fetchAllList.stream().forEach( index -> this.FetchFromBroker(bulkData, index));
					else 
						this.FetchAllFromBroker(bulkData, fetchAllList, this.ontologyName, this.selectFields, this.matchingFields);
				

				// Sending events
				if (bulkData.getRecordsFound().size() > 0)
					sendOkEvent(bulkData.getRecordsFound(), "records-found");
				if (bulkData.getRecordsNotFound().size() > 0)
					sendErrorEvent(bulkData.getRecordsNotFound(), "records-not-found");
			}

		} catch (Exception e) {
			if(context.getOnErrorRecord() != OnRecordError.DISCARD) logger.error("Exception in worker: " + e.getMessage(), e);
		}
		return bulks;
	}

	/**
	 * Computes record to get it from cache or adds it to the fetch list
	 * 
	 * @param bulkData
	 * @param record
	 * @param fetchAllList
	 * @param index
	 * @param query
	 */
	private void ComputeCacheRecords(BulkInstance bulkData, Record record, List<Integer> fetchAllList,
			Integer index, String query) {
		List<Field> entry = this.entriesCache.getIfPresent(query);

		if (entry != null) {
			try {
				this.AddDataToRecord(bulkData.getModifiedRecords(), record, entry, this.multipleValues);
			} catch (OnRecordErrorException e) {
				if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("Couldn't add data to record: " + e.getMessage(), e);
					bulkData.getRecordsError().add(e);
				}
			}

			bulkData.getRecordsFound().add(record); // Events
		} else {
			fetchAllList.add(index);
		}
	}

	/**
	 * Adds record to event list and send it to error if applicable, else is left
	 * untouched
	 * 
	 * @param bulk
	 * @param index
	 */
	private void ComputeRecordsNotFound(BulkInstance bulk, Integer index) {
		bulk.getRecordsNotFound().add(bulk.getOriginalRecords().get(index)); // For events
		
		
		switch (this.noValueBehavior) {
			case ERROR:
				if(context.getOnErrorRecord() != OnRecordError.DISCARD) 
					bulk.getRecordsError().add(new OnRecordErrorException(bulk.getOriginalRecords().get(index), Errors.ERROR_42,
						"No matching value with the specified parameters."));
				break;
			case UNCHANGED:
				bulk.getModifiedRecords().add(bulk.getOriginalRecords().get(index));
				break;
			case NULLS:
				if(this.selectFields.size() < 1) {
					bulk.getModifiedRecords().add(bulk.getOriginalRecords().get(index));
				} else {
					Map<String, Field> map = new HashMap<String, Field>();
					List<Field> entries = new ArrayList<Field>();
					
					this.selectFields.stream().forEach(select -> map.put(select, Field.create(Field.Type.STRING, null)) );
					entries.add(Field.create(Field.Type.LIST_MAP, map));
					
					try {
						AddDataToRecord(bulk.getModifiedRecords(), bulk.getOriginalRecords().get(index), entries, MultipleValuesFoundEnum.FIRST);
					} catch (OnRecordErrorException e) {
						bulk.getModifiedRecords().add(bulk.getOriginalRecords().get(index));
					}
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Adds data returned by the cache or API to the record and adds them to the
	 * batchmaker
	 * 
	 * @param record
	 * @param entries
	 * @param multipleValues
	 * @return LinkedList<Record>
	 * @throws Exception
	 */
	private void AddDataToRecord(List<Record> modRecords, Record record, List<Field> entries,
			MultipleValuesFoundEnum multipleValues) throws OnRecordErrorException {
		try {
			Field fieldOnPath = record.get(this.dataPosition);
			switch (multipleValues) {
				case FIRST:
					record.set(this.dataPosition, fieldOnPath == null ? entries.get(0) : MergeFields(fieldOnPath, entries.get(0)) );
					modRecords.add(record);
					break;
				case CREATELIST:
					record.set(this.dataPosition, fieldOnPath == null ? Field.create(Field.Type.LIST, entries) : MergeListFields(fieldOnPath, entries) );
					modRecords.add(record);
					break;
				case GENERATEMULTIPLE:
					for (Field field : entries) {
						Record newRecord = this.context.cloneRecord(record);
						newRecord.set(this.dataPosition, fieldOnPath == null ? field : MergeFields(fieldOnPath, field));
						modRecords.add(newRecord);
					}
					break;
				default:
					throw new Exception("Multiple values behavior is not set.");
			}
		} catch (Exception e) {
			if(context.getOnErrorRecord() != OnRecordError.DISCARD) logger.error("There was an error adding data to record: " + e.getMessage(), e);
			throw new OnRecordErrorException(record, Errors.ERROR_40, "There was an error adding data to record: " + e.getMessage());
		}
	}
	
	/**
	 * This method merges two fields if the origin is a Map, otherwise the field fetched will be returned.
	 * @param origin
	 * @param fetched
	 * @return
	 */
	protected Field MergeFields(Field origin, Field fetched) {
		switch (origin.getType()) {
			case MAP:
			case LIST_MAP:
				Map<String,Field> fieldMapOrigin = origin.getValueAsMap();
				fieldMapOrigin.putAll(fetched.getValueAsMap());
				return Field.create(Field.Type.LIST_MAP, fieldMapOrigin);
			default:
				return fetched;
		}
	}
	
	/**
	 * This method merges two lists if the origin is a List, otherwise the fields fetched will be returned.
	 * @param origin
	 * @param fetched
	 * @return
	 */
	protected Field MergeListFields(Field origin, List<Field> fetched) {
		switch (origin.getType()) {
			case LIST:
				List<Field> fieldListOrigin = origin.getValueAsList();
				fieldListOrigin.addAll(fetched);
				return Field.create(Field.Type.LIST, fieldListOrigin);
			default:
				return Field.create(Field.Type.LIST, fetched);
		}
	}

	/**
	 * This method should be invoked when there are 2 or more documents not cached
	 * and should be retrieved to the source.
	 * 
	 * @param bulk
	 * @param fetchAllList
	 * @param ontology
	 * @param selectFields
	 * @param matchingFields
	 */
	protected void FetchAllFromBroker(BulkInstance bulk, List<Integer> fetchAllList, String ontology,
			List<String> selectFields, List<OnesaitFieldColumnMapping> matchingFields) {

		List<QueryInstance> queries;
		if (matchingFields.size() == 1)
			queries = GenerateFetchAllSQLOne(bulk, fetchAllList, ontology, selectFields, matchingFields);
		else
			queries = GenerateFetchAllSQL(bulk, fetchAllList, ontology, selectFields, matchingFields);

		for (QueryInstance query : queries) {
			String response = "";
			try {
				response = this.deviceOperations.query(this.ontologyName, query.getQuery(), "SQL");
				ParseResponseAll(bulk, query.getIndexes(), selectFields, matchingFields, response);
			} catch (Exception e) {
				if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("Error doing query and parsing response: " + e.getMessage(), e);
					logger.error("Query: " + query.getQuery());
					logger.error("Response: " + response);
					for (Integer index : query.getIndexes()) {
						bulk.getRecordsError().add(new OnRecordErrorException(bulk.getOriginalRecords().get(index),
								Errors.ERROR_44, "Error doing query: " + e.getMessage()));
					}
				}
			}
		}
	}

	/**
	 * Generate the SQLs needed to perform the queries. This method should be
	 * invoked only when the match field is one.
	 * 
	 * @param bulk
	 * @param fetchAllList
	 * @param ontology
	 * @param selectFields
	 * @param matchingFields
	 * @return
	 */
	private List<QueryInstance> GenerateFetchAllSQLOne(BulkInstance bulk, List<Integer> fetchAllList,
			String ontology, List<String> selectFields, List<OnesaitFieldColumnMapping> matchingFields) {
		final Integer MAX_CHARS = this.maxCharacters;
		List<QueryInstance> queries = new ArrayList<QueryInstance>();

		// Main query
		String query = "SELECT " + GenerateSQLSelectFields(selectFields, matchingFields) + " FROM " + ontology
				+ " WHERE ";

		String subQuery = query; // Temporal query
		ListIterator<Integer> recordList = fetchAllList.listIterator();
		String fieldList = " " + matchingFields.get(0).GetColumnName() + " IN ()";

		// Filling the where conditions and create the final queries
		String tempFieldList = fieldList;
		List<Integer> indexes = new ArrayList<Integer>();
		List<String> alreadyInserted = new ArrayList<String>();

		while (recordList.hasNext()) {
			String temp = query;
			Integer indexToProcess = recordList.next();

			StringBuffer newField = new StringBuffer(tempFieldList);
			String toBeInserted = bulk.getQueryData().get(indexToProcess).get(matchingFields.get(0).GetColumnName());
			
			if(!alreadyInserted.contains(toBeInserted)) {
				alreadyInserted.add(toBeInserted);
				Integer insertIndex = newField.lastIndexOf(")");

				if (newField.substring(insertIndex - 1, insertIndex).equals("("))
					newField.insert(insertIndex, toBeInserted);
				else
					newField.insert(insertIndex, ", " + toBeInserted);

				tempFieldList = newField.toString();
				temp = temp + newField.toString();
			}

			if (temp.length() > MAX_CHARS) {
				queries.add(new QueryInstance(indexes, subQuery));
				indexes = new ArrayList<Integer>();
				recordList.previous();
				tempFieldList = fieldList;
			} else {
				if (recordList.hasNext()) {
					subQuery = temp;
					indexes.add(indexToProcess);
				} else {
					indexes.add(indexToProcess);
					queries.add(new QueryInstance(indexes, temp));
				}
			}
		}

		return queries;
	}

	/**
	 * Generate the SQLs needed to perform the queries. This method should be
	 * invoked only when the match fields are 2 or more.
	 * 
	 * @param bulk
	 * @param fetchAllList
	 * @param ontology
	 * @param selectFields
	 * @param matchingFields
	 * @return LinkedList<QueryInstance>
	 */
	private List<QueryInstance> GenerateFetchAllSQL(BulkInstance bulk, List<Integer> fetchAllList,
			String ontology, List<String> selectFields, List<OnesaitFieldColumnMapping> matchingFields) {
		final Integer MAX_CHARS = this.maxCharacters;
		List<QueryInstance> queries = new ArrayList<QueryInstance>();

		// Main query
		String query = "SELECT " + GenerateSQLSelectFields(selectFields, matchingFields) + " FROM " + ontology+ " WHERE ";

		String subQuery = query; // Temporal query
		ListIterator<Integer> recordList = fetchAllList.listIterator();
		List<Integer> indexes = new ArrayList<Integer>();
		List<String> alreadyInserted = new ArrayList<String>();

		while (recordList.hasNext()) {
			String temp = subQuery;
			String where = subQuery != query ? " OR " : " ";
			Integer indexToProcess = recordList.next();

			for (int i = 0; i < matchingFields.size(); i++) {
				String toBeInserted = matchingFields.get(i).GetColumnName() + matchingFields.get(i).GetOperator()
						+ bulk.getQueryData().get(indexToProcess).get(matchingFields.get(i).GetColumnName());

				toBeInserted = i == 0 ? toBeInserted : " AND " + toBeInserted;
				where += toBeInserted;
			}

			if(!alreadyInserted.contains(where)) temp = temp + where;
			else alreadyInserted.add(where);

			if (temp.length() > MAX_CHARS) {
				queries.add(new QueryInstance(indexes, subQuery));
				recordList.previous();
				subQuery = query;
				indexes = new ArrayList<Integer>();
			} else {
				if (recordList.hasNext()) {
					subQuery = temp;
					indexes.add(indexToProcess);
				} else {
					indexes.add(indexToProcess);
					queries.add(new QueryInstance(indexes, temp));
				}
			}
		}

		return queries;
	}

	/**
	 * Generate the select fields in the SQL
	 * 
	 * @param selects
	 * @return select fields
	 */
	private String GenerateSQLSelectFields(List<String> selects, List<OnesaitFieldColumnMapping> matchingFields) {
		String select = "";
		if (selects.size() == 0)
			select += "*";
		else {
			for (String sel : selects)
				select += sel + ", ";
			for (OnesaitFieldColumnMapping onesaitFieldColumnMapping : matchingFields)
				if (!selects.contains(onesaitFieldColumnMapping.GetColumnName()))
					select += onesaitFieldColumnMapping.GetColumnName() + ", ";

			select = select.substring(0, select.length() - 2);
		}

		return select;
	}

	/**
	 * Parses response and evaluates it to find the given indexes (fetchAllList) It
	 * adds errors to the instance or records to the batchmaker if applicable
	 * 
	 * @param bulk
	 * @param fetchAllList
	 * @param matchingFields
	 * @param response
	 */
	private void ParseResponseAll(BulkInstance bulk, List<Integer> fetchAllList, List<String> selectFields,
			List<OnesaitFieldColumnMapping> matchingFields, String response) {
		try {
			JsonArray recs = (new JsonParser()).parse(response).getAsJsonArray();
		      
		    List<JsonObject> records = new ArrayList<JsonObject>();
		    recs.forEach(rec -> records.add(rec.getAsJsonObject()));

			for (Integer index : fetchAllList) {
				List<JsonObject> found = records.stream()
						.filter(record -> RecordFilter(record, matchingFields, bulk.getOriginalRecords().get(index)))
						.collect(Collectors.toList());

				if (found.size() > 0) {
					List<Field> fields = new ArrayList<Field>();
					for (JsonObject obj : found) {
						try {
							Map<String, Field> mappedJson = FieldParser.ParseJsonObject(obj);

							if (selectFields.size() > 0)
								for (OnesaitFieldColumnMapping column : matchingFields)
									if (!selectFields.contains(column.GetColumnName())) {
										if (mappedJson.remove(column.GetColumnName()) == null
												&& !column.GetSearchPath().trim().equals("")) {
											mappedJson.remove(column.GetSearchPath().trim());
										}
									}

							fields.add(Field.create(Field.Type.LIST_MAP, mappedJson));
						} catch (Exception e) {
							if(context.getOnErrorRecord() != OnRecordError.DISCARD) 
								bulk.getRecordsError().add(new OnRecordErrorException(bulk.getOriginalRecords().get(index),
									Errors.ERROR_41, e.getMessage()));
						}
					}

					try {
						this.AddDataToRecord(bulk.getModifiedRecords(), bulk.getOriginalRecords().get(index), fields, this.multipleValues);
						if (this.useCache) 
							this.entriesCache.put(bulk.getQueries().get(index), multipleValues == MultipleValuesFoundEnum.FIRST ? fields.subList(0, 1) : fields);
						
					} catch (OnRecordErrorException e) {
						if(context.getOnErrorRecord() != OnRecordError.DISCARD) bulk.getRecordsError().add(e);
					}

					bulk.getRecordsFound().add(bulk.getOriginalRecords().get(index)); // For events

				} else {
					this.missingValues.put(bulk.getQueries().get(index), 1);
					this.ComputeRecordsNotFound(bulk, index);
				}
			}
		} catch (JsonParseException e) {
			if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
				logger.error("Exception parsing JSON: " + e.getMessage(), e);
				for (Integer index : fetchAllList) {
					bulk.getRecordsError().add(new OnRecordErrorException(bulk.getOriginalRecords().get(index),
							Errors.ERROR_41, e.getMessage()));
				}
			}
		}
	}

	/**
	 * Returns true if the record is found in the stream
	 * 
	 * @param record
	 * @param matchingFields
	 * @param values
	 * @return boolean
	 */
	private static boolean RecordFilter(JsonObject json, List<OnesaitFieldColumnMapping> matchingFields, Record record) {
		for (int i = 0; i < matchingFields.size(); i++) {
			if (!record.get(matchingFields.get(i).GetField()).getValueAsString()
					.equals(ReturnJsonValue(json, matchingFields.get(i))))
				return false;
		}
		return true;
	}

	/**
	 * Returns a value given a JSON path
	 * 
	 * @param jsonObject
	 * @param path
	 * @return value
	 */
	private static String ReturnJsonValue(JsonObject jsonObject, OnesaitFieldColumnMapping matchingField) {
		return jsonObject.get(matchingField.GetSearchPath().isEmpty() ? matchingField.GetColumnName() : matchingField.GetSearchPath()).getAsString();
	}

	/**
	 * Call function to fetch data individually
	 * 
	 * @param query
	 * @return LinkedList<Map<String, Field>>
	 * @throws Exception
	 */
	private void FetchFromBroker(BulkInstance bulk, Integer index) {
		String query = bulk.getQueries().get(index);
		Record record = bulk.getOriginalRecords().get(index);

		try {
			String response = this.deviceOperations.query(this.ontologyName, query, "SQL");

			try {
				List<Field> fields = ParseResponse(response);

				if (fields.isEmpty()) {
					this.missingValues.put(query, 1);
					this.ComputeRecordsNotFound(bulk, index);
				} else {
					try {
						this.AddDataToRecord(bulk.getModifiedRecords(), record, fields, this.multipleValues);
						if (this.useCache)
							this.entriesCache.put(query, fields);
					} catch (OnRecordErrorException e) {
						bulk.getRecordsError().add(e);
					}

					bulk.getRecordsFound().add(record); // For events;
				}

			} catch (JsonParseException e) {
				if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("Error parsing response: " + e.getMessage(), e);
					bulk.getRecordsError().add(new OnRecordErrorException(record, Errors.ERROR_41, e.getMessage()));
				}
			} catch (Exception e) {
				if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("Error parsing JSON Object: " + e.getMessage(), e);
					bulk.getRecordsError().add(new OnRecordErrorException(record, Errors.ERROR_41, e.getMessage()));
				}
			}
		} catch (Exception e) {
			if(context.getOnErrorRecord() != OnRecordError.DISCARD) {
				logger.error("Error doing query: " + e.getMessage(), e);
				bulk.getRecordsError().add(new OnRecordErrorException(record, Errors.ERROR_44, e.getMessage()));
			}
		}
	}

	/**
	 * Parses rest response as JSON and returns a list of Map
	 * 
	 * @param response
	 * @return LinkedList<Map<String, Field>>
	 */
	private List<Field> ParseResponse(String response) throws JsonParseException, Exception {
		List<Field> fields = new ArrayList<>();
		JsonArray docs = (new JsonParser()).parse(response).getAsJsonArray();

		for (JsonElement obj : docs)
			fields.add(Field.create(Field.Type.LIST_MAP, FieldParser.ParseJsonObject(obj.getAsJsonObject())));

		return fields;
	}

	// EVENTS

	private void sendOkEvent(List<Record> records, String optional) {
		List<Field> fields = records.stream().map(rec -> rec.get()).collect(Collectors.toList());
		OnesaitProcessorEvents.RECORD_FOUND.create(this.context).withFieldList(optional, fields).createAndSend();
	}

	private void sendErrorEvent(List<Record> records, String optional) {
		List<Field> fields = records.stream().map(rec -> rec.get()).collect(Collectors.toList());
		OnesaitProcessorEvents.RECORD_NOT_FOUND.create(this.context).withFieldList(optional, fields).createAndSend();
	}

}
