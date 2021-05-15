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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.MoodleSEBRestriction;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.NoSEBRestrictionException;

/** The MoodleLmsAPITemplate is separated into two parts:
 * - MoodleCourseAccess implements the course access API
 * - MoodleCourseRestriction implements the SEB restriction API
 * - Both uses the MoodleRestTemplateFactore to create a spring based RestTemplate to access the LMS API
 *
 * NOTE: Because of the missing integration on Moodle side so far the MoodleCourseAccess
 * needs to deal with Moodle's standard API functions that don't allow to filter and page course/quiz data
 * in an easy and proper way. Therefore we have to fetch all course and quiz data from Moodle before
 * filtering and paging can be applied. Since there are possibly thousands of active courses and quizzes
 * this moodle course access implements an synchronous fetch as well as an asynchronous fetch strategy.
 * The asynchronous fetch strategy is started within a background task that batches the course and quiz
 * requests to Moodle and fill up a shared cache. A SEB Server LMS API request will start the
 * background task if needed and return immediately to do not block the request.
 * The planed Moodle integration on moodle side also defines an improved course access API. This will
 * possibly make this synchronous fetch strategy obsolete in the future. */
public class MoodleLmsAPITemplate implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(MoodleLmsAPITemplate.class);

    private final MoodleCourseAccess moodleCourseAccess;
    private final MoodleCourseRestriction moodleCourseRestriction;

    protected MoodleLmsAPITemplate(
            final MoodleCourseAccess moodleCourseAccess,
            final MoodleCourseRestriction moodleCourseRestriction) {

        this.moodleCourseAccess = moodleCourseAccess;
        this.moodleCourseRestriction = moodleCourseRestriction;
    }

    @Override
    public LmsType getType() {
        return LmsType.MOODLE;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.moodleCourseAccess
                .getApiTemplateDataSupplier()
                .getLmsSetup();
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        return this.moodleCourseAccess.initAPIAccess();
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        throw new NoSEBRestrictionException();
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this.moodleCourseAccess
                .getQuizzes(filterMap)
                .map(quizzes -> quizzes.stream()
                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                        .collect(Collectors.toList()));
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        final Map<String, QuizData> mapping = this.moodleCourseAccess
                .quizzesSupplier(ids)
                .get()
                .stream()
                .collect(Collectors.toMap(qd -> qd.id, Function.identity()));

        return ids.stream()
                .map(id -> {
                    final QuizData data = mapping.get(id);
                    return (data == null) ? Result.<QuizData> ofRuntimeError("Missing id: " + id) : Result.of(data);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<QuizData> getQuizFromCache(final String id) {
        return this.moodleCourseAccess.getQuizFromCache(id)
                .orElse(() -> getQuiz(id));
    }

    @Override
    public void clearCache() {
        this.moodleCourseAccess.clearCache();
    }

    @Override
    public Collection<Result<QuizData>> getQuizzesFromCache(final Set<String> ids) {
        return this.moodleCourseAccess.getQuizzesFromCache(ids)
                .getOrElse(() -> getQuizzes(ids));
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
            log.debug("Get SEB Client restriction for Exam: {}", exam.externalId);
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
