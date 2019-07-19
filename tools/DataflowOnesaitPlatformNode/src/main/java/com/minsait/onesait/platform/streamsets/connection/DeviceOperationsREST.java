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
package com.minsait.onesait.platform.streamsets.connection;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.Asserts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minsait.onesait.platform.streamsets.OnesaitResponseException;
import com.minsait.onesait.platform.streamsets.Errors;
import com.minsait.onesait.platform.streamsets.connection.DeviceOperations;
import com.minsait.onesait.platform.streamsets.destination.InstancesStt;
import com.minsait.onesait.platform.streamsets.destination.beans.TimeseriesConfig;
import com.minsait.onesait.platform.streamsets.destination.ontology.OnesaitplatformOntology;
import com.minsait.onesait.platform.streamsets.destination.beans.OntologyProcessInstance;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.SSLContext;

public class DeviceOperationsREST implements DeviceOperations {

    private static final Logger log = LoggerFactory.getLogger(DeviceOperationsREST.class);
	
	private String protocol;
	private String host;
	private String token;
	private int port;
	
    private String path;
    private String clientPlatform;
    private boolean connected = false;
    private boolean avoidSSLCertificate = false;
    private boolean ignoreNulls = false;
    private OnRecordError onRecordEx;
    private String sessionKey;
    private String clientPlatformId;
    
    private boolean useProxy;
    private ProxyConfig proxy;
    
    private String rootNode;
    OntologyProcessInstance ontologyProcessInstance;
    private TimeseriesConfig tsConfig;

    private static final String joinTemplate = "/rest/client/join?token=%s&clientPlatform=%s&clientPlatformId=%s";
    private static final String leaveTemplate = "/rest/client/leave";
    private static final String insertTemplate = "/rest/ontology/%s";
    private static final String queryTemplate = "/rest/ontology/%s?query=%s&queryType=%s";
	private static final String updateTemplate = "/rest/ontology/%s/update?ids=false";
	
	//String Response when 0 updated records and 200
	private static final String responseNoUpdate = "{\"nModified\":0}";

	private static final String MAX_CHAR = "500";
	
	public DeviceOperationsREST(String protocol, String host, Integer port, String token, String device, 
			String deviceId, TimeseriesConfig tsConfig, String rootNode, 
			OntologyProcessInstance ontologyProcessInstance, boolean avoidSSL, 
			OnRecordError onRecordError, boolean ignoreNulls, boolean useProxy, ProxyConfig proxy){
		this.protocol = protocol;
		this.host=host;
		this.port=port;
		this.clientPlatform = device;
		this.clientPlatformId = deviceId;
		this.token=token;
		this.path = getUrl();
		this.tsConfig = tsConfig;
		this.rootNode = rootNode;
		this.ontologyProcessInstance = ontologyProcessInstance;
		this.avoidSSLCertificate = avoidSSL;
		this.onRecordEx = onRecordError;
		this.ignoreNulls = ignoreNulls;
		this.useProxy = useProxy;
		this.proxy = proxy;
		this.doJoin();
	}
	
	private String getUrl () {
		return protocol + "://"+host+":"+port+"/iot-broker";
	}
	
	@Override
	public void leave() throws Exception {
		this.doLeave();	
	}
	
	@Override
	public String query(String ontology, String query, String queryType) throws Exception{
		String result = this.doQuery(generateURLQuery(ontology, query, queryType));
		return result;	
	}
	
