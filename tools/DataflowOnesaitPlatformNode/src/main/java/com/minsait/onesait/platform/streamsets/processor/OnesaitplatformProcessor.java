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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.RecordEL;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperationsREST;
import com.minsait.onesait.platform.streamsets.connection.ProxyConfig;
import com.minsait.onesait.platform.streamsets.format.FieldParser;
import com.minsait.onesait.platform.streamsets.processor.OnesaitplatformDProcessor.ExpirationTypeEnum;
import com.minsait.onesait.platform.streamsets.processor.OnesaitplatformDProcessor.MultipleValuesFoundEnum;
import com.minsait.onesait.platform.streamsets.processor.OnesaitplatformDProcessor.NoValueTypeEnum;
import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.base.SingleLaneRecordProcessor;
import com.streamsets.pipeline.api.el.ELEval;
import com.streamsets.pipeline.api.el.ELVars;
import com.streamsets.pipeline.api.impl.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OnesaitplatformProcessor extends SingleLaneRecordProcessor {

	private static final Logger logger = LoggerFactory.getLogger(OnesaitplatformProcessor.class);

	public abstract String getProtocol();
	public abstract String getHost();
	public abstract Integer getPort();
	public abstract boolean getAvoidSSL();
	public abstract String getToken();
	public abstract String getDevice();
	public abstract String getDeviceId();
	public abstract String getOntology();
	public abstract Integer getMaxCharacters();
	public abstract Integer getBulk();
	public abstract Integer getThread();
	public abstract Boolean getLocalCache();
	public abstract Boolean getLimitCache();
	public abstract Integer getEntriesNumber();
	public abstract Boolean getCacheExpiration();
	public abstract Integer getTimeValue();
	public abstract TimeUnit getTimeUnit();
	public abstract Integer getTimeValueMissing();
	public abstract TimeUnit getTimeUnitMissing();
	public abstract ExpirationTypeEnum getExpirationPolicy();
	public abstract MultipleValuesFoundEnum getMultipleValues();
	public abstract String GetDataPosition();
	public abstract NoValueTypeEnum getNoValuePolicy();
	public abstract boolean getRetry();
	public abstract List<OnesaitFieldColumnMapping> getColumnMappings();
	public abstract List<String> getColumnSelector();
	public abstract boolean IsSQL();
	public abstract String GetQuery();
	public abstract boolean getInitialLoad();
	public abstract int getMaxRecordInitialLoad();
	public abstract boolean getUseProxy();
	public abstract String getProxyHost();
	public abstract int getProxyPort();
	public abstract String getProxyUser();
	public abstract String getProxyUserPassword();	

	private ELEval queryEval;
	private List<DeviceOperations> deviceOperations;
	private ExecutorService executor;
	private Cache<String, List<Field>> entriesCache;
	private Cache<String, Integer> missingEntries;
	
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
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Host required"));
		}
		if (getPort() < 0) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Port required"));
		}
		if (getToken().equals("invalidValue")) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Token required"));
		}
		if (getDevice().equals("invalidValue")) {
			issues.add(
					getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Device required"));
		}
		if (getOntology().equals("invalidValue")) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Ontology required"));
		}

		if (getColumnMappings().size() == 0) {
			issues.add(getContext().createConfigIssue(Groups.SOURCE.name(), "Source", Errors.ERROR_00, "Matching columns must be greather than 0"));
		}

		this.deviceOperations = new ArrayList<>();
		
		this.proxy = getUseProxy() ? new ProxyConfig(getProxyHost(), getProxyPort(), getProxyUser(), getProxyUserPassword()) : null;

		// TODO create a pool of devices
		// TODO create a pool of http connections
		for (int i = 0; i < getThread(); i++) {
			try {
				this.deviceOperations.add(new DeviceOperationsREST(getProtocol(), getHost(), getPort(), getToken(),
						getDevice(), getDeviceId(), null, null, null, getAvoidSSL(), getContext().getOnErrorRecord(),
						false, getUseProxy(), this.proxy));
			} catch (Exception e) {
				logger.error("[LOOKUP] Error init rest operation ", e);
			}
		}

		if (IsSQL())
			queryEval = getContext().createELEval("query");

		logger.info("[LOOKUP] ---------------------- init --------------------------");
		logger.info("[LOOKUP] Number of device operation is " + this.deviceOperations.size());
		logger.info("[LOOKUP] ------------------------------------------------");

		// Creating cache instance
		if (getLocalCache()) {
			logger.info("[LOOKUP] ------------------------------------------------");
			logger.info("[LOOKUP] -----------------Creating cache-----------------");

			CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

			if (getLimitCache()) {
				logger.info("[LOOKUP] Cache limited to: " + getEntriesNumber());
				builder.maximumSize(getEntriesNumber());
			}

			if (getCacheExpiration()) {
				if (getExpirationPolicy().equals(ExpirationTypeEnum.ACCESS)) {
					logger.info("[LOOKUP] Cache expiration policy ACCESS: Expiration after " + getTimeValue()
							+ getTimeUnit().toString());
					builder.expireAfterAccess(getTimeValue(), getTimeUnit());
				} else {
					logger.info("[LOOKUP] Cache expiration policy WRITE: Expiration after " + getTimeValue()
							+ getTimeUnit().toString());
					builder.expireAfterWrite(getTimeValue(), getTimeUnit());
				}
			}

			this.entriesCache = builder.build();

			if (getInitialLoad() && !IsSQL()) this.CacheInit();

			logger.info("[LOOKUP] ------------------------------------------------");
		}
		
		// If retry is active, it will retry after eviction configured with the time value and time unit, otherwise it will never evict this cache and never perform the same query twice
		this.missingEntries = this.getRetry() ? CacheBuilder.newBuilder().expireAfterWrite(getTimeValueMissing(), getTimeUnitMissing()).build() : CacheBuilder.newBuilder().build();
		
		
		
		this.executor = Executors.newFixedThreadPool(getThread());

		// If issues is not empty, the UI will inform the user of each
		// configuration issue in the list.

		return issues;
	}

	private void CacheInit() {
		DeviceOperations dev = this.deviceOperations.get(0);
		try {
			String response = dev.query(getOntology(), "SELECT COUNT(*) AS value FROM "+getOntology(), "SQL");
			int value = (new JsonParser()).parse(response).getAsJsonArray().get(0).getAsJsonObject().get("value").getAsInt();
			if(value == 0) throw new Exception("[LOOKUP] Ontology is empty");
			
			int limit = getMaxRecordInitialLoad();
			int pages = (int) Math.ceil((value / limit));

			logger.info("[LOOKUP] Populating cache. Found " + value + " documents.");

			List<String> selects = new ArrayList<String>();
			selects.addAll(getColumnSelector());
			selects.addAll(getColumnMappings().stream().map(col -> col.GetColumnName()).collect(Collectors.toList()));
			String fieldsSelector = selects.stream().distinct().collect(Collectors.joining(","));

			ExecutorService executor = Executors.newFixedThreadPool(Math.min(getThread().intValue(), pages));

			List<String> result = IntStream.range(0, pages)
					.mapToObj(i -> CompletableFuture.supplyAsync(() -> this.executeQuery(i, ("SELECT " + fieldsSelector + " FROM " + this.getOntology() + " OFFSET " + (i * limit)+ " LIMIT " + limit)), executor))
					.collect(Collectors.toList())
					.stream()
					.map(CompletableFuture::join).collect(Collectors.toList());

			executor.shutdown();

			logger.debug("[LOOKUP][DEBUG] Populating cache. IoT Broker responses: " + result.size());
			
			int page = 1;
			for (String json : result) {
				logger.debug("[LOOKUP][DEBUG] Populating cache. Page " + page + " of " + pages);
				long init = System.currentTimeMillis();

				JsonArray docs = (new JsonParser()).parse(json).getAsJsonArray();

				logger.debug("[LOOKUP][DEBUG] Batch " + page + " : JsonParsed. Elapsed " + (System.currentTimeMillis() - init) + " ms.");

				for (JsonElement jsonElement : docs) {
					JsonObject obj = jsonElement.getAsJsonObject();
					Field field = Field.create(Field.Type.LIST_MAP, FieldParser.ParseJsonObject(obj));

					String where;
					try {
						where = getColumnMappings().stream()
									.map(col -> {
										try {
											return col.GetColumnName() + col.GetOperator() + getValueToCache(col, obj);
										} catch (Exception e) {
											throw new RuntimeException(e.getMessage());
										}
									}).collect(Collectors.joining(" AND "));
					} catch (RuntimeException e) {
						logger.debug("[LOOKUP][DEBUG] ERROR: "+e.getMessage(), e);
						logger.debug("[LOOKUP][DEBUG] Document: "+obj.toString());
						continue;
					}

					List<Field> val = this.entriesCache.getIfPresent(where);
					if (val == null) {
						List<Field> fields = new ArrayList<Field>();
						fields.add(field);
						this.entriesCache.put(where, fields);
						continue;
					} 
					if (getMultipleValues() != OnesaitplatformDProcessor.MultipleValuesFoundEnum.FIRST) {
						val.add(field);
						this.entriesCache.put(where, val);
					}
				}

				logger.debug("[LOOKUP][DEBUG] Added " + docs.size() + " entries of a total of " + value);
				long elapsed = System.currentTimeMillis() - init;
				int througput = Math.round((float) (docs.size() / (elapsed/1000)));
				logger.debug("[LOOKUP][DEBUG] Batch " + page + " : FINISHED IN " + elapsed + " ms. Througput: " + througput+" documents/sec");
				page++;
			}
			
		} catch (Exception e) {
			logger.error("[LOOKUP] Error populating cache. Added " + this.entriesCache.size() + " entries.", e);
		}
	}

	private String getValueToCache(OnesaitFieldColumnMapping col, JsonObject obj) throws Exception {
		String path = col.GetSearchPath().isEmpty() ? col.GetColumnName() : col.GetSearchPath();
		JsonElement elem = obj.get(path);
		
		if(elem == null) throw new Exception("JSON field does not exist. Path: "+path);
		if(elem.isJsonNull()) return "NULL";
		if(elem.isJsonPrimitive()) {
			String value = elem.getAsJsonPrimitive().isString() ? "'" + elem.getAsString().replaceAll("((?<![\\\\])['\"])", "\\\\'") + "'" : elem.getAsString();
			return col.isTimeStamp() ? "TIMESTAMP(" + value + ")" : value;
		} 
		
		throw new Exception("JSON field is not a primitive value. Path: "+path+" Value: "+elem.toString());
	}

	private String executeQuery(Integer page, String sql) {
		long init = System.currentTimeMillis();
		logger.debug("[LOOKUP][DEBUG] Batch " + (page.intValue() + 1) + " : Init SQL: " + sql);

		// TODO get devices from pool
		DeviceOperationsREST dev = new DeviceOperationsREST(getProtocol(), getHost(), getPort(), 
				getToken(), getDevice(), getDeviceId(), null, null, null, getAvoidSSL(), 
				getContext().getOnErrorRecord(), false, getUseProxy(), this.proxy);
		int maxRetries = 3;
		int count = 0;

		while (true) {
			try {
				logger.debug("[LOOKUP][DEBUG] Batch " + (page.intValue() + 1) + " : Query executed. Elapsed "+ (System.currentTimeMillis() - init) + " ms.");
				return dev.query(getOntology(), sql, "SQL");
			} catch (Exception e) {
				count++;
				if (count == maxRetries) {
					logger.error("[LOOKUP][DEBUG] Error doing query. SQL: " + sql, e);
					return "";
				}
			}
		}
	}

	@Override
	public void destroy() {
		// Clean up any open resources.
		super.destroy();
		for (DeviceOperations deviceOperation : this.deviceOperations) {
			try {
				deviceOperation.leave();
			} catch (Exception e) {
				logger.error("[LOOKUP] Error leave ", e);
			}
		}

		try {
			if (this.executor != null)
				this.executor.shutdown();
			else
				logger.error("[LOOKUP] Error shutdown was null");
		} catch (Exception e) {
			logger.error("[LOOKUP] Error shutdown executor ", e);
		}

		if (getLocalCache().booleanValue()) {
			this.entriesCache.cleanUp();
			this.entriesCache.invalidateAll();
		}
	}

	@Override
	public void process(Batch batch, SingleLaneBatchMaker batchMaker) throws StageException {
		if (!batch.getRecords().hasNext()) {
			logger.debug("[LOOKUP] Cleaning up missingEntries");
			missingEntries.cleanUp();
			
			if(this.getLocalCache()) {
				// No records - take the opportunity to clean up the cache so that we don't hold
				// on to memory indefinitely
				long start = entriesCache.stats().evictionCount();
				entriesCache.cleanUp();
				long evicted = entriesCache.stats().evictionCount() - start;
				if (evicted > 0)
					logger.debug("[LOOKUP] Cleaned up the cache: {} entries evicted", evicted);
			}
		}

		Iterator<Record> batchIterator = batch.getRecords();
		List<BulkInstance> bulks = separateRecordsInBulkMessages(batchMaker, batchIterator, this.getBulk(),
				getOntology(), getColumnMappings());
		if (!bulks.isEmpty()) {
			Map<Integer, List<BulkInstance>> treadData = separateBulkMessagesForThreads(bulks);
			executeThreads(treadData, batchMaker);
		}

		logger.debug("[LOOKUP] Procesed all records in the processor");
	}

	@Override
	protected void process(Record record, SingleLaneBatchMaker batchMaker) throws StageException {

	}

	/**
	 * It organizes and separates the records in the batch to create bulks of
	 * records returned in a List.
	 * 
	 * @param batchIterator
	 * @param bulkSize
	 * @return List<BulkInstance>
	 */
	private List<BulkInstance> separateRecordsInBulkMessages(SingleLaneBatchMaker batchMaker,
			Iterator<Record> batchIterator, Integer bulkSize, String ontology,
			List<OnesaitFieldColumnMapping> filters) {
		List<BulkInstance> recordsPerBulk = new ArrayList<BulkInstance>();

		recordsPerBulk.addAll(generateInstancesFromRecords(batchMaker, batchIterator, bulkSize, ontology, filters));

		return recordsPerBulk;
	}

	/**
	 * Generates the instances based on the records
	 * 
	 * @param batchMaker
	 * @param batchIterator
	 * @param bulkSize
	 * @param ontology
	 * @param filters
	 * @return
	 */
	private List<BulkInstance> generateInstancesFromRecords(SingleLaneBatchMaker batchMaker,
			Iterator<Record> batchIterator, int bulkSize, String ontology, List<OnesaitFieldColumnMapping> filters) {
		int itemsInBulk = 0;
		List<BulkInstance> recordsPerBulk = new ArrayList<BulkInstance>();
		BulkInstance bulk = new BulkInstance(new ArrayList<String>(), new ArrayList<Record>(),
				new ArrayList<Map<String, String>>(), batchMaker);

		while (batchIterator.hasNext()) {
			Record record = batchIterator.next();
			try {
				if (IsSQL()) {
					ELVars elVars = getContext().createELVars();
					RecordEL.setRecordInContext(elVars, record);
					String prepSQL = queryEval.eval(elVars, GetQuery(), String.class);
					bulk.getQueries().add(prepSQL);
				} else {
					// String prepSQL = this.FormatSQL(record, ontology, filters, bulk.getQueryData());
					String where = this.GenerateWhereSQL(record, filters, bulk.getQueryData());
					bulk.getQueries().add(where);
				}

				bulk.getOriginalRecords().add(record);
				itemsInBulk++;
				if (!batchIterator.hasNext() || itemsInBulk >= bulkSize) {
					recordsPerBulk.add(bulk);
				}

				if (itemsInBulk >= bulkSize) {
					bulk = new BulkInstance(new ArrayList<String>(), new ArrayList<Record>(), new ArrayList<Map<String, String>>(), batchMaker);
					itemsInBulk = 0;
				}

			} catch (Exception e) {
				if (getContext().getOnErrorRecord() != OnRecordError.DISCARD) {
					logger.error("[LOOKUP] Error preparing query: " + e.getMessage(), e);
					bulk.getRecordsError().add(new OnRecordErrorException(record, Errors.ERROR_43, "Error preparing query: " + e.getMessage()));
				}
				if (!batchIterator.hasNext())
					recordsPerBulk.add(bulk);
			}

		}
		return recordsPerBulk;
	}

	/**
	 * Generates the WHERE clause for the SQL query
	 * 
	 * @param record
	 * @param filters
	 * @param queryData
	 * @return
	 * @throws Exception
	 */
	private String GenerateWhereSQL(Record record, List<OnesaitFieldColumnMapping> filters, List<Map<String, String>> queryData) throws Exception {
		String where = "";
		String value;

		Map<String, String> mappingValues = new HashMap<String, String>();
		for (OnesaitFieldColumnMapping cMap : filters) {
			String field = cMap.GetField();
			String cName = cMap.GetColumnName();

			try {
				Object val = FieldParser.ParseFieldValue(record.get(field));
				if(val == null) value = "NULL";
				else if (val instanceof String) {
					value = "'" + val.toString().replaceAll("((?<![\\\\])['\"])", "\\\\'") + "'"; 
				} else
					value = val.toString();

				if (cMap.isTimeStamp())
					value = "TIMESTAMP(" + value + ")";

			} catch (Exception e) {
				if (getContext().getOnErrorRecord() != OnRecordError.DISCARD)
					logger.error("[LOOKUP] Could not parse the field passed to the mapping", e);
				throw new Exception("Could not parse the field passed to the mapping");
			}

			where += cName + cMap.GetOperator() + value + " AND ";
			mappingValues.put(cName, value);
		}

		queryData.add(mappingValues);

		if (filters.size() > 0)
			where = where.substring(0, where.length() - 5);
		else {
			if (getContext().getOnErrorRecord() != OnRecordError.DISCARD)
				logger.error("[LOOKUP] There are no filters in WHERE clause.");
			throw new Exception("There are no filters in WHERE clause.");
		}

		return where;
	}

	/**
	 * Separates the bulks into the available threads
	 * 
	 * @param recordsPerBulk
	 * @return
	 */
	private Map<Integer, List<BulkInstance>> separateBulkMessagesForThreads(
			List<BulkInstance> recordsPerBulk) {

		Map<Integer, List<BulkInstance>> threadData = new HashMap<>();

		int threadId = 0;
		for (BulkInstance recordBulk : recordsPerBulk) {

			if (threadData.get(threadId) == null) {
				threadData.put(threadId, new ArrayList<BulkInstance>());
			}

			threadData.get(threadId).add(recordBulk);

			threadId++;

			if (threadId == deviceOperations.size()) {
				threadId = 0;
			}
		}

		return threadData;
	}

	/**
	 * Executes the work divided in bulks by the threads
	 * 
	 * @param treadData
	 * @param batchmaker
	 * @throws StageException
	 */
	private void executeThreads(Map<Integer, List<BulkInstance>> treadData, SingleLaneBatchMaker batchmaker)
			throws StageException {
		logger.debug("[LOOKUP][DEBUG] Number of threads to execute is " + getThread() + "  --  Workers to create: "+ treadData.size());
		Set<Future<List<BulkInstance>>> set = new HashSet<>();

		// Instead of using getThread, its better to use thread data
		// because it may have less ThreadId than getThread and
		// it may cause null bulkdata in the worker
		for (Map.Entry<Integer, List<BulkInstance>> entry : treadData.entrySet()) {
			Callable<List<BulkInstance>> worker = new OnesaitplatformWorker(deviceOperations.get(entry.getKey()),
					getOntology(), entry.getValue(), this.getContext(), this.getLocalCache(), this.entriesCache,
					this.getColumnSelector(), this.getColumnMappings(), this.getMultipleValues(),
					this.getNoValuePolicy(), this.getMaxCharacters(), this.getRetry(), this.missingEntries,
					this.GetDataPosition(), this.IsSQL());

			set.add(executor.submit(worker));
		}

		for (Future<List<BulkInstance>> future : set) {
			try {
				List<BulkInstance> bulks = future.get();
				for (BulkInstance bulkdata : bulks) {
					for (Record record : bulkdata.getModifiedRecords())
						batchmaker.addRecord(record);

					// IF ERRORS ARE FOUND
					if (bulkdata.getRecordsError().size() > 0) {
						for (OnRecordErrorException error : bulkdata.getRecordsError()) {
							try {
								throw error;
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
					}
				}

			} catch (IllegalStateException e) {
				logger.error("[LOOKUP] IllegalStateException - getOnErrorRecord() not defined", e);
			} catch (StageException e) {
				logger.error("[LOOKUP] StageException: Error writting in thread target ", e);
				throw e;
			} catch (InterruptedException e) {
				logger.error("[LOOKUP] InterruptedException - Error executing thread ", e);
			} catch (ExecutionException e) {
				logger.error("[LOOKUP] ExecutionException - Error executing thread ", e);
			} catch (Exception e) {
				logger.error("[LOOKUP] Exception -  Error writting in thread target ", e);
			}
		}

	}

}