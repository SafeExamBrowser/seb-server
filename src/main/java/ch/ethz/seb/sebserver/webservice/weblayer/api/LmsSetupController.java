/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityKeyAndName;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LMSType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_LMS_SETUP)
public class LmsSetupController {

    private final LmsSetupDAO lmsSetupDAO;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final BulkActionService bulkActionService;

    public LmsSetupController(
            final LmsSetupDAO lmsSetupDAO,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final BulkActionService bulkActionService) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.authorizationGrantService = authorizationGrantService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.bulkActionService = bulkActionService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<LmsSetup> getAll(
            @RequestParam(name = LmsSetup.FILTER_ATTR_INSTITUTION, required = false) final Long institutionId,
            @RequestParam(name = LmsSetup.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = LmsSetup.FILTER_ATTR_LMS_TYPE, required = false) final LMSType lmsType,
            @RequestParam(name = LmsSetup.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        checkBaseReadPrivilege();

        final SEBServerUser currentUser = this.authorizationGrantService
                .getUserService()
                .getCurrentUser();
        final Long usersInstitution = currentUser.institutionId();
        final Long instId = (institutionId != null) ? institutionId : usersInstitution;

        if (!this.authorizationGrantService.hasBasePrivilege(
                EntityType.LMS_SETUP,
                PrivilegeType.READ_ONLY) &&
                instId != usersInstitution) {

            throw new PermissionDeniedException(
                    EntityType.LMS_SETUP,
                    PrivilegeType.READ_ONLY,
                    currentUser.getUserInfo().uuid);
        }

        return this.lmsSetupDAO
                .allMatching(instId, name, lmsType, active)
                .getOrThrow();
    }

    @RequestMapping(path = "/names", method = RequestMethod.GET)
    public Collection<EntityKeyAndName> getNames(
            @RequestParam(name = LmsSetup.FILTER_ATTR_INSTITUTION, required = false) final Long institutionId,
            @RequestParam(name = LmsSetup.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = LmsSetup.FILTER_ATTR_LMS_TYPE, required = false) final LMSType lmsType,
            @RequestParam(name = LmsSetup.FILTER_ATTR_ACTIVE, required = false) final Boolean active) {

        return getAll(institutionId, name, lmsType, active)
                .stream()
                .map(LmsSetup::toName)
                .collect(Collectors.toList());
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public LmsSetup getById(@PathVariable final Long id) {

        checkBaseReadPrivilege();

        return this.lmsSetupDAO
                .byId(id)
                .flatMap(inst -> this.authorizationGrantService.checkGrantOnEntity(
                        inst,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();
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
    public LmsSetup activate(@PathVariable final Long id) {
        return setActive(id, true);
    }

    @RequestMapping(value = "/{id}/deactivate", method = RequestMethod.POST)
    public LmsSetup deactivate(@PathVariable final Long id) {
        return setActive(id, false);
    }

    @RequestMapping(path = "/{id}/delete", method = RequestMethod.DELETE)
    public EntityProcessingReport deleteUser(@PathVariable final Long id) {
        checkPrivilegeForInstitution(id, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.DEACTIVATE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)));
    }

    @RequestMapping(path = "/{id}/hard-delete", method = RequestMethod.DELETE)
    public EntityProcessingReport hardDeleteUser(@PathVariable final Long id) {
        checkPrivilegeForInstitution(id, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.HARD_DELETE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)));
    }

    private void checkPrivilegeForInstitution(final Long id, final PrivilegeType type) {
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.LMS_SETUP,
                type);

        this.lmsSetupDAO.byId(id)
                .flatMap(institution -> this.authorizationGrantService.checkGrantOnEntity(
                        institution,
                        type))
                .getOrThrow();
    }

    private LmsSetup setActive(final Long id, final boolean active) {
        checkPrivilegeForInstitution(id, PrivilegeType.MODIFY);

        this.bulkActionService.doBulkAction(new BulkAction(
                (active) ? Type.ACTIVATE : Type.DEACTIVATE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)));

        return this.lmsSetupDAO
                .byId(id)
                .getOrThrow();
    }

    private Result<LmsSetup> save(final LmsSetup lmsSetup, final PrivilegeType privilegeType) {

        final ActivityType activityType = (lmsSetup.id == null)
                ? ActivityType.CREATE
                : ActivityType.MODIFY;

        return this.authorizationGrantService
                .checkGrantOnEntity(lmsSetup, privilegeType)
                .flatMap(this.lmsSetupDAO::save)
                .flatMap(inst -> this.userActivityLogDAO.log(activityType, inst));
    }

    private void checkBaseReadPrivilege() {
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.LMS_SETUP,
                PrivilegeType.READ_ONLY);
    }

}
