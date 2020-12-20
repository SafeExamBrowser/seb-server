FROM openjdk:11-jre-stretch

ENV SEBSERVER_MODE="webservice"
ENV SERVER_PORT="8080"
ENV SECRET=somePW
ENV DB_USER=sebserver
ENV DB_PASSWORD=somePW
ENV DB_HOST=sebserver-mariadb
# ENV DB_DATABASE=SEBServer
ENV DB_PORT=3306

RUN mkdir -p /sebserver/config/spring
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

RUN pwd >/dev/stderr && ls -l >/dev/stderr

# Test if existing files prohibit mounting of Kubernetes ConfigMaps
# COPY docker/testing/distributed/webservice/config/ /sebserver/config/
COPY docker/testing/distributed/start-sebserver.sh seb-server.jar /sebserver/

WORKDIR /sebserver

CMD /bin/sh /sebserver/start-sebserver.sh

EXPOSE 8080
