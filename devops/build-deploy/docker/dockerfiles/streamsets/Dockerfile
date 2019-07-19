FROM streamsets/datacollector:3.3.0

# Metadata
LABEL module.maintainer "onesaitplatform@indra.es" \
      module.name="streamsets"

USER root

# create onesait user/group
RUN addgroup -S onesait -g 433 && adduser -u 431 -S -g onesait -h /usr/local -s /sbin/nologin onesait 

EXPOSE 18630

ENV SPARK_DIR=/opt/streamsets-datacollector-3.3.0/streamsets-libs/streamsets-datacollector-cdh_spark_2_1_r1-lib

ADD sdc /etc/sdc
ADD onesaitplatform-streamsets /opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets
ADD spark-libs /tmp/spark-libs

RUN chown -R onesait:onesait /etc/sdc && \
    chmod -R 777 /etc/sdc && \
	chown -R onesait:onesait /logs && \
    chmod -R 777 /logs && \    
	chown -R onesait:onesait /data && \
    chmod -R 777 /data && \        
	chown -R onesait:onesait /opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets && \
    chmod -R 777 /opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets && \
	chown -R onesait:onesait /opt/streamsets-datacollector-3.3.0/streamsets-libs && \
    chmod -R 777 /opt/streamsets-datacollector-3.3.0/streamsets-libs

# Install Python 3    
RUN apk add --no-cache python3 && \
    python3 -m ensurepip && \
    rm -r /usr/lib/python*/ensurepip && \
    pip3 install --upgrade pip setuptools && \
    if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi && \
    if [[ ! -e /usr/bin/python ]]; then ln -sf /usr/bin/python3 /usr/bin/python; fi && \
    rm -r /root/.cache    
    
# Install gcc compiler
RUN apk add --no-cache build-base gfortran python python-dev py-pip build-base wget freetype-dev libpng-dev openblas-dev net-snmp net-snmp-tools  

# Install Spark 2 connector
RUN wget -O /tmp/spark-connector.tgz http://archives.streamsets.com/datacollector/3.3.0/tarball/streamsets-datacollector-cdh_spark_2_1_r1-lib-3.3.0.tgz && \
    tar -zxvf /tmp/spark-connector.tgz -C /tmp && \
    mv /tmp/streamsets-datacollector-3.3.0/streamsets-libs/streamsets-datacollector-cdh_spark_2_1_r1-lib /opt/streamsets-datacollector-3.3.0/streamsets-libs && \
    cp /tmp/spark-libs/*.jar /opt/streamsets-datacollector-3.3.0/streamsets-libs/streamsets-datacollector-cdh_spark_2_1_r1-lib/lib/ && \
    rm /tmp/spark-connector.tgz && \
    rm -rf /tmp/spark-libs /tmp/streamsets-datacollector-3.3.0
    
# Delete obsolete dependencies   
RUN rm $SPARK_DIR/lib/netty-buffer-4.1.16.Final.jar && \
	rm $SPARK_DIR/lib/netty-codec-4.1.16.Final.jar && \
	rm $SPARK_DIR/lib/netty-common-4.1.16.Final.jar && \
	rm $SPARK_DIR/lib/netty-transport-4.1.16.Final.jar && \
	rm $SPARK_DIR/lib/netty-transport-native-epoll-4.1.16.Final-linux-x86_64.jar && \
	rm $SPARK_DIR/lib/netty-transport-native-unix-common-4.1.16.Final.jar && \
	rm $SPARK_DIR/lib/jackson-core-2.6.5.jar && \
	rm $SPARK_DIR/lib/jackson-databind-2.6.5.jar && \
	rm $SPARK_DIR/lib/jackson-jaxrs-base-2.6.5.jar && \
	rm $SPARK_DIR/lib/jackson-jaxrs-json-provider-2.6.5.jar && \
	rm $SPARK_DIR/lib/jackson-module-jaxb-annotations-2.6.5.jar    
               
RUN cp -r /opt/streamsets-datacollector-3.3.0/streamsets-libs /tmp   

ENV OP_STREAMSETS_XMX=1024m \
    OP_STREAMSETS_XMS=1024m \
    OP_STREAMSETS_POOL_SIZE=50 \
    OP_STREAMSETS_SAMPLING_SAMPLE_SIZE=1 \
    OP_STREAMSETS_SAMPLING_POPULATION_SIZE=10000 \
    OP_STREAMSETS_UI_REFRESH_INTERVAL_MS=2000 \
    OP_STREAMSETS_UI_JVMMETRICS_REFRESH_INTERVAL_MS=4000

#USER onesait

VOLUME ["sdc-stagelibs", "sdc-data"]

COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["dc", "-exec"]
