FROM openjdk:11-jre-stretch

RUN  apt-get update && apt-get install -y openssl

ENV OPENSSL_SUBJ="/C=CH/ST=Zurich/L=Zurich"
ENV OPENSSL_CA="${OPENSSL_SUBJ}/CN=demo-CA"
ENV OPENSSL_SERVER="${OPENSSL_SUBJ}/CN=localhost"
ENV OPENSSL_CLIENT="${OPENSSL_SUBJ}/CN=localhost"
ENV ADDITIONAL_DNS="dns:localhost,dns:127.0.0.1,dns:seb-server"

WORKDIR /certs

CMD cp -a /host/config/. /config/ \
    && secret=$(cat /config/secret) \
    && openssl genrsa -out ca-key.pem 2048 \
    && openssl req -new -x509 -key ca-key.pem -nodes -days 3600 -subj "${OPENSSL_CA}" -out ca.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_SERVER}" -keyout server-key.pem -out server-req.pem \
    && openssl rsa -in server-key.pem -out server-key.pem \
    && openssl x509 -req -in server-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_CLIENT}" -keyout client-key.pem -out client-req.pem \
    && openssl rsa -in client-key.pem -out client-key.pem \
    && openssl x509 -req -in client-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out client-cert.pem \
    && openssl verify -CAfile ca.pem server-cert.pem client-cert.pem \
    && openssl pkcs12 -export -out client-cert.pkcs12 -in client-cert.pem -inkey client-key.pem -passout pass:${secret} \
    && keytool -genkeypair -alias sebserver -dname "CN=localhost, OU=ETHZ, O=ETHZ, L=Zurich, S=Zurich, C=CH" -ext san="${ADDITIONAL_DNS}" -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore seb-server-keystore.pkcs12 -storepass ${secret} -validity 3650 \
    && keytool -export -alias sebserver -keystore seb-server-keystore.pkcs12 -rfc -file sebserver.cert -storetype PKCS12 -storepass ${secret} -noprompt \
    && keytool -importcert -trustcacerts -alias sebserver -file sebserver.cert -keystore seb-server-truststore.pkcs12 -storetype PKCS12 -storepass ${secret} -noprompt \
    && keytool -import -alias mariadb-ca -file ca.pem -keystore seb-server-truststore.pkcs12 -storepass ${secret} -srcstoretype PKCS12 -noprompt \
    && keytool -import -alias mariadb-client -file client-cert.pem -keystore seb-server-truststore.pkcs12 -storepass ${secret} -srcstoretype PKCS12 -noprompt \
    && keytool -import -alias mariadb-server -file server-cert.pem -keystore seb-server-keystore.pkcs12 -storepass ${secret} -srcstoretype PKCS12 -noprompt \
    && chmod 777 -R . \
    && cp seb-server-keystore.pkcs12 /host/config/ \
    && cp seb-server-truststore.pkcs12 /host/config/ \
    && rm /host/config/secret