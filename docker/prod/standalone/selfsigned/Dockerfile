# Clone git repository form specified tag
FROM alpine/git

ARG GIT_TAG

WORKDIR /sebserver
RUN if [ "x$arg" = "x" ] ; \
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
ENV KEYSTORE_PWD=

ENTRYPOINT ["sh", "-c"]

CMD ["java -jar seb-server-${SEBSERVER_VERSION}.jar --spring.config.location=classpath:/config/,file:/sebserver/ --server.address=${SERVER_ADDRESS} --spring.profiles.active=dev"]

# CMD java \
#    -Dfile.encoding=UTF-8 \
#    -Djavax.net.ssl.keyStore=seb-server-keystore.pkcs12 \
#    -Djavax.net.ssl.keyStorePassword="${KEYSTORE_PWD}" \
#    -Djavax.net.ssl.trustStore=seb-server-truststore.pkcs12 \
#    -Djavax.net.ssl.trustStorePassword="${KEYSTORE_PWD}" \
#    -jar seb-server-"${SEBSERVER_VERSION}".jar \
#    --spring.config.location=classpath:/config/,file:/sebserver/ \
#    --server.address="${SERVER_ADDRESS}" \
#    --spring.profiles.active=dev \
#    --datastore.mariadb.server.address="${DBSERVER_ADDRESS}" \
#    --datastore.mariadb.server.port="${DBSERVER_PORT}"

EXPOSE $PORT