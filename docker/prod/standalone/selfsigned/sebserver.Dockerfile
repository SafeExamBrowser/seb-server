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
ENV SEBSERVER_VERSION=${SEBSERVER_VERSION}
ENV KEYSTORE_PWD=
ENV MYSQL_ROOT_PASSWORD=
ENV SEBSERVER_PWD=
ENV JAVA_NET_DEBUG="ssl:handshake"

WORKDIR /sebserver
COPY --from=1 /sebserver/target/seb-server-"$SEBSERVER_VERSION".jar /sebserver

ENTRYPOINT exec java \
        -Djavax.net.debug="${JAVA_NET_DEBUG}" \
        -jar seb-server-"${SEBSERVER_VERSION}".jar \
        --spring.profiles.active=prod \
        --spring.config.location=file:/sebserver/,classpath:/config/ \
        --sebserver.certs.password="${KEYSTORE_PWD}" \ 
        --sebserver.mariadb.password="${MYSQL_ROOT_PASSWORD}" \
        --sebserver.password="${SEBSERVER_PWD}"

EXPOSE 443 8080