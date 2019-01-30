/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.SEBServerRestEndpoints;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + SEBServerRestEndpoints.ENDPOINT_LMS_SETUP)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private final LmsAPIService lmsAPIService;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService) {

        super(authorizationGrantService,
                bulkActionService,
                lmsSetupDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.lmsAPIService = lmsAPIService;
    }

    @Override
    protected Class<LmsSetup> modifiedDataType() {
        return LmsSetup.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return LmsSetupRecordDynamicSqlSupport.lmsSetupRecord;
    }

    @RequestMapping(
            path = "/SEB_Configuration/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE) // TODO check if this is the right format
    public void downloadSEBConfig(
            @PathVariable final Long id,
            final HttpServletResponse response) {

        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.LMS_SETUP,
                PrivilegeType.WRITE);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setStatus(HttpStatus.OK.value());

        try {
            final InputStream sebConfigFileIn = this.lmsAPIService
                    .createSEBStartConfiguration(id)
                    .getOrThrow();

            IOUtils.copyLarge(sebConfigFileIn, response.getOutputStream());
            response.flushBuffer();

        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error while trying to creae SEB start config: ", e);
        }
    }

    @Override
    protected LmsSetup createNew(final POSTMapper postParams) {
        return new LmsSetup(null, postParams);
    }

}
