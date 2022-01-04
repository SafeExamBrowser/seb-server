/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

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
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;

public class MockupLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(MockupLmsAPITemplate.class);

    private final Collection<QuizData> mockups;
    private final WebserviceInfo webserviceInfo;
    private final APITemplateDataSupplier apiTemplateDataSupplier;

    MockupLmsAPITemplate(
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final WebserviceInfo webserviceInfo) {

        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.webserviceInfo = webserviceInfo;
        this.mockups = new ArrayList<>();

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final Long lmsSetupId = lmsSetup.id;
        final Long institutionId = lmsSetup.getInstitutionId();
        final LmsType lmsType = lmsSetup.getLmsType();

        this.mockups.add(new QuizData(
                "quiz1", institutionId, lmsSetupId, lmsType, "Demo Quiz 1 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2020-01-01T09:00:00Z", null, "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz2", institutionId, lmsSetupId, lmsType, "Demo Quiz 2 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2020-01-01T09:00:00Z", "2025-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz3", institutionId, lmsSetupId, lmsType, "Demo Quiz 3 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2018-07-30T09:00:00Z", "2018-08-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz4", institutionId, lmsSetupId, lmsType, "Demo Quiz 4 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2018-01-01T00:00:00Z", "2025-01-01T00:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz5", institutionId, lmsSetupId, lmsType, "Demo Quiz 5 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2018-01-01T09:00:00Z", "2025-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz6", institutionId, lmsSetupId, lmsType, "Demo Quiz 6 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2019-01-01T09:00:00Z", "2025-01-01T09:00:00Z", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz7", institutionId, lmsSetupId, lmsType, "Demo Quiz 7 (MOCKUP)", "<p>Demo Quiz Mockup</p>",
                "2018-01-01T09:00:00Z", "2025-01-01T09:00:00Z", "http://lms.mockup.com/api/"));

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
    public LmsType getType() {
        return LmsType.MOCKUP;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.apiTemplateDataSupplier.getLmsSetup();
    }

    private List<APIMessage> checkAttributes() {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials lmsClientCredentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        }
        if (!lmsClientCredentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }
        if (!lmsClientCredentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }
        return missingAttrs;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        log.info("Test Lms Binding for Mockup and LmsSetup: {}", this.apiTemplateDataSupplier.getLmsSetup());

        final List<APIMessage> missingAttrs = checkAttributes();

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.MOCKUP, missingAttrs);
        }

        if (authenticate()) {
            return LmsSetupTestResult.ofOkay(LmsType.MOCKUP);
        } else {
            return LmsSetupTestResult.ofTokenRequestError(LmsType.MOCKUP, "Illegal access");
        }
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return LmsSetupTestResult.ofQuizRestrictionAPIError(LmsType.MOCKUP, "unsupported");
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
    public Result<QuizData> getQuiz(final String id) {
        return Result.of(this.mockups
                .stream()
                .filter(q -> id.equals(q.id))
                .findFirst()
                .get());
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        return Result.tryCatch(() -> {
            if (!authenticate()) {
                throw new IllegalArgumentException("Wrong clientId or secret");
            }

            return this.mockups
                    .stream()
                    .map(this::getExternalAddressAlias)
                    .filter(mock -> ids.contains(mock.id))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void clearCache() {

    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.ofError(new UnsupportedOperationException());
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return Result.ofError(new UnsupportedOperationException());
    }

    @Override
    public String getExamineeName(final String examineeSessionId) {
        return "--" + " (" + examineeSessionId + ")";
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

            final CharSequence plainClientId = this.apiTemplateDataSupplier.getLmsClientCredentials().clientId;
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