	@Override
	public List<ErrorResponseOriginalRecord> insert(InstancesStt message, String ontology){
		List<ErrorResponseOriginalRecord> leror;
		if(tsConfig != null) {
			leror = new ArrayList<ErrorResponseOriginalRecord>();
			for(int i=0;i<message.getInsertableRest().size();i++) {
				leror.addAll(this.doUpdate(ontology, message.getInsertableRest().get(i), message.getOriginalValues().get(i), message));
			}
		}
		else {
			leror = this.doInsert(ontology, message.getInsertableRest().toString(), message.getOriginalValues(), message);
		}
		return leror;
	}
	
	
	public List<ErrorResponseOriginalRecord> update(InstancesStt message, String ontology){
		List<ErrorResponseOriginalRecord> leror = new ArrayList<ErrorResponseOriginalRecord>();
		for(int i = 0; i < message.getUpdateableRest().size(); i++) {
			try {
				String query = generateURLQuery(ontology, message.getUpdateableRest().get(i), "SQL");
				String response = this.doQuery(query);
				if(responseNoUpdate.equals(response)) {
					// Records not updated (events)
					for(Record record : message.getOriginalValues().get(i)) {
						message.getRecordsNotUpdated().add(record);
		    		}
				} else {
					// Records updated (events)
					for(Record record : message.getOriginalValues().get(i)) {
						message.getRecordsOkUpdated().add(record);
		    		}
				}
			} catch (UnsupportedEncodingException e) {
				if(this.onRecordEx != OnRecordError.DISCARD) {
					log.error("UnsupportedEncodingException (update): "+e.getMessage(), e);
					for(Record record : message.getOriginalValues().get(i)) {
		    			leror.add(new ErrorResponseOriginalRecord(record,"Error while generating URL Query (Update): " + e.getMessage(), Errors.ERROR_31));
		    		}
				}
			} catch (Exception e) {
				if(this.onRecordEx != OnRecordError.DISCARD) {
					log.error("Exception doing query (update): "+e.getMessage(), e);
					for(Record record : message.getOriginalValues().get(i)) {
		    			leror.add(new ErrorResponseOriginalRecord(record,"Error while doing query (Update): " + e.getMessage(), Errors.ERROR_31));
		    		}
				}
			}
		}
		return leror;
	}
    
    public boolean isConnected() {
    	return this.connected;
    }
    
