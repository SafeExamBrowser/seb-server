/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

public class MoodleLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(MoodleLmsAPITemplate.class);

    private final LmsSetup lmsSetup;
    private final MoodleCourseAccess moodleCourseAccess;
    private final MoodleCourseRestriction moodleCourseRestriction;

    protected MoodleLmsAPITemplate(
            final LmsSetup lmsSetup,
            final MoodleCourseAccess moodleCourseAccess,
            final MoodleCourseRestriction moodleCourseRestriction) {

        this.lmsSetup = lmsSetup;
        this.moodleCourseAccess = moodleCourseAccess;
        this.moodleCourseRestriction = moodleCourseRestriction;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.lmsSetup;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        return this.moodleCourseAccess.initAPIAccess();
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return this.moodleCourseRestriction.initAPIAccess();
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this.moodleCourseAccess.getQuizzes(filterMap);
    }

    @Override
    public Collection<Result<QuizData>> getQuizzesFromCache(final Set<String> ids) {
        return this.moodleCourseAccess.getQuizzesFromCache(ids)
                .getOrElse(() -> getQuizzes(ids));
    }

    @Override
    public Result<QuizData> getQuizFromCache(final String id) {
        return this.moodleCourseAccess.getQuizFromCache(id)
                .orElse(() -> getQuiz(id));
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.tryCatch(() -> this.moodleCourseAccess
                .getCourseChaptersSupplier(courseId)
                .get());
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeSessionId) {
        return this.moodleCourseAccess.getExamineeAccountDetails(examineeSessionId);
    }

    @Override
    public String getExamineeName(final String examineeSessionId) {
        return this.moodleCourseAccess.getExamineeName(examineeSessionId);
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        if (log.isDebugEnabled()) {
            log.debug("Get SEB Client restriction for Exam: {}", exam);
        }

        return this.moodleCourseRestriction
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

        return this.moodleCourseRestriction
                .updateSEBRestriction(
                        externalExamId,
                        MoodleSEBRestriction.from(sebRestrictionData))
                .map(result -> sebRestrictionData);
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        if (log.isDebugEnabled()) {
            log.debug("Release SEB Client restriction for Exam: {}", exam);
        }

        return this.moodleCourseRestriction
                .deleteSEBRestriction(exam.externalId)
                .map(result -> exam);
    }

}
