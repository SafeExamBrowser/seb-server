/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.InputStream;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_LMS_SETUP)
public class LmsSetupController extends ActivatableEntityController<LmsSetup, LmsSetup> {

    private final LmsSetupDAO lmsSetupDAO;
    private final LmsAPIService lmsAPIService;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService,
            final PaginationService paginationService) {

        super(authorizationGrantService, bulkActionService, lmsSetupDAO, userActivityLogDAO, paginationService);

        this.lmsSetupDAO = lmsSetupDAO;
        this.lmsAPIService = lmsAPIService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorizationGrantService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<LmsSetup> getAll(
            @RequestParam(
                    name = LmsSetup.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = LmsSetup.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = LmsSetup.FILTER_ATTR_LMS_TYPE, required = false) final LmsType lmsType,
            @RequestParam(name = LmsSetup.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        checkReadPrivilege(institutionId);

        return this.lmsSetupDAO
                .allMatching(institutionId, name, lmsType, active)
                .getOrThrow();
    }

    @RequestMapping(
            path = "/create_seb_config/{id}",
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

}
