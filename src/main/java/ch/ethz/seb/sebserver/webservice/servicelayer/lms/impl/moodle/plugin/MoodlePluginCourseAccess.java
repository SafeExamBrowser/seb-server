/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;

public class MoodlePluginCourseAccess implements CourseAccessAPI {

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearCourseCache() {
        // TODO Auto-generated method stub

    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getExamineeName(final String examineeUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        // TODO Auto-generated method stub
        return null;
    }

}
