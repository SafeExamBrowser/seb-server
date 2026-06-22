/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

public interface DAOUserService {

    /** Get the UUID of the current user. If there is no current user for the process this returns null
     * @return current users UUID or null */
    String getCurrentUserUUID();

    /** Get the user display name: "username (name surname)" for a given user UUID or null if there is no such user.
     *
     * @param UUID The users UUID
     * @return display name of the User */
    String getUserNameForUUID(String UUID);

}
