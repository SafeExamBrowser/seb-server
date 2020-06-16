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

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

public class MoodleLmsAPITemplate implements LmsAPITemplate {

    private final LmsSetup lmsSetup;
    private final MoodleCourseAccess moodleCourseAccess;

    protected MoodleLmsAPITemplate(
            final LmsSetup lmsSetup,
            final MoodleCourseAccess moodleCourseAccess) {

        this.lmsSetup = lmsSetup;
        this.moodleCourseAccess = moodleCourseAccess;
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
        return LmsSetupTestResult.ofQuizRestrictionAPIError("Not available yet");
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
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        return Result.ofError(new UnsupportedOperationException("SEB Restriction API not available yet"));
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {

        return Result.ofError(new UnsupportedOperationException("SEB Restriction API not available yet"));
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        return Result.ofError(new UnsupportedOperationException("SEB Restriction API not available yet"));
    }

}
