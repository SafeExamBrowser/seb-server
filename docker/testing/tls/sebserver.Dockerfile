# Clone git repository form specified tag
FROM alpine/git

ARG SEBSERVER_VERSION
ARG GIT_TAG="v${SEBSERVER_VERSION}"

WORKDIR /sebserver
RUN if [ "x${GIT_TAG}" = "x" ] ; \
    then git clone --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; \
    else git clone -b "$GIT_TAG" --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; fi

# Build with maven (skip tests)
FROM maven:latest

ARG SEBSERVER_VERSION

WORKDIR /sebserver
COPY --from=0 /sebserver/seb-server /sebserver
RUN mvn clean install -DskipTests

FROM openjdk:11-jre-stretch

ARG SEBSERVER_VERSION
ENV SEBSERVER_JAR="seb-server-${SEBSERVER_VERSION}.jar"
ENV SERVER_PORT="8080"
ENV JMX_PORT=

WORKDIR /sebserver
COPY --from=1 /sebserver/target/"$SEBSERVER_JAR" /sebserver

CMD if [ "x${JMX_PORT}" = "x" ] ; \
        then secret=$(cat /sebserver/config/secret) && exec java \
            -Xms64M \
            -Xmx1G \
            -jar "${SEBSERVER_JAR}" \
            --spring.profiles.active=prod,prod-gui,prod-ws \
            --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
            --sebserver.certs.password="${secret}" \ 
            --sebserver.mariadb.password="${secret}" \
            --sebserver.password="${secret}" ; \
        else secret=$(cat /sebserver/config/secret) && exec java \
            -Xms64M \
            -Xmx1G \
            -Dcom.sun.management.jmxremote \
            -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
            -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
            -Djava.rmi.server.hostname=localhost \
            -Dcom.sun.management.jmxremote.local.only=false \
            -Dcom.sun.management.jmxremote.ssl=false \
            -Dcom.sun.management.jmxremote.authenticate=true \
            -Dcom.sun.management.jmxremote.password.file=/sebserver/config/jmx/jmxremote.password \
            -Dcom.sun.management.jmxremote.access.file=/sebserver/config/jmx/jmxremote.access \
            -jar "${SEBSERVER_JAR}" \
            --spring.profiles.active=prod,prod-gui,prod-ws \
            --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
            --sebserver.certs.password="${secret}" \ 
            --sebserver.mariadb.password="${secret}" \
            --sebserver.password="${secret}" ; \
        fi

EXPOSE $SERVER_PORT $JMX_PORT