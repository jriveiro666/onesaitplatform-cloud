#!/bin/sh

echo "Substituting environment variables in Quasar properties"	
	
grep -rl '${REALTIMEDBSERVERS}' /usr/local/onesait/quasar | xargs sed -i 's/${REALTIMEDBSERVERS}/'"$REALTIMEDBSERVERS"'/g'
grep -rl '${AUTHPARAMS}' /usr/local/onesait/quasar | xargs sed -i 's/${AUTHPARAMS}/'"$AUTHPARAMS"'/g'
grep -rl '${AUTHDB}' /usr/local/onesait/quasar | xargs sed -i 's/${AUTHDB}/'"$AUTHDB"'/g'
grep -rl '${QUERYTIMEOUT}' /usr/local/onesait/quasar | xargs sed -i 's/${QUERYTIMEOUT}/'"$QUERYTIMEOUT"'/g'
	
echo "Executing Quasar..."	
java $JAVA_OPTS -Dlog4j.configuration=file:/usr/local/onesait/quasar/log4j.properties -jar quasar-web-assembly-14.2.6.jar -c config.json