FROM alpine:latest

CMD cp -a /host/config/. /config/ \
    && secret=$(cat /config/secret) \
    && rm /host/config/secret \
    && printf "monitorRoleUser  ${secret}\ncontrolRoleUser  ${secret}" > /config/jmxremote.password \
    && chmod 700 /config/jmxremote.password