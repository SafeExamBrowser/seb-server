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

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;

/** The open edX SEB course restriction API implementation.
 *
 * See also : https://seb-openedx.readthedocs.io/en/latest/ */
public class OpenEdxCourseRestriction implements SEBRestrictionAPI {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxCourseRestriction.class);

    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO = "/seb-openedx/seb-info";
    private static final String OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_PATH =
            "/seb-openedx/api/v1/course/%s/configuration/";

    private final JSONMapper jsonMapper;
    private final OpenEdxRestTemplateFactory openEdxRestTemplateFactory;

    private OAuth2RestTemplate restTemplate;

    protected OpenEdxCourseRestriction(
            final JSONMapper jsonMapper,
            final OpenEdxRestTemplateFactory openEdxRestTemplateFactory,
            final int restrictionAPIPushCount) {

        this.jsonMapper = jsonMapper;
        this.openEdxRestTemplateFactory = openEdxRestTemplateFactory;
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {

        final LmsSetupTestResult attributesCheck = this.openEdxRestTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<OAuth2RestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            return LmsSetupTestResult.ofTokenRequestError(
                    LmsType.OPEN_EDX,
                    "Failed to gain access token from OpenEdX Rest API:\n tried token endpoints: " +
                            this.openEdxRestTemplateFactory.knownTokenAccessPaths);
        }

        final OAuth2RestTemplate restTemplate = restTemplateRequest.get();
        try {

            // NOTE: since the OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO endpoint is
            //       not accessible within OAuth2 authentication (just with user - authentication),
            //       we can only check if the endpoint is available for now. This is checked
            //       if there is no 404 response.
            // TODO: Ask eduNEXT to implement also OAuth2 API access for this endpoint to be able
            //       to check the version of the installed plugin.
            final LmsSetup lmsSetup = this.openEdxRestTemplateFactory.apiTemplateDataSupplier.getLmsSetup();
            final String url = lmsSetup.lmsApiUrl + OPEN_EDX_DEFAULT_COURSE_RESTRICTION_API_INFO;

            restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Object.class);

        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return LmsSetupTestResult.ofQuizRestrictionAPIError(
                        LmsType.OPEN_EDX,
                        "Failed to verify course restriction API: " + e.getMessage());
            }

            if (log.isDebugEnabled()) {
                log.debug("Successfully checked SEB Open edX integration Plugin");
            }
        }

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_EDX);
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("GET SEB Client restriction on exam: {}", exam);
        }

        final String courseId = exam.externalId;
        final LmsSetup lmsSetup = this.openEdxRestTemplateFactory.apiTemplateDataSupplier.getLmsSetup();
        return Result.tryCatch(() -> {
            final String url = lmsSetup.lmsApiUrl + getSEBRestrictionUrl(courseId);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

            try {
                final OpenEdxSEBRestriction data = this.restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(httpHeaders),
                        OpenEdxSEBRestriction.class)
                        .getBody();

                if (log.isDebugEnabled()) {
                    log.debug("Successfully GET SEB Client restriction on course: {}", courseId);
                }
                return SEBRestriction.from(exam.id, data);
            } catch (final HttpClientErrorException ce) {
                if (ce.getStatusCode() == HttpStatus.NOT_FOUND || ce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new NoSEBRestrictionException(ce);
                }
                throw ce;
            }
        });
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        if (log.isDebugEnabled()) {
            log.debug("PUT SEB Client restriction on course: {} : {}", externalExamId, sebRestrictionData);
        }

        return Result.tryCatch(() -> {
            final LmsSetup lmsSetup = this.openEdxRestTemplateFactory.apiTemplateDataSupplier.getLmsSetup();
            final String url = lmsSetup.lmsApiUrl + getSEBRestrictionUrl(externalExamId);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            final OpenEdxSEBRestriction body = this
                    .getRestTemplate()
                    .getOrThrow()
                    .exchange(
                            url,
                            HttpMethod.PUT,
                            new HttpEntity<>(toJson(OpenEdxSEBRestriction.from(sebRestrictionData)), httpHeaders),
                            OpenEdxSEBRestriction.class)
                    .getBody();

            if (log.isDebugEnabled()) {
                log.debug("Successfully PUT SEB Client restriction on course: {} : {}", externalExamId, body);
            }

            return sebRestrictionData;
        });
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("DELETE SEB Client restriction on exam: {}", exam);
        }

        final String courseId = exam.externalId;
        return Result.tryCatch(() -> {
            final LmsSetup lmsSetup = this.openEdxRestTemplateFactory.apiTemplateDataSupplier.getLmsSetup();
            final String url = lmsSetup.lmsApiUrl + getSEBRestrictionUrl(courseId);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            final ResponseEntity<Object> exchange = this
                    .getRestTemplate()
                    .getOrThrow()
                    .exchange(
                            url,
                            HttpMethod.DELETE,
                            new HttpEntity<>(httpHeaders),
                            Object.class);

            if (exchange.getStatusCode() == HttpStatus.NO_CONTENT) {
                if (log.isDebugEnabled()) {
                    log.debug("Successfully PUT SEB Client restriction on course: {}", courseId);
                }
                return exam;
            } else {
                throw new RuntimeException("Unexpected response for deletion: " + exchange);
            }
        });

    }

    private String getSEBRestrictionUrl(final String courseId) {
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

    private String toJson(final OpenEdxSEBRestriction restriction) {
        try {
            return this.jsonMapper.writeValueAsString(restriction);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Unexpected: ", e);
        }
    }

}
