/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.util.function.BooleanSupplier;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSebRestrictionException;

public class OpenEdxCourseRestriction {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxCourseRestriction.class);

    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO = "/seb-openedx/seb-info";
    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_PATH =
            "/seb-openedx/api/v1/course/%s/configuration/";

    private final LmsSetup lmsSetup;
    private final JSONMapper jsonMapper;
    private final OpenEdxRestTemplateFactory openEdxRestTemplateFactory;
    private final int restrictionAPIPushCount;

    private OAuth2RestTemplate restTemplate;

    protected OpenEdxCourseRestriction(
            final LmsSetup lmsSetup,
            final JSONMapper jsonMapper,
            final OpenEdxRestTemplateFactory openEdxRestTemplateFactory,
            final int restrictionAPIPushCount) {

        this.lmsSetup = lmsSetup;
        this.jsonMapper = jsonMapper;
        this.openEdxRestTemplateFactory = openEdxRestTemplateFactory;
        this.restrictionAPIPushCount = restrictionAPIPushCount;
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

    Result<OpenEdxCourseRestrictionData> getSebRestriction(final String courseId) {

        if (log.isDebugEnabled()) {
            log.debug("GET SEB Client restriction on course: {}", courseId);
        }

        return Result.tryCatch(() -> {
            final String url = this.lmsSetup.lmsApiUrl + getSebRestrictionUrl(courseId);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            try {
                final OpenEdxCourseRestrictionData data = this.restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(httpHeaders),
                        OpenEdxCourseRestrictionData.class)
                        .getBody();

                if (log.isDebugEnabled()) {
                    log.debug("Successfully GET SEB Client restriction on course: {}", courseId);
                }
                return data;
            } catch (final HttpClientErrorException ce) {
                if (ce.getStatusCode() == HttpStatus.NOT_FOUND || ce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new NoSebRestrictionException(ce);
                }
                throw ce;
            }
        });
    }

    Result<Boolean> pushSebRestriction(
            final String courseId,
            final OpenEdxCourseRestrictionData restriction) {

        if (log.isDebugEnabled()) {
            log.debug("PUT SEB Client restriction on course: {} : {}", courseId, restriction);
        }

        return handleSebRestriction(processSebRestrictionUpdate(pushSebRestrictionFunction(
                restriction,
                courseId)));
    }

    Result<Boolean> deleteSebRestriction(final String courseId) {

        if (log.isDebugEnabled()) {
            log.debug("DELETE SEB Client restriction on course: {}", courseId);
        }

        return handleSebRestriction(processSebRestrictionUpdate(deleteSebRestrictionFunction(courseId)));
    }

    private BooleanSupplier processSebRestrictionUpdate(final BooleanSupplier restrictionUpdate) {
        return () -> {
            if (this.restrictionAPIPushCount > 0) {
                // NOTE: This is a temporary work-around for SEB Restriction API within Open edX SEB integration plugin to
                //       apply on load-balanced infrastructure or infrastructure that has several layers of cache.
                //       The reason for this is that the API (Open edX system) internally don't apply a resource-change that is
                //       done within HTTP API call immediately from an outside perspective.
                //       After a resource-change on the API is done, the system toggles between the old and the new resource
                //       while constantly calling GET. This usually happens for about a minute or two then it stabilizes on the new resource
                //
                //       This may source on load-balancing or internally caching on Open edX side.
                //       To mitigate this effect the SEB Server can be configured to apply a resource-change on the
                //       API several times in a row to flush as match caches and reach as match as possible server instances.
                //
                //       Since this is a brute-force method to mitigate the problem, this should only be a temporary
                //       work-around until a better solution on Open edX SEB integration side has been found and applied.

                log.warn("SEB restriction update with multiple API push "
                        + "(this is a temporary work-around for SEB Restriction API within Open edX SEB integration plugin)");

                for (int i = 0; i < this.restrictionAPIPushCount; i++) {
                    if (!restrictionUpdate.getAsBoolean()) {
                        Result.ofRuntimeError(
                                "Failed to process SEB restriction update. See logs for more information");
                    }
                }

                return true;
            } else {
                return restrictionUpdate.getAsBoolean();
            }
        };
    }

    private BooleanSupplier pushSebRestrictionFunction(
            final OpenEdxCourseRestrictionData restriction,
            final String courseId) {

        final String url = this.lmsSetup.lmsApiUrl + getSebRestrictionUrl(courseId);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return () -> {
            try {
                final OpenEdxCourseRestrictionData body = this.restTemplate.exchange(
                        url,
                        HttpMethod.PUT,
                        new HttpEntity<>(toJson(restriction), httpHeaders),
                        OpenEdxCourseRestrictionData.class)
                        .getBody();

                if (log.isDebugEnabled()) {
                    log.debug("Successfully PUT SEB Client restriction on course: {} : {}", courseId, body);
                }
            } catch (final Exception e) {
                log.error("Unexpected error while trying to call API for PUT. Course: ", courseId, e);
                return false;
            }

            return true;
        };
    }

    private BooleanSupplier deleteSebRestrictionFunction(final String courseId) {

        final String url = this.lmsSetup.lmsApiUrl + getSebRestrictionUrl(courseId);
        return () -> {
            try {
                final ResponseEntity<Object> exchange = this.restTemplate.exchange(
                        url,
                        HttpMethod.DELETE,
                        new HttpEntity<>(new HttpHeaders()),
                        Object.class);

                if (exchange.getStatusCode() == HttpStatus.NO_CONTENT) {
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully PUT SEB Client restriction on course: {}", courseId);
                    }
                } else {
                    log.error("Unexpected response for deletion: {}", exchange);
                    return false;
                }
            } catch (final Exception e) {
                log.error("Unexpected error while trying to call API for DELETE. Course: ", courseId, e);
                return false;
            }

            return true;
        };
    }

    private Result<Boolean> handleSebRestriction(final BooleanSupplier task) {
        return getRestTemplate()
                .map(restTemplate -> {
                    try {
                        return task.getAsBoolean();
                    } catch (final HttpClientErrorException ce) {
                        if (ce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                            throw new APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of(ce.getMessage()
                                    + " Unable to get access for API. Please check the corresponding LMS Setup "));
                        }
                        throw ce;
                    } catch (final Exception e) {
                        throw new RuntimeException("Unexpected: ", e);
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

    private String toJson(final OpenEdxCourseRestrictionData restriction) {
        try {
            return this.jsonMapper.writeValueAsString(restriction);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Unexpected: ", e);
        }
    }

}
