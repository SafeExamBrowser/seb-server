FROM alpine:latest

CMD cp -a /host/config/. /config/ \
    && secret=$(cat /config/secret) \
    && rm /host/config/secret \
    && sudo chmod 777 /host/config/secret