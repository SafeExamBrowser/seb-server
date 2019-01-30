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

public interface SEBServerAuthorizationContext {

    boolean isValid();

    boolean isLoggedIn();

    boolean login(String username, String password);

    boolean logout();

    Result<UserInfo> getLoggedInUser();

    public boolean hasRole(UserRole role);

    RestTemplate getRestTemplate();

}
