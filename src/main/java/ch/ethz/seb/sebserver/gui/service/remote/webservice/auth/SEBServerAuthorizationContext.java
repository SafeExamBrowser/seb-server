/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Defines functionality for the SEB Server webservice authorization context used to
 * manage a user session on GUI service. */
public interface SEBServerAuthorizationContext {

    /** Indicates if this authorization context is still valid
     *
     * @return true if the SEBServerAuthorizationContext is valid. False of not. */
    boolean isValid();

    /** Indicated whether a user is logged in within this authorization context or not.
     *
     * @return whether a user is logged in within this authorization context or not */
    boolean isLoggedIn();

    /** Requests a login with username and password on SEB Server webservice.
     * This uses OAuth 2 and Springs OAuth2RestTemplate to exchange user/client credentials
     * with an access and refresh token.
     *
     * @param username the username for login
     * @param password the password for login
     * @return true if login was successful, false if no */
    boolean login(String username, CharSequence password);

    /** Requests a logout on SEB Server webservice if a user is currently logged in
     * This uses OAuth 2 and Springs OAuth2RestTemplate to make a revoke token request for the
     * currently logged in user and also invalidates this SEBServerAuthorizationContext
     *
     * @return true if logout was successful */
    boolean logout();

    /** Gets a Result of the UserInfo data of currently logged in user or of an error if no user is logged in
     * or there was an unexpected error while trying to get the user information.
     *
     * @return Result of logged in user data or of an error on fail */
    Result<UserInfo> getLoggedInUser();

    void refreshUser(UserInfo userInfo);

    /** Returns true if a current logged in user has the specified role.
     *
     * @param role the UserRole to check
     * @return true if a current logged in user has the specified role */
    boolean hasRole(UserRole role);

    /** Get the underling RestTemplate to connect and communicate with the SEB Server webservice.
     *
     * @return the underling RestTemplate to connect and communicate with the SEB Server webservice */
    RestTemplate getRestTemplate();

}
