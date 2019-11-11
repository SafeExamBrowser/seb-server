/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;

public class OpenEdxCourseRestriction {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxCourseRestriction.class);

    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO = "/seb-openedx/seb-info";
    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_PATH =
            "/seb-openedx/api/v1/course/%s/configuration/";

    private final LmsSetup lmsSetup;
    private final JSONMapper jsonMapper;
    private final OpenEdxRestTemplateFactory openEdxRestTemplateFactory;

    private OAuth2RestTemplate restTemplate;

    protected OpenEdxCourseRestriction(
            final LmsSetup lmsSetup,
            final JSONMapper jsonMapper,
            final OpenEdxRestTemplateFactory openEdxRestTemplateFactory) {

        this.lmsSetup = lmsSetup;
        this.jsonMapper = jsonMapper;
        this.openEdxRestTemplateFactory = openEdxRestTemplateFactory;
    }

    LmsSetupTestResult initAPIAccess() {

        final LmsSetupTestResult attributesCheck = this.openEdxRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<OAuth2RestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            return LmsSetupTestResult.ofTokenRequestError(
                    "Failed to gain access token from OpenEdX Rest API:\n tried token endpoints: " +
                            this.openEdxRestTemplateFactory.knownTokenAccessPaths);
        }

        final OAuth2RestTemplate restTemplate = restTemplateRequest.get();

        // NOTE: since the OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO endpoint is
        //       not accessible within OAuth2 authentication (just with user - authentication),
        //       we can only check if the endpoint is available for now. This is checked
        //       if there is no 404 response.
        // TODO: Ask eduNEXT to implement also OAuth2 API access for this endpoint to be able
        //       to check the version of the installed plugin.
        final String url = this.lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO;
        try {

            restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Object.class);

        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return LmsSetupTestResult.ofQuizRestrictionAPIError(
                        "Failed to verify course restriction API: " + e.getMessage());
            }

            if (log.isDebugEnabled()) {
                log.debug("Sucessfully checked SEB Open edX integration Plugin");
            }
        }

        return LmsSetupTestResult.ofOkay();
    }

    Result<OpenEdxCourseRestrictionData> pushSebRestriction(
            final String courseId,
            final OpenEdxCourseRestrictionData restriction) {

        if (log.isDebugEnabled()) {
            log.debug("PUT SEB Client restriction on course: {} : {}", courseId, restriction);
        }

        return getRestTemplate()
                .map(restTemplate -> {

                    final String url = this.lmsSetup.lmsApiUrl + getSebRestrictionUrl(courseId);
                    final HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                    try {

                        final String json = this.jsonMapper.writeValueAsString(restriction);
                        final OpenEdxCourseRestrictionData confirm = restTemplate.exchange(
                                url,
                                HttpMethod.PUT,
                                new HttpEntity<>(json, httpHeaders),
                                OpenEdxCourseRestrictionData.class)
                                .getBody();

                        if (log.isDebugEnabled()) {
                            log.debug("Successfully PUT SEB Client restriction on course: {} : {}", courseId, confirm);
                        }

                        return confirm;

                    } catch (final Exception e) {
                        throw new RuntimeException("Unexpected: ", e);
                    }
                });
    }

    Result<Boolean> deleteSebRestriction(final String courseId) {

        if (log.isDebugEnabled()) {
            log.debug("DELETE SEB Client restriction on course: {}", courseId);
        }

        return getRestTemplate()
                .map(restTemplate -> {

                    final String url = this.lmsSetup.lmsApiUrl + getSebRestrictionUrl(courseId);
                    final ResponseEntity<Object> exchange = restTemplate.exchange(
                            url,
                            HttpMethod.DELETE,
                            new HttpEntity<>(new HttpHeaders()),
                            Object.class);

                    if (exchange.getStatusCode() == HttpStatus.NO_CONTENT) {
                        if (log.isDebugEnabled()) {
                            log.debug("Successfully PUT SEB Client restriction on course: {}", courseId);
                        }

                        return true;
                    } else {
                        throw new RuntimeException("Unexpected response for deletion: " + exchange);
                    }

                });
    }

    private String getSebRestrictionUrl(final String courseId) {
        return String.format(OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_PATH, courseId);
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
