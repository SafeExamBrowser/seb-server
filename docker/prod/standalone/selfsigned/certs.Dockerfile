FROM openjdk:11-jre-stretch

RUN  apt-get update && apt-get install -y openssl

ENV OPENSSL_SUBJ="/C=CH/ST=Zuerich/L=Zuerich"
ENV OPENSSL_CA="${OPENSSL_SUBJ}/CN=SEB_SEVER_CN"

VOLUME /certs
WORKDIR /certs

RUN export $(grep -v '^#' secrets | xargs)

CMD openssl genrsa -out ca-key.pem 2048 \
    && openssl req -new -x509 -key ca-key.pem -nodes -days 3600 -subj "${OPENSSL_CA}" -out ca.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -config certs.cnf -keyout server-key.pem -out server-req.pem \
    && openssl rsa -in server-key.pem -out server-key.pem \
    && openssl x509 -req -in server-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -config certs.cnf -keyout client-key.pem -out client-req.pem \
    && openssl rsa -in client-key.pem -out client-key.pem \
    && openssl x509 -req -in client-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out client-cert.pem \
    && openssl verify -CAfile ca.pem server-cert.pem client-cert.pem \
    && openssl x509 -in ca.pem -inform pem -out ca.der -outform der \
    && openssl pkcs12 -export -out client-cert.pkcs12 -in client-cert.pem -inkey client-key.pem -passout pass:"${KEYSTORE_PWD}" \
    && keytool -genkeypair -alias sebserver -dname "CN=localhost, OU=ETHZ, O=ETHZ, L=Zuerich, S=Zuerich, C=CH" -keyalg RSA -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore seb-server-keystore.pkcs12 -storepass "${KEYSTORE_PWD}" -validity 3650 \
    && keytool -export -alias sebserver -keystore seb-server-keystore.pkcs12 -rfc -file sebserver.cert -storetype PKCS12 -storepass "${KEYSTORE_PWD}" -noprompt \
    && keytool -importcert -trustcacerts -alias sebserver -file sebserver.cert -keystore seb-server-truststore.pkcs12 -storetype PKCS12 -storepass "${KEYSTORE_PWD}" -noprompt \
    && keytool -import -alias mariadb-ca -file ca.pem -keystore seb-server-truststore.pkcs12 -storepass "${KEYSTORE_PWD}" -srcstoretype PKCS12 -noprompt \
    && keytool -import -alias mariadb-client -file client-cert.pem -keystore seb-server-truststore.pkcs12 -storepass "${KEYSTORE_PWD}" -srcstoretype PKCS12 -noprompt \
    && keytool -import -alias mariadb-server -file server-cert.pem -keystore seb-server-keystore.pkcs12 -storepass "${KEYSTORE_PWD}" -srcstoretype PKCS12 -noprompt \