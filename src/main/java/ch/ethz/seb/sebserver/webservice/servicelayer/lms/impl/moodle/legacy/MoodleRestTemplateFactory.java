/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;

public class MoodleRestTemplateFactory {

    private static final Logger log = LoggerFactory.getLogger(MoodleRestTemplateFactory.class);

    final JSONMapper jsonMapper;
    final APITemplateDataSupplier apiTemplateDataSupplier;
    final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    final ClientCredentialService clientCredentialService;
    final Set<String> knownTokenAccessPaths;

    public MoodleRestTemplateFactory(
            final JSONMapper jsonMapper,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final String[] alternativeTokenRequestPaths) {

        this.jsonMapper = jsonMapper;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        this.knownTokenAccessPaths = new HashSet<>();
        this.knownTokenAccessPaths.add(MoodleAPIRestTemplate.MOODLE_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            this.knownTokenAccessPaths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }
    }

    APITemplateDataSupplier getApiTemplateDataSupplier() {
        return this.apiTemplateDataSupplier;
    }

    public LmsSetupTestResult test() {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();

        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        } else {
            // try to connect to the url
            if (!Utils.pingHost(lmsSetup.lmsApiUrl)) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_URL,
                        "lmsSetup:lmsUrl:url.invalid"));
            }
        }

        if (StringUtils.isBlank(lmsSetup.lmsRestApiToken)) {
            if (!credentials.hasClientId()) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_CLIENTNAME,
                        "lmsSetup:lmsClientname:notNull"));
            }
            if (!credentials.hasSecret()) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                        "lmsSetup:lmsClientsecret:notNull"));
            }
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.MOODLE, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE);
    }

    Result<MoodleAPIRestTemplate> createRestTemplate() {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();

        return this.knownTokenAccessPaths
                .stream()
                .map(this::createRestTemplate)
                .map(result -> {
                    if (result.hasError()) {
                        log.warn("Failed to get access token for LMS: {}({})",
                                lmsSetup.name,
                                lmsSetup.id,
                                result.getError());
                    }
                    return result;
                })
                .filter(Result::hasValue)
                .findFirst()
                .orElse(Result.ofRuntimeError(
                        "Failed to gain any access for LMS " +
                                lmsSetup.name + "(" + lmsSetup.id +
                                ") on paths: " + this.knownTokenAccessPaths));
    }

    Result<MoodleAPIRestTemplate> createRestTemplate(final String accessTokenPath) {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();

        return Result.tryCatch(() -> {
            final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
            final ProxyData proxyData = this.apiTemplateDataSupplier.getProxyData();

            final CharSequence plainClientId = credentials.clientId;
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(credentials)
                    .getOrThrow();

            final MoodleAPIRestTemplate restTemplate = new MoodleAPIRestTemplate(
                    this.jsonMapper,
                    lmsSetup.lmsApiUrl,
                    accessTokenPath,
                    lmsSetup.lmsRestApiToken,
                    plainClientId,
                    plainClientSecret);

            final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory(proxyData)
                    .getOrThrow();

            restTemplate.setRequestFactory(clientHttpRequestFactory);
            final CharSequence accessToken = restTemplate.getAccessToken();

            if (accessToken == null) {
                throw new RuntimeException("Failed to get access token for LMS " +
                        lmsSetup.name + "(" + lmsSetup.id +
                        ") on path: " + accessTokenPath);
            }

            return restTemplate;
        });
    }

    public class MoodleAPIRestTemplate extends RestTemplate {

        public static final String URI_VAR_USER_NAME = "username";
        public static final String URI_VAR_PASSWORD = "pwd";
        public static final String URI_VAR_SERVICE = "service";

        private static final String MOODLE_DEFAULT_TOKEN_REQUEST_PATH =
                "/login/token.php?username={" + URI_VAR_USER_NAME +
                        "}&password={" + URI_VAR_PASSWORD + "}&service={" + URI_VAR_SERVICE + "}";

        private static final String MOODLE_DEFAULT_REST_API_PATH = "/webservice/rest/server.php";
        private static final String REST_REQUEST_TOKEN_NAME = "wstoken";
        private static final String REST_REQUEST_FUNCTION_NAME = "wsfunction";
        private static final String REST_REQUEST_FORMAT_NAME = "moodlewsrestformat";
        private static final String REST_API_TEST_FUNCTION = "core_webservice_get_site_info";

        private final String serverURL;
        private final String tokenPath;

        private CharSequence accessToken;

        private final Map<String, String> tokenReqURIVars;
        private final HttpEntity<?> tokenReqEntity = new HttpEntity<>(new LinkedMultiValueMap<>());

        protected MoodleAPIRestTemplate(
                final JSONMapper jsonMapper,
                final String serverURL,
                final String tokenPath,
                final CharSequence accessToken,
                final CharSequence username,
                final CharSequence password) {

            this.serverURL = serverURL;
            this.tokenPath = tokenPath;
            this.accessToken = StringUtils.isNotBlank(accessToken) ? accessToken : null;

            this.tokenReqURIVars = new HashMap<>();
            this.tokenReqURIVars.put(URI_VAR_USER_NAME, String.valueOf(username));
            this.tokenReqURIVars.put(URI_VAR_PASSWORD, String.valueOf(password));
            this.tokenReqURIVars.put(URI_VAR_SERVICE, "moodle_mobile_app");

        }

        public String getService() {
            return this.tokenReqURIVars.get(URI_VAR_SERVICE);
        }

        public void setService(final String service) {
            this.tokenReqURIVars.put(URI_VAR_SERVICE, service);
        }

        public CharSequence getAccessToken() {
            if (this.accessToken == null) {
                requestAccessToken();
            }

            return this.accessToken;
        }

        public void testAPIConnection(final String... functions) {
            try {
                final String apiInfo = this.callMoodleAPIFunction(REST_API_TEST_FUNCTION);
                final WebserviceInfo webserviceInfo =
                        MoodleRestTemplateFactory.this.jsonMapper.readValue(apiInfo, WebserviceInfo.class);

                if (StringUtils.isBlank(webserviceInfo.username) || StringUtils.isBlank(webserviceInfo.userid)) {
                    throw new RuntimeException("Invalid WebserviceInfo: " + webserviceInfo);
                }

                final List<String> missingAPIFunctions = Arrays.stream(functions)
                        .filter(f -> !webserviceInfo.functions.containsKey(f))
                        .collect(Collectors.toList());

                if (!missingAPIFunctions.isEmpty()) {
                    throw new RuntimeException("Missing Moodle Webservice API functions: " + missingAPIFunctions);
                }

            } catch (final RuntimeException re) {
                throw re;
            } catch (final Exception e) {
                throw new RuntimeException("Failed to test Moodle rest API: ", e);
            }
        }

        public String callMoodleAPIFunction(final String functionName) {
            return callMoodleAPIFunction(functionName, null, null);
        }

        public String callMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryAttributes) {
            return callMoodleAPIFunction(functionName, null, queryAttributes);
        }

        public String callMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryParams,
                final MultiValueMap<String, String> queryAttributes) {

            getAccessToken();

            final UriComponentsBuilder queryParam = UriComponentsBuilder
                    .fromHttpUrl(this.serverURL + MOODLE_DEFAULT_REST_API_PATH)
                    .queryParam(REST_REQUEST_TOKEN_NAME, this.accessToken)
                    .queryParam(REST_REQUEST_FUNCTION_NAME, functionName)
                    .queryParam(REST_REQUEST_FORMAT_NAME, "json");

            if (queryParams != null && !queryParams.isEmpty()) {
                queryParam.queryParams(queryParams);
            }

            final boolean usePOST = queryAttributes != null && !queryAttributes.isEmpty();
            HttpEntity<?> functionReqEntity;
            if (usePOST) {
                final HttpHeaders headers = new HttpHeaders();
                headers.set(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                final String body = Utils.toAppFormUrlEncodedBody(queryAttributes);
                functionReqEntity = new HttpEntity<>(body, headers);

            } else {
                functionReqEntity = new HttpEntity<>(new LinkedMultiValueMap<>());
            }

            final ResponseEntity<String> response = super.exchange(
                    queryParam.toUriString(),
                    usePOST ? HttpMethod.POST : HttpMethod.GET,
                    functionReqEntity,
                    String.class);

            final LmsSetup lmsSetup = MoodleRestTemplateFactory.this.apiTemplateDataSupplier
                    .getLmsSetup();

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(
                        "Failed to call Moodle webservice API function: " + functionName + " lms setup: " +
                                lmsSetup + " response: " + response.getBody());
            }

            final String body = response.getBody();

            // NOTE: for some unknown reason, Moodles API error responses come with a 200 OK response HTTP Status
            //       So this is a special Moodle specific error handling here...
            if (body.startsWith("{exception") || body.contains("\"exception\":")) {
                // Reset access token to get new on next call (fix access if token is expired)
                // TODO find a way to verify token invalidity response from Moodle.
                //      Unfortunately there is not a lot of Moodle documentation for the API error handling around.
                this.accessToken = null;
                throw new RuntimeException(
                        "Failed to call Moodle webservice API function: " + functionName + " lms setup: " +
                                lmsSetup + " response: " + body);
            }

            return body;
        }

        private void requestAccessToken() {

            final LmsSetup lmsSetup = MoodleRestTemplateFactory.this.apiTemplateDataSupplier
                    .getLmsSetup();

            try {

                final ResponseEntity<String> response = super.exchange(
                        this.serverURL + this.tokenPath,
                        HttpMethod.GET,
                        this.tokenReqEntity,
                        String.class,
                        this.tokenReqURIVars);

                if (response.getStatusCode() != HttpStatus.OK) {
                    log.error("Failed to gain access token for LMS (Moodle): lmsSetup: {} response: {} : {}",
                            lmsSetup,
                            response.getStatusCode(),
                            response.getBody());
                    throw new RuntimeException("Failed to gain access token for LMS (Moodle): lmsSetup: " +
                            lmsSetup + " response: " + response.getBody());
                }

                try {
                    final MoodleToken moodleToken = MoodleRestTemplateFactory.this.jsonMapper.readValue(
                            response.getBody(),
                            MoodleToken.class);

                    if (moodleToken == null || moodleToken.token == null) {
                        throw new RuntimeException("Access Token request with 200 but no or invalid token body");
                    } else {
                        log.info("Successfully get access token from Moodle: {}",
                                lmsSetup);
                    }

                    this.accessToken = moodleToken.token;
                } catch (final Exception e) {
                    log.error("Failed to gain access token for LMS (Moodle): lmsSetup: {} response: {} : {}",
                            lmsSetup,
                            response.getStatusCode(),
                            response.getBody());
                    throw new RuntimeException("Failed to gain access token for LMS (Moodle): lmsSetup: " +
                            lmsSetup + " response: " + response.getBody(), e);
                }

            } catch (final Exception e) {
                log.error("Failed to gain access token for LMS (Moodle): lmsSetup: {} :",
                        lmsSetup,
                        e);
                throw new RuntimeException("Failed to gain access token for LMS (Moodle): lmsSetup: " +
                        lmsSetup + " cause: " + e.getMessage());
            }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private final static class MoodleToken {
        final String token;
        @SuppressWarnings("unused")
        final String privatetoken;

        @JsonCreator
        protected MoodleToken(
                @JsonProperty(value = "token") final String token,
                @JsonProperty(value = "privatetoken", required = false) final String privatetoken) {

            this.token = token;
            this.privatetoken = privatetoken;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private final static class WebserviceInfo {
        String username;
        String userid;
        Map<String, FunctionInfo> functions;

        @JsonCreator
        protected WebserviceInfo(
                @JsonProperty(value = "username") final String username,
                @JsonProperty(value = "userid") final String userid,
                @JsonProperty(value = "functions") final Collection<FunctionInfo> functions) {

            this.username = username;
            this.userid = userid;
            this.functions = (functions != null)
                    ? functions
                            .stream()
                            .collect(Collectors.toMap(fi -> fi.name, Function.identity()))
                    : Collections.emptyMap();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private final static class FunctionInfo {
        String name;
        @SuppressWarnings("unused")
        String version;

        @JsonCreator
        protected FunctionInfo(
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "version") final String version) {

            this.name = name;
            this.version = version;
        }
    }

}
