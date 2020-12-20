#!/bin/sh
if [ "${SEBSERVER_MODE}" = "gui" ]; then exec java \
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
    -jar "seb-server.jar" \
    --spring.profiles.active=ws,prod,prod-ws \
    --spring.config.location=file:/sebserver/config/spring/,classpath:/config/ \
    --datastore.mariadb.server.address="${DB_HOST}" \
    --datastore.mariadb.server.port="${DB_PORT}" \
    --spring.datasource.username="${DB_USER}" \
    --sebserver.mariadb.password="${DB_PASSWORD}" \
    --sebserver.password="${SECRET}" ; \
fi;