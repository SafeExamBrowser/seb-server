/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

final class MockupLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(MockupLmsAPITemplate.class);

    private final LmsSetup lmsSetup;
    private final ClientCredentials credentials;
    private final Collection<QuizData> mockups;
    private final WebserviceInfo webserviceInfo;

    MockupLmsAPITemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final WebserviceInfo webserviceInfo) {

        this.lmsSetup = lmsSetup;
        this.credentials = credentials;
        this.webserviceInfo = webserviceInfo;

        final Long lmsSetupId = lmsSetup.id;
        final Long institutionId = lmsSetup.getInstitutionId();
        final LmsType lmsType = lmsSetup.getLmsType();
        this.mockups = new ArrayList<>();
        this.mockups.add(new QuizData(
                "quiz1", institutionId, lmsSetupId, lmsType, "Demo Quiz 1 (MOCKUP)", "Demo Quiz Mockup",
                "2020-01-01T09:00:00Z", null, "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz2", institutionId, lmsSetupId, lmsType, "Demo Quiz 2 (MOCKUP)", "Demo Quiz Mockup",
                "2020-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz3", institutionId, lmsSetupId, lmsType, "Demo Quiz 3 (MOCKUP)", "Demo Quiz Mockup",
                "2018-07-30T09:00:00Z", "2018-08-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz4", institutionId, lmsSetupId, lmsType, "Demo Quiz 4 (MOCKUP)", "Demo Quiz Mockup",
                "2018-01-01T00:00:00Z", "2019-01-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz5", institutionId, lmsSetupId, lmsType, "Demo Quiz 5 (MOCKUP)", "Demo Quiz Mockup",
                "2018-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz6", institutionId, lmsSetupId, lmsType, "Demo Quiz 6 (MOCKUP)", "Demo Quiz Mockup",
                "2019-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz7", institutionId, lmsSetupId, lmsType, "Demo Quiz 7 (MOCKUP)", "Demo Quiz Mockup",
                "2018-01-01T09:00:00Z", "2021-01-01T09:00:00Z", "http://lms.mockup.com/api/"));

        this.mockups.add(new QuizData(
                "quiz10", institutionId, lmsSetupId, lmsType, "Demo Quiz 10 (MOCKUP)",
                "Starts in a minute and ends after five minutes",
                DateTime.now(DateTimeZone.UTC).plus(Constants.MINUTE_IN_MILLIS)
                        .toString(Constants.DEFAULT_DATE_TIME_FORMAT),
                DateTime.now(DateTimeZone.UTC).plus(6 * Constants.MINUTE_IN_MILLIS)
                        .toString(Constants.DEFAULT_DATE_TIME_FORMAT),
                "http://lms.mockup.com/api/"));
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.lmsSetup;
    }

    private List<APIMessage> checkAttributes() {
        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(this.lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        }
        if (!this.credentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }
        if (!this.credentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }
        return missingAttrs;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        log.info("Test Lms Binding for Mockup and LmsSetup: {}", this.lmsSetup);

        final List<APIMessage> missingAttrs = checkAttributes();

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(missingAttrs);
        }

        if (authenticate()) {
            return LmsSetupTestResult.ofOkay();
        } else {
            return LmsSetupTestResult.ofTokenRequestError("Illegal access");
        }
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        // TODO Auto-generated method stub
        return LmsSetupTestResult.ofQuizRestrictionAPIError("unsupported");
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return Result.tryCatch(() -> {
            if (!authenticate()) {
                throw new IllegalArgumentException("Wrong clientId or secret");
            }

            return this.mockups
                    .stream()
                    .map(this::getExternalAddressAlias)
                    .filter(LmsAPIService.quizFilterPredicate(filterMap))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        if (!authenticate()) {
            throw new IllegalArgumentException("Wrong clientId or secret");
        }

        return this.mockups
                .stream()
                .map(this::getExternalAddressAlias)
                .filter(mock -> ids.contains(mock.id))
                .map(Result::of)
                .collect(Collectors.toList());
    }

    @Override
    public Result<QuizData> getQuizFromCache(final String id) {
        return getQuiz(id);
    }

    @Override
    public Collection<Result<QuizData>> getQuizzesFromCache(final Set<String> ids) {
        return getQuizzes(ids);
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.ofError(new UnsupportedOperationException());
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        log.info("Apply SEB Client restriction for Exam: {}", exam);
        return Result.ofError(new NoSEBRestrictionException());
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        log.info("Apply SEB Client restriction: {}", sebRestrictionData);
        return Result.of(sebRestrictionData);
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        log.info("Release SEB Client restriction for Exam: {}", exam);
        return Result.of(exam);
    }

    private QuizData getExternalAddressAlias(final QuizData quizData) {
        final String externalAddressAlias = this.webserviceInfo.getLmsExternalAddressAlias("lms.mockup.com");
        if (StringUtils.isNoneBlank(externalAddressAlias)) {
            try {

                final String _externalStartURI =
                        this.webserviceInfo.getHttpScheme() +
                                "://" + externalAddressAlias + "/api/";

                if (log.isTraceEnabled()) {
                    log.trace("Use external address for course access: {}", _externalStartURI);
                }

                return new QuizData(
                        quizData.id, quizData.institutionId, quizData.lmsSetupId, quizData.lmsType,
                        quizData.name, quizData.description, quizData.startTime,
                        quizData.endTime, _externalStartURI, quizData.additionalAttributes);
            } catch (final Exception e) {
                log.error("Failed to create external address from alias: ", e);
                return quizData;
            }
        } else {
            return quizData;
        }
    }

    private boolean authenticate() {
        try {

            final CharSequence plainClientId = this.credentials.clientId;
            if (plainClientId == null || plainClientId.length() <= 0) {
                throw new IllegalAccessException("Wrong client credential");
            }

            return true;
        } catch (final Exception e) {
            log.info("Authentication failed: ", e);
            return false;
        }
    }

}
