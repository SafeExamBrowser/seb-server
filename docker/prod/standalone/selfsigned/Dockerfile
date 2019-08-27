# Clone git repository form specified tag
FROM alpine/git

ARG GIT_TAG

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

WORKDIR /sebserver
COPY --from=1 /sebserver/target/seb-server-"$SEBSERVER_VERSION".jar /sebserver

ARG SEBSERVER_VERSION
ENV SEBSERVER_VERSION=${SEBSERVER_VERSION}
ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_PORT=80
ENV DBSERVER_ADDRESS=localhost
ENV DBSERVER_PORT=3306
ENV DBSERVER_PWD=
ENV GUICLIENT_PWD=
ENV INTERNAL_PWD=
ENV KEYSTORE_PWD=

ENTRYPOINT exec java \
    -Dfile.encoding=UTF-8 \
    -Djavax.net.ssl.keyStore=seb-server-keystore.pkcs12 \
    -Djavax.net.ssl.keyStorePassword="${KEYSTORE_PWD}" \
    -Djavax.net.ssl.trustStore=seb-server-truststore.pkcs12 \
    -Djavax.net.ssl.trustStorePassword="${KEYSTORE_PWD}" \
    -jar seb-server-"${SEBSERVER_VERSION}".jar \
    --server.port="${SERVER_PORT}" \
    --spring.config.location=classpath:/config/ \
    --spring.profiles.active=prod \
    --datastore.mariadb.server.address="${DBSERVER_ADDRESS}" \
    --datastore.mariadb.server.port="${DBSERVER_PORT}" \
    --spring.datasource.password="${DBSERVER_PWD}" \
    --sebserver.webservice.api.admin.clientSecret="${GUICLIENT_PWD}" \
    --sebserver.webservice.internalSecret="${INTERNAL_PWD}"

EXPOSE $PORT