FROM maven:3.5.4-jdk-8-alpine

# metadata
LABEL module.maintainer="onesaitplatform@indra.es" \
      module.name="gravitee-management" 


RUN mkdir -p /gravitee && \
    mkdir -p /gravitee/cers && \
    chmod -R 777 /gravitee

ENV GRAVITEE_VERSION 1.24.0

    
RUN apk update && apk upgrade && \
    apk add --no-cache \
        wget \
        openssl \
        ca-certificates \
        nss \
    && wget https://download.gravitee.io/graviteeio-apim/distributions/graviteeio-full-${GRAVITEE_VERSION}.zip \
    && unzip -d /gravitee graviteeio-full-${GRAVITEE_VERSION}.zip \
    && rm graviteeio-full-${GRAVITEE_VERSION}.zip


COPY gravitee.yml /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-api-${GRAVITEE_VERSION}/config/ 
COPY logback.xml /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-api-${GRAVITEE_VERSION}/config/ 


RUN mkdir -p /var/log/platform-logs && \
    chmod -R 777 /gravitee && \
    chmod -R 777 /var/log/platform-logs &&\
    mkdir /gravitee/management &&\
    mv /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-api-${GRAVITEE_VERSION}/* /gravitee/management/ &&\
    rm -R /gravitee/graviteeio*

ENV JAVA_OPTS="$JAVA_OPTS -Xms1G -Xmx3G" \
    SERVER_NAME=localhost \   
    AUTHPARAMS=platformadmin:0pen-platf0rm-2018!@ \
    REALTIMEDBSERVERS=realtimedb:27017 \
    AUTHDB=admin \
    QUERYTIMEOUT=5000 \   
    REALTIMEDBWRITECONCERN=UNACKNOWLEDGED \ 
    ELASTICDBHOST=elasticdb \
    ELASTICDBPORT=9200 \ 
    OAUTH_CLIENTID=onesaitplatform\
    OAUTH_CLIENTSECRET=onesaitplatform\
    PORT=25000 \
    OAUTH_SERVER=oauthservice:21000 \
    APP_NAME=gravitee-man-api\
    LOG_PATH=/var/log/platform-logs

    
EXPOSE ${PORT}


ENTRYPOINT ./gravitee/management/bin/gravitee