FROM alpine/git

ARG GIT_TAG

WORKDIR /demo
RUN git clone -b "$GIT_TAG" --depth 1 https://github.com/SafeExamBrowser/seb-server.git

FROM maven:3.5-jdk-8-alpine

ARG JAR_VERSION

WORKDIR /demo
COPY --from=0 /demo/seb-server /demo
RUN mvn clean install -e -P Demo -DskipTests

FROM openjdk:8-jre-alpine

ARG JAR_VERSION
ENV JAR_VERSION ${JAR_VERSION}

WORKDIR /demo
COPY --from=1 /demo/target/seb-server-"$JAR_VERSION".jar /demo

ENTRYPOINT ["sh", "-c"]

CMD ["java -jar seb-server-${JAR_VERSION}.jar --spring.config.location=classpath:/config/,file:/demo/externalResources/ --server.address=0.0.0.0 --spring.profiles.active=demo"]

EXPOSE 8080