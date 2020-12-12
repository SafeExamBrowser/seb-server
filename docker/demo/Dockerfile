FROM alpine/git

ARG SEBSERVER_VERSION
ARG GIT_TAG="v${SEBSERVER_VERSION}"

WORKDIR /sebserver
RUN if [ "x${GIT_TAG}" = "x" ] ; \
    then git clone --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; \
    else git clone -b "$GIT_TAG" --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; fi

FROM maven:latest

ARG SEBSERVER_VERSION

WORKDIR /sebserver
COPY --from=0 /sebserver/seb-server /sebserver
RUN mvn clean install -DskipTests -Dbuild-version="${SEBSERVER_VERSION}"

FROM openjdk:11-jre-stretch

ARG SEBSERVER_VERSION
ENV SEBSERVER_JAR=${SEBSERVER_VERSION}
ENV SERVER_PORT="8080"
ENV JMX_PORT="9090"
ENV SERVER_PWD=


WORKDIR /sebserver
COPY --from=1 /sebserver/target/seb-server-"${SEBSERVER_JAR}".jar /sebserver

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