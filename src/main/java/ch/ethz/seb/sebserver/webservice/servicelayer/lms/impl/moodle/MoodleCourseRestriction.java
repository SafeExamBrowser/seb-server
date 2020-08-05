/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

/** GET:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction&courseId=123
 *
 * Response (JSON):
 *
 * <pre>
 * {
 *   "courseId": "123",
 *   "quizId": "456",
 *   "configKeys": [
 *       "key1",
 *       "key2",
 *       "key3"
 *   ],
 *   "browserKeys": [
 *       "bkey1",
 *       "bkey2",
 *       "bkey3"
 *  ]
 * }
 * </pre>
 *
 * Set keys:
 * POST:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction_update&courseId=123&configKey[0]=key1&configKey[1]=key2&browserKey[0]=bkey1&browserKey[1]=bkey2
 *
 * Delete all key (and remove restrictions):
 * POST:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction_delete&courseId=123 */
public class MoodleCourseRestriction {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseRestriction.class);

    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION = "seb_restriction";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_CREATE = "seb_restriction_create";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_UPDATE = "seb_restriction_update";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_DELETE = "seb_restriction_delete";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID = "courseId";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_QUIZ_ID = "quizId";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_CONFIG_KEY = "configKey";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_BROWSER_KEY = "browserKey";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory moodleRestTemplateFactory;

    private MoodleAPIRestTemplate restTemplate;

    protected MoodleCourseRestriction(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory moodleRestTemplateFactory) {

        this.jsonMapper = jsonMapper;
        this.moodleRestTemplateFactory = moodleRestTemplateFactory;
    }

    LmsSetupTestResult initAPIAccess() {
        // try to call the SEB Restrictions API
        try {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final String jsonResponse = template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION,
                    new LinkedMultiValueMap<>(),
                    null);

            final Error checkError = this.checkError(jsonResponse);
            if (checkError != null) {
                return LmsSetupTestResult.ofQuizRestrictionAPIError(checkError.exception);
            }

        } catch (final Exception e) {
            log.debug("Moodle SEB restriction API not available: ", e);
            return LmsSetupTestResult.ofQuizRestrictionAPIError(e.getMessage());
        }
        return LmsSetupTestResult.ofOkay();
    }

    Result<MoodleSEBRestriction> getSEBRestriction(
            final String externalId) {

        return Result.tryCatch(() -> {
            final String[] courseQuizId = StringUtils.split(externalId, ":");
            if (courseQuizId.length > 1) {
                // we only have the course id (this is a course)
                return getSEBRestriction(courseQuizId[0], null)
                        .getOrThrow();
            } else {
                // we have the course id and the quiz is (this is a quiz)
                return getSEBRestriction(courseQuizId[0], courseQuizId[1])
                        .getOrThrow();
            }
        });
    }

    Result<MoodleSEBRestriction> getSEBRestriction(
            final String courseId,
            final String quizId) {

        if (log.isDebugEnabled()) {
            log.debug("GET SEB Client restriction on course: {} quiz: {}", courseId, quizId);
        }

        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);
            if (quizId != null) {
                queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_QUIZ_ID, quizId);
            }

            final String resultJSON = template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION,
                    queryParams,
                    null);

            final Error error = this.checkError(resultJSON);
            if (error != null) {
                log.error("Failed to get SEB restriction: {}", error.toString());
                throw new NoSEBRestrictionException("Failed to get SEB restriction: " + error.exception);
            }

            final MoodleSEBRestriction restrictiondata = this.jsonMapper.readValue(
                    resultJSON,
                    new TypeReference<MoodleSEBRestriction>() {
                    });

            return restrictiondata;
        });
    }

    Result<MoodleSEBRestriction> createSEBRestriction(
            final String externalId,
            final MoodleSEBRestriction restriction) {

        return Result.tryCatch(() -> {
            final String[] courseQuizId = StringUtils.split(externalId, ":");
            if (courseQuizId.length > 1) {
                // we only have the course id (this is a course)
                return createSEBRestriction(courseQuizId[0], null, restriction)
                        .getOrThrow();
            } else {
                // we have the course id and the quiz is (this is a quiz)
                return createSEBRestriction(courseQuizId[0], courseQuizId[1], restriction)
                        .getOrThrow();
            }
        });
    }

    Result<MoodleSEBRestriction> createSEBRestriction(
            final String courseId,
            final String quizId,
            final MoodleSEBRestriction restriction) {

        if (log.isDebugEnabled()) {
            log.debug("POST SEB Client restriction on course: {} quiz: restriction : {}",
                    courseId,
                    quizId,
                    restriction);
        }

        return postSEBRestriction(
                courseId,
                quizId,
                MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_CREATE,
                restriction);
    }

    Result<MoodleSEBRestriction> updateSEBRestriction(
            final String externalId,
            final MoodleSEBRestriction restriction) {

        return Result.tryCatch(() -> {
            final String[] courseQuizId = StringUtils.split(externalId, ":");
            if (courseQuizId.length > 1) {
                // we only have the course id (this is a course)
                return updateSEBRestriction(courseQuizId[0], null, restriction)
                        .getOrThrow();
            } else {
                // we have the course id and the quiz is (this is a quiz)
                return updateSEBRestriction(courseQuizId[0], courseQuizId[1], restriction)
                        .getOrThrow();
            }
        });
    }

    Result<MoodleSEBRestriction> updateSEBRestriction(
            final String courseId,
            final String quizId,
            final MoodleSEBRestriction restriction) {

        if (log.isDebugEnabled()) {
            log.debug("POST SEB Client restriction on course: {} quiz: restriction : {}",
                    courseId,
                    quizId,
                    restriction);
        }

        return postSEBRestriction(
                courseId,
                quizId,
                MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_UPDATE,
                restriction);
    }

    Result<Boolean> deleteSEBRestriction(
            final String externalId) {

        return Result.tryCatch(() -> {
            final String[] courseQuizId = StringUtils.split(externalId, ":");
            if (courseQuizId.length > 1) {
                // we only have the course id (this is a course)
                return deleteSEBRestriction(courseQuizId[0], null)
                        .getOrThrow();
            } else {
                // we have the course id and the quiz is (this is a quiz)
                return deleteSEBRestriction(courseQuizId[0], courseQuizId[1])
                        .getOrThrow();
            }
        });
    }

    Result<Boolean> deleteSEBRestriction(
            final String courseId,
            final String quizId) {

        if (log.isDebugEnabled()) {
            log.debug("DELETE SEB Client restriction on course: {} quizId {}", courseId, quizId);
        }

        return Result.tryCatch(() -> {
            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_QUIZ_ID, quizId);

            final String jsonResponse = template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_DELETE,
                    queryParams,
                    null);

            final Error error = this.checkError(jsonResponse);
            if (error != null) {
                log.error("Failed to delete SEB restriction: {}", error.toString());
                return false;
            }

            return true;
        });
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        if (this.restTemplate == null) {
            final Result<MoodleAPIRestTemplate> templateRequest = this.moodleRestTemplateFactory
                    .createRestTemplate();
            if (templateRequest.hasError()) {
                return templateRequest;
            } else {
                this.restTemplate = templateRequest.get();
            }
        }

        return Result.of(this.restTemplate);
    }

    private Result<MoodleSEBRestriction> postSEBRestriction(
            final String courseId,
            final String quizId,
            final String function,
            final MoodleSEBRestriction restriction) {
        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_QUIZ_ID, quizId);

            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.addAll(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_CONFIG_KEY,
                    new ArrayList<>(restriction.configKeys));
            queryAttributes.addAll(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_BROWSER_KEY,
                    new ArrayList<>(restriction.browserExamKeys));

            final String resultJSON = template.callMoodleAPIFunction(
                    function,
                    queryParams,
                    queryAttributes);

            final Error error = this.checkError(resultJSON);
            if (error != null) {
                log.error("Failed to post SEB restriction: {}", error.toString());
                throw new NoSEBRestrictionException("Failed to post SEB restriction: " + error.exception);
            }

            final MoodleSEBRestriction restrictiondata = this.jsonMapper.readValue(
                    resultJSON,
                    new TypeReference<MoodleSEBRestriction>() {
                    });

            return restrictiondata;
        });
    }

    public Error checkError(final String jsonResponse) {
        if (jsonResponse.contains("exception") || jsonResponse.contains("errorcode")) {
            try {
                return this.jsonMapper.readValue(
                        jsonResponse,
                        new TypeReference<Error>() {
                        });
            } catch (final Exception e) {
                log.error("Failed to parse error response: {} cause: ", jsonResponse, e);
                return null;
            }
        }

        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Error {
        public final String exception;
        public final String errorcode;
        public final String message;

        @JsonCreator
        Error(
                @JsonProperty(value = "exception") final String exception,
                @JsonProperty(value = "errorcode") final String errorcode,
                @JsonProperty(value = "message") final String message) {
            this.exception = exception;
            this.errorcode = errorcode;
            this.message = message;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Error [exception=");
            builder.append(this.exception);
            builder.append(", errorcode=");
            builder.append(this.errorcode);
            builder.append(", message=");
            builder.append(this.message);
            builder.append("]");
            return builder.toString();
        }
    }

}
