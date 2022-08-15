/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.DAOUserServcie;

@Lazy
@Service
@WebServiceProfile
public class DAOUserServcieImpl implements DAOUserServcie {

    private static final Logger log = LoggerFactory.getLogger(DAOUserServcieImpl.class);

    private final AuthorizationService authorizationService;

    public DAOUserServcieImpl(final AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public String getCurrentUserUUID() {
        try {
            return this.authorizationService.getUserService().getCurrentUser().uuid();
        } catch (final Exception e) {
            log.error("Failed to get current user: ", e);
            return null;
        }
    }

}
