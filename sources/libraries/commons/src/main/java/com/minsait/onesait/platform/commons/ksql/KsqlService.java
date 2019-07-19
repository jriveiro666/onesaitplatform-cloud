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
package com.minsait.onesait.platform.commons.ksql;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.confluent.ksql.rest.client.KsqlRestClient;
import io.confluent.ksql.rest.client.RestResponse;
import io.confluent.ksql.rest.entity.CommandStatus;
import io.confluent.ksql.rest.entity.CommandStatusEntity;
import io.confluent.ksql.rest.entity.KsqlEntity;
import io.confluent.ksql.rest.entity.KsqlEntityList;
import io.confluent.ksql.rest.entity.KsqlErrorMessage;
import io.confluent.ksql.rest.entity.Queries;
import io.confluent.ksql.rest.entity.RunningQuery;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(prefix = "onesaitplatform.kafka.ksql", name = "enable", havingValue = "true")
@Slf4j
@Service
public class KsqlService {
	@Value("${onesaitplatform.ksql.server.url:http://localhost:8088}")
	private String ksqlServerUrl;
	private KsqlRestClient ksqlClient;

	@PostConstruct
	public void init() {
		log.info("Starting KSQL Rest Client...");
		ksqlClient = new KsqlRestClient(ksqlServerUrl);
		log.info("KSQL Rest Client started !");
	}

	public void executeKsqlRequest(String request) throws KsqlExecutionException {
		RestResponse<KsqlEntityList> response = ksqlClient.makeKsqlRequest(request);
		if (response.get() instanceof KsqlErrorMessage) {
			KsqlErrorMessage error = (KsqlErrorMessage) response.get();
			log.error("Ksql error code = {}. Cause = {}.", error.getErrorCode(), error.getMessage());
			throw new KsqlExecutionException(error.getMessage());
		}
		KsqlEntityList list = response.getResponse();
		if (list.get(0) instanceof CommandStatusEntity) {
			CommandStatusEntity status = (CommandStatusEntity) list.get(0);
			if (status.getCommandStatus().getStatus() == CommandStatus.Status.ERROR) {

				log.error("Ksql error code = {}. ", status.getCommandStatus().getMessage());
				throw new KsqlExecutionException(status.getCommandStatus().getMessage());
			}
		}
	}

	public void deleteQueriesFromSink(String sinkIdentification) throws KsqlExecutionException {
		RestResponse<KsqlEntityList> response = ksqlClient.makeKsqlRequest("show queries;");
		if (response.get() instanceof KsqlErrorMessage) {
			KsqlErrorMessage error = (KsqlErrorMessage) response.get();
			log.error("Unable to retrieve queries from KSQL Server.");
			log.error("Ksql error code = {}. Cause = {}.", error.getErrorCode(), error.getMessage());
			throw new KsqlExecutionException(error.getMessage());
		}
		try {
			KsqlEntityList queriesResponse = response.getResponse();
			for (KsqlEntity queries : queriesResponse) {
				for (RunningQuery query : ((Queries) queries).getQueries()) {
					for (String sink : query.getSinks()) {
						if (sink.equalsIgnoreCase(sinkIdentification)) {
							// Delete Query
							response = ksqlClient.makeKsqlRequest("terminate " + query.getId().getId() + ";");
							if (response.get() instanceof KsqlErrorMessage) {
								KsqlErrorMessage error = (KsqlErrorMessage) response.get();
								log.error("Unable to DELETE query= {} from KSQL Server.", query.getId().getId());
								log.error("Ksql error code = {}. Cause = {}.", error.getErrorCode(),
										error.getMessage());
								throw new KsqlExecutionException(error.getMessage());
							}
							log.info("Ksql query = {} deleted.", query.getId());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Error while deleting queries for sink = {}.", sinkIdentification);
			throw new KsqlExecutionException(e.getMessage());
		}
	}

	public void deleteQueriesByStatement(String statement) throws KsqlExecutionException {

		RestResponse<KsqlEntityList> response = ksqlClient.makeKsqlRequest("show queries;");
		if (response.get() instanceof KsqlErrorMessage) {
			KsqlErrorMessage error = (KsqlErrorMessage) response.get();
			log.error("Unable to retrieve queries from KSQL Server.");
			log.error("Ksql error code = {}. Cause = {}.", error.getErrorCode(), error.getMessage());
			throw new KsqlExecutionException(error.getMessage());
		}
		try {
			KsqlEntityList queriesResponse = response.getResponse();
			for (KsqlEntity queries : queriesResponse) {
				for (RunningQuery query : ((Queries) queries).getQueries()) {
					if (statement.equalsIgnoreCase(query.getQueryString())) {
						// Delete Query
						response = ksqlClient.makeKsqlRequest("terminate " + query.getId().getId() + ";");
						if (response.get() instanceof KsqlErrorMessage) {
							KsqlErrorMessage error = (KsqlErrorMessage) response.get();
							log.error("Unable to DELETE query= {} from KSQL Server.", query.getId().getId());
							log.error("Ksql error code = {}. Cause = {}.", error.getErrorCode(), error.getMessage());
							throw new KsqlExecutionException(error.getMessage());
						}
						log.info("Ksql query = {} deleted.", query.getId());
					}
				}
			}
		} catch (Exception e) {
			log.error("Error while deleting queries from statement = {}.", statement);
			throw new KsqlExecutionException(e.getMessage());
		}
	}
}
