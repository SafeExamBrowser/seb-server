/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

final class OpenEdxLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxLmsAPITemplate.class);

    private static final String OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH = "/oauth2/access_token";
    private static final String OPEN_EDX_DEFAULT_COURSE_ENDPOINT = "/api/courses/v1/courses/";
    private static final String OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX = "/courses/";

    private final String lmsSetupId;
    private final LmsSetupDAO lmsSetupDAO;
    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final ClientCredentialService clientCredentialService;
    private final Set<String> knownTokenAccessPaths;

    private OAuth2RestTemplate restTemplate = null;

    OpenEdxLmsAPITemplate(
            final String lmsSetupId,
            final LmsSetupDAO lmsSetupDAO,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactory clientHttpRequestFactory,
            final String[] alternativeTokenRequestPaths) {

        this.lmsSetupId = lmsSetupId;
        this.lmsSetupDAO = lmsSetupDAO;
        this.clientHttpRequestFactory = clientHttpRequestFactory;
        this.clientCredentialService = clientCredentialService;

        this.knownTokenAccessPaths = new HashSet<>();
        this.knownTokenAccessPaths.add(OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            this.knownTokenAccessPaths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }
    }

    @Override
    public Result<LmsSetup> lmsSetup() {
        return this.lmsSetupDAO
                .byModelId(this.lmsSetupId);
    }

    @Override
    public LmsSetupTestResult testLmsSetup() {

        final LmsSetup lmsSetup = lmsSetup().getOrThrow();

        log.info("Test Lms Binding for OpenEdX and LmsSetup: {}", lmsSetup);

        // validation of LmsSetup
        if (lmsSetup.lmsType != LmsType.MOCKUP) {
            return LmsSetupTestResult.ofMissingAttributes(LMS_SETUP.ATTR_LMS_TYPE);
        }
        final List<String> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(LMS_SETUP.ATTR_LMS_TYPE);
        }
        if (StringUtils.isBlank(lmsSetup.getLmsAuthName())) {
            missingAttrs.add(LMS_SETUP.ATTR_LMS_CLIENTNAME);
        }
        if (StringUtils.isBlank(lmsSetup.getLmsAuthSecret())) {
            missingAttrs.add(LMS_SETUP.ATTR_LMS_CLIENTSECRET);
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(missingAttrs);
        }

        // request OAuth2 access token on OpenEdx API
        initRestTemplateAndRequestAccessToken(lmsSetup);
        if (this.restTemplate == null) {
            return LmsSetupTestResult.ofTokenRequestError(
                    "Failed to gain access token form OpenEdX Rest API: tried token endpoints: " +
                            this.knownTokenAccessPaths);
        }

        // query quizzes TODO!?

        return LmsSetupTestResult.ofOkay();
    }

    @Override
    public Result<Page<QuizData>> getQuizzes(
            final String name,
            final Long from,
            final String sort,
            final int pageNumber,
            final int pageSize) {

        return this.lmsSetup()
                .flatMap(this::initRestTemplateAndRequestAccessToken)
                .map(lmsSetup -> {

                    // TODO sort and pagination
                    final HttpHeaders httpHeaders = new HttpHeaders();

                    final ResponseEntity<EdXPage> response = this.restTemplate.exchange(
                            lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_ENDPOINT,
                            HttpMethod.GET,
                            new HttpEntity<>(httpHeaders),
                            EdXPage.class);
                    final EdXPage edxpage = response.getBody();

                    final List<QuizData> content = edxpage.results
                            .stream()
                            .reduce(
                                    new ArrayList<QuizData>(),
                                    (list, courseData) -> {
                                        list.add(quizDataOf(lmsSetup, courseData));
                                        return list;
                                    },
                                    (list1, list2) -> {
                                        list1.addAll(list2);
                                        return list1;
                                    });

                    return new Page<>(edxpage.num_pages, pageNumber, sort, content);
                });
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void reset() {
        this.restTemplate = null;
    }

    private Result<LmsSetup> initRestTemplateAndRequestAccessToken(final LmsSetup lmsSetup) {

        log.info("Initialize Rest Template for OpenEdX API access. LmsSetup: {}", lmsSetup);

        return Result.tryCatch(() -> {
            if (this.restTemplate != null) {
                try {
                    this.restTemplate.getAccessToken();
                    return lmsSetup;
                } catch (final Exception e) {
                    log.warn(
                            "Error while trying to get access token within already existing OAuth2RestTemplate instance. Try to create new one.",
                            e);
                    this.restTemplate = null;
                }
            }

            final ClientCredentials credentials = this.lmsSetupDAO
                    .getLmsAPIAccessCredentials(this.lmsSetupId)
                    .getOrThrow();

            final Iterator<String> tokenAccessPaths = this.knownTokenAccessPaths.iterator();
            while (tokenAccessPaths.hasNext()) {
                final String accessTokenRequestPath = tokenAccessPaths.next();
                try {

                    final OAuth2RestTemplate template = createRestTemplate(
                            lmsSetup,
                            credentials,
                            accessTokenRequestPath);

                    final OAuth2AccessToken accessToken = template.getAccessToken();
                    if (accessToken != null) {
                        this.restTemplate = template;
                        return lmsSetup;
                    }
                } catch (final Exception e) {
                    log.info("Failed to request access token on access token request path: {}", accessTokenRequestPath,
                            e);
                }
            }

            throw new IllegalArgumentException("Unable to establish OpenEdX API connection for lmsSetup: " + lmsSetup);
        });
    }

    private OAuth2RestTemplate createRestTemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final String accessTokenRequestPath) {

        final CharSequence plainClientId = this.clientCredentialService.getPlainClientId(credentials);
        final CharSequence plainClientSecret = this.clientCredentialService.getPlainClientSecret(credentials);

        final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setAccessTokenUri(lmsSetup.lmsApiUrl + accessTokenRequestPath);
        details.setClientId(plainClientId.toString());
        details.setClientSecret(plainClientSecret.toString());
        details.setGrantType("client_credentials");

        // TODO: accordingly to the documentation (https://course-catalog-api-guide.readthedocs.io/en/latest/authentication/#create-an-account-on-edx-org-for-api-access)
        //      token_type=jwt is needed for token request but is it possible to set this within ClientCredentialsResourceDetails
        //      or within the request header on API call. To clarify

        final OAuth2RestTemplate template = new OAuth2RestTemplate(details);
        template.setRequestFactory(this.clientHttpRequestFactory);
        return template;
    }

    private QuizData quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData) {

        final String startURI = lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_START_URL_PREFIX + courseData.id;
        return new QuizData(
                courseData.id,
                courseData.name,
                courseData.short_description,
                courseData.start,
                courseData.end,
                startURI);
    }

    static final class EdXPage {
        public Integer count;
        public Integer previous;
        public Integer num_pages;
        public Integer next;
        public List<CourseData> results;
    }

    static final class CourseData {
        public String id;
        public String course_id;
        public String name;
        public String short_description;
        public String blocks_url;
        public String start;
        public String end;
    }

    /*
     * pagination
     * count 2
     * previous null
     * num_pages 1
     * next null
     * results
     * 0
     * blocks_url "http://ralph.ethz.ch:18000/api/courses/v1/blocks/?course_id=course-v1%3AedX%2BDemoX%2BDemo_Course"
     * effort null
     * end null
     * enrollment_start null
     * enrollment_end null
     * id "course-v1:edX+DemoX+Demo_Course"
     * media
     * course_image
     * uri "/asset-v1:edX+DemoX+Demo_Course+type@asset+block@images_course_image.jpg"
     * course_video
     * uri null
     * image
     * raw "http://ralph.ethz.ch:18000/asset-v1:edX+DemoX+Demo_Course+type@asset+block@images_course_image.jpg"
     * small "http://ralph.ethz.ch:18000/asset-v1:edX+DemoX+Demo_Course+type@asset+block@images_course_image.jpg"
     * large "http://ralph.ethz.ch:18000/asset-v1:edX+DemoX+Demo_Course+type@asset+block@images_course_image.jpg"
     * name "edX Demonstration Course"
     * number "DemoX"
     * org "edX"
     * short_description null
     * start "2013-02-05T05:00:00Z"
     * start_display "Feb. 5, 2013"
     * start_type "timestamp"
     * pacing "instructor"
     * mobile_available false
     * hidden false
     * invitation_only false
     * course_id "course-v1:edX+DemoX+Demo_Course"
     */

}
