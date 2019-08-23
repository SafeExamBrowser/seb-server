FROM alpine/git

ARG SEBSERVER_VERSION

WORKDIR /demo
RUN if [ "x${SEBSERVER_VERSION}" = "x" ] ; \
    then git clone --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; \
    else git clone -b "v$SEBSERVER_VERSION" --depth 1 https://github.com/SafeExamBrowser/seb-server.git ; fi

FROM maven:3.5-jdk-8-alpine

ARG SEBSERVER_VERSION

WORKDIR /demo
COPY --from=0 /demo/seb-server /demo
RUN mvn clean install -e -P Demo -DskipTests

FROM openjdk:8-jre-alpine

ARG SEBSERVER_VERSION
ENV SEBSERVER_VERSION=${SEBSERVER_VERSION}
ENV SERVER_ADDRESS="0.0.0.0"
ENV SERVER_PORT="8080"
ENV DBSERVER_ADDRESS="seb-server-mariadb"
ENV DBSERVER_PORT="3306"
ENV DBSERVER_PWD="[TO_SET]"
ENV GUICLIENT_PWD="[TO_SET]"
ENV INTERNAL_PWD="[TO_SET]"

WORKDIR /demo
COPY --from=1 /demo/target/seb-server-"$SEBSERVER_VERSION"-SNAPSHOT.jar /demo

ENTRYPOINT ["sh", "-c"]

CMD ["java -jar seb-server-${SEBSERVER_VERSION}-SNAPSHOT.jar --server.address=${SERVER_ADDRESS} --server.port=${SERVER_PORT} --spring.config.location=classpath:/config/ --datastore.mariadb.server.address=${DBSERVER_ADDRESS} --datastore.mariadb.server.port${DBSERVER_PORT} --spring.profiles.active=demo --spring.datasource.password=${DBSERVER_PWD} --sebserver.webservice.api.admin.clientSecret=${GUICLIENT_PWD} --sebserver.webservice.internalSecret=${INTERNAL_PWD}"]

EXPOSE 8080