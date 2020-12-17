FROM openjdk:11-jre-stretch

ENV SEBSERVER_MODE="webservice"
ENV SERVER_PORT="8080"
ENV SECRET=somePW

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY docker/testing/distributed/webservice/config/ /sebserver/config/
COPY  /sebserver/target/seb-server.jar /sebserver/seb-server.jar

WORKDIR /sebserver

CMD if [ ${SEBSERVER_MODE} == "gui" ]; then exec java \
    -Xms64M \
    -Xmx1G \
    -jar "seb-server.jar" \
    --spring.profiles.active=gui,prod,prod-gui \
    --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
    --sebserver.password="${SECRET}" ; \
    else \
    exec java \
    -Xms64M \
    -Xmx1G \
    -jar "${SEBSERVER_JAR}" \
    --spring.profiles.active=ws,prod,prod-ws \
    --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
    --sebserver.mariadb.password="${SECRET}" \
    --sebserver.password="${SECRET}" ; \
    fi;

EXPOSE 8080