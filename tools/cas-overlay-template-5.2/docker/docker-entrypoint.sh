#!/bin/sh


echo "Executing keytool..."
keytool -noprompt -import -v -trustcacerts -alias onesait_cer -file /etc/cas/cacerts/platform.crt -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -keypass changeit -storepass changeit

echo "Resetting saml config for CAS installation"
rm -r /etc/cas/saml/*

echo "Arrancando el CAS..."	
java $JAVA_OPTS -Dspring.config.location=/etc/cas/config/ -Djava.security.egd=file:/dev/./urandom -Dcas.log.dir=/var/log/platform-logs -Dspring.profiles.active=docker -jar /cas.war

exit 0