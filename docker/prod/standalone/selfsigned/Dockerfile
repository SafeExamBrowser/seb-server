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

WORKDIR /sebserver
COPY --from=1 /sebserver/target/seb-server-"$SEBSERVER_VERSION".jar /sebserver

ENTRYPOINT exec java -jar seb-server-"${SEBSERVER_VERSION}".jar --spring.config.location=classpath:/config/,file:/config

EXPOSE 80