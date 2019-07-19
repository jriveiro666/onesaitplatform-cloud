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
package com.minsait.onesait.platform.api.rest.api;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.minsait.onesait.platform.api.service.ApiServiceInterface;
import com.minsait.onesait.platform.api.service.impl.ApiServiceImpl.ChainProcessingStatus;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/server")
@Slf4j
public class ApiManagerEntryPoint {
	public final static String ENTRY_POINT_SERVLET_URI = "/server/api";
	@Autowired
	private ApiServiceInterface apiService;

	@RequestMapping(value = "/api/**", method = RequestMethod.OPTIONS)
	public ResponseEntity<String> processOptions() {
		final HttpHeaders headers = getOptionsHeaders();
		return new ResponseEntity<>("[\"OPTIONS\",\"OPTIONS\"]", headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/api/**", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
			RequestMethod.DELETE })
	public ResponseEntity<String> processRequest(HttpServletRequest request, HttpServletResponse response,
			@RequestBody(required = false) String requestBody) {
		Map<String, Object> mData = null;
		try {
			mData = apiService.processRequestData(request, response, requestBody);
			ChainProcessingStatus status = ChainProcessingStatus
					.valueOf((String) mData.get(ApiServiceInterface.STATUS));
			if (status == ChainProcessingStatus.STOP) {
				log.error("STOP state detected: exiting");
				return buildErrorResponse(mData);
			}

			mData = apiService.processLogic(mData);
			status = ChainProcessingStatus.valueOf((String) mData.get(ApiServiceInterface.STATUS));
			if (status == ChainProcessingStatus.STOP) {
				log.error("Error Processing Query, Stop Execution detected");
				return buildErrorResponse(mData);

			}

			mData = apiService.processOutput(mData);

			return buildResponse(mData);

		} catch (final Exception e) {
			return buildErrorResponse(mData);
		}

	}

	private ResponseEntity<String> buildResponse(Map<String, Object> mData) {
		final String contentType = (String) mData.get(ApiServiceInterface.CONTENT_TYPE);
		final HttpHeaders headers = getDefaultHeaders(contentType);
		if (mData.get(ApiServiceInterface.HTTP_RESPONSE_CODE) != null)
			return new ResponseEntity<>((String) mData.get(ApiServiceInterface.OUTPUT), headers,
					(HttpStatus) mData.get(ApiServiceInterface.HTTP_RESPONSE_CODE));
		else
			return new ResponseEntity<>((String) mData.get(ApiServiceInterface.OUTPUT), headers, HttpStatus.OK);
	}

	private ResponseEntity<String> buildErrorResponse(Map<String, Object> mData) {
		final String contentType = (String) mData.get(ApiServiceInterface.CONTENT_TYPE);
		final HttpHeaders headers = getDefaultHeaders(contentType);
		if (mData.get(ApiServiceInterface.HTTP_RESPONSE_CODE) != null)
			return new ResponseEntity<>((String) mData.get(ApiServiceInterface.REASON), headers,
					(HttpStatus) mData.get(ApiServiceInterface.HTTP_RESPONSE_CODE));
		else
			return new ResponseEntity<>((String) mData.get(ApiServiceInterface.REASON), headers,
					HttpStatus.INTERNAL_SERVER_ERROR);

	}

	private HttpHeaders getDefaultHeaders(String contentType) {
		final HttpHeaders headers = new HttpHeaders();
		// headers.add("Access-Control-Allow-Origin", "*");
		// TODO Externalize this one
		// headers.add("X-Frame-Options", "SAMEORIGIN");
		headers.add(HttpHeaders.CONTENT_TYPE, contentType);
		return headers;
	}

	private HttpHeaders getOptionsHeaders() {
		final HttpHeaders headers = getDefaultHeaders(MediaType.APPLICATION_JSON_VALUE);
		headers.add("Access-Control-Allow-Headers", "X-SOFIA2-APIKey,auth-token,Content-Type");
		headers.add("Access-Control-Allow-Methods", "POST,GET,DELETE,PUT,OPTIONS");
		return headers;
	}

}
