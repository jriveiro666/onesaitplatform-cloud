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
package com.minsait.onesait.platform.api.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeasy.rules.api.Facts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.github.opendevl.JFlat;
import com.minsait.onesait.platform.api.audit.aop.ApiManagerAuditable;
import com.minsait.onesait.platform.api.processor.ApiProcessorDelegate;
import com.minsait.onesait.platform.api.processor.utils.ApiProcessorUtils;
import com.minsait.onesait.platform.api.rule.DefaultRuleBase.ReasonType;
import com.minsait.onesait.platform.api.rule.RuleManager;
import com.minsait.onesait.platform.api.service.ApiServiceInterface;
import com.minsait.onesait.platform.api.service.api.ApiManagerService;
import com.minsait.onesait.platform.config.model.Api;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiServiceImpl extends ApiManagerService implements ApiServiceInterface {

	@Autowired
	private RuleManager ruleManager;

	@Autowired
	private ApiProcessorDelegate processorDelegate;

	private static final String TEXT_CSV = "text/csv";

	public enum ChainProcessingStatus {
		STOP, FOLLOW
	}

	@SuppressWarnings("unchecked")
	@Override
	@ApiManagerAuditable
	public Map<String, Object> processRequestData(HttpServletRequest request, HttpServletResponse response,
			String requestBody) throws Exception {

		final Facts facts = new Facts();
		facts.put(RuleManager.REQUEST, request);
		facts.put(RuleManager.RESPONSE, response);

		final Map<String, Object> dataFact = new HashMap<>();
		dataFact.put(ApiServiceInterface.BODY, requestBody);

		facts.put(RuleManager.FACTS, dataFact);
		ruleManager.fire(facts);

		final Map<String, Object> data = (Map<String, Object>) facts.get(RuleManager.FACTS);
		final Boolean stopped = (Boolean) facts.get(RuleManager.STOP_STATE);
		String REASON = "";
		String REASON_TYPE;

		if (stopped != null && stopped == true) {
			REASON = ((String) facts.get(RuleManager.REASON));
			REASON_TYPE = ((String) facts.get(RuleManager.REASON_TYPE));

			if (REASON_TYPE.equals(ReasonType.API_LIMIT.name())) {
				data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.TOO_MANY_REQUESTS);
			} else if (REASON_TYPE.equals(ReasonType.SECURITY.name())) {
				data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.FORBIDDEN);
			} else {
				data.put(ApiServiceInterface.HTTP_RESPONSE_CODE, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			final String messageError = ApiProcessorUtils.generateErrorMessage(REASON_TYPE,
					"Stopped Execution, Found Stop State", REASON);
			data.put(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
			data.put(ApiServiceInterface.STATUS, ChainProcessingStatus.STOP);
			data.put(ApiServiceInterface.REASON, messageError);

			// Add output to body for camel processing without exceptions
			// TO-DO REMOVE THIS
			data.put(ApiServiceInterface.OUTPUT, messageError);

		} else {
			data.put(ApiServiceInterface.STATUS, "FOLLOW");
		}
		return data;

	}

	@Override
	@ApiManagerAuditable
	public Map<String, Object> processLogic(Map<String, Object> data) throws Exception {
		final Api api = (Api) data.get(ApiServiceInterface.API);
		data = processorDelegate.proxyProcessor(api).process(data);

		return data;
	}

	@Override
	@ApiManagerAuditable
	public Map<String, Object> processOutput(Map<String, Object> data) throws Exception {

		// String objectId = null;
		// Boolean queryById = null;

		final String FORMAT_RESULT = (String) data.get(ApiServiceInterface.FORMAT_RESULT);
		String OUTPUT = (String) data.get(ApiServiceInterface.OUTPUT);
		// objectId = (String) data.get(ApiServiceInterface.OBJECT_ID);
		// queryById = (Boolean) data.get(ApiServiceInterface.QUERY_BY_ID);
		if (StringUtils.isEmpty(OUTPUT)) {
			OUTPUT = "{}";
		}

		String CONTENT_TYPE = MediaType.TEXT_PLAIN_VALUE;

		// if (output == null || output.equalsIgnoreCase("") && queryById != null) {
		// output = "{\"RESULT\":\"We can´t find a Resource for ID:" + objectId + "\"}";
		// exchange.getIn().setHeader(ApiServiceInterface.HTTP_RESPONSE_CODE_HEADER,
		// ApiServiceInterface.HTTP_RESPONSE_CODE_NOT_FOUND);
		// }
		if (StringUtils.isEmpty(FORMAT_RESULT)) {
			CONTENT_TYPE = (String) data.get(ApiServiceInterface.CONTENT_TYPE_OUTPUT);
		}
		final JSONObject jsonObj = toJSONObject(OUTPUT);
		final JSONArray jsonArray = toJSONArray(OUTPUT);
		String outputBody = OUTPUT;

		if (FORMAT_RESULT.equalsIgnoreCase("JSON") || CONTENT_TYPE.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
			CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
		} else if (FORMAT_RESULT.equalsIgnoreCase("XML")
				|| CONTENT_TYPE.equalsIgnoreCase(MediaType.APPLICATION_ATOM_XML_VALUE)
				|| CONTENT_TYPE.equalsIgnoreCase("application/xml")) {
			if (jsonObj != null)
				outputBody = XML.toString(jsonObj);
			if (jsonArray != null)
				outputBody = XML.toString(jsonArray);
			CONTENT_TYPE = MediaType.APPLICATION_ATOM_XML_VALUE;
		} else if (FORMAT_RESULT.equalsIgnoreCase("CSV") || CONTENT_TYPE.equalsIgnoreCase(TEXT_CSV)) {
			if (jsonObj != null) {
				final List<Object[]> json2csv = new JFlat(outputBody).json2Sheet().headerSeparator(".")
						.getJsonAsSheet();
				outputBody = deserializeCSV2D(json2csv);
			}
			if (jsonArray != null) {
				final List<Object[]> json2csv = new JFlat(outputBody).json2Sheet().headerSeparator(".")
						.getJsonAsSheet();
				outputBody = deserializeCSV2D(json2csv);
			}
			CONTENT_TYPE = TEXT_CSV;
		}
		// if (output != null && objectId != null && !objectId.equalsIgnoreCase("")) {
		// try {
		// jsonObj = toJSONObject(output);
		// if (jsonObj == null)
		// jsonArray = toJSONArray(output);
		// if (jsonObj == null && jsonArray != null && jsonArray.length() == 1 &&
		// queryById != null)
		// jsonObj = jsonArray.getJSONObject(0);
		// if (jsonObj != null && jsonObj.get(VALUE) != null)
		// output = jsonObj.get(VALUE).toString();
		// else if (jsonArray != null && jsonArray.length() > 0) {
		// final List<JSONObject> newArray = new ArrayList();
		// JSONObject newNode = null;
		// for (int i = 0; i < jsonArray.length(); i++) {
		// newNode = (JSONObject) jsonArray.get(i);
		// newArray.add((JSONObject) newNode.get(VALUE));
		// }
		// output = newArray.toString();
		// }
		// } catch (final JSONException e) {
		// log.warn("Not value in result...");
		// }
		// }
		data.put(ApiServiceInterface.OUTPUT, outputBody);
		data.put(ApiServiceInterface.CONTENT_TYPE, CONTENT_TYPE);
		return data;

	}

	private JSONObject toJSONObject(String input) {
		JSONObject jsonObj = null;

		try {
			jsonObj = new JSONObject(input);
		} catch (final JSONException e) {
			return null;
		}
		return jsonObj;
	}

	private JSONArray toJSONArray(String input) {
		JSONArray jsonObj = null;
		try {
			jsonObj = new JSONArray(input);
		} catch (final JSONException e) {
			return null;
		}
		return jsonObj;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private static JSONObject getJsonFromMap(Map<String, Object> map) throws JSONException {
		final JSONObject jsonData = new JSONObject();
		for (final String key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof Map<?, ?>) {
				value = getJsonFromMap((Map<String, Object>) value);
			}
			jsonData.put(key, value);
		}
		return jsonData;
	}

	private static String deserializeCSV2D(List<Object[]> matrix) {
		final StringBuilder builder = new StringBuilder();
		final int size = matrix.get(0).length;
		matrix.forEach(a -> {
			final List<Object> columns = Arrays.asList(a);
			for (int i = 0; i < size; i++) {
				builder.append(columns.get(i));
				if (i + 1 != size)
					builder.append(",");
			}
			builder.append(System.getProperty("line.separator"));
		});
		return builder.toString();
	}

}
