/**
 * Copyright Indra Soluciones Tecnologías de la Información, S.L.U.
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
package com.minsait.onesait.platform.persistence.mongodb.timeseries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.jline.utils.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minsait.onesait.platform.commons.model.TimeSeriesResult;
import com.minsait.onesait.platform.config.model.Ontology;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesProperty;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesProperty.PropertyDataType;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesProperty.PropertyType;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesWindow;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesWindow.FrecuencyUnit;
import com.minsait.onesait.platform.config.model.OntologyTimeSeriesWindow.WindowType;
import com.minsait.onesait.platform.config.repository.OntologyRepository;
import com.minsait.onesait.platform.config.repository.OntologyTimeSeriesPropertyRepository;
import com.minsait.onesait.platform.config.repository.OntologyTimeSeriesWindowRepository;
import com.minsait.onesait.platform.persistence.mongodb.template.MongoDbTemplate;
import com.minsait.onesait.platform.persistence.mongodb.timeseries.exception.TimeSeriesFrecuencyNotSupportedException;
import com.minsait.onesait.platform.persistence.mongodb.timeseries.exception.WindowNotSupportedException;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MongoDBTimeSeriesProcessorImpl implements MongoDBTimeSeriesProcessor {

	private final static String TIMESTAMP_PROPERTY = "timestamp";
	private final static String PROPERTY_NAME = "propertyName";
	private final static String WINDOW_TYPE = "windowType";
	private final static String WINDOW_FRECUENCY_UNIT = "windowFrecuencyUnit";
	private final static String WINDOW_FRECUENCY = "windowFrecuency";

	private final static String SDATE = "$date";
	private final static String CONTEXT_DATA = "contextData";

	private final static String FORMAT_WINDOW_SECONDS = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private final static String FORMAT_WINDOW_MINUTES = "yyyy-MM-dd'T'HH:mm";
	private final static String FORMAT_WINDOW_HOURS = "yyyy-MM-dd'T'HH";
	private final static String FORMAT_WINDOW_DAYS = "yyyy-MM-dd";
	private final static String FORMAT_WINDOW_MONTHS = "yyyy-MM";

	@Value("${onesaitplatform.database.mongodb.database:onesaitplatform_rtdb}")
	@Getter
	@Setter
	private String database;

	@Getter
	@Setter
	private long queryExecutionTimeout;

	@Value("${onesaitplatform.database.timeseries.timezone:UTC}")
	private String timeZone;

	@Autowired
	private OntologyTimeSeriesPropertyRepository ontologyTimeSeriesPropertyRepository;

	@Autowired
	private OntologyTimeSeriesWindowRepository ontologyTimeSeriesWindowRepository;

	@Autowired
	private MongoDbTemplate mongoDbConnector;

	@Autowired
	private OntologyRepository ontologyRepository;

	protected ObjectMapper objectMapper;

	@PostConstruct
	public void init() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
	}

	@Override
	public List<TimeSeriesResult> processTimeSerie(String ontology, String instance) {

		log.info("Process TimeSerie instance for ontology {}", ontology);
		List<TimeSeriesResult> result = new ArrayList<TimeSeriesResult>();

		// Get Properties declared for the Timeserie ontology
		List<OntologyTimeSeriesProperty> lProperties = ontologyTimeSeriesPropertyRepository
				.findByOntologyIdentificaton(ontology);

		// Get Windows declared for the Timeserie ontology
		List<OntologyTimeSeriesWindow> lTimeSeriesWindows = ontologyTimeSeriesWindowRepository
				.findByOntologyIdentificaton(ontology);

		// Divide Root element and Data of the instance
		JSONObject oInstance = new JSONObject(instance);
		JSONObject instanceData;

		Optional<String> rootElement = Optional.empty();
		if (oInstance.keySet().size() == 2 && oInstance.keySet().contains(CONTEXT_DATA)) {// Check if there is root
																							// element

			rootElement = oInstance.keySet().stream().filter(p -> !((String) p).equals(CONTEXT_DATA)).findFirst();

			instanceData = (JSONObject) oInstance.get(rootElement.get());

		} else {
			instanceData = oInstance;
		}

		final Optional<String> rootkey = rootElement;

		// Extract Tags from instance
		Map<OntologyTimeSeriesProperty, Object> mTags = this.extractTags(lProperties, instanceData);

		// Extract Properties from instance
		Map<OntologyTimeSeriesProperty, Object> mFields = this.extractFields(lProperties, instanceData);

		// Extract Timestamp from instance
		JSONObject timestamp = (JSONObject) instanceData.get(TIMESTAMP_PROPERTY);
		String formattedDate = (String) timestamp.get(SDATE);

		// Process Instance for each declared window
		lTimeSeriesWindows.forEach(window -> {
			try {
				log.debug("Process window {} for ontology {}", window.getWindowType().name(), ontology);
				result.addAll(manageWindow(ontology, rootkey, mTags, mFields, formattedDate, window));
			} catch (TimeSeriesFrecuencyNotSupportedException | WindowNotSupportedException e) {
				log.error("Error processing TimeSeries Window", e);
			}

		});

		return result;

	}

	/**
	 * Extract Tag (Inmutable properties) properties for the instance
	 * 
	 * @param lProperties
	 * @param oInstance
	 * @return
	 */
	private Map<OntologyTimeSeriesProperty, Object> extractTags(List<OntologyTimeSeriesProperty> lProperties,
			JSONObject oInstance) {
		// Extract Tags from instance
		Map<OntologyTimeSeriesProperty, Object> mTags = new HashMap<OntologyTimeSeriesProperty, Object>();
		lProperties.stream().filter(p -> p.getPropertyType() == PropertyType.TAG).forEach(p -> {
			mTags.put(p, oInstance.get(p.getPropertyName()));
		});

		return mTags;
	}

	/**
	 * Extract Fields (Variable properties) properties for the instance
	 * 
	 * @param lProperties
	 * @param oInstance
	 * @return
	 */
	private Map<OntologyTimeSeriesProperty, Object> extractFields(List<OntologyTimeSeriesProperty> lProperties,
			JSONObject oInstance) {
		Map<OntologyTimeSeriesProperty, Object> mFields = new HashMap<OntologyTimeSeriesProperty, Object>();
		lProperties.stream().filter(p -> p.getPropertyType() == PropertyType.SERIE_FIELD).forEach(p -> {
			mFields.put(p, oInstance.get(p.getPropertyName()));
		});

		return mFields;
	}

	/**
	 * Process The instance for each Window
	 * 
	 * @param ontology
	 * @param rootElement
	 * @param mTags
	 * @param mFields
	 * @param formattedDate
	 * @param window
	 * @throws TimeSeriesFrecuencyNotSupportedException
	 * @throws WindowNotSupportedException
	 */
	private List<TimeSeriesResult> manageWindow(String ontology, Optional<String> rootElement,
			Map<OntologyTimeSeriesProperty, Object> mTags, Map<OntologyTimeSeriesProperty, Object> mFields,
			String formattedDate, OntologyTimeSeriesWindow window)
			throws TimeSeriesFrecuencyNotSupportedException, WindowNotSupportedException {

		Ontology stats = ontologyRepository.findByIdentification(ontology + "_stats");

		List<TimeSeriesResult> result = new ArrayList<TimeSeriesResult>();

		// Validations and recover SimpleDateformat for the current window
		SimpleDateFormat sdfInstancePrecision = this.validateWindowAndGetDateFormat(window);

		// Check if the document has root to append it as prefix to all properties
		final String propertyPrefix = getDocumentBase(rootElement);
		final String statsPrefix = "Stats";
		try {
			// Get Calendar with the maximum precission for this window
			SimpleDateFormat sdfSeconds = new SimpleDateFormat(FORMAT_WINDOW_SECONDS);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(sdfSeconds.parse(formattedDate));

			sdfInstancePrecision.setTimeZone(TimeZone.getTimeZone(timeZone));

			Date dInstance = sdfInstancePrecision.parse(formattedDate);

			for (Entry<OntologyTimeSeriesProperty, Object> field : mFields.entrySet()) {
				try {
					// Build the query object with all tags, the concrete field and the timestamp
					// Append Tags to the query
					BasicDBObject objQuery = new BasicDBObject();
					mTags.forEach((key, value) -> {
						if (key.getPropertyDataType().equals(PropertyDataType.OBJECT))
							objQuery.put(propertyPrefix.concat(key.getPropertyName()), JSON.parse(value.toString()));
						else
							objQuery.put(propertyPrefix.concat(key.getPropertyName()), value);
					});

					// Append concrete Field to the query
					objQuery.put(propertyPrefix.concat(PROPERTY_NAME), field.getKey().getPropertyName());

					// Append Timestamp to the query
					objQuery.put(propertyPrefix.concat(TIMESTAMP_PROPERTY), dInstance);

					// Append window Type to the query
					objQuery.put(propertyPrefix.concat(WINDOW_TYPE), window.getWindowType().name());

					// Append window Frecuency to the query
					objQuery.put(propertyPrefix.concat(WINDOW_FRECUENCY), window.getFrecuency());
					objQuery.put(propertyPrefix.concat(WINDOW_FRECUENCY_UNIT), window.getFrecuencyUnit().name());

					// Append timestamp of the document to the query
					objQuery.put(propertyPrefix.concat(TIMESTAMP_PROPERTY), dInstance);

					// Add stats if allowed
					if (stats != null) {
						BasicDBObject objStat = new BasicDBObject();
						ArrayList<BasicDBObject> tags = new ArrayList<BasicDBObject>();
						for (Entry<OntologyTimeSeriesProperty, Object> tag : mTags.entrySet()) {
							BasicDBObject tagDBObj = new BasicDBObject();
							tagDBObj.put("name", tag.getKey().getPropertyName());
							if (tag.getKey().getPropertyDataType().equals(PropertyDataType.OBJECT)) {
								tagDBObj.put("value", JSON.parse(tag.getValue().toString()));
							} else {
								tagDBObj.put("value", tag.getValue());
							}
							tags.add(tagDBObj);
						}
						objStat.put("tag", tags);
						objStat.put("field", field.getKey().getPropertyName());
						objStat.put(WINDOW_TYPE, window.getWindowType().name());
						objStat.put(WINDOW_FRECUENCY, window.getFrecuency());
						objStat.put(WINDOW_FRECUENCY_UNIT, window.getFrecuencyUnit().name());
						BasicDBObject lastValue;
						if (field.getKey().getPropertyDataType().equals(PropertyDataType.OBJECT)) {
							lastValue = new BasicDBObject("value", JSON.parse(field.getValue().toString()));
						} else {
							lastValue = new BasicDBObject("value", field.getValue());
						}
						objStat.put("lastValue", lastValue);
						BasicDBObject sample = new BasicDBObject(statsPrefix, objStat);
						mongoDbConnector.insert(database, stats.getIdentification(), sample);
					}

					// Build update
					String update = this.buildUpdateSet(propertyPrefix, field, calendar, window);

					log.debug("Try to update TimeSeries ontology {} for window {}", ontology,
							window.getWindowType().name());

					// try to update the document --> Consider it exists, if not, it will be created
					long nUpdated = mongoDbConnector
							.update(database, ontology, objQuery.toString(), update, false, false).getCount();

					if (nUpdated == 0) {// Check if exists and previously created. If not, Build the document
						log.debug("Check if document exits for TimeSeries ontology {} for window {}", ontology,
								window.getWindowType().name());
						boolean documentExists = mongoDbConnector
								.find(database, ontology, objQuery, null, null, 0, 1, 5000).iterator().hasNext();

						if (!documentExists) {// Document not exists, create new one and insert it
							log.debug("Create new document for TimeSeries ontology {} for window {}", ontology,
									window.getWindowType().name());
							BasicDBObject timeIntance = this.buildDocument(rootElement, calendar, field, dInstance,
									mTags, window);
							mongoDbConnector.insert(database, ontology, timeIntance);
						}
					}

					TimeSeriesResult partialResult = new TimeSeriesResult();
					partialResult.setFieldName(field.getKey().getPropertyName());
					partialResult.setOk(true);
					partialResult.setWindowType(window.getWindowType().name());

					result.add(partialResult);

				} catch (Exception e) {
					Log.error("Error processing window for ontology {} and property {}", ontology,
							field.getKey().getPropertyName(), e);

					TimeSeriesResult partialResult = new TimeSeriesResult();
					partialResult.setFieldName(field.getKey().getPropertyName());
					partialResult.setOk(false);
					partialResult.setWindowType(window.getWindowType().name());
					partialResult.setErrorMessage(e.getMessage());

					result.add(partialResult);
				}
			}

		} catch (Exception e) {
			Log.error("Error processing window for ontology {}", ontology, e);
		}

		return result;

	}

	private SimpleDateFormat validateWindowAndGetDateFormat(OntologyTimeSeriesWindow window)
			throws TimeSeriesFrecuencyNotSupportedException, WindowNotSupportedException {
		WindowType windowType = window.getWindowType();
		FrecuencyUnit frecuencyUnit = window.getFrecuencyUnit();
		if (windowType == WindowType.MINUTES) {
			if (frecuencyUnit != FrecuencyUnit.SECONDS) {
				throw new TimeSeriesFrecuencyNotSupportedException(
						"In minutes Window only Second frecuency is supported");
			}
			return new SimpleDateFormat(FORMAT_WINDOW_MINUTES);

		} else if (windowType == WindowType.HOURS) {
			if (frecuencyUnit != FrecuencyUnit.SECONDS && frecuencyUnit != FrecuencyUnit.MINUTES) {
				throw new TimeSeriesFrecuencyNotSupportedException(
						"In hours Window only Seconds and Minutes frecuencies are supported");
			}
			return new SimpleDateFormat(FORMAT_WINDOW_HOURS);
		} else if (windowType == WindowType.DAYS) {
			if (frecuencyUnit != FrecuencyUnit.SECONDS && frecuencyUnit != FrecuencyUnit.MINUTES
					&& frecuencyUnit != FrecuencyUnit.HOURS) {
				throw new TimeSeriesFrecuencyNotSupportedException(
						"In hours Window only Seconds, Minutes and Hours frecuencies are supported");
			}
			return new SimpleDateFormat(FORMAT_WINDOW_DAYS);

		} else if (windowType == WindowType.MONTHS) {
			if (frecuencyUnit != FrecuencyUnit.SECONDS && frecuencyUnit != FrecuencyUnit.MINUTES
					&& frecuencyUnit != FrecuencyUnit.HOURS && frecuencyUnit != FrecuencyUnit.DAYS) {
				throw new TimeSeriesFrecuencyNotSupportedException(
						"In hours Window only Seconds, Minutes, Hours and Days frecuencies are supported");
			}
			return new SimpleDateFormat(FORMAT_WINDOW_MONTHS);
		} else {
			throw new WindowNotSupportedException("Window type " + windowType.name() + " not supported");
		}
	}

	private String buildUpdateSet(String propertyPrefix, Entry<OntologyTimeSeriesProperty, Object> field,
			Calendar calendar, OntologyTimeSeriesWindow window)
			throws JsonProcessingException, WindowNotSupportedException {
		WindowType windowType = window.getWindowType();
		int frecuency = window.getFrecuency();

		String update = "{$set :{\"" + propertyPrefix + "values.v.";
		if (windowType == WindowType.MINUTES) {
			update += Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency)
					+ "\": " + this.toJsonValue(field.getKey(), field.getValue()) + "}}";

			return update;
		} else if (windowType == WindowType.HOURS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				update += Integer.toString(calendar.get(Calendar.MINUTE)) + "."
						+ Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency)
						+ "\": ";
				break;

			case MINUTES:
				update += Integer.toString(calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency)
						+ "\": ";
				break;
			}

		} else if (windowType == WindowType.DAYS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				update += Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)) + "."
						+ Integer.toString(calendar.get(Calendar.MINUTE)) + "."
						+ Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency)
						+ "\": ";
				break;

			case MINUTES:
				update += Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)) + "."
						+ Integer.toString(calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency)
						+ "\": ";
				break;
			case HOURS:
				update += Integer.toString(
						calendar.get(Calendar.HOUR_OF_DAY) - calendar.get(Calendar.HOUR_OF_DAY) % frecuency) + "\":";
			}

		} else if (windowType == WindowType.MONTHS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				update += Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)) + "."
						+ Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)) + "."
						+ Integer.toString(calendar.get(Calendar.MINUTE)) + "."
						+ Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency)
						+ "\": ";
				break;

			case MINUTES:
				update += Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)) + "."
						+ Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)) + "."
						+ Integer.toString(calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency)
						+ "\": ";
				break;
			case HOURS:
				update += Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)) + "."
						+ Integer.toString(
								calendar.get(Calendar.HOUR_OF_DAY) - calendar.get(Calendar.HOUR_OF_DAY) % frecuency)
						+ "\":";
				break;
			case DAYS:
				update += Integer.toString(
						calendar.get(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) % frecuency) + "\":";
			}

		} else {
			throw new WindowNotSupportedException("Window type " + windowType.name() + " not supported");
		}

		// TODO SOPORTE PARA AGREGACIÓN
		// if(field.getValue() instanceof Integer || field.getValue() instanceof Long ||
		// field.getValue() instanceof Double) {
		// Object value;
		// switch (window.getAggregationFunction()) {
		// case AVG:
		// break;
		// case MAX:
		// break;
		// case MIN:
		// break;
		// case FIRST:
		// break;
		// case LAST:
		// break;
		// }
		// }

		update += this.toJsonValue(field.getKey(), field.getValue()) + "}}";

		return update;
	}

	private BasicDBObject buildDocument(Optional<String> rootElement, Calendar calendar,
			Entry<OntologyTimeSeriesProperty, Object> field, Date dInstance,
			Map<OntologyTimeSeriesProperty, Object> mTags, OntologyTimeSeriesWindow window)
			throws WindowNotSupportedException, JsonProcessingException {

		WindowType windowType = window.getWindowType();

		Map<String, Object> vMeasures;

		if (windowType == WindowType.MINUTES) {
			vMeasures = this.buildNewMinuteMap(calendar, window.getFrecuency());
		} else if (windowType == WindowType.HOURS) {
			vMeasures = this.buildNewHourMap(window.getFrecuencyUnit(), calendar, window.getFrecuency());
		} else if (windowType == WindowType.DAYS) {
			vMeasures = this.buildNewDayMap(window.getFrecuencyUnit(), calendar, window.getFrecuency());
		} else if (windowType == WindowType.MONTHS) {
			vMeasures = this.buildNewMonthMap(window.getFrecuencyUnit(), calendar, window.getFrecuency());
		} else {
			throw new WindowNotSupportedException("Window type " + windowType.name() + " not supported");
		}

		if (field.getKey().getPropertyDataType() == PropertyDataType.OBJECT)
			vMeasures = this.setFirstValueOfDocument(vMeasures, window, calendar,
					JSON.parse(field.getValue().toString()));
		else
			vMeasures = this.setFirstValueOfDocument(vMeasures, window, calendar, field.getValue());

		BasicDBObject vArray = new BasicDBObject(vMeasures);

		BasicDBObject v = new BasicDBObject();
		v.put("v", vArray);

		BasicDBObject data = new BasicDBObject();
		data.put("values", v);

		mTags.forEach((key, value) -> {
			if (key.getPropertyDataType().equals(PropertyDataType.OBJECT))
				data.put(key.getPropertyName(), JSON.parse(value.toString()));
			else
				data.put(key.getPropertyName(), value);

		});

		data.put(TIMESTAMP_PROPERTY, dInstance);
		data.put(PROPERTY_NAME, field.getKey().getPropertyName());
		data.put(WINDOW_TYPE, windowType.name());
		data.put(WINDOW_FRECUENCY, window.getFrecuency());
		data.put(WINDOW_FRECUENCY_UNIT, window.getFrecuencyUnit().name());

		BasicDBObject timeIntance;
		if (rootElement.isPresent()) {
			timeIntance = new BasicDBObject(rootElement.get(), data);
		} else {
			timeIntance = data;
		}

		return timeIntance;
	}

	@SuppressWarnings({ "unchecked", "incomplete-switch" })
	private Map<String, Object> setFirstValueOfDocument(Map<String, Object> vMeasures, OntologyTimeSeriesWindow window,
			Calendar calendar, Object value) throws JsonProcessingException, WindowNotSupportedException {
		WindowType windowType = window.getWindowType();
		int frecuency = window.getFrecuency();

		if (windowType == WindowType.MINUTES) {
			vMeasures.put(Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency),
					value);
		} else if (windowType == WindowType.HOURS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				((Map<String, Object>) vMeasures.get(Integer.toString(calendar.get(Calendar.MINUTE)))).put(
						Integer.toString(calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency),
						value);
				break;
			case MINUTES:
				vMeasures.put(
						Integer.toString(calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency),
						value);

				break;
			}

		} else if (windowType == WindowType.DAYS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				((Map<String, Object>) ((Map<String, Object>) vMeasures
						.get(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))))
								.get(Integer.toString(calendar.get(Calendar.MINUTE)))).put(Integer.toString(
										calendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND) % frecuency),
										value);

				break;

			case MINUTES:
				((Map<String, Object>) vMeasures.get(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)))).put(
						Integer.toString(calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency),
						value);

				break;
			case HOURS:

				vMeasures.put(
						Integer.toString(
								calendar.get(Calendar.HOUR_OF_DAY) - calendar.get(Calendar.HOUR_OF_DAY) % frecuency),
						value);
				break;
			}

		} else if (windowType == WindowType.MONTHS) {
			switch (window.getFrecuencyUnit()) {
			case SECONDS:
				((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) vMeasures
						.get(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))))
								.get(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))))
										.get(Integer.toString(calendar.get(Calendar.MINUTE))))
												.put(Integer.toString(calendar.get(Calendar.SECOND)
														- calendar.get(Calendar.SECOND) % frecuency), value);

				break;

			case MINUTES:
				((Map<String, Object>) ((Map<String, Object>) vMeasures
						.get(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))))
								.get(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)))).put(Integer.toString(
										calendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE) % frecuency),
										value);
				break;
			case HOURS:
				((Map<String, Object>) vMeasures.get(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)))).put(
						Integer.toString(
								calendar.get(Calendar.HOUR_OF_DAY) - calendar.get(Calendar.HOUR_OF_DAY) % frecuency),
						value);
				break;
			case DAYS:
				vMeasures.put(
						Integer.toString(
								calendar.get(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) % frecuency),
						value);
			}

		} else {
			throw new WindowNotSupportedException("Window type " + windowType.name() + " not supported");
		}

		return vMeasures;
	}

	private Map<String, Object> buildNewMinuteMap(Calendar calendar, int frecuency) {
		Map<String, Object> vSeconds = new LinkedHashMap<String, Object>();
		for (int i = 0; i < 60; i = i + frecuency) {
			vSeconds.put(Integer.toString(i), null);
		}

		return vSeconds;
	}

	private Map<String, Object> buildNewHourMap(FrecuencyUnit frecuencyUnit, Calendar calendar, int frecuency) {
		Map<String, Object> vMinutes = new LinkedHashMap<String, Object>();
		if (frecuencyUnit == FrecuencyUnit.MINUTES) {
			for (int i = 0; i < 60; i = i + frecuency) {
				vMinutes.put(Integer.toString(i), null);
			}

		} else {// Seconds
			for (int i = 0; i < 60; i++) {
				vMinutes.put(Integer.toString(i), this.buildNewMinuteMap(calendar, frecuency));
			}
		}

		return vMinutes;
	}

	private Map<String, Object> buildNewDayMap(FrecuencyUnit frecuencyUnit, Calendar calendar, int frecuency) {
		Map<String, Object> vHours = new LinkedHashMap<String, Object>();
		if (frecuencyUnit == FrecuencyUnit.HOURS) {
			for (int i = 0; i < 24; i = i + frecuency) {
				vHours.put(Integer.toString(i), null);
			}
		} else {// Minutes or Seconds
			for (int i = 0; i < 24; i++) {
				vHours.put(Integer.toString(i), this.buildNewHourMap(frecuencyUnit, calendar, frecuency));
			}
		}

		return vHours;
	}

	private Map<String, Object> buildNewMonthMap(FrecuencyUnit frecuencyUnit, Calendar calendar, int frecuency) {
		Map<String, Object> vDays = new LinkedHashMap<String, Object>();
		if (frecuencyUnit == FrecuencyUnit.DAYS) {
			for (int i = 1; i <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i = i + frecuency) {
				vDays.put(Integer.toString(i), null);
			}
		} else {// Hours, Minutes or Seconds
			for (int i = 1; i <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH); i++) {
				vDays.put(Integer.toString(i), this.buildNewDayMap(frecuencyUnit, calendar, frecuency));
			}
		}

		return vDays;
	}

	private String toJsonValue(OntologyTimeSeriesProperty property, Object value) throws JsonProcessingException {
		switch (property.getPropertyDataType()) {
		case INTEGER:
		case NUMBER:
			return value.toString();
		case STRING:
			return value.toString();
		case OBJECT:
			return value.toString();
		default:
			return "";

		}
	}

	private String getDocumentBase(Optional<String> rootElement) {
		String base = "";
		if (rootElement.isPresent()) {
			base = rootElement.get() + ".";
		}
		return base;
	}

}
