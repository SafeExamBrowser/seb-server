FROM openjdk:11-jre-stretch

ENV SERVER_PORT="8080"
ENV JMX_PORT=
ENV JAVA_HEAP_MIN=
ENV JAVA_HEAP_MAX=

#just try wihtout user to check if we habe a permission problem
RUN groupadd --system spring && useradd --system --gid spring spring && mkdir /sebserver && chown spring:spring /sebserver
USER spring:spring

COPY docker/start-sebserver.sh seb-server.jar /sebserver/

WORKDIR /sebserver

# NOTE: This seems not to work, maybe some permission problems, how knows!?
#CMD /bin/sh /sebserver/start-sebserver.sh

CMD if [ "x${JMX_PORT}" = "x" ] ; \
    then exec java \
        -Xms${JAVA_HEAP_MIN} \
        -Xmx${JAVA_HEAP_MAX} \
        -jar "seb-server.jar" \
        --spring.config.location=file:/sebserver/config/spring/,classpath:/config/; \
    else echo "admin ${SEBSERVER_SECRET}" > jmxremote.password && chown spring:spring /sebserver/jmxremote.password && chmod 400 /sebserver/jmxremote.password && exec java \
        -Xms${JAVA_HEAP_MIN} \
        -Xmx${JAVA_HEAP_MAX} \
        -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
        -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
        -Djava.rmi.server.hostname=localhost \
        -Dcom.sun.management.jmxremote.local.only=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.authenticate=true \
        -Dcom.sun.management.jmxremote.password.file=/sebserver/jmxremote.password \
        -Dcom.sun.management.jmxremote.access.file=/sebserver/config/jmx/jmxremote.access \
        -jar "seb-server.jar" \
        --spring.config.location=file:/sebserver/config/spring/,classpath:/config/; \
fi

EXPOSE $SERVER_PORT $JMX_PORT