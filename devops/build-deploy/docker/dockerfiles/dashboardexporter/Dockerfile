FROM node:12.4.0-slim

# Metadata
LABEL module.maintainer "onesaitplatform@indra.es" \
      module.name="dashboardexporter"

RUN  apt-get update \
     # Install latest chrome dev package, which installs the necessary libs to
     # make the bundled version of Chromium that Puppeteer installs work.
     && apt-get install -y wget --no-install-recommends \
     && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
     && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
     && apt-get update \
     && apt-get install -y google-chrome-unstable --no-install-recommends \
     && rm -rf /var/lib/apt/lists/* \
     && wget --quiet https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh -O /usr/sbin/wait-for-it.sh \
     && chmod +x /usr/sbin/wait-for-it.sh

# create onesait user/group
RUN addgroup --system onesait -gecos 433
RUN adduser --uid 431 --system --gecos onesait --home /usr/local --shell /sbin/nologin onesait

WORKDIR /opt/exporter

RUN mkdir -p /var/log/dashboardexporter
RUN chown onesait:onesait /opt/exporter /var/log/dashboardexporter /usr/local

USER onesait

COPY server.mjs .
COPY package*.json ./

COPY lib/ ./lib/
RUN ls -la ./lib/*
COPY conf/ ./conf/
RUN ls -la ./conf/*

RUN npm ci --only=production

EXPOSE 20300

CMD [ "npm", "start" ]