FROM alpine/git

ARG GIT_TAG
ARG SEBSERVER_VERSION

WORKDIR /sebserver
RUN if [ "x${GIT_TAG}" = "x" ] ; \
    then git clone --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; \
    else git clone -b "$GIT_TAG" --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; fi

FROM maven:3.5-jdk-8-alpine

ARG SEBSERVER_VERSION

WORKDIR /sebserver
COPY --from=0 /sebserver/seb-server /sebserver
RUN mvn clean install -e -P Demo -DskipTests

FROM openjdk:8-jre-alpine

ARG SEBSERVER_VERSION
ENV SEBSERVER_VERSION=${SEBSERVER_VERSION}
ENV SERVER_PORT="8080"
ENV DBSERVER_PWD=
ENV GUICLIENT_PWD=
ENV INTERNAL_PWD=

WORKDIR /sebserver
COPY --from=1 /sebserver/target/seb-server-"$SEBSERVER_VERSION"-SNAPSHOT.jar /sebserver

ENTRYPOINT exec java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=9090 \
    -Dcom.sun.management.jmxremote.rmi.port=9090 \
    -Djava.rmi.server.hostname=127.0.0.1 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -jar seb-server-"${SEBSERVER_VERSION}"-SNAPSHOT.jar \
    --server.port="${SERVER_PORT}" \
    --spring.profiles.active=demo \
    --spring.config.location=file:/sebserver/config/,classpath:/config/ \
    --spring.datasource.password="${DBSERVER_PWD}" \
    --sebserver.webservice.api.admin.clientSecret="${GUICLIENT_PWD}" \
    --sebserver.webservice.internalSecret="${INTERNAL_PWD}"

EXPOSE $SERVER_PORT 9090