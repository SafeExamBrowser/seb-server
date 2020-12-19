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

# Test if existing files prohibit mounting of Kubernetes ConfigMaps
# COPY docker/testing/distributed/webservice/config/ /sebserver/config/
COPY  seb-server.jar docker/testing/distributed/start-sebserver.sh /sebserver/
RUN chmod 755 /sebserver/start-sebserver.sh

WORKDIR /sebserver

CMD /sebserver/start-sebserver.sh

EXPOSE 8080
