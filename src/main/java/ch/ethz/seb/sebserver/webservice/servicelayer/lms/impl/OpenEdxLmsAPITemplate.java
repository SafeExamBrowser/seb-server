/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.async.MemoizingCircuitBreaker;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 *
 * See also: https://course-catalog-api-guide.readthedocs.io */
final class OpenEdxLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxLmsAPITemplate.class);

    private static final String OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH = "/oauth2/access_token";
    private static final String OPEN_EDX_DEFAULT_COURSE_ENDPOINT = "/api/courses/v1/courses/";
    private static final String OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX = "/courses/";

    private final LmsSetup lmsSetup;
    private final ClientCredentials credentials;
    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final ClientCredentialService clientCredentialService;
    private final Set<String> knownTokenAccessPaths;
    private final WebserviceInfo webserviceInfo;

    private OAuth2RestTemplate restTemplate = null;
    private final MemoizingCircuitBreaker<List<QuizData>> allQuizzesSupplier;

    OpenEdxLmsAPITemplate(
            final AsyncService asyncService,
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final String[] alternativeTokenRequestPaths,
            final WebserviceInfo webserviceInfo) {

        this.lmsSetup = lmsSetup;
        this.clientCredentialService = clientCredentialService;
        this.credentials = credentials;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.webserviceInfo = webserviceInfo;
        this.knownTokenAccessPaths = new HashSet<>();
        this.knownTokenAccessPaths.add(OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            this.knownTokenAccessPaths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }

        this.allQuizzesSupplier = asyncService.createMemoizingCircuitBreaker(
                allQuizzesSupplier(),
                3,
                Constants.MINUTE_IN_MILLIS,
                Constants.MINUTE_IN_MILLIS,
                true,
                Constants.HOUR_IN_MILLIS);
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.lmsSetup;
    }

    @Override
    public LmsSetupTestResult testLmsSetup() {

        log.info("Test Lms Binding for OpenEdX and LmsSetup: {}", this.lmsSetup);

        final List<APIMessage> missingAttrs = attributeValidation(this.credentials);
        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(missingAttrs);
        }

        // request OAuth2 access token on OpenEdx API
        initRestTemplateAndRequestAccessToken();
        if (this.restTemplate == null) {
            return LmsSetupTestResult.ofTokenRequestError(
                    "Failed to gain access token from OpenEdX Rest API:\n tried token endpoints: " +
                            this.knownTokenAccessPaths);
        }

        try {
            this.getEdxPage(this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT);
        } catch (final RuntimeException e) {
            if (this.restTemplate != null) {
                this.restTemplate.setAuthenticator(new EdxOAuth2RequestAuthenticator());
            }
            try {
                this.getEdxPage(this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT);
            } catch (final RuntimeException ee) {
                return LmsSetupTestResult.ofQuizRequestError(ee.getMessage());
            }
        }

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this.allQuizzesSupplier.get()
                .map(LmsAPIService.quizzesFilterFunction(filterMap));
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        // TODO this can be improved in the future
        return getQuizzes(new FilterMap())
                .getOrElse(() -> Collections.emptyList())
                .stream()
                .filter(quiz -> ids.contains(quiz.id))
                .map(quiz -> Result.of(quiz))
                .collect(Collectors.toList());
    }

    private Result<LmsSetup> initRestTemplateAndRequestAccessToken() {

        return Result.tryCatch(() -> {
            if (this.restTemplate != null) {
                try {
                    this.restTemplate.getAccessToken();
                    return this.lmsSetup;
                } catch (final Exception e) {
                    log.warn(
                            "Error while trying to get access token within already existing OAuth2RestTemplate instance. Try to create new one.",
                            e);
                    this.restTemplate = null;
                }
            }

            log.info("Initialize Rest Template for OpenEdX API access. LmsSetup: {}", this.lmsSetup);

            final Iterator<String> tokenAccessPaths = this.knownTokenAccessPaths.iterator();
            while (tokenAccessPaths.hasNext()) {
                final String accessTokenRequestPath = tokenAccessPaths.next();
                try {

                    final OAuth2RestTemplate template = createRestTemplate(
                            this.lmsSetup,
                            this.credentials,
                            accessTokenRequestPath);

                    final OAuth2AccessToken accessToken = template.getAccessToken();
                    if (accessToken != null) {
                        this.restTemplate = template;
                        return this.lmsSetup;
                    }
                } catch (final Exception e) {
                    log.info("Failed to request access token on access token request path: {}", accessTokenRequestPath,
                            e);
                }
            }

            throw new IllegalArgumentException(
                    "Unable to establish OpenEdX API connection for lmsSetup: " + this.lmsSetup);
        });
    }

    private OAuth2RestTemplate createRestTemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final String accessTokenRequestPath) {

        final CharSequence plainClientId = credentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService.getPlainClientSecret(credentials);

        final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setAccessTokenUri(lmsSetup.lmsApiUrl + accessTokenRequestPath);
        details.setClientId(plainClientId.toString());
        details.setClientSecret(plainClientSecret.toString());

        // TODO get with proxy configuration if applied in LMSSetup
        final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                .getClientHttpRequestFactory()
                .getOrThrow();

        final OAuth2RestTemplate template = new OAuth2RestTemplate(details);
        template.setRequestFactory(clientHttpRequestFactory);
        template.setAccessTokenProvider(new EdxClientCredentialsAccessTokenProvider());

        return template;
    }

    private Supplier<List<QuizData>> allQuizzesSupplier() {
        return () -> {
            return initRestTemplateAndRequestAccessToken()
                    .map(this::collectAllQuizzes)
                    .getOrThrow();
        };
    }

    private ArrayList<QuizData> collectAllQuizzes(final LmsSetup lmsSetup) {
        final String externalStartURI = getExternalLMSServerAddress(lmsSetup);
        return collectAllCourses(lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT)
                .stream()
                .reduce(
                        new ArrayList<QuizData>(),
                        (list, courseData) -> {
                            list.add(quizDataOf(lmsSetup, courseData, externalStartURI));
                            return list;
                        },
                        (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        });
    }

    private String getExternalLMSServerAddress(final LmsSetup lmsSetup) {
        final String externalAddressAlias = this.webserviceInfo.getExternalAddressAlias(lmsSetup.lmsApiUrl);
        String _externalStartURI = lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX;
        if (StringUtils.isNoneBlank(externalAddressAlias)) {
            try {
                final URL url = new URL(lmsSetup.lmsApiUrl);
                final int port = url.getPort();
                _externalStartURI = this.webserviceInfo.getHttpScheme() +
                        Constants.URL_ADDRESS_SEPARATOR + externalAddressAlias +
                        ((port >= 0)
                                ? Constants.URL_PORT_SEPARATOR + port
                                : StringUtils.EMPTY)
                        + OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX;

                if (log.isDebugEnabled()) {
                    log.debug("Use external address for course access: {}", _externalStartURI);
                }
            } catch (final Exception e) {
                log.error("Failed to create external address from alias: ", e);
            }
        }
        return _externalStartURI;
    }

    private List<CourseData> collectAllCourses(final String pageURI) {
        final List<CourseData> collector = new ArrayList<>();
        EdXPage page = getEdxPage(pageURI).getBody();
        if (page != null) {
            collector.addAll(page.results);
            while (page != null && StringUtils.isNotBlank(page.next)) {
                page = getEdxPage(page.next).getBody();
                if (page != null) {
                    collector.addAll(page.results);
                }
            }
        }

        return collector;
    }

    private ResponseEntity<EdXPage> getEdxPage(final String pageURI) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        return this.restTemplate.exchange(
                pageURI,
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                EdXPage.class);
    }

    private static QuizData quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix) {

        final String startURI = uriPrefix + courseData.id;
        final Map<String, String> additionalAttrs = new HashMap<>();
        additionalAttrs.put("blocks_url", courseData.blocks_url);
        return new QuizData(
                courseData.id,
                lmsSetup.getInstitutionId(),
                lmsSetup.id,
                lmsSetup.getLmsType(),
                courseData.name,
                courseData.short_description,
                courseData.start,
                courseData.end,
                startURI);
    }

    /** Maps a OpenEdX course API course page */
    static final class EdXPage {
        public Integer count;
        public String previous;
        public Integer num_pages;
        public String next;
        public List<CourseData> results;
    }

    /** Maps the OpenEdX course API course data */
    static final class CourseData {
        public String id;
        public String name;
        public String short_description;
        public String blocks_url;
        public String start;
        public String end;
    }

    /** A custom ClientCredentialsAccessTokenProvider that adapts the access token request to Open edX
     * access token request protocol using a form-URL-encoded POST request according to:
     * https://course-catalog-api-guide.readthedocs.io/en/latest/authentication/index.html#getting-an-access-token */
    private class EdxClientCredentialsAccessTokenProvider extends ClientCredentialsAccessTokenProvider {

        @Override
        public OAuth2AccessToken obtainAccessToken(
                final OAuth2ProtectedResourceDetails details,
                final AccessTokenRequest request)
                throws UserRedirectRequiredException,
                AccessDeniedException,
                OAuth2AccessDeniedException {

            if (details instanceof ClientCredentialsResourceDetails) {
                final ClientCredentialsResourceDetails resource = (ClientCredentialsResourceDetails) details;
                final HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("grant_type", "client_credentials");
                params.add("client_id", resource.getClientId());
                params.add("client_secret", resource.getClientSecret());

                final OAuth2AccessToken retrieveToken = retrieveToken(request, resource, params, headers);
                return retrieveToken;
            } else {
                return super.obtainAccessToken(details, request);
            }
        }
    }

    private class EdxOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator {

        @Override
        public void authenticate(
                final OAuth2ProtectedResourceDetails resource,
                final OAuth2ClientContext clientContext,
                final ClientHttpRequest request) {

            final OAuth2AccessToken accessToken = clientContext.getAccessToken();
            if (accessToken == null) {
                throw new AccessTokenRequiredException(resource);
            }

            request.getHeaders().set("Authorization", String.format("%s %s", "Bearer", accessToken.getValue()));
        }

    }

}
