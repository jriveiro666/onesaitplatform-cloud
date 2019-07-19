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
package com.minsait.onesait.platform.controlpanel.services.gravitee;

import java.util.List;

import com.minsait.onesait.platform.config.model.Api;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiCreate;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiPage;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiPlan;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.ApiUpdate;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.GraviteeApi;
import com.minsait.onesait.platform.controlpanel.gravitee.dto.GraviteeException;

public interface GraviteeService {

	ApiUpdate createApi(ApiCreate api) throws GraviteeException;

	ApiUpdate updateApi(ApiUpdate api) throws GraviteeException;

	ApiPlan createDefaultPlan(String apiId) throws GraviteeException;

	void deletePlan(String apiId, String planId) throws GraviteeException;

	List<ApiPlan> getApiPlans(String apiId) throws GraviteeException;

	void startApi(String apiId) throws GraviteeException;

	void stopApi(String apiId) throws GraviteeException;

	void deployApi(String apiId) throws GraviteeException;

	ApiUpdate getApi(String apiId) throws GraviteeException;

	void deleteApi(String apiId) throws GraviteeException;

	ApiPage createSwaggerDocPage(String apiId, Api api) throws GraviteeException;

	GraviteeApi processApi(Api api);

}
