FROM openjdk:11-jre-stretch

ARG SEBSERVER_VERSION
ENV SEBSERVER_JAR="seb-server-${SEBSERVER_VERSION}.jar"
ENV SERVER_PORT="8080"
ENV JMX_PORT=
ENV JAVA_HEAP_MIN=
ENV JAVA_HEAP_MAX=

RUN groupadd --system spring && useradd --system --gid spring spring && mkdir /sebserver && chown spring:spring /sebserver
USER spring:spring

COPY docker/start-sebserver.sh seb-server.jar /sebserver/

WORKDIR /sebserver

CMD /bin/sh /sebserver/start-sebserver.sh

EXPOSE $SERVER_PORT $JMX_PORT