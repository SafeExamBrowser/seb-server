/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_INSTITUTION)
public class InstitutionController {

    private final InstitutionDAO institutionDAO;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserService userService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InstitutionController(
            final InstitutionDAO institutionDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserService userService, final UserActivityLogDAO userActivityLogDAO,
            final ApplicationEventPublisher applicationEventPublisher) {

        this.institutionDAO = institutionDAO;
        this.authorizationGrantService = authorizationGrantService;
        this.userService = userService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.applicationEventPublisher = applicationEventPublisher;
    }

}
