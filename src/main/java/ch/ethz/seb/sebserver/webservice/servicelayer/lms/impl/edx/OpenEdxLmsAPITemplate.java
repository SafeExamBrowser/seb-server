/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

/** The OpenEdxLmsAPITemplate is separated into two parts:
 * - OpenEdxCourseAccess implements the course access API
 * - OpenEdxCourseRestriction implements the SEB restriction API
 * - Both uses the OpenEdxRestTemplateFactory to create a spring based RestTemplate to access the LMS API */
final class OpenEdxLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(OpenEdxLmsAPITemplate.class);

    private final OpenEdxCourseAccess openEdxCourseAccess;
    private final OpenEdxCourseRestriction openEdxCourseRestriction;

    OpenEdxLmsAPITemplate(
            final OpenEdxCourseAccess openEdxCourseAccess,
            final OpenEdxCourseRestriction openEdxCourseRestriction) {

        this.openEdxCourseAccess = openEdxCourseAccess;
        this.openEdxCourseRestriction = openEdxCourseRestriction;
    }

    @Override
    public LmsType getType() {
        return LmsType.OPEN_EDX;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.openEdxCourseAccess
                .getApiTemplateDataSupplier()
                .getLmsSetup();
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        return this.openEdxCourseAccess.initAPIAccess();
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return this.openEdxCourseRestriction.initAPIAccess();
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this.openEdxCourseAccess
                .getQuizzes(filterMap)
                .map(quizzes -> quizzes.stream()
                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        return Result.tryCatch(() -> {
            final QuizData quizFromCache = this.openEdxCourseAccess.getQuizFromCache(id);
            if (quizFromCache != null) {
                return quizFromCache;
            }

            return this.openEdxCourseAccess.getQuizFromLMS(id);
        });
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        return this.openEdxCourseAccess.getQuizzesFromCache(ids);
    }

    @Override
    public void clearCache() {
        this.openEdxCourseAccess.clearCache();
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.tryCatch(() -> this.openEdxCourseAccess
                .getCourseChaptersSupplier(courseId)
                .get());
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return this.openEdxCourseAccess.getExamineeAccountDetails(examineeSessionId);
    }

    @Override
    public String getExamineeName(final String examineeSessionId) {
        return this.openEdxCourseAccess.getExamineeName(examineeSessionId);
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        if (log.isDebugEnabled()) {
            log.debug("Get SEB Client restriction for Exam: {}", exam);
        }

        return this.openEdxCourseRestriction
                .getSEBRestriction(exam.externalId)
                .map(restriction -> SEBRestriction.from(exam.id, restriction));
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        if (log.isDebugEnabled()) {
            log.debug("Apply SEB Client restriction: {}", sebRestrictionData);
        }

        return this.openEdxCourseRestriction
                .putSEBRestriction(
                        externalExamId,
                        OpenEdxSEBRestriction.from(sebRestrictionData))
                .map(result -> sebRestrictionData);
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {

        if (log.isDebugEnabled()) {
            log.debug("Release SEB Client restriction for Exam: {}", exam);
        }

        return this.openEdxCourseRestriction
                .deleteSEBRestriction(exam.externalId)
                .map(result -> exam);
    }

}
