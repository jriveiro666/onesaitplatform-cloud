FROM maven:3.5.3-jdk-8-alpine

# metadata
LABEL module.maintainer="onesaitplatform@indra.es" \
	  module.name="cas-service"	


# Timezone 
RUN apk add --no-cache tzdata

# web projects folder & logs folder
RUN mkdir -p /var/log/platform-logs && \
	mkdir -p /etc/cas && \
	mkdir -p /etc/cas/cacerts

ADD cas.war cas.war
COPY etc/cas/ /etc/cas/

# create sofia user/group
#RUN addgroup -S onesait -g 433 && adduser -u 431 -S -g onesait -h /usr/local -s /sbin/nologin onesait 


RUN chmod -R 777 /var/log && \
    chmod -R 777 /etc/cas && \
    chmod -R 777 cas.war
    


RUN apk update && apk upgrade && \
	apk add \
    openssl \
    ca-certificates \
    libc6-compat \
   	libstdc++ \
    nss
    
    
VOLUME ["/var/log/platform-logs", "/etc/cas/cacerts"]


#USER onesait

EXPOSE 27000


ENV JAVA_OPTS="$JAVA_OPTS -Xms1G -Xmx3G" \
    SERVER_NAME=localhost \    
	AUTHPARAMS=platformadmin:0pen-platf0rm-2018!@ \
    REALTIMEDBSERVERS=realtimedb:27017 \
    AUTHDB=admin \
    QUERYTIMEOUT=5000 \ 
	REALTIMEDBPORT=27017 \
	REALTIMEDBHOST=realtimedb \
    CONFIGDBSERVERS=configdb:3306 \
 	CONFIGDB_USER=root \
 	CONFIGDB_PASSWORD=changeIt! \
 	LDAP_SERVER= \
    REALTIMEDBNAME=onesaitplatform_rtdb \
    PATH="/usr/local/bin:${PATH}" \
    API_KEY=46c734014c544601b51690ecc49cd29c

COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]
 
