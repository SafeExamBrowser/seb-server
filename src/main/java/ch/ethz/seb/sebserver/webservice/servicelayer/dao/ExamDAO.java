/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamDAO extends ActivatableEntityDAO<Exam> {

    Result<Exam> importFromQuizData(QuizData quizData);

    Result<Exam> byQuizId(String quizId);

    Result<Collection<Exam>> allMatching(
            Long institutionId,
            Long lmsSetupId,
            String name,
            Exam.ExamStatus status,
            Exam.ExamType type,
            Long startTime,
            String owner,
            Boolean active);

}
