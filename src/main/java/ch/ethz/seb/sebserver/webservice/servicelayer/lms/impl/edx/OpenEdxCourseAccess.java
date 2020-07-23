/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.CourseAccess;

/** Implements the LmsAPITemplate for Open edX LMS Course API access.
 *
 * See also: https://course-catalog-api-guide.readthedocs.io */
final class OpenEdxCourseAccess extends CourseAccess {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxCourseAccess.class);

    private static final String OPEN_EDX_DEFAULT_COURSE_ENDPOINT = "/api/courses/v1/courses/";
    private static final String OPEN_EDX_DEFAULT_BLOCKS_ENDPOINT =
            "/api/courses/v1/blocks/?depth=1&all_blocks=true&course_id=";
    private static final String OPEN_EDX_DEFAULT_BLOCKS_TYPE_CHAPTER = "chapter";
    private static final String OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX = "/courses/";
    private static final String OPEN_EDX_DEFAULT_USER_PROFILE_ENDPOINT = "/api/user/v1/accounts?username=";

    private final JSONMapper jsonMapper;
    private final LmsSetup lmsSetup;
    private final OpenEdxRestTemplateFactory openEdxRestTemplateFactory;
    private final WebserviceInfo webserviceInfo;

    private OAuth2RestTemplate restTemplate;

    public OpenEdxCourseAccess(
            final JSONMapper jsonMapper,
            final LmsSetup lmsSetup,
            final OpenEdxRestTemplateFactory openEdxRestTemplateFactory,
            final WebserviceInfo webserviceInfo,
            final AsyncService asyncService) {

        super(asyncService);
        this.jsonMapper = jsonMapper;
        this.lmsSetup = lmsSetup;
        this.openEdxRestTemplateFactory = openEdxRestTemplateFactory;
        this.webserviceInfo = webserviceInfo;
    }

    LmsSetupTestResult initAPIAccess() {

        final LmsSetupTestResult attributesCheck = this.openEdxRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<OAuth2RestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from OpenEdX Rest API:\n tried token endpoints: " +
                    this.openEdxRestTemplateFactory.knownTokenAccessPaths;
            log.error(message, restTemplateRequest.getError());
            return LmsSetupTestResult.ofTokenRequestError(message);
        }

        final OAuth2RestTemplate restTemplate = restTemplateRequest.get();

        try {
            this.getEdxPage(this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT, restTemplate);
        } catch (final RuntimeException e) {

            restTemplate.setAuthenticator(new EdxOAuth2RequestAuthenticator());

            try {
                this.getEdxPage(this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT, restTemplate);
            } catch (final RuntimeException ee) {
                log.error("Failed to access Open edX course API: ", ee);
                return LmsSetupTestResult.ofQuizAccessAPIError(ee.getMessage());
            }
        }

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return Result.tryCatch(() -> {
            final OAuth2RestTemplate template = getRestTemplate()
                    .getOrThrow();

            final String externalStartURI = this.webserviceInfo.getLmsExternalAddressAlias(this.lmsSetup.lmsApiUrl);
            final String uri = (externalStartURI != null)
                    ? externalStartURI + OPEN_EDX_DEFAULT_USER_PROFILE_ENDPOINT + examineeSessionId
                    : this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_USER_PROFILE_ENDPOINT + examineeSessionId;
            final HttpHeaders httpHeaders = new HttpHeaders();
            final String responseJSON = template.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(httpHeaders),
                    String.class)
                    .getBody();

            final EdxUserDetails[] userDetails = this.jsonMapper.<EdxUserDetails[]> readValue(
                    responseJSON,
                    new TypeReference<EdxUserDetails[]>() {
                    });

            if (userDetails == null || userDetails.length <= 0) {
                throw new RuntimeException("No user details on Open edX API request");
            }

            final Map<String, String> additionalAttributes = new HashMap<>();
            additionalAttributes.put("bio", userDetails[0].bio);
            additionalAttributes.put("country", userDetails[0].country);
            additionalAttributes.put("date_joined", userDetails[0].date_joined);
            additionalAttributes.put("gender", userDetails[0].gender);
            additionalAttributes.put("is_active", String.valueOf(userDetails[0].is_active));
            additionalAttributes.put("mailing_address", userDetails[0].mailing_address);
            additionalAttributes.put("secondary_email", userDetails[0].secondary_email);

            return new ExamineeAccountDetails(
                    userDetails[0].username,
                    userDetails[0].name,
                    userDetails[0].username,
                    userDetails[0].email,
                    additionalAttributes);
        });
    }

    @Override
    protected Supplier<List<QuizData>> allQuizzesSupplier() {
        return () -> getRestTemplate()
                .map(this::collectAllQuizzes)
                .getOrThrow();
    }

    @Override
    protected Supplier<Chapters> getCourseChaptersSupplier(final String courseId) {
        return () -> {
            final String uri =
                    this.lmsSetup.lmsApiUrl +
                            OPEN_EDX_DEFAULT_BLOCKS_ENDPOINT +
                            Utils.encodeFormURL_UTF_8(courseId);
            return new Chapters(getCourseBlocks(uri)
                    .getBody().blocks.values()
                            .stream()
                            .filter(block -> OPEN_EDX_DEFAULT_BLOCKS_TYPE_CHAPTER.equals(block.type))
                            .map(block -> new Chapters.Chapter(block.display_name, block.block_id))
                            .collect(Collectors.toList()));
        };
    }

    private ArrayList<QuizData> collectAllQuizzes(final OAuth2RestTemplate restTemplate) {
        final String externalStartURI = getExternalLMSServerAddress(this.lmsSetup);
        return collectAllCourses(
                this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT,
                restTemplate)
                        .stream()
                        .reduce(
                                new ArrayList<>(),
                                (list, courseData) -> {
                                    list.add(quizDataOf(this.lmsSetup, courseData, externalStartURI));
                                    return list;
                                },
                                (list1, list2) -> {
                                    list1.addAll(list2);
                                    return list1;
                                });
    }

    private String getExternalLMSServerAddress(final LmsSetup lmsSetup) {
        final String externalAddressAlias = this.webserviceInfo.getLmsExternalAddressAlias(lmsSetup.lmsApiUrl);
        String _externalStartURI = lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX;
        if (StringUtils.isNoneBlank(externalAddressAlias)) {
            try {
                final URL url = new URL(lmsSetup.lmsApiUrl);
                final int port = url.getPort();
                _externalStartURI = url.getProtocol() +
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

    private List<CourseData> collectAllCourses(final String pageURI, final OAuth2RestTemplate restTemplate) {
        final List<CourseData> collector = new ArrayList<>();
        EdXPage page = getEdxPage(pageURI, restTemplate).getBody();
        if (page != null) {
            collector.addAll(page.results);
            while (page != null && StringUtils.isNotBlank(page.next)) {
                page = getEdxPage(page.next, restTemplate).getBody();
                if (page != null) {
                    collector.addAll(page.results);
                }
            }
        }

        return collector;
    }

    private ResponseEntity<EdXPage> getEdxPage(final String pageURI, final OAuth2RestTemplate restTemplate) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        return restTemplate.exchange(
                pageURI,
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                EdXPage.class);
    }

    private ResponseEntity<Blocks> getCourseBlocks(final String uri) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        return getRestTemplateNoEncoding()
                .getOrThrow()
                .exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(httpHeaders),
                        Blocks.class);
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class EdXPage {
        public Integer count;
        public String previous;
        public Integer num_pages;
        public String next;
        public List<CourseData> results;
    }

    /** Maps the OpenEdX course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CourseData {
        public String id;
        public String name;
        public String short_description;
        public String blocks_url;
        public String start;
        public String end;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Blocks {
        public String root;
        public Map<String, Block> blocks;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Block {
        public String block_id;
        public String display_name;
        public String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class EdxUserDetails {
        final String username;
        final String bio;
        final String name;
        final String secondary_email;
        final String country;
        final Boolean is_active;
        final String gender;
        final String mailing_address;
        final String email;
        final String date_joined;

        protected EdxUserDetails(
                @JsonProperty(value = "username") final String username,
                @JsonProperty(value = "bio") final String bio,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "secondary_email") final String secondary_email,
                @JsonProperty(value = "country") final String country,
                @JsonProperty(value = "is_active") final Boolean is_active,
                @JsonProperty(value = "gender") final String gender,
                @JsonProperty(value = "mailing_address") final String mailing_address,
                @JsonProperty(value = "email") final String email,
                @JsonProperty(value = "date_joined") final String date_joined) {

            this.username = username;
            this.bio = bio;
            this.name = name;
            this.secondary_email = secondary_email;
            this.country = country;
            this.is_active = is_active;
            this.gender = gender;
            this.mailing_address = mailing_address;
            this.email = email;
            this.date_joined = date_joined;
        }
    }

    private static final class EdxOAuth2RequestAuthenticator implements OAuth2RequestAuthenticator {

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

    private Result<OAuth2RestTemplate> getRestTemplateNoEncoding() {
        return this.openEdxRestTemplateFactory
                .createOAuthRestTemplate()
                .map(tempalte -> {
                    final DefaultUriBuilderFactory builderFactory = new DefaultUriBuilderFactory();
                    builderFactory.setEncodingMode(EncodingMode.NONE);
                    tempalte.setUriTemplateHandler(builderFactory);
                    return tempalte;
                });
    }

    private Result<OAuth2RestTemplate> getRestTemplate() {
        if (this.restTemplate == null) {
            final Result<OAuth2RestTemplate> templateRequest = this.openEdxRestTemplateFactory
                    .createOAuthRestTemplate();
            if (templateRequest.hasError()) {
                return templateRequest;
            } else {
                this.restTemplate = templateRequest.get();
            }
        }

        return Result.of(this.restTemplate);
    }

}
