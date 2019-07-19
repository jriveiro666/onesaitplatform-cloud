: Change docker container ID in both places with your dataflow docker contaniner id

set CONTAINER_ID=%1
call mvn clean package -DskipTests
call docker cp .\target\zeppelin-onesait-platform-1.0.1.jar %CONTAINER_ID%:/zeppelin/interpreter/onesaitplatform/zeppelin-onesait-platform-1.0.1.jar
call docker restart %CONTAINER_ID%
