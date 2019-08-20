FROM openjdk:11-jre-stretch

RUN  apt-get update && apt-get install -y openssl

ENV KEYSTORE_PWD=
ENV SERVER_CN="localhost"
ENV CLIENT_CN="localhost"
ENV OPENSSL_SUBJ="/C=CH/ST=Zuerich/L=Zuerich"
ENV OPENSSL_CA="${OPENSSL_SUBJ}/CN=demo-CA"
ENV OPENSSL_SERVER="${OPENSSL_SUBJ}/CN=${SERVER_CN}"
ENV OPENSSL_CLIENT="${OPENSSL_SUBJ}/CN=${CLIENT_CN}"

VOLUME /certs

WORKDIR /certs

# This works on windows
CMD openssl genrsa -out ca-key.pem 2048 \
    && openssl req -new -x509 -key ca-key.pem -nodes -days 3600 -subj "${OPENSSL_CA}" -out ca.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_SERVER}" -keyout server-key.pem -out server-req.pem \
    && openssl rsa -in server-key.pem -out server-key.pem \
    && openssl x509 -req -in server-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem \
    && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_CLIENT}" -keyout client-key.pem -out client-req.pem \
    && openssl rsa -in client-key.pem -out client-key.pem \
    && openssl x509 -req -in client-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out client-cert.pem \
    && openssl verify -CAfile ca.pem server-cert.pem client-cert.pem \
    && openssl x509 -in ca.pem -inform pem -out ca.der -outform der \
    && openssl pkcs12 -export -out client-cert.pkcs12 -in client-cert.pem -inkey client-key.pem -passout pass:"${KEYSTORE_PWD}" \
    && keytool -importkeystore -destkeystore seb-server-keystore.pkcs12 -deststorepass "${KEYSTORE_PWD}" -srckeystore client-cert.pkcs12 -srcstoretype PKCS12 -srcstorepass "${KEYSTORE_PWD}" \
    && keytool -import -file ca.pem -keystore seb-server-truststore.pkcs12 -storepass "${KEYSTORE_PWD}" -srcstoretype PKCS12 -noprompt