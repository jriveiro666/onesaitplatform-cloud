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
package com.minsait.onesait.platform.api.processor.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.stereotype.Component;

import com.minsait.onesait.platform.api.processor.ScriptProcessorFactory;
import com.minsait.onesait.platform.api.service.ApiServiceInterface;

import lombok.extern.slf4j.XSlf4j;

@Component
@XSlf4j
public class ScriptProcessorFactoryImpl implements ScriptProcessorFactory {

	private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
	
	@PostConstruct
	public void init() {
		engine = new ScriptEngineManager().getEngineByName("nashorn");
	}

	@Override
	public ScriptEngine getScriptEngine() {
		return engine;
	}

	@Override
	public Object invokeScript(String script,Object...data) throws ScriptException{
		try {
			String scriptPostprocessFunction = "function postprocess(data){ " + script + " }";
			ByteArrayInputStream scriptInputStream = new ByteArrayInputStream(
					scriptPostprocessFunction.getBytes(StandardCharsets.UTF_8));
			engine.eval(new InputStreamReader(scriptInputStream));
			return ((Invocable)engine).invokeFunction("postprocess", data);
		} catch (NoSuchMethodException e) {
			throw new ScriptException(e);
		}
	}
}
