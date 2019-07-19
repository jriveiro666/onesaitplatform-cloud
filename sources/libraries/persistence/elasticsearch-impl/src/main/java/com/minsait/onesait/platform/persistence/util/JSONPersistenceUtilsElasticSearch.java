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
package com.minsait.onesait.platform.persistence.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONPersistenceUtilsElasticSearch {

	private final static List<String> geoShapes = Arrays.asList("point", "linestring", "polygon", "multipoint",
			"multilinestring", "multipolygon", "geometrycollection", "envelope", "circle");

	/**
	 * Check if the json contains $schema field
	 * 
	 * @param schemaString
	 * @return boolean
	 * @throws JsonSyntaxException
	 */
	public static boolean isJSONSchema(final String schemaString) throws JsonSyntaxException {
		try {
			final JsonObject jsonSchema = new JsonParser().parse(schemaString).getAsJsonObject();
			if (jsonSchema.has("$schema"))
				return true;
			else
				return false;
		} catch (final Exception e) {
			log.error("There was an error parsing the schema.", e);
			throw new JsonSyntaxException("There was an error parsing the schema.");
		}
	}

	/**
	 * Gets the elastic search index from json schema
	 * 
	 * @param schemaString
	 * @return String containing a json with the elastic search index
	 * @throws JsonSyntaxException
	 * @throws JsonParseException
	 */
	public static String getElasticSearchSchemaFromJSONSchema(final String schemaString)
			throws JsonSyntaxException, JsonParseException {
		try {
			final JsonObject jsonSchema = new JsonParser().parse(schemaString).getAsJsonObject();
			if (jsonSchema.has("properties") && jsonSchema.get("properties").isJsonObject()
					&& jsonSchema.getAsJsonObject("properties").entrySet().size() > 0)
				return getElasticIndexFromObject(jsonSchema, jsonSchema, "properties").toString();
			else
				return "";
		} catch (final JsonParseException e) {
			log.error("There was an error parsing the schema.", e);
			throw e;
		}
	}

	/**
	 * Gets the elastic search type based on the json type schema or the custom
	 * 
	 * @param type
	 * @return string containing the elastic search type
	 */
	public static String getElasticType(final String type) {
		switch (type) {
		case "string":
			return "text";
		case "number":
			return "float";
		case "boolean":
		case "integer":
		default:
			return type;
		}
	}

	/**
	 * Creates recursively the elastic search index based on the json schema
	 * 
	 * @param jsonSchema
	 *            - the full json schema in GSON JsonObject
	 * @param jsonObject
	 *            - a json object in GSON JsonObject
	 * @param propName
	 *            - the property name
	 * @return - a json object in GSON representing a elastic search field
	 * @throws JsonSyntaxException
	 */
	// TODO support recursive schema
	public static JsonObject getElasticIndexFromObject(final JsonObject jsonSchema, final JsonObject jsonObject,
			final String propName) throws JsonSyntaxException {
		final JsonObject property = jsonObject.has("$ref") ? resolveSchemaRef(jsonSchema, jsonObject) : jsonObject;
		final JsonObject elasticProperty = new JsonObject();
		final JsonObject elasticProperties = new JsonObject();

		if (property.has("type") && !property.get("type").isJsonNull()
				&& property.get("type").isJsonPrimitive()
				&& property.get("type").getAsJsonPrimitive().isString()) {
			final String propType = property.get("type").getAsString().toLowerCase().trim();

			switch (propType) {
			case "array":
				if (property.has("items")) {
					// Items is array...
					if (property.get("items").isJsonArray() && property.getAsJsonArray("items").size() > 0) {

						final List<JsonObject> itemsObjects = StreamSupport
								.stream(property.get("items").getAsJsonArray().spliterator(), false).map(it -> {
									if (it.isJsonObject()) {
										return it.getAsJsonObject().has("$ref")
												? resolveSchemaRef(jsonSchema, it.getAsJsonObject())
												: it.getAsJsonObject();
									} else
										throw new JsonSyntaxException("Property " + propName
												+ " is type array, but an items's field is not an object.");
								}).collect(Collectors.toList());

						final int itemsSize = itemsObjects.stream().map(itemJsonArray -> {
							if (itemJsonArray.has("type") && itemJsonArray.get("type").isJsonPrimitive()
									&& itemJsonArray.get("type").getAsJsonPrimitive().isString()) {
								return itemJsonArray.get("type").getAsString();
							} else
								throw new JsonSyntaxException(
										"Property " + propName + " is type array, but an item's type is not string.");
						}).distinct().collect(Collectors.toList()).size();

						if (itemsSize > 1)
							throw new JsonSyntaxException("Property " + propName
									+ " is type array and has differents types in it. Elastic search 6+ does not support multitype mapping.");

						return getElasticIndexFromObject(jsonSchema, itemsObjects.get(0), propName);

						// Items is object...
					} else if (property.get("items").isJsonObject()) {
						return getElasticIndexFromObject(jsonSchema, property.getAsJsonObject("items"), propName);
					} else
						throw new JsonSyntaxException(
								"Property '" + propName + "' is type array but field items is empty.");

				} else
					throw new JsonSyntaxException(
							"Property '" + propName + "' is type array, but no field items was found.");

			case "object":
				if (property.has("properties") && property.get("properties").isJsonObject()
						&& property.getAsJsonObject("properties").entrySet().size() > 0) {
					final JsonObject objectProperties = property.getAsJsonObject("properties");
					// If it's a mongo-date type... (should not be selectable on ui...)
					if (objectProperties.has("$date")) {
						elasticProperties.addProperty("type", "date");
						break;
						// It it's a geometry type - GeoJson...
					} else if (objectProperties.entrySet().size() == 2 && objectProperties.has("coordinates")
							&& objectProperties.has("type") && objectProperties.get("coordinates").isJsonObject()
							&& objectProperties.get("type").isJsonObject()
							&& objectProperties.getAsJsonObject("type").has("enum")
							&& objectProperties.getAsJsonObject("type").get("enum").isJsonArray()
							&& objectProperties.getAsJsonObject("type").get("enum").getAsJsonArray().size() == 1
							&& objectProperties.getAsJsonObject("type").get("enum").getAsJsonArray().get(0)
									.isJsonPrimitive()
							&& objectProperties.getAsJsonObject("type").get("enum").getAsJsonArray().get(0)
									.getAsJsonPrimitive().isString()
							&& geoShapes.contains(objectProperties.getAsJsonObject("type").get("enum").getAsJsonArray()
									.get(0).getAsString().toLowerCase())) {

						elasticProperties.addProperty("type", "geo_shape");

						// Otherwise...
					} else {
						objectProperties.entrySet().stream().forEach(pro -> {
							if (pro.getValue().isJsonObject())
								elasticProperty.add(pro.getKey(), getElasticIndexFromObject(jsonSchema,
										pro.getValue().getAsJsonObject(), pro.getKey()));
							else
								throw new JsonSyntaxException("Property " + pro.getKey() + " of property " + propName
										+ " is not an json object.");
						});

						elasticProperties.add("properties", elasticProperty);
					}

				} else
					elasticProperties.addProperty("type", "object");

				break;
			case "string":
				// If is a date format...
				if (property.has("format") && property.get("format").isJsonPrimitive()
						&& property.getAsJsonPrimitive("format").isString()
						&& (property.getAsJsonPrimitive("format").getAsString().equalsIgnoreCase("date-time")
								|| property.getAsJsonPrimitive("format").getAsString().equalsIgnoreCase("date"))) {

					elasticProperties.addProperty("type", "date");
					break;
				} // let it pass to default...
			default:
				elasticProperties.addProperty("type", getElasticType(propType));
				break;

			}

		} else
			throw new JsonSyntaxException("Type of property '" + propName + "' is not a string or does not exist.");

		return elasticProperties;
	}

	/**
	 * Resolves $ref from json schemas
	 * 
	 * @param jsonSchema
	 *            - the complete json schema in GSON JsonObject
	 * @param obj
	 *            - the json object containing $ref param in GSON JsonObject
	 * @return - the json referenced locally in GSON JsonObject
	 */
	private static JsonObject resolveSchemaRef(final JsonObject jsonSchema, final JsonObject obj) {
		if (obj.has("$ref")) {
			final String ref = obj.get("$ref").getAsString();
			if (ref.startsWith("#/")) {
				final String jsonPath = ref.substring(2).replaceAll("/", "\\.");
				final JsonElement jsonElement = getJsonElement(jsonSchema, jsonPath);
				if (jsonElement.isJsonObject())
					return jsonElement.getAsJsonObject();
				else
					throw new JsonSyntaxException(
							"Can not resolve reference. Reference '" + ref + "' not found or is not an object");
			} else
				throw new JsonSyntaxException("Can not resolve reference. It does not starts with #/.");
		} else
			throw new JsonSyntaxException("Object does not contain $ref field.");
	}

	/**
	 * Returns a JSON sub-element from the given JsonElement and the given path
	 *
	 * @param json
	 *            - a Gson JsonElement
	 * @param path
	 *            - a JSON path, e.g. a.b.c[2].d
	 * @return - a sub-element of json according to the given path
	 */
	private static JsonElement getJsonElement(JsonElement json, String path) {
		final String[] parts = path.split("\\.|\\[|\\]");
		JsonElement result = json;

		for (String key : parts) {

			key = key.trim();
			if (key.isEmpty())
				continue;

			if (result == null) {
				result = JsonNull.INSTANCE;
				break;
			}

			if (result.isJsonObject()) {
				result = ((JsonObject) result).get(key);
			} else if (result.isJsonArray()) {
				final int ix = Integer.valueOf(key) - 1;
				result = ((JsonArray) result).get(ix);
			} else
				break;
		}

		return result;
	}

}
