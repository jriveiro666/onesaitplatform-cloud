FROM nginx:alpine

# Metadata
LABEL module.maintainer "onesaitplatform@indra.es" \
	  module.name="nginx"

# Default hostname
ENV SERVER_NAME=localhost \
	ZEPPELIN_HOST=localhost \
	RESOLVER=localhost
	
VOLUME /usr/local/webprojects

# custom nginx config location
VOLUME /usr/local/nginxconfig
	
# Create www-data user before start nginx container
RUN addgroup -g 1000 -S www-data \
 && adduser -u 1000 -D -S -G www-data www-data && \
 mkdir -p /etc/nginx/conf.d/ && \
 mkdir -p /var/www/html && \
 mkdir -p /var/www/error && \ 
 mkdir -p /etc/nginx/ssl && \
 mkdir -p /usr/local/webprojects && \
 mkdir -p /usr/local/nginxconfig && \
 chown -R www-data:www-data /etc/nginx && \
 chown -R www-data:www-data /var/cache/nginx && \
 chmod 777 -R /etc/nginx && \
 chmod 777 -R /var/cache/nginx

# Copy nginx config file
COPY nginx.conf /etc/nginx/nginx_template.conf

# Copy default config
COPY default.conf /etc/nginx/conf.d/default.conf

# Copy default error page
COPY manteinance.html /var/www/error/manteinance.html

# Copy certs
COPY platform.cer /etc/nginx/ssl/
COPY platform.key /etc/nginx/ssl/

COPY docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]				