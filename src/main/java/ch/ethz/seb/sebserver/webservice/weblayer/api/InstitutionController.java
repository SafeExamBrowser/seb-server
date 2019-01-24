/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

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
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorizationGrantService,
                bulkActionService,
                institutionDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.institutionDAO = institutionDAO;
    }

    @Override
    protected Class<Institution> modifiedDataType() {
        return Institution.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return InstitutionRecordDynamicSqlSupport.institutionRecord;
    }

    @RequestMapping(path = "/self", method = RequestMethod.GET)
    public Institution getOwn() {
        final SEBServerUser currentUser = this.authorizationGrantService
                .getUserService()
                .getCurrentUser();

        final Long institutionId = currentUser.institutionId();
        return this.institutionDAO.byPK(institutionId).getOrThrow();
    }

}
