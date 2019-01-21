/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_INSTITUTION)
public class InstitutionController extends ActivatableEntityController<Institution, Institution> {

    private final InstitutionDAO institutionDAO;

    public InstitutionController(
            final InstitutionDAO institutionDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final PaginationService paginationService) {

        super(authorizationGrantService, bulkActionService, institutionDAO, userActivityLogDAO, paginationService);
        this.institutionDAO = institutionDAO;
    }

    @RequestMapping(path = "/self", method = RequestMethod.GET)
    public Institution getOwn() {
        final SEBServerUser currentUser = this.authorizationGrantService
                .getUserService()
                .getCurrentUser();

        final Long institutionId = currentUser.institutionId();
        return this.institutionDAO.byPK(institutionId).getOrThrow();
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<Institution> getAll(
            @RequestParam(name = Institution.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        if (!this.authorizationGrantService.hasBasePrivilege(
                EntityType.INSTITUTION,
                PrivilegeType.READ_ONLY)) {

            // User has only institutional privilege, can see only the institution he/she belongs to
            return Arrays.asList(getOwn());
        } else {
            return this.institutionDAO.all(null, active).getOrThrow();
        }
    }

}
