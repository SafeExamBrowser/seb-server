/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.SEBServerUser;

/** The Data Access Object for all User related data like get user data within UserInfo,
 * save and modify user related data within UserMod and get internal user principal data
 * within SEBServerUser. */
public interface UserDAO {

    /** Use this to get UserInfo by database identifier
     *
     * @param id the data base identifier of the user
     * @return UserInfo data from user with the specified database identifier
     * @throws ResourceNotFoundException */
    UserInfo byId(Long id);

    /** Use this to get UserInfo by users UUID
     *
     * @param uuid The UUID of the user to get UserInfo from
     * @return UserInfo data from user with the specified UUID
     * @throws ResourceNotFoundException */
    UserInfo byUuid(String uuid);

    /** Use this to get UserInfo by users username
     *
     * @param username The username of the user to get UserInfo from
     * @return UserInfo data from user with the specified username
     * @throws ResourceNotFoundException */
    UserInfo byUserName(String username);

    /** Use this to get the SEBServerUser principal for a given username.
     *
     * @param username The username of the user to get SEBServerUser from
     * @return SEBServerUser for specified username
     * @throws ResourceNotFoundException */
    SEBServerUser sebServerUserByUserName(String username);

    /** Use this to get a Collection of UserInfo for all active users.
     *
     * @return Collection of UserInfo for all active users */
    Collection<UserInfo> allActive();

    /** Use this to get a Collection of UserInfo that matches a given predicate.
     *
     * NOTE: This first gets all UserRecord from database, for each creates new UserInfo
     * tests then matching predicate. So predicate filtering is not really fast
     * If you need a fast filtering user all with UserFilter
     *
     * @return Collection of UserInfo that matches a given predicate */
    Collection<UserInfo> all(Predicate<UserInfo> predicate);

    /** Use this to a Collection of filtered UserInfo. The filter criteria
     * from given UserFilter instance will be translated to SQL query and
     * the filtering happens on data-base level
     *
     * @param filter The UserFilter instance containing all filter criteria
     * @return Collection of filtered UserInfo */
    Collection<UserInfo> all(UserFilter filter);

    /** Use this to save/modify user data.
     * If the UUID from given UserMod is null or not exists already, a new user is created.
     * If the UUID is available and matches an existing user record, all user data that are
     * not null on UserMod instance are updated within the existing user record.
     *
     * @param userMod UserMod instance containing new user record data
     * @param principal the user principal that requests the save/modification
     * @return A Result of UserInfo where the successfully saved/modified user data is available or a reported
     *         exception */
    Result<UserInfo> save(SEBServerUser principal, UserMod userMod);

}
