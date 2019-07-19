#!/bin/sh

echo "Substituting environment variables in Gravitee"	

grep -rl '${GRAVITEE_MAN_API}' /gravitee/ui | xargs sed -i 's~${GRAVITEE_MAN_API}~'"$GRAVITEE_MAN_API"'~'


cd /gravitee/ui/
echo "Executing httpserver..." 
httpserver -p ${PORT}

