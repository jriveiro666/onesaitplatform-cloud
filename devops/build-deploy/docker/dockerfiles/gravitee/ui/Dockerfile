FROM node:alpine

# metadata
LABEL module.maintainer="onesaitplatform@indra.es" \
	  module.name="gravitee-man-ui"	


# web projects folder & logs folder
RUN mkdir -p /gravitee

ENV GRAVITEE_VERSION=1.24.0 \
    GRAVITEE_MAN_API=https://development.onesaitplatform.com/gravitee/management \
    SERVER_NAME=localhost \
    PORT=25002 \
    APP_NAME=gravitee-man-ui


RUN apk update && apk upgrade && \
    apk add --no-cache \
        wget \
        openssl \
        ca-certificates \
    && wget https://download.gravitee.io/graviteeio-apim/distributions/graviteeio-full-${GRAVITEE_VERSION}.zip \
    && unzip -d /gravitee graviteeio-full-${GRAVITEE_VERSION}.zip \
    && rm graviteeio-full-${GRAVITEE_VERSION}.zip


COPY index.html /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-ui-${GRAVITEE_VERSION}/ 
COPY constants.json /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-ui-${GRAVITEE_VERSION}/ 
COPY docker-entrypoint.sh /gravitee

RUN npm install -g httpserver && \
    mkdir /gravitee/ui && \
    chmod +x /gravitee/docker-entrypoint.sh && \
    cp -R /gravitee/graviteeio-full-${GRAVITEE_VERSION}/graviteeio-management-ui-${GRAVITEE_VERSION}/* /gravitee/ui/ &&\
    rm -R /gravitee/graviteeio*


EXPOSE ${PORT}

WORKDIR /gravitee


ENTRYPOINT ["/gravitee/docker-entrypoint.sh"]


