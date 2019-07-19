#!/bin/sh

# first check if custom nginx config exists
file="/usr/local/nginxconfig/nginx.conf"

if [ -f "$file" ]
then
	echo "$file custom file found."
	cp -f /usr/local/nginxconfig/nginx.conf  /etc/nginx/
else
	echo "$file custom file not found, applying default config"
	echo "Copying nginx config"
	envsubst '\$SERVER_NAME' < /etc/nginx/nginx_template.conf > /etc/nginx/nginx.conf	
fi

echo "Using nginx config:"
cat /etc/nginx/nginx.conf

echo "Starting nginx"
nginx -c /etc/nginx/nginx.conf -g "daemon off;"