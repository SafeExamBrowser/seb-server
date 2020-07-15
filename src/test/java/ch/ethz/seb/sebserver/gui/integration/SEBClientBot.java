/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.session.RunningExamInfo;

public class SEBClientBot {

    private static final Logger log = LoggerFactory.getLogger(SEBClientBot.class);

    private static final Character COMMA = ',';
    private static final Character AMPERSAND = '&';
    private static final Character EQUALITY_SIGN = '=';

    private static final String LIST_SEPARATOR = COMMA.toString();
    private static final String FORM_URL_ENCODED_SEPARATOR = AMPERSAND.toString();
    private static final String FORM_URL_ENCODED_NAME_VALUE_SEPARATOR = EQUALITY_SIGN.toString();

    private static final String PARAM_INSTITUTION_ID = "institutionId";
    private static final String EXAM_API_PARAM_EXAM_ID = "examId";
    private static final String EXAM_API_SEB_CONNECTION_TOKEN = "SEBConnectionToken";
    private static final String EXAM_API_USER_SESSION_ID = "seb_user_session_id";
    private static final String EXAM_API_HANDSHAKE_ENDPOINT = "/handshake";
    private static final String EXAM_API_CONFIGURATION_REQUEST_ENDPOINT = "/examconfig";
    private static final String EXAM_API_PING_ENDPOINT = "/sebping";
    private static final String EXAM_API_PING_TIMESTAMP = "timestamp";
    private static final String EXAM_API_PING_NUMBER = "ping-number";
    private static final String EXAM_API_EVENT_ENDPOINT = "/seblog";

    private static final long ONE_SECOND = 1000; // milliseconds
    static final long TEN_SECONDS = 10 * ONE_SECOND;
    static final long ONE_MINUTE = 60 * ONE_SECOND;
    @SuppressWarnings("unused")
    private static final long ONE_HOUR = 60 * ONE_MINUTE;

    //private final ExecutorService executorService;
    private final List<String> scopes = Arrays.asList("read", "write");
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Random random = new Random();

    String webserviceAddress = "http://localhost:8080";
    String accessTokenEndpoint = "/oauth/token";
    String clientId = "test";
    String sessionId = null;
    String clientSecret = "test";
    String apiPath = "/exam-api";
    String apiVersion = "v1";
    String examId = "2";
    String institutionId = "1";
    int numberOfConnections = 4;
    long establishDelay = 0;
    long pingInterval = 100;
    long pingPause = 0;
    long pingPauseDelay = 0;
    long errorInterval = ONE_SECOND;
    long warnInterval = ONE_SECOND / 2;
    long runtime = ONE_SECOND * 2;
    int connectionAttempts = 1;

    public SEBClientBot(final ClientCredentials credentials, final String examId, final String instId)
            throws Exception {

        this.clientId = credentials.clientIdAsString();
        this.clientSecret = credentials.secretAsString();
        this.examId = examId;
        this.institutionId = instId;

        //this.executorService = Executors.newFixedThreadPool(this.numberOfConnections);

        for (int i = 0; i < this.numberOfConnections; i++) {
            final String sessionId = StringUtils.isNotBlank(this.sessionId)
                    ? this.sessionId
                    : "connection_" + getRandomName();

            new ConnectionBot(sessionId).run();
            //this.executorService.execute(new ConnectionBot(sessionId));
        }

        //this.executorService.shutdown();
    }

