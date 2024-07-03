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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;

/** Service used to maintain Teacher Ad-Hoc Accounts */
public interface TeacherAccountService {

    String AD_HOC_TEACHER_ID_PREFIX = "TEACHER_ACCOUNT_";

    /** Creates an Ad-Hoc Teacher account for a given existing Exam.
     *
     * @param exam The Exam instance
     * @param adHocAccountData The account data for new Ad-Hoc account
     * @return Result refer to the accounts UserInfo instance or to an error when happened.
     */
    Result<UserInfo> createNewTeacherAccountForExam(
            Exam exam,
            final AdHocAccountData adHocAccountData);

    /** Get the identifier for certain Teacher account for specified Exam.
     *
     * @param exam The Exam instance
     * @param adHocAccountData the account data
     * @return account identifier
     */
    default String getTeacherAccountIdentifier(
            final Exam exam,
            final AdHocAccountData adHocAccountData) {

        return getTeacherAccountIdentifier(
                String.valueOf(exam.lmsSetupId),
                adHocAccountData.userId);
    }

    /** Get the identifier for certain Teacher account for specified Exam.
     *
     * @param lmsId The Lms identifier
     * @param userId the account id
     * @return account identifier
     */
    String getTeacherAccountIdentifier(String lmsId, String userId);

    /** Get a One Time Access JWT Token for auto-login for a specific ad-hoc Teacher account.
     *
     * @param exam The involved Exam instance
     * @param adHocAccountData the account data
     * @param createIfNotExists Indicates if a ad-hoc Teacher account shall be created if there is none for given
     *                          account data.
     * @return Result refer to the One Time Access JWT Token or to an error when happened.
     */
    Result<String> getOneTimeTokenForTeacherAccount(
            Exam exam,
            AdHocAccountData adHocAccountData,
            boolean createIfNotExists);

    /** Used to verify a given One Time Access JWT Token. This must have the expected claims and must not be expired
     *
     * @param token The One Time Access JWT Token to verify access for
     * @return Result refer to the login information for auto-login or to an error when happened or access is denied
     */
    Result<TokenLoginInfo> verifyOneTimeTokenForTeacherAccount(String token);

}
