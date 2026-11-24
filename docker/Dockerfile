FROM eclipse-temurin:25-jre

ENV SERVER_PORT="8080"

#just try wihtout user to check if we habe a permission problem
RUN groupadd --system spring && useradd --system --gid spring spring && mkdir /sebserver && chown spring:spring /sebserver
USER spring:spring

COPY seb-server.jar /sebserver/

WORKDIR /sebserver

CMD exec java \
        -Xms${JAVA_HEAP_MIN} \
        -Xmx${JAVA_HEAP_MAX} \
        -XX:-UseBASE64Intrinsics \
        -jar "seb-server.jar" \
        --spring.config.location=classpath:/config/,optional:file:/sebserver/config/spring/; \

EXPOSE $SERVER_PORT