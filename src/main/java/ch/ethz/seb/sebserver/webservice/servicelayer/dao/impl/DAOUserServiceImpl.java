/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOUserService;

@Lazy
@Service
public class DAOUserServiceImpl implements DAOUserService {

    private static final Logger log = LoggerFactory.getLogger(DAOUserServiceImpl.class);

    private final AuthorizationService authorizationService;

    public DAOUserServiceImpl(final AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public String getCurrentUserUUID() {
        try {
            return this.authorizationService.getUserService().getCurrentUser().uuid();
        } catch (final Exception e) {
            log.warn("Failed to get current user: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getUserNameForUUID(final String userUUID) {
        if (userUUID == null) {
            return null;
        }

        final UserInfo user = authorizationService.getUserService().getUser(userUUID);
        if (user == null || user.username == null) {
            return null;
        }

        return user.username + " (" + user.name + " " + user.surname + ")";
    }

    @Override
    public String getCurrentUserUUIDOrAnonymousUser() {
        try {
            return this.authorizationService.getUserService().getCurrentUser().uuid();
        } catch (final Exception e) {
            return this.authorizationService.getAnonymousUserUUID();
        }
    }

}
