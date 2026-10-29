FROM eclipse-temurin:21-jre

# Temporary: remove eMudhra G3 trust until the JDK vendor ships a fix
RUN rm /etc/ssl/certs/emSign_ECC_Root_CA_-_C3.pem
RUN rm /etc/ssl/certs/emSign_Root_CA_-_C1.pem
RUN rm /etc/ssl/certs/emSign_Root_CA_-_G1.pem
RUN keytool -delete -noprompt -trustcacerts -cacerts -alias "cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" -storepass changeit
RUN keytool -delete -noprompt -trustcacerts -cacerts -alias "cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" -keystore cacerts -storepass changeit
RUN keytool -delete -noprompt -trustcacerts -cacerts -alias "cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" -keystore cacerts -storepass changeit
RUN keytool -delete -noprompt -trustcacerts -cacerts -alias "cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]" -keystore cacerts -storepass changeit

ENV SERVER_PORT="8080"
ENV JMX_PORT=
ENV JAVA_HEAP_MIN=
ENV JAVA_HEAP_MAX=

#just try wihtout user to check if we habe a permission problem
RUN groupadd --system spring && useradd --system --gid spring spring && mkdir /sebserver && chown spring:spring /sebserver
USER spring:spring

COPY seb-server.jar /sebserver/

WORKDIR /sebserver

CMD if [ "x${JMX_PORT}" = "x" ] ; \
    then exec java \
        -Xms${JAVA_HEAP_MIN} \
        -Xmx${JAVA_HEAP_MAX} \
        -jar "seb-server.jar" \
        -Dcom.sun.net.ssl.checkRevocation=true \
        -Dcom.sun.security.enableCRLDP=true \
        --spring.config.location=classpath:/config/,optional:file:/sebserver/config/spring/; \
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
        --spring.config.location=classpath:/config/,optional:file:/sebserver/config/spring/; \
fi

EXPOSE $SERVER_PORT $JMX_PORT