    private boolean doJoin() {
        try{
            String clientId = this.clientPlatformId != null && !this.clientPlatformId.trim().isEmpty() ? this.clientPlatform : UUID.randomUUID().toString() ;
            String join = String.format(joinTemplate, this.token , this.clientPlatform, this.clientPlatform + ":" + clientId);
            String resultStr = callRestAPI(join, "GET");
            
            this.sessionKey = (new JsonParser()).parse(resultStr).getAsJsonObject().get("sessionKey").getAsString();
            this.connected = true;
        }
        catch(Exception e){
            log.error("Error en Join: " + e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    public boolean doLeave() {
        try{
            String leave = String.format(leaveTemplate);
            callRestAPI(leave, "GET");
            this.connected = false;
        }
        catch(Exception e){
            log.error("Error en Leave: " + e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    public String generateURLQuery(String ontology, String query, String queryType) throws UnsupportedEncodingException {
        query = String.format(queryTemplate, ontology, URLEncoder.encode(query, "UTF-8"), queryType);
        return query;
    }
    
    public String doQuery(String query) throws Exception {
    	ResponseRest resultResponse;
        try{
            resultResponse = callRestAPI(query, "GET","");
        } catch(ClientProtocolException e){
        	if(this.onRecordEx != OnRecordError.DISCARD) {
        		log.error("Error ClientProtocolException doing query: " + e.getMessage(), e);
        		log.error("Query error (limited to "+MAX_CHAR+" chars): " + String.format("%1."+MAX_CHAR+"s", query) ); 
        	}
            throw new Exception("Error ClientProtocolException in callRestAPI: " +e.getMessage());
        } catch(IOException eio){
        	if(this.onRecordEx != OnRecordError.DISCARD) {
	            log.error("Error IOException doing query: " + eio.getMessage(), eio);
	            log.error("Query error (limited to "+MAX_CHAR+" chars): " + String.format("%1."+MAX_CHAR+"s", query) );
        	}
            throw new Exception("Error IOException in callRestAPI: " +eio.getMessage());
        }
        
        if(resultResponse.getResCode()/100 != 2) {
        	if(this.onRecordEx != OnRecordError.DISCARD) {
	        	log.error("Doing query: error code not 2xx");
	        	log.error("Query error (limited to "+MAX_CHAR+" chars): " + String.format("%1."+MAX_CHAR+"s", query) );
	        	log.error("Response text: " + resultResponse.getResponseText());
        	}
        	throw new Exception("Error code not 2xx, " + resultResponse.getResCode() + ", " + resultResponse.getResponseText());
        } else {
        	log.trace("Query: "+query);
        	log.trace("Query response: "+resultResponse.getResponseText());
        	return resultResponse.getResponseText();
        }
    }
    
    public ResponseRest singleInsert(String ontology, String instances) throws Exception{
    	ResponseRest result;
    	try {
    		result = callRestAPI(generateURLInsert(ontology), "POST", instances);
		} catch(ClientProtocolException e){
			if(this.onRecordEx != OnRecordError.DISCARD) log.error("Error ClientProtocolException in singleInsert: " + e.getMessage());
            throw new Exception("Error ClientProtocolException in singleInsert: " +e.getMessage());
        } catch(IOException eio){
        	if(this.onRecordEx != OnRecordError.DISCARD) log.error("Error IOException in singleInsert: " + eio.getMessage(), eio);
            throw new Exception("Error IOException in singleInsert: " +eio.getMessage());
        }
    	
    	if(result.getResCode()/100 != 2) {
    		if(this.onRecordEx != OnRecordError.DISCARD) {
	    		log.error("Single insert: HTTP Status "+result.getResCode());
	    		log.error("Instances error (limited to "+MAX_CHAR+" chars): " + String.format("%1."+MAX_CHAR+"s", instances));
	    		log.error("Response text: " + result.getResponseText());
    		}
    		if(result.getResCode() == 400) {
    			
    			try {
					JsonObject error = (new JsonParser()).parse(result.getResponseText()).getAsJsonObject().get("error").getAsJsonObject();
					Asserts.notNull(error.get("message"), "Response is not a error object json");
					Asserts.notNull(error.get("errors"), "Response is not a error object json");
					String message = error.get("message").getAsString();
					JsonArray errors = error.get("errors").getAsJsonArray();
					
					if(errors.size() > 0) {
						// TODO Get all errors and put them in the record
						// We get the only the first error for now
						JsonObject errorPersistence = errors.get(0).getAsJsonObject();
						try {
							Asserts.notNull(errorPersistence.get("originalMessage"), "originalMessage can't be null");
							Asserts.notNull(errorPersistence.get("errorCode"), "errorCode can't be null");
							throw new OnesaitResponseException(errorPersistence.get("errorCode").getAsInt(), errorPersistence.get("originalMessage").getAsString());
						} catch (IllegalStateException e) {
							throw new Exception(message);
						}
					} else {
						throw new Exception(message);
					}
				} catch (OnesaitResponseException e) {
					throw e;
				} catch (Exception e) {
					throw new Exception(result.getResponseText(), e);
				}
    			
    		} else throw new Exception(result.getResponseText());
    	}
    	else {
    		log.trace("Query response: "+result.getResponseText());
    		return result;
    	}
    }
    
    public ResponseRest singleUpdate(String ontology, String query, List<Record> originalValues) throws Exception {
    	ResponseRest result;
    	try {
    		result = callRestAPI(generateURLUpdate(ontology), "PUT", query);
	    } catch(ClientProtocolException e){
	    	if(this.onRecordEx != OnRecordError.DISCARD) {
		        log.error("Error ClientProtocolException calling rest API in singleUpdate: " + e.getMessage());
		        log.error("Query error: " + String.format("%1."+MAX_CHAR+"s", query) );
	    	}
	        throw new Exception("Error ClientProtocolException in singleUpdate: " +e.getMessage());
	    } catch(IOException eio){
	    	if(this.onRecordEx != OnRecordError.DISCARD) {
		        log.error("Error IOException calling rest API in singleUpdate: " + eio.getMessage(), eio);
		        log.error("Query error: " + String.format("%1."+MAX_CHAR+"s", query) ); 
	    	}
	        throw new Exception("Error IOException in singleUpdate: " +eio.getMessage());
	    }
    	        
        if(responseNoUpdate.equals(result.getResponseText())) {
        	ResponseRest res;
        	res = insertBaseTSInstance(ontology, originalValues.get(0));
        	// Rama izquierda, lanza excepción
        	
        	// IF INSERT ERROR -> THROW EXCEPTION
        	if(!res.getResponseText().contains("{\"id\":\"")) {
        		throw new Exception(res.getResponseText());
        	}
        	else {
        		try {
        			result = callRestAPI(generateURLUpdate(ontology), "PUT", query);
        		} catch(ClientProtocolException e){
        			if(this.onRecordEx != OnRecordError.DISCARD) {
	        	        log.error("Error ClientProtocolException calling rest API in singleUpdate after retry: " + e.getMessage());
	        	        log.error("Query error: " + String.format("%1."+MAX_CHAR+"s", query) );
        			}
        	        throw new Exception("Error ClientProtocolException in singleUpdate after retry: " +e.getMessage());
        	    } catch(IOException eio){
        	    	if(this.onRecordEx != OnRecordError.DISCARD) {
	        	        log.error("Error IOException calling rest API in singleUpdate after retry: " + eio.getMessage(), eio);
	        	        log.error("Query error: " + String.format("%1."+MAX_CHAR+"s", query) );
        	    	}
        	        throw new Exception("Error IOException in singleUpdate after retry: " +eio.getMessage());
        	    }
        	}
        }
        
    	if(result.getResCode()/100!=2) {
    		if(this.onRecordEx != OnRecordError.DISCARD) {
	    		log.error("Single update: error code "+result.getResCode());
	    		log.error("Query error (limited to "+MAX_CHAR+" chars): " + String.format("%1."+MAX_CHAR+"s", query) );
	    		log.error("Response text: " + result.getResponseText());
    		}
    		throw new Exception(result.getResponseText());
    	} else {
    		log.trace("Query response: "+result.getResponseText());
    		return result;    	
    	}
    }
    
    private ResponseRest insertBaseTSInstance(String ontology, Record originalValue) throws Exception{
		return singleInsert(ontology, OnesaitplatformOntology.instanceToBaseInsert(originalValue, ontology, this.rootNode, tsConfig, ontologyProcessInstance));
    } 
    
    public List<ErrorResponseOriginalRecord> doUpdate(String ontology, String instances, List<Record> originalValues, InstancesStt message) {
    	List<ErrorResponseOriginalRecord> leror = new ArrayList<ErrorResponseOriginalRecord>();
    	ResponseRest rest;
    	try{   
            rest = singleUpdate(ontology, instances, originalValues);
            
            if( responseNoUpdate.equals(rest.getResponseText()) ) {
            	// Add not inserted records to instance
            	message.getRecordsNotInserted().addAll(originalValues);
            } else {
            	// Add inserted records to instance
    			message.getRecordsOkInserted().addAll(originalValues);
            }
        } catch(OnesaitResponseException ex) {
        	for(Record record : originalValues) {
    			if(this.onRecordEx != OnRecordError.DISCARD) leror.add(new ErrorResponseOriginalRecord(record, ex.getDetailedMessage(), Errors.ERROR_30));
				if(ex.getErrorCode() == 300) {
					if(this.ignoreNulls) message.getRecordsOkInserted().add(record); // We assume the data hasn't been modified because the updated value is the same and the document already exist with the same key
					else message.getRecordsDupKeys().add(record);
				}
				else message.getRecordsNotInserted().add(record);
    		}
    	} catch(Exception e){
            doLeave();
            if(doJoin()) {
            	try{    
                    rest = singleUpdate(ontology, instances, originalValues);
                    
                    if( responseNoUpdate.equals(rest.getResponseText()) ) {
                    	// Add not inserted records to instance
                    	message.getRecordsNotInserted().addAll(originalValues);
                    } else {
                    	// Add inserted records to instance
                    	message.getRecordsOkInserted().addAll(originalValues);
                    }
                } catch(OnesaitResponseException ex){
            		for(Record record : originalValues) {
            			if(this.onRecordEx != OnRecordError.DISCARD) leror.add(new ErrorResponseOriginalRecord(record, ex.getDetailedMessage(), Errors.ERROR_30));
        				if(ex.getErrorCode() == 300) message.getRecordsDupKeys().add(record);
    					else message.getRecordsNotInserted().add(record);
            		}
            	} catch(Exception e2){
            		for(Record record : originalValues) {
            			if(this.onRecordEx != OnRecordError.DISCARD) leror.add(new ErrorResponseOriginalRecord(record, e2.getMessage(), Errors.ERROR_30));
        				message.getRecordsNotInserted().add(record);
            		}
            	}
            }else {
            	if(this.onRecordEx != OnRecordError.DISCARD) {
	            	log.error("Error joining again to platform");
	            	for(Record record : originalValues) {
	        			leror.add(new ErrorResponseOriginalRecord(record, "Error join to platform after error in update: " + e.getMessage(), Errors.ERROR_32));
	        		}
            	}
            	message.getRecordsNotInserted().addAll(originalValues); // For events
            }
        }
    	return leror;
    }
    
    public List<ErrorResponseOriginalRecord> doInsert(String ontology, String instances, List<List<Record>> lrds, InstancesStt message){
    	List<ErrorResponseOriginalRecord> leror = new ArrayList<ErrorResponseOriginalRecord>();
    	ResponseRest rest;
    	try{    
            rest = singleInsert(ontology, instances);
            // Test insert
            if (rest.getResponseText().contains("\"id\":") || rest.getResponseText().contains("{\"nInserted\":") ) {
            	for(List<Record> lrd : lrds) {
            		message.getRecordsOkInserted().addAll(lrd);
        		}
            } else {
            	for(List<Record> lrd : lrds) {
            		message.getRecordsNotInserted().addAll(lrd);
        		}
            }
        }
        catch(Exception e){
            doLeave();
            if(doJoin()) {
            	try{    
                    rest = singleInsert(ontology, instances);
                    if (rest.getResponseText().contains("\"id\":") || rest.getResponseText().contains("{\"nInserted\":") ) {
                    	for(List<Record> lrd : lrds) {
                    		message.getRecordsOkInserted().addAll(lrd);
                		}
                    } else {
                    	for(List<Record> lrd : lrds) {
                    		message.getRecordsNotInserted().addAll(lrd); // For events
                		}
                    }
                } catch(OnesaitResponseException ex){
            		for(List<Record> lrd : lrds) {
            			for (Record record : lrd) {
            				if(this.onRecordEx != OnRecordError.DISCARD) leror.add(new ErrorResponseOriginalRecord(record, ex.getDetailedMessage(), Errors.ERROR_30));
            				if(ex.getErrorCode() == 300) message.getRecordsDupKeys().add(record);
        					else message.getRecordsNotInserted().add(record);
						}
            		}
            	} catch(Exception e2){
            		for(List<Record> lrd : lrds) {
            			for (Record record : lrd) {
            				if(this.onRecordEx != OnRecordError.DISCARD) leror.add(new ErrorResponseOriginalRecord(record, e2.getMessage(), Errors.ERROR_30));
            				message.getRecordsNotInserted().add(record);
						}
            		}
            	}
            }
            else {
            	for(List<Record> lrd : lrds) {
            		if(this.onRecordEx != OnRecordError.DISCARD) {
	            		for (Record record : lrd) {
	            			leror.add(new ErrorResponseOriginalRecord(record, "Error join to platform after error in insert: " + e.getMessage(), Errors.ERROR_32));
						}
            		}
            		message.getRecordsNotInserted().addAll(lrd);
            	}
            }
        }
    	return leror;
    }
    
    private String generateURLInsert(String ontology){
        return String.format(insertTemplate, ontology);
    }
    
    private String generateURLUpdate(String ontology){
        return String.format(updateTemplate, ontology);
    }
    
    private String callRestAPI(String targetURL, String method) throws Exception {
        return callRestAPI(targetURL, method, "").getResponseText();
    }
    
    private void addAuthorizationHeader(HttpRequest http) {
    	http.addHeader("Authorization", this.sessionKey);
    }
    
    private ResponseRest callRestAPI(String targetURL, String method, String jsonData) throws ClientProtocolException, IOException, Exception  {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        
        if(this.avoidSSLCertificate) avoidSSLCertificate(clientBuilder);
        
        RequestConfig requestConfig;
		if(this.useProxy) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
	        credsProvider.setCredentials(
	            new AuthScope(this.proxy.getHost(), this.proxy.getPort()),
	            new UsernamePasswordCredentials(this.proxy.getUser(), this.proxy.getPassword())
	        );
	        
	        HttpHost prox = new HttpHost(this.proxy.getHost(), this.proxy.getPort());
	        
	        requestConfig = RequestConfig.custom()
	        		.setProxy(prox)
	        		.build();
	        
	        clientBuilder
	        	.setDefaultRequestConfig(requestConfig)
	        	.setDefaultCredentialsProvider(credsProvider);
		} else {
        	requestConfig = RequestConfig.custom().build();
        	clientBuilder.setDefaultRequestConfig(requestConfig);
        }
		
		CloseableHttpClient	httpClient = clientBuilder.build();
        
        String result = null;
        HttpResponse httpResponse = null;
        try {
            HttpRequest http = null;
            HttpHost httpHost = new HttpHost(this.host, this.port, this.protocol);
            StringEntity entity = new StringEntity(jsonData, ContentType.APPLICATION_JSON);
            
            if("".equals(targetURL)){
                targetURL = this.path;
            }
            else{
                targetURL = this.path + targetURL;
            }
            switch (method) {
	            case "GET":
	                http = new HttpGet(targetURL);
	                break;
	            case "POST":
	                http = new HttpPost(targetURL);
	                ((HttpPost) http).setEntity(entity);
	                break;
	            case "PUT":
	                http = new HttpPut(targetURL);
	                ((HttpPut) http).setEntity(entity);
	                break;
	            case "DELETE":
	                http = new HttpDelete(targetURL);
	                break;
            }
            addAuthorizationHeader(http);
            // Execute HTTP request
            httpResponse = httpClient.execute(httpHost, http);
            
            // Get hold of the response entity
            HttpEntity entityResponse = httpResponse.getEntity();
            result = EntityUtils.toString(entityResponse, "UTF-8");
            
        } catch (ClientProtocolException e) {
        	httpClient.close();
        	throw new ClientProtocolException(e);
        } catch (IOException e) {
        	httpClient.close();
        	throw new IOException(e);
        } finally {
			httpClient.close();            
        }
        return new ResponseRest(httpResponse.getStatusLine().getStatusCode(), result);
    }
    
    private void avoidSSLCertificate(HttpClientBuilder client)  {
    	try {
    		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
    	    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
    	    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    	    
    	    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
    	    		.register("https", sslsf)
    	    		.register("http", new PlainConnectionSocketFactory())
    	    		.build();
    		
			BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
			client.setSSLSocketFactory(sslsf).setSSLHostnameVerifier(new NoopHostnameVerifier()).setConnectionManager(connectionManager);
    	} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
    		if(this.onRecordEx != OnRecordError.DISCARD) log.error("ERROR avoiding SSL ("+e.getClass().getSimpleName()+"): "+e.getMessage());
		} 
    }
    
}