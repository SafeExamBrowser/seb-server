/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseRestriction;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginFullIntegration;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MooldePluginLmsAPITemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
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

public class MoodleRestTemplateFactoryImpl implements MoodleRestTemplateFactory {

    private static final Logger log = LoggerFactory.getLogger(MoodleRestTemplateFactoryImpl.class);

    public final JSONMapper jsonMapper;
    public final APITemplateDataSupplier apiTemplateDataSupplier;
    public final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    public final ClientCredentialService clientCredentialService;
    public final Set<String> knownTokenAccessPaths;

    private Result<MoodleAPIRestTemplate> activeRestTemplate = Result.ofRuntimeError("Not Initialized");

    public MoodleRestTemplateFactoryImpl(
            final JSONMapper jsonMapper,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final String[] alternativeTokenRequestPaths) {

        this.jsonMapper = jsonMapper;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        final Set<String> paths = new HashSet<>();
        paths.add(MoodleAPIRestTemplate.MOODLE_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            paths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }
        this.knownTokenAccessPaths = Utils.immutableSetOf(paths);
    }

    @Override
    public Set<String> getKnownTokenAccessPaths() {
        return this.knownTokenAccessPaths;
    }

    @Override
    public Result<MoodleAPIRestTemplate> getRestTemplate() {
        if (activeRestTemplate.hasError()) {
            createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME);
        }
        return activeRestTemplate;
    }

    @Override
    public APITemplateDataSupplier getApiTemplateDataSupplier() {
        return this.apiTemplateDataSupplier;
    }

    @Override
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
            if (!credentials.hasAccessToken()) {
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
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.MOODLE, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE);
    }

    @Override
    public Result<MoodleAPIRestTemplate> createRestTemplate(final String service) {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        this. activeRestTemplate = this.knownTokenAccessPaths
                .stream()
                .map(path -> this.createRestTemplate(service, path))
                .peek(result -> {
                    if (result.hasError()) {
                        log.warn("Failed to get access token for LMS: {}({}), error {}",
                                lmsSetup.name,
                                lmsSetup.id,
                                result.getError().getMessage());
                    }
                })
                .filter(Result::hasValue)
                .findFirst()
                .orElse(Result.ofRuntimeError(
                        "Failed to gain any access for LMS " +
                                lmsSetup.name + "(" + lmsSetup.id +
                                ") on paths: " + this.knownTokenAccessPaths));

        log.info("Created new MoodleAPIRestTemplate for service: {} factory: {}", service, this.hashCode());

        return activeRestTemplate;
    }

    @Override
    public Result<MoodleAPIRestTemplate> createRestTemplate(final String service, final String accessTokenPath) {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        return Result.tryCatch(() -> {
            final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
            final ProxyData proxyData = this.apiTemplateDataSupplier.getProxyData();

            final CharSequence plainClientId = credentials.clientId;
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(credentials)
                    .getOr(StringUtils.EMPTY);
            final CharSequence plainAPIToken = this.clientCredentialService
                    .getPlainAccessToken(credentials)
                    .getOr(StringUtils.EMPTY);

            final MoodleAPIRestTemplateImpl restTemplate = new MoodleAPIRestTemplateImpl(
                    this.jsonMapper,
                    this.apiTemplateDataSupplier,
                    lmsSetup.lmsApiUrl,
                    accessTokenPath,
                    service,
                    plainAPIToken,
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

            activeRestTemplate = Result.of(restTemplate);
            return restTemplate;
        });
    }

    public static class MoodleAPIRestTemplateImpl extends RestTemplate implements MoodleAPIRestTemplate {

        private static final String REST_API_TEST_FUNCTION = "core_webservice_get_site_info";

        final JSONMapper jsonMapper;
        final APITemplateDataSupplier apiTemplateDataSupplier;

        private final String serverURL;
        private final String tokenPath;

        private final CharSequence apiToken;
        private CharSequence accessToken;

        private final Map<String, String> tokenReqURIVars;
        private final HttpEntity<?> tokenReqEntity = new HttpEntity<>(new LinkedMultiValueMap<>());

        private MoodlePluginVersion moodlePluginVersion = null;

        protected MoodleAPIRestTemplateImpl(
                final JSONMapper jsonMapper,
                final APITemplateDataSupplier apiTemplateDataSupplier,
                final String serverURL,
                final String tokenPath,
                final String service,
                final CharSequence apiToken,
                final CharSequence username,
                final CharSequence password) {

            this.jsonMapper = jsonMapper;
            this.apiTemplateDataSupplier = apiTemplateDataSupplier;

            this.serverURL = serverURL;
            this.tokenPath = tokenPath;
            this.apiToken = apiToken;
            this.accessToken = StringUtils.isNotBlank(apiToken) ? apiToken : null;

            this.tokenReqURIVars = new HashMap<>();
            this.tokenReqURIVars.put(URI_VAR_USER_NAME, String.valueOf(username));
            this.tokenReqURIVars.put(URI_VAR_PASSWORD, String.valueOf(password));
            this.tokenReqURIVars.put(URI_VAR_SERVICE, service);
        }

        @Override
        public String getService() {
            return this.tokenReqURIVars.get(URI_VAR_SERVICE);
        }

        @Override
        public MoodlePluginVersion getMoodlePluginVersion() {
            if (moodlePluginVersion == null) {
                try {
                    final String apiInfo = this.callMoodleAPIFunction(REST_API_TEST_FUNCTION);
                    final WebserviceInfo webserviceInfo = this.jsonMapper.readValue(
                            apiInfo,
                            WebserviceInfo.class);
                    if (webserviceInfo.functions.containsKey(MoodlePluginCourseRestriction.RESTRICTION_SET_FUNCTION_NAME)) {
                        if (webserviceInfo.functions.containsKey(MoodlePluginFullIntegration.FUNCTION_NAME_SEBSERVER_CONNECTION)) {
                            this.moodlePluginVersion = MoodlePluginVersion.V2_0;
                        } else {
                            this.moodlePluginVersion = MoodlePluginVersion.V1_0;
                        }
                    } else {
                        this.moodlePluginVersion = MoodlePluginVersion.NONE;
                    }
                } catch (final Exception e) {
                    log.warn("Failed to verify MoodlePluginVersion. Error: {}", e.getMessage());
                }
            }
            return moodlePluginVersion;
        }

        @Override
        public CharSequence getAccessToken() {
            if (this.accessToken == null) {
                requestAccessToken();
            }

            return this.accessToken;
        }

        @Override
        public void testAPIConnection(final String... functions) {
            try {
                final String apiInfo = this.callMoodleAPIFunction(REST_API_TEST_FUNCTION);
                final WebserviceInfo webserviceInfo = this.jsonMapper.readValue(
                        apiInfo,
                        WebserviceInfo.class);

                if (StringUtils.isBlank(webserviceInfo.username) || StringUtils.isBlank(webserviceInfo.userid)) {
                    if (apiInfo != null && (apiInfo.startsWith("{exception") || apiInfo.contains("\"exception\":"))) {
                        if (apiInfo.contains("sitemaintenance")) {
                            throw new RuntimeException("Moodle is currently in maintenance mode!");
                        }
                        throw new RuntimeException("Moodle respond with error: " + apiInfo);
                    }
                    throw new RuntimeException("Invalid WebserviceInfo Response: " + apiInfo);
                }

                if (functions != null) {

                    final List<String> missingAPIFunctions = Arrays.stream(functions)
                            .filter(f -> !webserviceInfo.functions.containsKey(f))
                            .toList();

                    if (!missingAPIFunctions.isEmpty()) {
                        throw new RuntimeException("Missing Moodle Webservice API functions: " + missingAPIFunctions);
                    }
                }

            } catch (final RuntimeException re) {
                log.warn("Failed to Moodle API access: {}", re.getMessage());
                throw re;
            } catch (final Exception e) {
                log.warn("Failed to Moodle API access: {}", e.getMessage());
                throw new RuntimeException("Failed to test Moodle rest API: ", e);
            }
        }

        @Override
        public String callMoodleAPIFunction(final String functionName) {
            return callMoodleAPIFunction(functionName, null, null);
        }

        @Override
        public String callMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryAttributes) {
            return callMoodleAPIFunction(functionName, null, queryAttributes);
        }

        @Override
        public String postToMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryParams,
                final Map<String, Map<String, String>> queryAttributes) {
            getAccessToken();

            final UriComponentsBuilder queryParam = UriComponentsBuilder
                    .fromHttpUrl(this.serverURL + MOODLE_DEFAULT_REST_API_PATH)
                    .queryParam(REST_REQUEST_TOKEN_NAME, this.accessToken)
                    .queryParam(REST_REQUEST_FUNCTION_NAME, functionName)
                    .queryParam(REST_REQUEST_FORMAT_NAME, "json");

            if (queryParams != null && !queryParams.isEmpty()) {
                queryParam.queryParams(queryParams);
            }

            final String body = createMoodleFormPostBody(queryAttributes);

            if (log.isDebugEnabled()) {
                try {
                    final String uriString = URLDecoder.decode(queryParam.toUriString(), "UTF8");
                    log.info("POST To Moodle URI (decoded UTF8): {}, body: {}", uriString, body);
                } catch (final Exception e) {
                    // ignore
                }
            }

            final HttpHeaders headers = new HttpHeaders();
            headers.set(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            final HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

            return doRequest(functionName, queryParam, true, httpEntity);
        }

        private String createMoodleFormPostBody(final Map<String, Map<String, String>> queryAttributes) {
            if (queryAttributes == null) {
                return null;
            }

            final StringBuffer sb = new StringBuffer();
            queryAttributes.forEach(
                    (name1, value1) -> value1.forEach(
                            (key, value) -> sb.append(name1).append("[").append(key).append("]=").append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&")));

            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
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

            //final boolean usePOST = queryAttributes != null && !queryAttributes.isEmpty();
            final HttpEntity<?> functionReqEntity;
            if ( queryAttributes != null && !queryAttributes.isEmpty()) {
                final HttpHeaders headers = new HttpHeaders();
                headers.set(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                final String body = Utils.toAppFormUrlEncodedBody(queryAttributes);
                functionReqEntity = new HttpEntity<>(body, headers);

            } else {
                functionReqEntity = new HttpEntity<>(new LinkedMultiValueMap<>());
            }

            return doRequest(functionName, queryParam, true, functionReqEntity);
        }

        @Override
        public String uploadMultiPart(
                final String uploadEndpoint,
                final String quizId,
                final String fileName,
                final byte[] configData) {

            final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
            final StringBuilder uri = new StringBuilder(lmsSetup.lmsApiUrl + uploadEndpoint);
            getAccessToken();

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            final MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
            final ContentDisposition contentDisposition = ContentDisposition
                    .builder("form-data")
                    .name("file")
                    .filename(fileName)
                    .build();
            fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
            final HttpEntity<byte[]> fileEntity = new HttpEntity<>(configData, fileMap);

            final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("token", this.accessToken.toString());
            body.add("quizid", quizId);
            body.add("file", fileEntity);

            final HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            final ResponseEntity<String> exchange = super.exchange(
                    uri.append("?token=").append(this.accessToken).toString(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            return exchange.getBody();
        }

        private String doRequest(
                final String functionName,
                final UriComponentsBuilder queryParam,
                final boolean usePOST,
                final HttpEntity<?> functionReqEntity) {

            final ResponseEntity<String> response = super.exchange(
                    queryParam.toUriString(),
                    usePOST ? HttpMethod.POST : HttpMethod.GET,
                    functionReqEntity,
                    String.class);

            final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(
                        "Failed to call Moodle webservice API function: " + functionName + " lms setup: " +
                                lmsSetup.name + " response: " + response.getBody());
            }

            final String body = response.getBody();

            // NOTE: for some unknown reason, Moodles API error responses come with a 200 OK response HTTP Status
            //       So this is a special Moodle specific error handling here...
            if (body != null && (body.startsWith("{exception") || body.contains("\"exception\":"))) {
                // if no courses has been found for this page, just return (Plugin)
                if (body.contains("nocoursefound")) {
                    return body;
                }
                // Reset access token to get new on next call (fix access if token is expired)
                // NOTE: find a way to verify token invalidity response from Moodle.
                //      Unfortunately there is not a lot of Moodle documentation for the API error handling around.
                this.accessToken = null;
                log.warn(
                        "Failed to call Moodle webservice API function: {} lms setup: {} response: {}",
                        functionName, lmsSetup.name, body);
            }

            return body;
        }

        private void requestAccessToken() {

            if (StringUtils.isNotBlank(this.apiToken)) {
                this.accessToken = this.apiToken;
                return;
            }

            final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
            try {

                final ResponseEntity<String> response = super.exchange(
                        this.serverURL + this.tokenPath,
                        HttpMethod.POST,
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
                    final MoodleToken moodleToken = this.jsonMapper.readValue(
                            response.getBody(),
                            MoodleToken.class);

                    if (moodleToken == null || moodleToken.token == null) {
                        throw new RuntimeException("Access Token request with 200 but no or invalid token body");
                    } else {
                        log.info("Successfully get access token from Moodle: {}", lmsSetup.name);
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
        private MoodleToken(
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
        private WebserviceInfo(
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
        private FunctionInfo(
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "version") final String version) {

            this.name = name;
            this.version = version;
        }
    }

}
