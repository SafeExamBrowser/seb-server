FROM openjdk:11-jre-stretch

ENV SEBSERVER_JAR=demo
ENV SERVER_PORT="8080"
ENV JMX_PORT="9090"
ENV SERVER_PWD=somePW

RUN groupadd --system spring && useradd --system --gid spring spring
# USER spring:spring

COPY docker/demo/config/ /sebserver/config/
COPY seb-server-$SEBSERVER_JAR.jar /sebserver/seb-server-$SEBSERVER_JAR.jar

WORKDIR /sebserver

CMD exec java \
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