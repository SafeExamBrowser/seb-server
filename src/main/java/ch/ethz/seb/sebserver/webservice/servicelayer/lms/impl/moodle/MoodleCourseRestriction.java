/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

/** GET:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction&courseId=123
 *
 * Response (JSON):
 * {"courseId"="123", "configKeys"=["key1","key2","key3",...], "browserKeys"=["bkey1", "bkey2", "bkey3",...]}
 *
 * Set keys:
 * POST:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction&courseId=123&configKey[0]=key1&configKey[1]=key2&browserKey[0]=bkey1&browserKey[1]=bkey2
 *
 * Delete all key (and remove restrictions):
 * POST:
 * http://yourmoodle.org/webservice/rest/server.php?wstoken={token}&moodlewsrestformat=json&wsfunction=seb_restriction_delete&courseId=123 */
public class MoodleCourseRestriction {

    private static final Logger log = LoggerFactory.getLogger(MoodleCourseRestriction.class);

    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION = "seb_restriction";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_DELETE = "seb_restriction_delete";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID = "courseId";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_CONFIG_KEY = "configKey";
    private static final String MOODLE_DEFAULT_COURSE_RESTRICTION_BROWSER_KEY = "browserKey";

    private final JSONMapper jsonMapper;
    private final LmsSetup lmsSetup;
    private final MoodleRestTemplateFactory moodleRestTemplateFactory;

    private MoodleAPIRestTemplate restTemplate;

    protected MoodleCourseRestriction(
            final JSONMapper jsonMapper,
            final LmsSetup lmsSetup,
            final MoodleRestTemplateFactory moodleRestTemplateFactory) {

        this.jsonMapper = jsonMapper;
        this.lmsSetup = lmsSetup;
        this.moodleRestTemplateFactory = moodleRestTemplateFactory;
    }

    LmsSetupTestResult initAPIAccess() {
        // TODO test availability
        return LmsSetupTestResult.ofQuizAccessAPIError("not available yet");
    }

    Result<MoodleSEBRestriction> getSEBRestriction(final String courseId) {

        if (log.isDebugEnabled()) {
            log.debug("GET SEB Client restriction on course: {}", courseId);
        }

        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);

            final String resultJSON = template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION,
                    queryParams,
                    null);

            final MoodleSEBRestriction restrictiondata = this.jsonMapper.readValue(
                    resultJSON,
                    new TypeReference<MoodleSEBRestriction>() {
                    });

            return restrictiondata;
        });
    }

    Result<Boolean> putSEBRestriction(
            final String courseId,
            final MoodleSEBRestriction restriction) {

        if (log.isDebugEnabled()) {
            log.debug("PUT SEB Client restriction on course: {} : {}", courseId, restriction);
        }

        return Result.tryCatch(() -> {

            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);

            final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.addAll(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_CONFIG_KEY,
                    new ArrayList<>(restriction.configKeys));
            queryAttributes.addAll(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_BROWSER_KEY,
                    new ArrayList<>(restriction.browserExamKeys));

            template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION,
                    queryParams,
                    queryAttributes);

            return true;
        });
    }

    Result<Boolean> deleteSEBRestriction(final String courseId) {

        if (log.isDebugEnabled()) {
            log.debug("DELETE SEB Client restriction on course: {}", courseId);
        }

        return Result.tryCatch(() -> {
            final MoodleAPIRestTemplate template = getRestTemplate()
                    .getOrThrow();

            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(MOODLE_DEFAULT_COURSE_RESTRICTION_COURSE_ID, courseId);

            template.callMoodleAPIFunction(
                    MOODLE_DEFAULT_COURSE_RESTRICTION_WS_FUNCTION_DELETE,
                    queryParams,
                    null);

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

}
