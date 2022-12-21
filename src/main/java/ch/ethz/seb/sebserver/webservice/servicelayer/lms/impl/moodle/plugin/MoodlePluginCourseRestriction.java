/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleQuizRestriction;

public class MoodlePluginCourseRestriction implements SEBRestrictionAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginCourseRestriction.class);

    public static final String RESTRICTION_GET_FUNCTION_NAME = "quizaccess_sebserver_get_restriction";
    public static final String RESTRICTION_SET_FUNCTION_NAME = "quizaccess_sebserver_set_restriction";

    public static final String ATTRIBUTE_QUIZ_ID = "quiz_id";
    public static final String ATTRIBUTE_CONFIG_KEYS = "config_keys";
    public static final String ATTRIBUTE_BROWSER_EXAM_KEYS = "browser_exam_keys";
    public static final String ATTRIBUTE_QUIT_URL = "quit_link";
    public static final String ATTRIBUTE_QUIT_SECRET = "quit_secret";

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;
    private final ExamConfigurationValueService examConfigurationValueService;

    private MoodleAPIRestTemplate restTemplate;

    public MoodlePluginCourseRestriction(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory restTemplateFactory,
            final ExamConfigurationValueService examConfigurationValueService) {

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;
        this.examConfigurationValueService = examConfigurationValueService;
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {

        final LmsSetupTestResult attributesCheck = this.restTemplateFactory.test();
        if (!attributesCheck.isOk()) {
            return attributesCheck;
        }

        final Result<MoodleAPIRestTemplate> restTemplateRequest = getRestTemplate();
        if (restTemplateRequest.hasError()) {
            final String message = "Failed to gain access token from Moodle Rest API:\n tried token endpoints: " +
                    this.restTemplateFactory.getKnownTokenAccessPaths();
            log.error(message + " cause: {}", restTemplateRequest.getError().getMessage());
            return LmsSetupTestResult.ofTokenRequestError(LmsType.MOODLE_PLUGIN, message);
        }

        try {
            final MoodleAPIRestTemplate restTemplate = restTemplateRequest.get();
            restTemplate.testAPIConnection(
                    RESTRICTION_GET_FUNCTION_NAME,
                    RESTRICTION_SET_FUNCTION_NAME);

        } catch (final RuntimeException e) {
            log.error("Failed to access Moodle course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.MOODLE_PLUGIN, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsType.MOODLE_PLUGIN);
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        return getRestTemplate().map(restTemplate -> {

            if (log.isDebugEnabled()) {
                log.debug("Get SEB Client restriction on exam: {}", exam);
            }

            final String quizId = MoodleUtils.getQuizId(exam.getExternalId());
            final LinkedMultiValueMap<String, String> addQuery = new LinkedMultiValueMap<>();
            addQuery.add(ATTRIBUTE_QUIZ_ID, quizId);

            final String srJSON = restTemplate.callMoodleAPIFunction(RESTRICTION_GET_FUNCTION_NAME, addQuery);

            try {

                final MoodleQuizRestriction moodleRestriction = this.jsonMapper.readValue(
                        srJSON,
                        MoodleUtils.MoodleQuizRestriction.class);

                return toSEBRestriction(exam, moodleRestriction);
            } catch (final Exception e) {
                throw new RuntimeException("Unexpected error while get SEB restriction: ", e);
            }
        });
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final Exam exam,
            final SEBRestriction sebRestrictionData) {

        return Result.tryCatch(() -> {

            if (log.isDebugEnabled()) {
                log.debug("Apply SEB Client restriction on exam: {}", exam);
            }

            final String quizId = MoodleUtils.getQuizId(exam.getExternalId());
            final LinkedMultiValueMap<String, String> addQuery = new LinkedMultiValueMap<>();
            addQuery.add(ATTRIBUTE_QUIZ_ID, quizId);

            final ArrayList<String> beks = new ArrayList<>(sebRestrictionData.browserExamKeys);
            final ArrayList<String> configKeys = new ArrayList<>(sebRestrictionData.configKeys);
            final String quitLink = this.examConfigurationValueService.getQuitLink(exam.id);
            final String quitSecret = this.examConfigurationValueService.getQuitSecret(exam.id);
            final String additionalBEK = sebRestrictionData.additionalProperties.get(
                    SEBRestrictionService.ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK);
            if (additionalBEK != null) {
                beks.add(additionalBEK);
            }

            final LinkedMultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.put(ATTRIBUTE_CONFIG_KEYS, configKeys);
            queryAttributes.put(ATTRIBUTE_BROWSER_EXAM_KEYS, beks);
            queryAttributes.add(ATTRIBUTE_QUIT_URL, quitLink);
            queryAttributes.add(ATTRIBUTE_QUIT_SECRET, quitSecret);

            final String srJSON = this.restTemplate.callMoodleAPIFunction(
                    RESTRICTION_SET_FUNCTION_NAME,
                    addQuery,
                    queryAttributes);

            try {

                final MoodleQuizRestriction moodleRestriction = this.jsonMapper.readValue(
                        srJSON,
                        MoodleUtils.MoodleQuizRestriction.class);

                return toSEBRestriction(exam, moodleRestriction);
            } catch (final Exception e) {
                throw new RuntimeException("Unexpected error while get SEB restriction: ", e);
            }
        });
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        return Result.tryCatch(() -> {
            if (log.isDebugEnabled()) {
                log.debug("Release SEB Client restriction on exam: {}", exam);
            }

            final String quizId = MoodleUtils.getQuizId(exam.getExternalId());
            final LinkedMultiValueMap<String, String> addQuery = new LinkedMultiValueMap<>();
            addQuery.add(ATTRIBUTE_QUIZ_ID, quizId);

            final String quitLink = this.examConfigurationValueService.getQuitLink(exam.id);
            final String quitSecret = this.examConfigurationValueService.getQuitSecret(exam.id);

            final LinkedMultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.add(ATTRIBUTE_QUIT_URL, quitLink);
            queryAttributes.add(ATTRIBUTE_QUIT_SECRET, quitSecret);

            this.restTemplate.callMoodleAPIFunction(
                    RESTRICTION_SET_FUNCTION_NAME,
                    addQuery,
                    queryAttributes);

            return exam;
        });
    }

    private SEBRestriction toSEBRestriction(final Exam exam, final MoodleQuizRestriction moodleRestriction) {
        final List<String> configKeys = Arrays.asList(StringUtils.split(
                moodleRestriction.config_keys,
                Constants.LIST_SEPARATOR));
        final List<String> browserExamKeys = Arrays.asList(StringUtils.split(
                moodleRestriction.browser_exam_keys,
                Constants.LIST_SEPARATOR));
        final Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put(ATTRIBUTE_QUIT_URL, moodleRestriction.quit_link);

        final String additionalBEK = exam.getAdditionalAttribute(
                SEBRestrictionService.ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK);

        if (additionalBEK != null) {
            browserExamKeys.remove(additionalBEK);
        }

        return new SEBRestriction(
                exam.id,
                configKeys,
                browserExamKeys,
                additionalProperties);
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        if (this.restTemplate == null) {
            final Result<MoodleAPIRestTemplate> templateRequest = this.restTemplateFactory
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
