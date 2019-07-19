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
package com.minsait.onesait.platform.api.rule.rules;

import java.io.IOException;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import com.minsait.onesait.platform.api.rule.DefaultRuleBase;
import com.minsait.onesait.platform.api.rule.RuleManager;
import com.minsait.onesait.platform.api.service.ApiServiceInterface;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

@Component
@Rule
public class ValidBodyRule extends DefaultRuleBase {

	@Priority
	public int getPriority() {
		return 2;
	}

	@Condition
	public boolean existsRequest(Facts facts) {
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) facts.get(RuleManager.FACTS);
		Object body = data.get(ApiServiceInterface.BODY);
		if ( body!=null)
			return true;
		else
			return false;
	}

	@Action
	public void setFirstDerivedData(Facts facts) throws IOException, ParseException {
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) facts.get(RuleManager.FACTS);

		String body = (String)data.get(ApiServiceInterface.BODY);
		
		if (!"".equals(body)) {
			boolean valid = isValidJSON(body);
			boolean validMongo = isValidJSONtoMongo(body);
			
			
			if (valid && validMongo) {
				
				String bodyDepured = depureJSON(body);
				if (bodyDepured!=null)
					data.put(ApiServiceInterface.BODY, bodyDepured);
			}
				
			else 
				stopAllNextRules(facts, "BODY IS NOT JSON PARSEABLE ",DefaultRuleBase.ReasonType.GENERAL);
		}
		
		

	}
	
	public boolean isValidJSON(String toTestStr) {
		JSONObject jsonObj = toJSONObject(toTestStr);
		JSONArray jsonArray = toJSONArray(toTestStr);
		
		if (jsonObj!=null || jsonArray!=null) return true;
		else return false;
	}
	
	private JSONObject toJSONObject(String input) {
		JSONObject jsonObj =null;
		try {
			jsonObj = new JSONObject(input);
		} catch (JSONException e) {
			return null;
		}
		return jsonObj;
	}
	
	private JSONArray toJSONArray(String input) {
		JSONArray jsonObj =null;
		try {
			jsonObj = new JSONArray(input);
		} catch (JSONException e) {
			return null;
		}
		return jsonObj;
	}
	
	public boolean isValidJSONtoMongo(String body) {
		try {
			DBObject dbObject = (DBObject) JSON.parse(body);
			
			
			
			if (dbObject!=null) return true;
			else return false;
		}
		catch (Exception e) {return false;}
	
	}
	
	public String depureJSON(String body) {
		
		DBObject dbObject =null;
		try {
			dbObject = (DBObject) JSON.parse(body);
			if (dbObject==null) return null;
			else {
				return dbObject.toString();
			}
		}
		catch (Exception e) {return null;}
	
	}
}