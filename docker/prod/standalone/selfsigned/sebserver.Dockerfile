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

CMD secret=$(cat /sebserver/config/secret) \
        && exec java \
            -Xms64M \
            -Xmx1G \
# Set this for SSL debunging
#            -Djavax.net.debug=ssl \
            -Dcom.sun.management.jmxremote \
            -Dcom.sun.management.jmxremote.port=9090 \
            -Dcom.sun.management.jmxremote.rmi.port=9090 \
            -Djava.rmi.server.hostname=127.0.0.1 \
            -Dcom.sun.management.jmxremote.ssl=false \
            -Dcom.sun.management.jmxremote.authenticate=false \
            -jar seb-server-"${SEBSERVER_VERSION}".jar \
            --spring.profiles.active=prod \
            --spring.config.location=file:/sebserver/config/,classpath:/config/ \
            --sebserver.certs.password="${secret}" \ 
            --sebserver.mariadb.password="${secret}" \
            --sebserver.password="${secret}"
        

EXPOSE 443 8080 9090