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
ENV SEBSERVER_JAR="seb-server-${SEBSERVER_VERSION}.jar"
ENV SERVER_PORT="8080"
ENV SECRET=somePW

WORKDIR /sebserver
COPY --from=1 /sebserver/target/"${SEBSERVER_JAR}" /sebserver

CMD exec java \
    -Xms64M \
    -Xmx1G \
    -jar "${SEBSERVER_JAR}" \
    --spring.profiles.active=gui,prod,prod-gui \
    --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
    --sebserver.password="${SECRET}" ; 

EXPOSE 8080