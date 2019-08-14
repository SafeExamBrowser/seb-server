FROM debian:jessie

RUN  apt-get update && apt-get install -y openssl

ENV OPENSSL_SUBJ="/C=CH/ST=Zuerich/L=Zuerich"
ENV OPENSSL_CA="${OPENSSL_SUBJ}/CN=demo-CA"
ENV OPENSSL_SERVER="${OPENSSL_SUBJ}/CN=demo-server"
ENV OPENSSL_CLIENT="${OPENSSL_SUBJ}/CN=demo-client"

COPY gencerts.sh /
RUN chmod +x /gencerts.sh

VOLUME /certs

WORKDIR /certs

# This works on windows
CMD openssl genrsa -out ca-key.pem 2048 && openssl req -new -x509 -key ca-key.pem -nodes -days 3600 -subj "${OPENSSL_CA}" -out ca.pem && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_SERVER}" -keyout server-key.pem -out server-req.pem && openssl rsa -in server-key.pem -out server-key.pem && openssl x509 -req -in server-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem && openssl req -newkey rsa:2048 -days 3600 -nodes -subj "${OPENSSL_CLIENT}" -keyout client-key.pem -out client-req.pem && openssl rsa -in client-key.pem -out client-key.pem && openssl x509 -req -in client-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out client-cert.pem && openssl verify -CAfile ca.pem server-cert.pem client-cert.pem && openssl x509 -in ca.pem -inform pem -out ca.der -outform der

# This don't work on windows
#CMD /gencerts.sh