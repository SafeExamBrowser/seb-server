/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collection;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface LmsAPITemplate {

    LmsSetup lmsSetup();

    LmsSetupTestResult testLmsSetup();

    Page<QuizData> getQuizzes(
            String name,
            Long from,
            String sort,
            int pageNumber,
            int pageSize);

    Collection<Result<QuizData>> getQuizzes(Set<String> ids);

    Result<ExamineeAccountDetails> getExamineeAccountDetails(String examineeUserId);

}