    private String getRandomName() {
        final StringBuilder sb = new StringBuilder(String.valueOf(this.random.nextInt(100)));
        while (sb.length() < 3) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

    private final class ConnectionBot implements Runnable {

        private final String name;
        private final OAuth2RestTemplate restTemplate;

        private final String handshakeURI = SEBClientBot.this.webserviceAddress +
                SEBClientBot.this.apiPath + "/" +
                SEBClientBot.this.apiVersion + EXAM_API_HANDSHAKE_ENDPOINT;
        private final String configurartionURI = SEBClientBot.this.webserviceAddress +
                SEBClientBot.this.apiPath + "/" +
                SEBClientBot.this.apiVersion + EXAM_API_CONFIGURATION_REQUEST_ENDPOINT;
        private final String pingURI = SEBClientBot.this.webserviceAddress +
                SEBClientBot.this.apiPath + "/" +
                SEBClientBot.this.apiVersion + EXAM_API_PING_ENDPOINT;
        private final String eventURI = SEBClientBot.this.webserviceAddress +
                SEBClientBot.this.apiPath + "/" +
                SEBClientBot.this.apiVersion + EXAM_API_EVENT_ENDPOINT;

        private final HttpEntity<?> connectBody;

        protected ConnectionBot(final String name) {
            this.name = name;
            this.restTemplate = createRestTemplate(null);
            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            this.connectBody = new HttpEntity<>(PARAM_INSTITUTION_ID +
                    FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                    SEBClientBot.this.institutionId
//                    + Constants.FORM_URL_ENCODED_SEPARATOR
//                    + API.EXAM_API_PARAM_EXAM_ID
//                    + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR
//                    + this.examId
                    ,
                    headers);

        }

        @Override
        public void run() {

            int attempt = 0;
            String connectionToken = null;

            while (connectionToken == null && attempt < SEBClientBot.this.connectionAttempts) {
                attempt++;
                log.info("ConnectionBot {} : Try to request access-token; attempt: {}", this.name, attempt);
                try {

                    final OAuth2AccessToken accessToken = this.restTemplate.getAccessToken();
                    log.info("ConnectionBot {} : Got access token: {}", this.name, accessToken);
                    connectionToken = createConnection();

                } catch (final Exception e) {
                    log.error("ConnectionBot {} : Failed to request access-token: ", this.name, e);
                    if (attempt >= SEBClientBot.this.connectionAttempts) {
                        log.error("ConnectionBot {} : Gave up afer {} connection attempts: ", this.name, attempt);
                        throw new RuntimeException("Connection Error. See Logs", e);
                    }
                }
            }

            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.set(EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            final MultiValueMap<String, String> eventHeaders = new LinkedMultiValueMap<>();
            eventHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            eventHeaders.set(EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);

            if (connectionToken != null) {
                if (getConfig(headers) && establishConnection(headers)) {

                    final PingEntity pingHeader = new PingEntity(headers);
                    final EventEntity errorHeader = new EventEntity(eventHeaders, "ERROR_LOG");
                    final EventEntity warnHeader = new EventEntity(eventHeaders, "WARN_LOG");

                    try {
                        final long startTime = System.currentTimeMillis();
                        final long endTime = startTime + SEBClientBot.this.runtime;
                        final long pingPauseStart = startTime + SEBClientBot.this.pingPauseDelay;
                        final long pingPauseEnd = pingPauseStart + SEBClientBot.this.pingPause;
                        long currentTime = startTime;
                        long lastPingTime = startTime;
                        long lastErrorTime = startTime;
                        long lastWarnTime = startTime;

                        while (currentTime < endTime) {
                            if (currentTime - lastPingTime >= SEBClientBot.this.pingInterval &&
                                    !(currentTime > pingPauseStart && currentTime < pingPauseEnd)) {

                                pingHeader.next();
                                if (!sendPing(pingHeader)) {
                                    // expecting a quit instruction was sent here
                                    return;
                                }
                                lastPingTime = currentTime;
                            }
                            if (currentTime - lastErrorTime >= SEBClientBot.this.errorInterval) {
                                errorHeader.next();
                                sendEvent(errorHeader);
                                lastErrorTime = currentTime;
                            }
                            if (currentTime - lastWarnTime >= SEBClientBot.this.warnInterval) {
                                warnHeader.next();
                                sendEvent(warnHeader);
                                lastWarnTime = currentTime;
                            }
                            try {
                                Thread.sleep(50);
                            } catch (final Exception e) {
                            }
                            currentTime = System.currentTimeMillis();
                        }
                    } catch (final Throwable t) {
                        log.error("ConnectionBot {} : Error sending events: ", this.name, t);
                        throw new RuntimeException("ConnectionBot {} : Error sending events: ");
                    } finally {
                        disconnect(connectionToken);
                    }
                }
            }
        }

        private String createConnection() {
            log.info("ConnectionBot {} : init connection", this.name);

            try {
                final ResponseEntity<String> exchange = this.restTemplate.exchange(
                        this.handshakeURI,
                        HttpMethod.POST,
                        this.connectBody,
                        new ParameterizedTypeReference<String>() {
                        });

                final HttpStatus statusCode = exchange.getStatusCode();
                if (statusCode.isError()) {
                    throw new RuntimeException("Webservice answered with error: " + exchange.getBody());
                }

                final Collection<RunningExamInfo> body = SEBClientBot.this.jsonMapper.readValue(
                        exchange.getBody(),
                        new TypeReference<Collection<RunningExamInfo>>() {
                        });
                final String token = exchange.getHeaders().getFirst(EXAM_API_SEB_CONNECTION_TOKEN);

                log.info("ConnectionBot {} : successfully created connection, token: {} body: {} ",
                        this.name,
                        token,
                        body);

                return token;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed to init connection", this.name, e);
                throw new RuntimeException("ConnectionBot {} : Failed to init connection: ", e);
            }
        }

        public boolean getConfig(final MultiValueMap<String, String> headers) {
            final HttpEntity<?> configHeader = new HttpEntity<>(headers);

            log.info("ConnectionBot {} : get SEB Configuration", this.name);

            try {
                final ResponseEntity<byte[]> exchange = this.restTemplate.exchange(
                        this.configurartionURI + "?" + EXAM_API_PARAM_EXAM_ID +
                                FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                                SEBClientBot.this.examId,
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
                    log.debug(
                            "ConnectionBot {} : successfully requested exam config: " + SEBClientBot.toString(config),
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

            if (SEBClientBot.this.establishDelay > 0) {
                try {

                    log.info("Wait for connection activation -> {}", SEBClientBot.this.establishDelay);

                    Thread.sleep(SEBClientBot.this.establishDelay);
                } catch (final Exception e) {
                    log.error("Failed to wait for connection activiation -> {} : {}",
                            SEBClientBot.this.establishDelay,
                            e.getMessage());
                    throw new RuntimeException();
                }
            }

            final HttpEntity<?> configHeader = new HttpEntity<>(
                    EXAM_API_USER_SESSION_ID +
                            FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
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
                throw new RuntimeException();
            }
        }

        private boolean sendPing(final HttpEntity<String> pingHeader) {
            try {

                final ResponseEntity<String> exchange = this.restTemplate.exchange(
                        this.pingURI,
                        HttpMethod.POST,
                        pingHeader,
                        new ParameterizedTypeReference<String>() {
                        });

                if (exchange.hasBody() && exchange.getBody().contains("SEB_QUIT")) {
                    log.info("SEB_QUIT client {}, response: {}",
                            pingHeader.getHeaders().get(EXAM_API_SEB_CONNECTION_TOKEN),
                            exchange.getBody());
                    return false;
                }

                return true;
            } catch (final Exception e) {
                log.error("ConnectionBot {} : Failed send ping", this.name, e);
                throw new RuntimeException();
            }
        }

        private boolean sendEvent(final HttpEntity<String> eventHeader) {
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
                throw new RuntimeException();
            }
        }

        public boolean disconnect(final String connectionToken) {
            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.set(EXAM_API_SEB_CONNECTION_TOKEN, connectionToken);
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
                throw new RuntimeException();
            }
        }
    }

    private OAuth2RestTemplate createRestTemplate(final String scopes) {
        final ClientCredentialsResourceDetails clientCredentialsResourceDetails =
                new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails
                .setAccessTokenUri(this.webserviceAddress + this.accessTokenEndpoint);
        clientCredentialsResourceDetails.setClientId(this.clientId);
        clientCredentialsResourceDetails.setClientSecret(this.clientSecret);
        if (StringUtils.isBlank(scopes)) {
            clientCredentialsResourceDetails.setScope(this.scopes);
        } else {
            clientCredentialsResourceDetails.setScope(
                    Arrays.asList(StringUtils.split(scopes, LIST_SEPARATOR)));
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
        simpleClientHttpRequestFactory.setReadTimeout(30000);
        simpleClientHttpRequestFactory.setOutputStreaming(false);
        restTemplate.setRequestFactory(simpleClientHttpRequestFactory);

        return restTemplate;
    }

    private static class PingEntity extends HttpEntity<String> {
        private final String pingBodyTemplate = EXAM_API_PING_TIMESTAMP +
                FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
                "%s" +
                FORM_URL_ENCODED_SEPARATOR +
                EXAM_API_PING_NUMBER +
                FORM_URL_ENCODED_NAME_VALUE_SEPARATOR +
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
                "{ \"type\": \"%s\", \"timestamp\": %s, \"text\": \"some error " + UUID.randomUUID() + " \" }";

        private boolean first = true;
        private long timestamp = 0;
        private final String eventType;

        protected EventEntity(final MultiValueMap<String, String> headers, final String eventType) {
            super(headers);
            this.eventType = eventType;
        }

        void next() {
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String getBody() {
            if (this.first) {
                this.first = false;
                final String longText =
                        "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT"
                                + "DEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHTDEDSFGREZHT";
                final String template =
                        "{ \"type\": \"%s\", \"timestamp\": %s, \"text\": \"some error " + longText + " \" }";
                return String.format(template, this.eventType, this.timestamp);
            } else {
                return String.format(this.eventBodyTemplate, this.eventType, this.timestamp);
            }
        }

        @Override
        public boolean hasBody() {
            return true;
        }
    }

    public static CharBuffer toCharBuffer(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return CharBuffer.allocate(0);
        }

        byteBuffer.rewind();
        return StandardCharsets.UTF_8.decode(byteBuffer);
    }

    public static String toString(final ByteBuffer byteBuffer) {
        return toCharBuffer(byteBuffer).toString();
    }

    public static String toString(final byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        return toString(ByteBuffer.wrap(byteArray));
    }

}
