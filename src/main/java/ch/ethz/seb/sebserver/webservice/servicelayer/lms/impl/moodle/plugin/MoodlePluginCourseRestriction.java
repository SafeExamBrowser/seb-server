/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleQuizRestrictions;

public class MoodlePluginCourseRestriction implements SEBRestrictionAPI {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginCourseRestriction.class);

    public static final String RESTRICTION_GET_FUNCTION_NAME = "quizaccess_sebserver_get_restriction";
    public static final String RESTRICTION_SET_FUNCTION_NAME = "quizaccess_sebserver_set_restriction";

    public static final String ATTRIBUTE_QUIZ_ID = "quizid";
    public static final String ATTRIBUTE_CONFIG_KEYS = "configkeys[]";
    public static final String ATTRIBUTE_BROWSER_EXAM_KEYS = "browserkeys[]";
    public static final String ATTRIBUTE_QUIT_URL = "quitlink";
    public static final String ATTRIBUTE_QUIT_SECRET = "quitsecret";

    private static final String DELETED_RESTRICTION_WARNING = "You have deleted restriction";
    private static final String RESTRICTION_NOT_SET_WARNING = "SEB Server is not enabled for quiz";

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

            final String srJSON = restTemplate.callMoodleAPIFunction(
                    RESTRICTION_GET_FUNCTION_NAME,
                    addQuery,
                    new LinkedMultiValueMap<>());

            return restrictionFromJson(exam, srJSON);
        });
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final Exam exam,
            final SEBRestriction sebRestrictionData) {

        return getRestTemplate().map(restTemplate -> {

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
            final String additionalBEK = exam.getAdditionalAttribute(
                    SEBRestrictionService.ADDITIONAL_ATTR_ALTERNATIVE_SEB_BEK);

            if (additionalBEK != null && !beks.contains(additionalBEK)) {
                beks.add(additionalBEK);
            }

            final LinkedMultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
            queryAttributes.put(ATTRIBUTE_CONFIG_KEYS, configKeys);
            queryAttributes.put(ATTRIBUTE_BROWSER_EXAM_KEYS, beks);
            queryAttributes.add(ATTRIBUTE_QUIT_URL, quitLink);
            queryAttributes.add(ATTRIBUTE_QUIT_SECRET, quitSecret);

            final String srJSON = restTemplate.callMoodleAPIFunction(
                    RESTRICTION_SET_FUNCTION_NAME,
                    addQuery,
                    queryAttributes);

            return restrictionFromJson(exam, srJSON);
        });
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        return getRestTemplate().map(restTemplate -> {
            if (log.isDebugEnabled()) {
                log.debug("Release SEB Client restriction on exam: {}", exam);
            }

            final String quizId = MoodleUtils.getQuizId(exam.getExternalId());
            final LinkedMultiValueMap<String, String> addQuery = new LinkedMultiValueMap<>();
            addQuery.add(ATTRIBUTE_QUIZ_ID, quizId);

            final LinkedMultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();

            queryAttributes.add(ATTRIBUTE_CONFIG_KEYS, StringUtils.EMPTY);
            queryAttributes.add(ATTRIBUTE_BROWSER_EXAM_KEYS, StringUtils.EMPTY);

            final String srJSON = restTemplate.callMoodleAPIFunction(
                    RESTRICTION_SET_FUNCTION_NAME,
                    addQuery,
                    queryAttributes);

            final SEBRestriction restriction = restrictionFromJson(exam, srJSON);
            if (StringUtils.isNotBlank(restriction.warningMessage)) {
                if (restriction.warningMessage.contains(DELETED_RESTRICTION_WARNING)) {
                    return exam;
                }
                throw new RuntimeException("LMS Warnings: " + restriction.warningMessage);
            }

            return exam;
        });
    }

    private SEBRestriction restrictionFromJson(final Exam exam, final String srJSON) {
        try {

            // check blank result
            if (StringUtils.isBlank(srJSON)) {
                return new SEBRestriction(
                        exam.id,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyMap(),
                        "Missing or blank response from Moodle");
            }

            // fist try to get from multiple data
            final MoodleQuizRestrictions moodleRestrictions = this.jsonMapper.readValue(
                    srJSON,
                    MoodleUtils.MoodleQuizRestrictions.class);

            String warningMessages = null;
            if (moodleRestrictions.warnings != null && !moodleRestrictions.warnings.isEmpty()) {
                warningMessages = StringUtils.join(
                        moodleRestrictions.warnings
                                .stream()
                                .map(MoodleAPIRestTemplate.Warning::getMessage)
                                .collect(Collectors.toList()),
                        Constants.LIST_SEPARATOR);

                if (!warningMessages.contains(DELETED_RESTRICTION_WARNING)) {
                    if (warningMessages.contains(RESTRICTION_NOT_SET_WARNING)) {
                        log.info("Message from Moodle: {}", moodleRestrictions.warnings);
                    } else {
                        log.warn("Warnings from Moodle: {}", moodleRestrictions.warnings);
                    }
                }
            }

            return toSEBRestriction(exam, moodleRestrictions, warningMessages);
        } catch (final Exception e) {
            try {
                // then try to get from single
                final MoodleQuizRestriction moodleRestriction = this.jsonMapper.readValue(
                        srJSON,
                        MoodleUtils.MoodleQuizRestriction.class);

                return toSEBRestriction(exam, moodleRestriction, null);
            } catch (final Exception ee) {
                throw new RuntimeException("Unexpected error while get SEB restriction: ", ee);
            }
        }
    }

    private SEBRestriction toSEBRestriction(
            final Exam exam,
            final MoodleQuizRestrictions moodleRestrictions,
            final String warnings) {

        if (moodleRestrictions.data == null || moodleRestrictions.data.isEmpty()) {
            if (StringUtils.isNotBlank(warnings)) {
                return new SEBRestriction(
                        exam.id,
                        null,
                        null,
                        null,
                        warnings);
            } else {
                throw new IllegalArgumentException("Expecting MoodleQuizRestriction not available. Exam: " + exam);
            }
        }

        return toSEBRestriction(exam, moodleRestrictions.data.iterator().next(), warnings);
    }

    private SEBRestriction toSEBRestriction(
            final Exam exam,
            final MoodleQuizRestriction moodleRestriction,
            final String warnings) {

        final Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put(ATTRIBUTE_QUIT_URL, moodleRestriction.quitlink);
        additionalProperties.put(ATTRIBUTE_QUIT_SECRET, moodleRestriction.quitsecret);

        return new SEBRestriction(
                exam.id,
                moodleRestriction.configkeys,
                moodleRestriction.browserkeys,
                additionalProperties,
                warnings);
    }

    private Result<MoodleAPIRestTemplate> getRestTemplate() {
        if (this.restTemplate == null) {

            final Result<MoodleAPIRestTemplate> templateRequest = this.restTemplateFactory
                    .createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME);
            if (templateRequest.hasError()) {
                return templateRequest;
            } else {
                this.restTemplate = templateRequest.get();
            }
        }

        return Result.of(this.restTemplate);
    }

    public String toTestString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MoodlePluginCourseRestriction [restTemplate=");
        builder.append(this.restTemplate);
        builder.append("]");
        return builder.toString();
    }

}
