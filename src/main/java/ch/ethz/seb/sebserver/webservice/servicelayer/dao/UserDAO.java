/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import jakarta.validation.constraints.NotNull;

/** The Data Access Object for all User related data like get user data within UserInfo,
 * save and modify user related data within UserMod and get internal user principal data
 * within SEBServerUser. */
public interface UserDAO extends ActivatableEntityDAO<UserInfo, UserMod>, BulkActionSupportDAO<UserInfo> {

    /** Use this to get the user id (PK) from a given modelId (users UUID).
     *
     * @param modelId The UUID of the user
     * @return the user id (PK) from a given UUID. */
    Result<Long> pkForModelId(String modelId);

    /** Use this to get UserInfo by users username
     *
     * @param username The username of the user to get UserInfo from
     * @return a Result of UserInfo data from user with the specified username. Or an exception result on error case */
    Result<UserInfo> byUsername(String username);

    /** Set given password as new password for specified user account.
     *
     * @param modelId the model id of the user account to change the password
     * @param newPassword the new verified password that is encrypted and stored as the new password for the user
     *            account
     * @return a Result of user account information. Or an exception result on error case */
    Result<UserInfo> changePassword(String modelId, CharSequence newPassword);

    /** Use this to get the SEBServerUser principal for a given username.
     * This should be used for internal authorization and consider only active user accounts
     *
     * @param username The username of the user to get SEBServerUser from
     * @return a Result of SEBServerUser for specified username. Or an exception result on error case */
    Result<SEBServerUser> sebServerUserByUsername(String username);

    /** Use this to get the SEBServerUser admin principal for a given username.
     *
     * @param username The username of the user to get SEBServerUser from
     * @return a Result of SEBServerUser for specified username. Or an exception result on error case */
    Result<SEBServerUser> sebServerAdminByUsername(String username);

    /** Use this to get a Collection containing EntityKey's of all entities that belongs to a given User.
     *
     * @param uuid The UUID of the user
     * @return a Collection containing EntityKey's of all entities that belongs to a given User */
    Collection<EntityKey> getAllUserRelatedData(String uuid);

    Result<Collection<EntityKey>> deleteAdHocAccountsForLMS(String adHocTeacherIdPrefix, Long lmsSetupId);

    /** Get a list of all active user account uuids
     *
     * @return Result with List of all active user account UUIDs or an error when happened */
    Result<Collection<String>> getAllActiveUsersUUID();

    /** Used to update User Roles of a given user. This is used for repair tasks for V30
     * This does not make a merge, it deletes the old roles and inserts the new given roles.
     *
     * @param userId The user Id to update the User Roles
     * @param roles Set of User Roles */
    void updateUserRoles(Long userId, @NotNull Set<UserRole> roles);

}
