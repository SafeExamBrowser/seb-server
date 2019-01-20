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
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_LMS_SETUP)
public class LmsSetupController {

    private final LmsSetupDAO lmsSetupDAO;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final BulkActionService bulkActionService;
    private final LmsAPIService lmsAPIService;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService,
            final LmsAPIService lmsAPIService) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.authorizationGrantService = authorizationGrantService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.bulkActionService = bulkActionService;
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

    @RequestMapping(path = "/names", method = RequestMethod.GET)
    public Collection<EntityKeyAndName> getNames(
            @RequestParam(
                    name = LmsSetup.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = LmsSetup.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = LmsSetup.FILTER_ATTR_LMS_TYPE, required = false) final LmsType lmsType,
            @RequestParam(name = LmsSetup.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        checkReadPrivilege(institutionId);

        return getAll(institutionId, name, lmsType, active)
                .stream()
                .map(LmsSetup::toName)
                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public LmsSetup getById(@PathVariable final Long id) {
        return this.lmsSetupDAO
                .byPK(id)
                .flatMap(lmsSetup -> this.authorizationGrantService.checkGrantOnEntity(
                        lmsSetup,
                        PrivilegeType.READ_ONLY))
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

    @RequestMapping(path = "/create", method = RequestMethod.PUT)
    public LmsSetup create(@Valid @RequestBody final LmsSetup lmsSetup) {
        return save(lmsSetup, PrivilegeType.WRITE)
                .getOrThrow();
    }

    @RequestMapping(path = "/save", method = RequestMethod.POST)
    public LmsSetup save(@Valid @RequestBody final LmsSetup lmsSetup) {
        return save(lmsSetup, PrivilegeType.MODIFY)
                .getOrThrow();
    }

    @RequestMapping(path = "/{id}/activate", method = RequestMethod.POST)
    public EntityProcessingReport activate(@PathVariable final Long id) {
        return setActive(id, true);
    }

    @RequestMapping(value = "/{id}/deactivate", method = RequestMethod.POST)
    public EntityProcessingReport deactivate(@PathVariable final Long id) {
        return setActive(id, false);
    }

    @RequestMapping(path = "/{id}/delete", method = RequestMethod.DELETE)
    public EntityProcessingReport deleteUser(@PathVariable final Long id) {
        checkPrivilegeForInstitution(id, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.DEACTIVATE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)))
                .getOrThrow();
    }

    @RequestMapping(path = "/{id}/hard-delete", method = RequestMethod.DELETE)
    public EntityProcessingReport hardDeleteUser(@PathVariable final Long id) {
        checkPrivilegeForInstitution(id, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.HARD_DELETE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)))
                .getOrThrow();
    }

    private void checkPrivilegeForInstitution(final Long lmsSetupId, final PrivilegeType type) {
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.LMS_SETUP,
                type);

        this.lmsSetupDAO.byPK(lmsSetupId)
                .flatMap(institution -> this.authorizationGrantService.checkGrantOnEntity(
                        institution,
                        type))
                .getOrThrow();
    }

    private EntityProcessingReport setActive(final Long id, final boolean active) {
        checkPrivilegeForInstitution(id, PrivilegeType.MODIFY);

        return this.bulkActionService.createReport(new BulkAction(
                (active) ? Type.ACTIVATE : Type.DEACTIVATE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)))
                .getOrThrow();
    }

    private Result<LmsSetup> save(final LmsSetup lmsSetup, final PrivilegeType privilegeType) {

        final ActivityType activityType = (lmsSetup.id == null)
                ? ActivityType.CREATE
                : ActivityType.MODIFY;

        return this.authorizationGrantService
                .checkGrantOnEntity(lmsSetup, privilegeType)
                .flatMap(this.lmsSetupDAO::save)
                .flatMap(exam -> this.userActivityLogDAO.log(activityType, exam));
    }

    private void checkReadPrivilege(final Long institutionId) {
        this.authorizationGrantService.checkPrivilege(
                EntityType.LMS_SETUP,
                PrivilegeType.READ_ONLY,
                institutionId);
    }

}
