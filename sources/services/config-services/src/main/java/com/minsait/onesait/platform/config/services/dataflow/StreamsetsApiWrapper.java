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
package com.minsait.onesait.platform.config.services.dataflow;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class StreamsetsApiWrapper {
    
    private StreamsetsApiWrapper() {
        
    }
    
    private static String CONTEXT = "/rest";
    private static String VERSION = "/v1";
    
    public static HttpHeaders setBasicAuthorization(HttpHeaders headers, String dataflowUser, String dataflowPass) {
        String basic = dataflowUser + ":" + dataflowPass;
        String encoding = new String(java.util.Base64.getEncoder().encode(basic.getBytes()));
        headers.set("Authorization", "Basic " + encoding);
        return headers;
    }

    public static String pipelineHistory(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl, String pipelineId, boolean fromBeginning) {
        // /rest/v1/pipeline/<pipelineId>/history
        // /rest/v1/pipeline/OPCUANODESINFOc266de0a-d81c-4032-b7f7-3592c3440ca4/history?rev=0&fromBeginning=false
        
        //configure headers
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> headersEntity = new HttpEntity<>(headers);
        
        //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/history")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder
                .fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder
                .fromUri(uri)
                .queryParam("rev", "0")
                .queryParam("fromBeginning", fromBeginning)                
                .build()
                .toUri();
        
        HttpEntity<String> response = rt.exchange(
                uri, 
                HttpMethod.GET, 
                headersEntity, 
                String.class);
        
        return response.getBody();
    }
    
    public static ResponseEntity<String> pipelineStart(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl, String pipelineId, String parameters) {
        // /rest/v1/pipeline/<pipelineId>/start
        // /rest/v1/pipeline/OPCUAdatab87df379-a083-4e25-846d-3d4b4859a28d/start?rev=0
        
        //configure headers
        headers.set("X-Requested-By", "sdc"); //this header is needed in POST, DELETE and PUT methods
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<?> headersEntity = new HttpEntity<>(parameters,headers);
        
      //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/start")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder.fromUri(uri)
                .queryParam("rev", "0")
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.POST, 
                headersEntity, 
                String.class);
    }
    
    public static ResponseEntity<String> pipelineStop(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl, String pipelineId) {
        // /rest/v1/pipeline/<pipelineId>/stop
        // /rest/v1/pipeline/OPCUAREADSIGNALSac1aec00-91d4-4563-b948-59f1fba8d4c5/stop?rev=0
        
        //configure headers
        headers.set("X-Requested-By", "sdc"); //this header is needed in POST, DELETE and PUT methods
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<?> headersEntity = new HttpEntity<>(headers);
        
      //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/stop")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder.fromUri(uri)
                .queryParam("rev", "0")
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.POST, 
                headersEntity, 
                String.class);
    }
    
    public static ResponseEntity<String> pipelineStatus(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl, String pipelineId) {
        // /rest/v1/pipeline/<pipelineId>/status
        // /rest/v1/pipeline/Exampleabfb6db8-8b00-4185-a40f-0c1695d912a2/status?rev=0
        
        HttpEntity<?> headersEntity = new HttpEntity<>(headers);
        
        //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/status")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder.fromUri(uri)
                .queryParam("rev", "0")
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.GET, 
                headersEntity, 
                String.class);
    }
    
    public static ResponseEntity<String> pipelinesStatus(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl) {
        // /rest/v1/pipelines/status        
        
        HttpEntity<?> headersEntity = new HttpEntity<>(headers);
        
        //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipelines/status")
                .toString();
        
        //construct uri with url
        URI uri = UriComponentsBuilder.fromUriString(url)
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.GET, 
                headersEntity, 
                String.class);
    }
    
    public static ResponseEntity<String> pipelineExport(RestTemplate rt, HttpHeaders headers, String dataflowServiceUrl, String pipelineId) {
        // /rest/v1/pipeline/<pipelineId>/export
        // /rest/v1/pipeline/Algo7b511a1f-ad22-4afc-bf71-ca9b58d51040/export?rev=0&attachment=false&includeLibraryDefinitions=true
        
        HttpEntity<?> headersEntity = new HttpEntity<>(headers);
        
        //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/export")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder.fromUri(uri)
                .queryParam("rev", "0")
                .queryParam("attachment", false)
                .queryParam("includeLibraryDefinitions", true)
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.GET, 
                headersEntity, 
                String.class);
    }
    
    public static ResponseEntity<String> pipelineImport(RestTemplate rt, HttpHeaders headers, 
            String dataflowServiceUrl, String pipelineId, boolean override, 
            boolean autoGeneratePipelineId, String config) {
        // /rest/v1/pipeline/<pipelineId>/start
        // /rest/v1/pipeline/OPCUAdatab87df379-a083-4e25-846d-3d4b4859a28d/start?rev=0
        
        //configure headers
        headers.set("X-Requested-By", "sdc"); //this header is needed in POST, DELETE and PUT methods
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<?> headersEntity = new HttpEntity<>(config,headers);
        
      //construct url
        StringBuilder urlBuilder = new StringBuilder();
        String url = urlBuilder.append(dataflowServiceUrl)
                .append(CONTEXT)
                .append(VERSION)
                .append("/pipeline/{pipelineId}/import")
                .toString();
        
        //construct uri with url parameters
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("pipelineId", pipelineId);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .buildAndExpand(uriParams)
                .toUri();
        
        //add query parameters
        uri = UriComponentsBuilder.fromUri(uri)
                .queryParam("rev", "0")
                .queryParam("overwrite", override)
                .queryParam("autoGeneratePipelineId", autoGeneratePipelineId)
                .queryParam("draft", false)
                .build()
                .toUri();
        
        return rt.exchange(
                uri, 
                HttpMethod.POST, 
                headersEntity, 
                String.class);
    }
    
    
}
