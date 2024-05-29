/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.TokenLoginInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;

public interface TeacherAccountService {

    default String getTeacherAccountIdentifier(
            final Exam exam,
            final FullLmsIntegrationService.AdHocAccountData adHocAccountData) {
        return getTeacherAccountIdentifier(exam.getModelId(), adHocAccountData.userId);
    }

    String getTeacherAccountIdentifier(String examId, String userId);

    Result<UserInfo> createNewTeacherAccountForExam(
            Exam exam,
            final FullLmsIntegrationService.AdHocAccountData adHocAccountData);

    Result<Exam> deactivateTeacherAccountsForExam(Exam exam);

    Result<String> getOneTimeTokenForTeacherAccount(
            Exam exam,
            FullLmsIntegrationService.AdHocAccountData adHocAccountData,
            boolean createIfNotExists);

    Result<TokenLoginInfo> verifyOneTimeTokenForTeacherAccount(String token);


}
