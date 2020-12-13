FROM openjdk:11-jre-stretch

ENV SEBSERVER_JAR=auto
ENV SERVER_PORT="8080"
ENV JMX_PORT="9090"
ENV SERVER_PWD=


WORKDIR /sebserver
COPY /home/runner/work/seb-server/seb-server/seb-server-auto.jar /sebserver

ENTRYPOINT exec java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9090 \
    -Dcom.sun.management.jmxremote.rmi.port=9090 \
    -Djava.rmi.server.hostname=localhost \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -jar seb-server-"${SEBSERVER_JAR}".jar \
    --server.port="${SERVER_PORT}" \
    --spring.profiles.active=ws,gui,demo \
    --spring.config.location=file:/sebserver/config/,classpath:/config/ \
    --sebserver.mariadb.password="${SERVER_PWD}" \
    --sebserver.password="${SERVER_PWD}"

EXPOSE $SERVER_PORT $JMX_PORT