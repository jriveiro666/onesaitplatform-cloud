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
package com.minsait.onesait.platform.streamsets.format;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.streamsets.pipeline.api.Field;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class FieldParser {

	/**
	 * Parses JSONObject and returns a Map to merge with the original record
	 * 
	 * @param JSONObject
	 * @return Map<String, Field>
	 */
	public static Map<String, Field> ParseJsonObject(JsonObject obj) throws Exception {
		Map<String, Field> field = new HashMap<String, Field>();
		try {
			for (String key : obj.keySet()) {
				JsonElement val = obj.get(key);
				if (val.isJsonNull()) {
					field.put(key, null);
					continue;
				}
				if (val.isJsonPrimitive()) {
					field.put(key, evaluateSingle(val.getAsJsonPrimitive()));
					continue;
				}
				if (val.isJsonObject()) {
					Field fieldParsed = evaluateMap(val.getAsJsonObject());
					if (fieldParsed != null)
						field.put(key, fieldParsed);
					continue;
				}
				if (val.isJsonArray()) {
					Field fieldParsed = evaluateArray(val.getAsJsonArray());
					if (fieldParsed != null)
						field.put(key, fieldParsed);
				}
			}
		} catch (Exception e) {
			throw new Exception("There was an error parsing the JsonObject: " + e.getMessage());
		}
		return field;
	}

	/**
	 * Evaluates array and parses it as a Field (LIST)
	 * 
	 * @param JSONArray
	 * @return Field
	 */
	private static Field evaluateArray(JsonArray array) {
		List<Field> list = new ArrayList<Field>();
		for (JsonElement key : array) {
			if (key.isJsonNull()) {
				list.add(Field.create(Field.Type.STRING, null));
				continue;
			}
			if (key.isJsonPrimitive()) {
				list.add(evaluateSingle(key.getAsJsonPrimitive()));
				continue;
			}
			if (key.isJsonObject()) {
				Field field = evaluateMap(key.getAsJsonObject());
				if (field != null)
					list.add(field);
				continue;
			}
			if (key.isJsonArray()) {
				Field field = evaluateArray(key.getAsJsonArray());
				if (field != null) {
					list.add(field);
				}
			}
		}
		if (list.isEmpty()) {
			return null;
		}
		return Field.create(Field.Type.LIST, list);
	}

	/**
	 * Evaluates map and parses it as a Field (MAP)
	 * 
	 * @param JSONObject
	 * @return Field
	 */
	private static Field evaluateMap(JsonObject json) {
		Map<String, Field> map = new HashMap<String, Field>();
		for (String key : json.keySet()) {
			if (json.get(key).isJsonNull()) {
				map.put(key, Field.create(Field.Type.STRING, null));
				continue;
			}
			if (json.get(key).isJsonPrimitive()) {
				map.put(key, evaluateSingle(json.get(key).getAsJsonPrimitive()));
				continue;
			}
			if (json.get(key).isJsonObject()) {
				Field field = evaluateMap(json.get(key).getAsJsonObject());
				if (field != null) map.put(key, field);
				continue;
			}
			if (json.get(key).isJsonArray()) {
				Field field = evaluateArray(json.get(key).getAsJsonArray());
				if (field != null) map.put(key, field);	
			}
		}
		if (map.isEmpty()) return null;
		return Field.create(Field.Type.LIST_MAP, map);
	}

	/**
	 * Evaluates primitive types and returns as Field
	 * 
	 * @param Object
	 * @return Field
	 */
	private static Field evaluateSingle(JsonPrimitive obj) {
		if (obj.isNumber())
			return Field.create(Field.Type.DOUBLE, Double.valueOf(obj.getAsDouble()));
		if (obj.isBoolean()) 
			return Field.create(Field.Type.BOOLEAN, Boolean.valueOf(obj.getAsBoolean()));
		
		return Field.create(Field.Type.STRING, obj.getAsString());
	}

	/**
	 * Returns the value of the field depending on the type
	 * It can return null
	 * 
	 * @param field
	 * @return Object
	 */
	public static Object ParseFieldValue(Field field) {
		if(field == null) return field;
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
				return formatToIsoDate(new DateTime(field.getValueAsDate()));
			case STRING:
			default:
				return field.getValueAsString();
		}
	}

	private static String formatToIsoDate(DateTime date) {
		return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").print(date);
	}

}
