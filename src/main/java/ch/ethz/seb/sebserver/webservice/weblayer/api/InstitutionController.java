/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
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

    @RequestMapping(method = RequestMethod.GET)
    public Collection<Institution> getAll(
            @RequestParam(name = Institution.FILTER_ATTR_ONLY_ACTIVE, required = false) final Boolean onlyActive) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.INSTITUTION,
                PrivilegeType.READ_ONLY);

        final boolean hasBasePrivilege = this.authorizationGrantService.hasBasePrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY);

        if (onlyActive == null || onlyActive) {

            return (hasBasePrivilege)
                    ? this.institutionDAO.allActive().getOrThrow()
                    : this.institutionDAO.all(
                            institution -> this.authorizationGrantService.hasGrant(
                                    institution,
                                    PrivilegeType.READ_ONLY))
                            .getOrThrow();

        } else {

            return this.institutionDAO
                    .all()
                    .getOrThrow();

        }
    }

}
