: Change docker container ID in both places with your dataflow docker contaniner id
call mvn clean package -DskipTests
call docker exec -u root -it streamsets-dc rm -rf /opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets*
call docker cp .\target\onesaitplatform-streamsets-1.3.2-rc1.tar.gz streamsets-dc:/opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets-1.3.2-rc1.tar.gz
call docker exec -u root -it streamsets-dc tar xvfz /opt/streamsets-datacollector-user-libs/onesaitplatform-streamsets-1.3.2-rc1.tar.gz -C /opt/streamsets-datacollector-user-libs
call docker restart streamsets-dc     
