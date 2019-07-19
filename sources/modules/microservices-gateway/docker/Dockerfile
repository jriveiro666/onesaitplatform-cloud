FROM openjdk:8-jdk-alpine

# Metadata
LABEL module.name="microservices-gateway"	


COPY *.jar app.jar

# logs folder
RUN mkdir -p /var/log && \
	mkdir ./target


# create user
RUN addgroup -S onesait -g 433 && adduser -u 431 -S -g onesait -h /usr/local -s /sbin/nologin onesait 

RUN  chown -R onesait:onesait /var/log && \
	 chmod -R 777 /var/log && \
	 chown onesait:onesait app.jar && \
	 chown -R onesait:onesait ./target && \    
     chmod -R 777 ./target
	 
	 
VOLUME ["/var/log/platform-logs"]


USER onesait

EXPOSE 26000


ENV JAVA_OPTS="$JAVA_OPTS -Xms1G -Xmx3G" \
    SERVER_NAME=localhost \
    GATEWAY_CLIENT=operations \
    GATEWAY_SECRET=operations

ENTRYPOINT java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker -jar /app.jar

	