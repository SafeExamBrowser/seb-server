/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.TokenLoginInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface TeacherAccountService {

    Result<UserInfo> createNewTeacherAccountForExam(
            Exam exam,
            String userId,
            String username,
            String timezone);

    Result<Exam> deleteTeacherAccountsForExam(final Exam exam);

    Result<String> getOneTimeTokenForTeacherAccount(
            Exam exam,
            String userId,
            String username,
            String timezone,
            final boolean createIfNotExists);

    Result<TokenLoginInfo> verifyOneTimeTokenForTeacherAccount(String token);


}
