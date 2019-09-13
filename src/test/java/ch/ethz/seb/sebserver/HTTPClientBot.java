/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.ArrayUtils;
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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.OAuth2ErrorHandler;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class HTTPClientBot {

    private static final long ONE_SECOND = 1000; // milliseconds
    private static final long TEN_SECONDS = 10 * ONE_SECOND;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;

    private static final Logger log = LoggerFactory.getLogger(HTTPClientBot.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

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

    private final Random random = new Random();

    public HTTPClientBot(final Map<String, String> args) {

        //this.webserviceAddress = args.getOrDefault("webserviceAddress", "http://ralph.ethz.ch:8080");
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
        this.connectionAttempts = Integer.parseInt(args.getOrDefault("connectionAttempts", "1"));

        for (int i = 0; i < this.numberOfConnections; i++) {
            this.executorService.execute(new ConnectionBot("connection_" + getRandomName()));
        }

        this.executorService.shutdown();
    }

    private String getRandomName() {
        final StringBuilder sb = new StringBuilder(String.valueOf(this.random.nextInt(100)));
        while (sb.length() < 3) {
            sb.insert(0, "0");
        }
        return sb.toString();
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
        private final OAuth2RestTemplate restTemplate;

        private final String handshakeURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + API.EXAM_API_HANDSHAKE_ENDPOINT;
        private final String configurartionURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT;
        private final String pingURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + API.EXAM_API_PING_ENDPOINT;
        private final String eventURI = HTTPClientBot.this.webserviceAddress +
                HTTPClientBot.this.apiPath + "/" +
                HTTPClientBot.this.apiVersion + API.EXAM_API_EVENT_ENDPOINT;

        private final HttpEntity<?> connectBody;

        protected ConnectionBot(final String name) {
            this.name = name;
            this.restTemplate = createRestTemplate(null);
            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            this.connectBody = new HttpEntity<>(API.PARAM_INSTITUTION_ID +
                    Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                    HTTPClientBot.this.institutionId
//                    + Constants.FORM_URL_ENCODED_SEPARATOR
//                    + API.EXAM_API_PARAM_EXAM_ID
//                    + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR
//                    + HTTPClientBot.this.examId
                    ,
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
            String connectionToken = null;

            while (connectionToken == null && attempt < HTTPClientBot.this.connectionAttempts) {
                attempt++;
                log.info("ConnectionBot {} : Try to request access-token; attempt: {}", this.name, attempt);
                try {

                    final OAuth2AccessToken accessToken = this.restTemplate.getAccessToken();
                    log.info("ConnectionBot {} : Got access token: {}", this.name, accessToken);
                    connectionToken = createConnection();

                } catch (final Exception e) {
                    log.error("ConnectionBot {} : Failed to request access-token: ", this.name, e);
                    if (attempt >= HTTPClientBot.this.connectionAttempts) {
                        log.error("ConnectionBot {} : Gave up afer {} connection attempts: ", this.name, attempt);
                        return;
                    }
                }
            }

            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            final MultiValueMap<String, String> eventHeaders = new LinkedMultiValueMap<>();
            eventHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            eventHeaders.set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            if (connectionToken != null) {
                if (getConfig(headers) && establishConnection(headers)) {

                    final PingEntity pingHeader = new PingEntity(headers);
                    final EventEntity eventHeader = new EventEntity(eventHeaders);

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
        }

        private String createConnection() {
            log.info("ConnectionBot {} : init connection", this.name);

            try {
                final ResponseEntity<Collection<RunningExamInfo>> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.POST,
                        this.connectBody,
                        new ParameterizedTypeReference<Collection<RunningExamInfo>>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                final Collection<RunningExamInfo> body = exchange.getBody();
                final String token = exchange.getHeaders().getFirst(API.EXAM_API_SEB_CONNECTION_TOKEN);

                log.info("ConnectionBot {} : successfully created connection, token: {} body: {} ", this.name, token,
                        body);

                return token;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed to init connection", this.name, e);
                return null;
            }
        }

        public boolean getConfig(final MultiValueMap<String, String> headers) {
            final HttpEntity<?> configHeader = new HttpEntity<>(headers);

            log.info("ConnectionBot {} : get SEB Configuration", this.name);

            try {
                final ResponseEntity<byte[]> exchange = this.restTemplate.exchange(
                        this.configurartionURI + "?" + API.EXAM_API_PARAM_EXAM_ID +
                                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                                HTTPClientBot.this.examId,
                        HttpMethod.GET,
                        configHeader,
                        new ParameterizedTypeReference<byte[]>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                final byte[] config = exchange.getBody();

                if (ArrayUtils.isEmpty(config)) {
                    log.error("No Exam config get from API. processing anyway");
                }

                if (log.isDebugEnabled()) {
                    log.debug("ConnectionBot {} : successfully requested exam config: " + Utils.toString(config),
                            this.name);
                } else {
                    log.info("ConnectionBot {} : successfully requested exam config", this.name);
                }

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get SEB Configuration", this.name, e);
                return false;
            }
        }

        public boolean establishConnection(final MultiValueMap<String, String> headers) {
            final HttpEntity<?> configHeader = new HttpEntity<>(
                    API.EXAM_API_USER_SESSION_ID +
                            Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                            this.name,
                    headers);

            log.info("ConnectionBot {} : Trying to establish SEB client connection", this.name);

            try {

                final ResponseEntity<String> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.PUT,
                        configHeader,
                        new ParameterizedTypeReference<String>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                log.info("ConnectionBot {} : successfully established SEB client connection", this.name);

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get established SEB client connection", this.name, e);
                return false;
            }
        }

        private boolean sendPing(final HttpEntity<String> pingHeader) {
            try {

                this.restTemplate.exchange(
                        this.pingURI,
                        HttpMethod.POST,
                        pingHeader,
                        new ParameterizedTypeReference<String>() {
                        });

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed send ping", this.name, e);
                return false;
            }
        }

        private boolean sendErrorEvent(final HttpEntity<String> eventHeader) {
            try {

                this.restTemplate.exchange(
                        this.eventURI,
                        HttpMethod.POST,
                        eventHeader,
                        new ParameterizedTypeReference<String>() {
                        });

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed send event", this.name, e);
                return false;
            }
        }

        public boolean disconnect(final String connectionToken) {
            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.set(API.EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);
            final HttpEntity<?> configHeader = new HttpEntity<>(headers);

            log.info("ConnectionBot {} : Trying to delete SEB client connection", this.name);

            try {

                final ResponseEntity<String> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.DELETE,
                        configHeader,
                        new ParameterizedTypeReference<String>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                log.info("ConnectionBot {} : successfully deleted SEB client connection", this.name);

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed get deleted SEB client connection", this.name, e);
                return false;
            }
        }
    }

    private OAuth2RestTemplate createRestTemplate(final String scopes) {
        final ClientCredentialsResourceDetails clientCredentialsResourceDetails =
                new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails.setAccessTokenUri(this.webserviceAddress + this.accessTokenEndpoint);
        clientCredentialsResourceDetails.setClientId(this.clientId);
        clientCredentialsResourceDetails.setClientSecret(this.clientSecret);
        if (StringUtils.isBlank(scopes)) {
            clientCredentialsResourceDetails.setScope(this.scopes);
        } else {
            clientCredentialsResourceDetails.setScope(
                    Arrays.asList(StringUtils.split(scopes, Constants.LIST_SEPARATOR)));
        }

        final OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(clientCredentialsResourceDetails);
        restTemplate.setErrorHandler(new OAuth2ErrorHandler(clientCredentialsResourceDetails) {

            @Override
            public void handleError(final ClientHttpResponse response) throws IOException {
                System.out.println("********************** handleError: " + response.getStatusCode());
                super.handleError(response);
            }

        });
        restTemplate
                .getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        //restTemplate.setRetryBadAccessTokens(true);

        final SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setReadTimeout(5000);
        simpleClientHttpRequestFactory.setOutputStreaming(false);
        restTemplate.setRequestFactory(simpleClientHttpRequestFactory);

        return restTemplate;
    }

    private static class PingEntity extends HttpEntity<String> {
        private final String pingBodyTemplate = API.EXAM_API_PING_TIMESTAMP +
                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                "%s" +
                Constants.FORM_URL_ENCODED_SEPARATOR +
                API.EXAM_API_PING_NUMBER +
                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                "%s";

        private long timestamp = 0;
        private int count = 0;

        protected PingEntity(final MultiValueMap<String, String> headers) {
            super(headers);
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
                "{ \"type\": \"ERROR_LOG\", \"timestamp\": %s, \"text\": \"some error " + UUID.randomUUID() + " \" }";

        private long timestamp = 0;

        protected EventEntity(final MultiValueMap<String, String> headers) {
            super(headers);
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
