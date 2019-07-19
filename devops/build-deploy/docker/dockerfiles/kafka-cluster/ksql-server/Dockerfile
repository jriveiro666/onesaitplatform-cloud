FROM confluentinc/cp-ksql-server:latest

# Metadata
LABEL module.maintainer "onesaitplatform@indra.es" \
      module.name="ksql-server"

ENV KSQL_BOOTSTRAP_SERVERS="development.onesaitplatform.com:9095" \
    KSQL_LISTENERS="http://0.0.0.0:8088/" \ 
    KSQL_KSQL_SERVICE_ID="confluent_standalone" \ 
    KSQL_SECURITY_PROTOCOL="SASL_PLAINTEXT" \ 
    KSQL_SASL_MECHANISM="PLAIN" \
    KSQL_SASL_JAAS_CONFIG="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin-secret\";"
      
EXPOSE 20200