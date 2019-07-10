/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.OAuth2ErrorHandler;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExam;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class HTTPClientBot {

    private static final long ONE_SECOND = 1000; // milliseconds
    private static final long TEN_SECONDS = 10 * ONE_SECOND;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;

    private static final Logger log = LoggerFactory.getLogger(HTTPClientBot.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final List<String> scopes = Arrays.asList("read", "write");

    private final String webserviceAddress;
    private final String accessTokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String apiPath;
    private final String apiVersion;
    private final String examId;
    private final String institutionId;

    private final int numberOfConnections;

    private final long pingInterval;
    private final long errorInterval;
    private final long runtime;
    private final int connectionAttempts;

    public HTTPClientBot(final Map<String, String> args) {
        this.webserviceAddress = args.getOrDefault("webserviceAddress", "http://localhost:8080");
        this.accessTokenEndpoint = args.getOrDefault("accessTokenEndpoint", "/oauth/token");
        this.clientId = args.getOrDefault("clientId", "TO_SET");
        this.clientSecret = args.getOrDefault("clientSecret", "TO_SET");
        this.apiPath = args.getOrDefault("apiPath", "/exam-api");
        this.apiVersion = args.getOrDefault("apiVersion", "v1");
        this.examId = args.getOrDefault("examId", "2");
        this.institutionId = args.getOrDefault("institutionId", "1");
        this.numberOfConnections = Integer.parseInt(args.getOrDefault("numberOfConnections", "1"));
        this.pingInterval = Long.parseLong(args.getOrDefault("pingInterval", "200"));
        this.errorInterval = Long.parseLong(args.getOrDefault("errorInterval", String.valueOf(TEN_SECONDS)));
        this.runtime = Long.parseLong(args.getOrDefault("runtime", String.valueOf(ONE_MINUTE)));
        this.connectionAttempts = Integer.parseInt(args.getOrDefault("connectionAttempts", "3"));

        for (int i = 0; i < this.numberOfConnections; i++) {
            this.executorService.execute(new ConnectionBot("connection_" + i));
        }

        this.executorService.shutdown();
    }

    public static void main(final String[] args) {
        final Map<String, String> argsMap = new HashMap<>();
        if (args.length > 0) {
            for (final String arg : StringUtils.split(args[0], Constants.LIST_SEPARATOR)) {
                final String[] nameValue = StringUtils.split(arg, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                argsMap.put(nameValue[0], nameValue[1]);
            }
        }
        new HTTPClientBot(argsMap);
    }

    private final class ConnectionBot implements Runnable {

        private final String name;
        private final OAuth2RestTemplate restTemplate = createRestTemplate();

        private final String handshakeURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + "/handshake";
        private final String configurartionURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + "/configuration";
        private final String pingURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + "/sebping";
        private final String eventURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + "/seblog";

        private final HttpEntity<?> connectBody;

        protected ConnectionBot(final String name) {
            this.name = name;
            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            this.connectBody = new HttpEntity<>(API.PARAM_INSTITUTION_ID +
                    Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                    HTTPClientBot.this.institutionId +
                    Constants.FORM_URL_ENCODED_SEPARATOR +
                    API.EXAM_API_PARAM_EXAM_ID +
                    Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                    HTTPClientBot.this.examId,
                    headers);

        }

        @Override
        public void run() {
            log.info("ConnectionBot {} : Client-Connection-Bot started: {}\n"
                    + "webserviceAddress: {}\n"
                    + "accessTokenEndpoint: {}\n"
                    + "clientId: {}\n"
                    + "clientSecret: {}\n"
                    + "apiPath: {}\n"
                    + "apiVersion: {}\n"
                    + "examId: {}\n"
                    + "institutionId: {}\n"
                    + "pingInterval: {}\n"
                    + "errorInterval: {}\n"
                    + "runtime: {}\n", this.name,
                    HTTPClientBot.this.webserviceAddress,
                    HTTPClientBot.this.accessTokenEndpoint,
                    HTTPClientBot.this.clientId,
                    HTTPClientBot.this.clientSecret,
                    HTTPClientBot.this.apiPath,
                    HTTPClientBot.this.apiVersion,
                    HTTPClientBot.this.examId,
                    HTTPClientBot.this.institutionId,
                    HTTPClientBot.this.pingInterval,
                    HTTPClientBot.this.errorInterval);

            int attempt = 0;

            while (attempt < HTTPClientBot.this.connectionAttempts) {
                attempt++;
                log.info("ConnectionBot {} : Try to request access-token; attempt: {}", this.name, attempt);
                try {

                    this.restTemplate.getAccessToken();

                    final String connectionToken = createConnection();
                    if (connectionToken != null) {
                        if (getConfig(connectionToken) && establishConnection(connectionToken)) {

                            final PingEntity pingHeader = new PingEntity(connectionToken);
                            final EventEntity eventHeader = new EventEntity(connectionToken);

                            try {
                                final long startTime = System.currentTimeMillis();
                                final long endTime = startTime + HTTPClientBot.this.runtime;
                                long currentTime = startTime;
                                long lastPingTime = startTime;
                                long lastErrorTime = startTime;

                                while (currentTime < endTime) {
                                    if (currentTime - lastPingTime >= HTTPClientBot.this.pingInterval) {
                                        pingHeader.next();
                                        sendPing(pingHeader);
                                        lastPingTime = currentTime;
                                    }
                                    if (currentTime - lastErrorTime >= HTTPClientBot.this.errorInterval) {
                                        eventHeader.next();
                                        sendErrorEvent(eventHeader);
                                        lastErrorTime = currentTime;
                                    }
                                    try {
                                        Thread.sleep(50);
                                    } catch (final Exception e) {
                                    }
                                    currentTime = System.currentTimeMillis();
                                }
                            } catch (final Throwable t) {
                                log.error("ConnectionBot {} : Error sending events: ", this.name, t);
                            } finally {
                                disconnect(connectionToken);
                            }
                        }
                    }

                } catch (final Exception e) {
                    log.error("ConnectionBot {} : Failed to request access-token: ", this.name, e);
                    if (attempt >= HTTPClientBot.this.connectionAttempts) {
                        log.error("ConnectionBot {} : Gave up afer {} connection attempts: ", this.name, attempt);
                    }
                }
            }
        }

        private String createConnection() {
            log.info("ConnectionBot {} : init connection", this.name);

            try {
                final ResponseEntity<Collection<RunningExam>> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.POST,
                        this.connectBody,
                        new ParameterizedTypeReference<Collection<RunningExam>>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                final Collection<RunningExam> body = exchange.getBody();
                final String token = exchange.getHeaders().getFirst(API.EXAM_API_SEB_CONNECTION_TOKEN);

                log.info("ConnectionBot {} : successfully created connection, token: {} body: {} ", token, body);

                return token;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed to init connection", e);
                return null;
            }
        }

        public boolean getConfig(final String connectionToken) {
            final HttpEntity<?> configHeader = new HttpEntity<>(
                    API.EXAM_API_PARAM_EXAM_ID +
                            Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                            HTTPClientBot.this.examId);
            configHeader.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            configHeader.getHeaders().set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            log.info("ConnectionBot {} : get SEB Configuration", this.name);

            try {
                final ResponseEntity<byte[]> exchange = this.restTemplate.exchange(
                        this.configurartionURI,
                        HttpMethod.GET,
                        configHeader,
                        new ParameterizedTypeReference<byte[]>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                final byte[] config = exchange.getBody();

                if (log.isDebugEnabled()) {
                    log.debug("ConnectionBot {} : successfully requested exam config: " + Utils.toString(config));
                } else {
                    log.info("ConnectionBot {} : successfully requested exam config");
                }

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get SEB Configuration", e);
                return false;
            }
        }

        public boolean establishConnection(final String connectionToken) {
            final HttpEntity<?> configHeader = new HttpEntity<>(
                    API.EXAM_API_USER_SESSION_ID +
                            Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                            this.name);
            configHeader.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            configHeader.getHeaders().set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            log.info("ConnectionBot {} : Trying to establish SEB client connection", this.name);

            try {

                final ResponseEntity<Object> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.PUT,
                        configHeader,
                        new ParameterizedTypeReference<>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                log.info("ConnectionBot {} : successfully established SEB client connection");

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get established SEB client connection", e);
                return false;
            }
        }

        private boolean sendPing(final HttpEntity<String> pingHeader) {
            try {

                this.restTemplate.exchange(
                        this.pingURI,
                        HttpMethod.POST,
                        pingHeader,
                        new ParameterizedTypeReference<>() {
                        });

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed send ping", e);
                return false;
            }
        }

        private boolean sendErrorEvent(final HttpEntity<String> eventHeader) {
            try {

                this.restTemplate.exchange(
                        this.eventURI,
                        HttpMethod.POST,
                        eventHeader,
                        new ParameterizedTypeReference<>() {
                        });

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed send ping", e);
                return false;
            }
        }

        public boolean disconnect(final String connectionToken) {
            final HttpEntity<?> configHeader = new HttpEntity<>(null);
            configHeader.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            configHeader.getHeaders().set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            log.info("ConnectionBot {} : Trying to delete SEB client connection", this.name);

            try {

                final ResponseEntity<Object> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.DELETE,
                        configHeader,
                        new ParameterizedTypeReference<>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                log.info("ConnectionBot {} : successfully deleted SEB client connection");

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get deleted SEB client connection", e);
                return false;
            }
        }
    }

    private OAuth2RestTemplate createRestTemplate() {
        final ClientCredentialsResourceDetails clientCredentialsResourceDetails =
                new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails.setAccessTokenUri(this.webserviceAddress + this.accessTokenEndpoint);
        clientCredentialsResourceDetails.setClientId(this.clientId);
        clientCredentialsResourceDetails.setClientSecret(this.clientSecret);
        clientCredentialsResourceDetails.setScope(this.scopes);

        final OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientCredentialsResourceDetails);
        restTemplate.setErrorHandler(new OAuth2ErrorHandler(clientCredentialsResourceDetails));
        restTemplate
                .getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

        return restTemplate;
    }

    private static class PingEntity extends HttpEntity<String> {
        private final String pingBodyTemplate = API.EXAM_API_PING_TIMESTAMP +
                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                "{}" +
                Constants.FORM_URL_ENCODED_SEPARATOR +
                API.EXAM_API_PING_NUMBER +
                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                "{}";

        private long timestamp = 0;
        private int count = 0;

        protected PingEntity(final String connectionToken) {
            super();
            getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            getHeaders().set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);
        }

        void next() {
            this.timestamp = System.currentTimeMillis();
            this.count++;
        }

        @Override
        public String getBody() {
            return String.format(this.pingBodyTemplate, this.timestamp, this.count);
        }

        @Override
        public boolean hasBody() {
            return true;
        }
    }

    private static class EventEntity extends HttpEntity<String> {
        private final String eventBodyTemplate =
                "{ \"type\": \"ERROR_LOG\", \"timestamp\": {}, \"text\": \"some error\" }";

        private long timestamp = 0;

        protected EventEntity(final String connectionToken) {
            super();
            getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            getHeaders().set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);
        }

        void next() {
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String getBody() {
            return String.format(this.eventBodyTemplate, this.timestamp);
        }

        @Override
        public boolean hasBody() {
            return true;
        }
    }

}
