/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import javax.validation.Valid;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.RevokeTokenEndpoint;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_USER_ACCOUNT)
public class UserAccountController {

    private final UserDAO userDao;
    private final AuthorizationGrantService authorizationGrantService;
    private final UserService userService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final PaginationService paginationService;
    private final BulkActionService bulkActionService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserAccountController(
            final UserDAO userDao,
            final AuthorizationGrantService authorizationGrantService,
            final UserService userService,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final ApplicationEventPublisher applicationEventPublisher) {

        this.userDao = userDao;
        this.authorizationGrantService = authorizationGrantService;
        this.userService = userService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.paginationService = paginationService;
        this.bulkActionService = bulkActionService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserInfo> getAll(
            @RequestParam(name = UserFilter.FILTER_ATTR_INSTITUTION, required = false) final Long institutionId,
            @RequestParam(name = UserFilter.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = UserFilter.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = UserFilter.FILTER_ATTR_USER_NAME, required = false) final String username,
            @RequestParam(name = UserFilter.FILTER_ATTR_EMAIL, required = false) final String email,
            @RequestParam(name = UserFilter.FILTER_ATTR_LOCALE, required = false) final String locale) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY);

        this.paginationService.setDefaultLimit(UserRecordDynamicSqlSupport.userRecord);
        return getAll(createUserFilter(institutionId, active, name, username, email, locale));
    }

    @RequestMapping(path = "/page", method = RequestMethod.GET)
    public Page<UserInfo> getPage(
            @RequestParam(name = UserFilter.FILTER_ATTR_INSTITUTION, required = false) final Long institutionId,
            @RequestParam(name = UserFilter.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = UserFilter.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = UserFilter.FILTER_ATTR_USER_NAME, required = false) final String username,
            @RequestParam(name = UserFilter.FILTER_ATTR_EMAIL, required = false) final String email,
            @RequestParam(name = UserFilter.FILTER_ATTR_LOCALE, required = false) final String locale,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String sortBy,
            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final Page.SortOrder sortOrder) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY);

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sortBy,
                sortOrder,
                UserRecordDynamicSqlSupport.userRecord,
                () -> getAll(createUserFilter(institutionId, active, name, username, email, locale)));

    }

    @RequestMapping(path = "/me", method = RequestMethod.GET)
    public UserInfo loggedInUser() {
        return this.userService
                .getCurrentUser()
                .getUserInfo();
    }

    @RequestMapping(path = "/{uuid}", method = RequestMethod.GET)
    public UserInfo accountInfo(@PathVariable final String uuid) {
        return this.userDao
                .byUuid(uuid)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantOnEntity(
                        userInfo,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();

    }

    @RequestMapping(path = "/create", method = RequestMethod.PUT)
    public UserInfo createUser(@Valid @RequestBody final UserMod userData) {
        return _saveUser(userData, PrivilegeType.WRITE)
                .getOrThrow();
    }

    @RequestMapping(path = "/save", method = RequestMethod.POST)
    public UserInfo saveUser(@Valid @RequestBody final UserMod userData) {
        return _saveUser(userData, PrivilegeType.MODIFY)
                .getOrThrow();

    }

    @RequestMapping(path = "/{uuid}/activate", method = RequestMethod.POST)
    public UserInfo activateUser(@PathVariable final String uuid) {
        return setActive(uuid, true);
    }

    @RequestMapping(value = "/{uuid}/deactivate", method = RequestMethod.POST)
    public UserInfo deactivateUser(@PathVariable final String uuid) {
        return setActive(uuid, false);
    }

    @RequestMapping(path = "/{uuid}/delete", method = RequestMethod.DELETE)
    public EntityProcessingReport deleteUser(@PathVariable final String uuid) {
        checkPrivilegeForUser(uuid, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.DEACTIVATE,
                EntityType.USER,
                new EntityKey(uuid, EntityType.USER, false)));
    }

    @RequestMapping(path = "/{uuid}/hard-delete", method = RequestMethod.DELETE)
    public EntityProcessingReport hardDeleteUser(@PathVariable final String uuid) {
        checkPrivilegeForUser(uuid, PrivilegeType.WRITE);

        return this.bulkActionService.createReport(new BulkAction(
                Type.HARD_DELETE,
                EntityType.USER,
                new EntityKey(uuid, EntityType.USER, false)));
    }

    private void checkPrivilegeForUser(final String uuid, final PrivilegeType type) {
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                type);

        this.userDao.byUuid(uuid)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantOnEntity(
                        userInfo,
                        type))
                .getOrThrow();
    }

    private UserInfo setActive(final String uuid, final boolean active) {
        this.checkPrivilegeForUser(uuid, PrivilegeType.MODIFY);

        this.bulkActionService.doBulkAction(new BulkAction(
                (active) ? Type.ACTIVATE : Type.DEACTIVATE,
                EntityType.USER,
                new EntityKey(uuid, EntityType.USER, false)));

        return this.userDao
                .byUuid(uuid)
                .getOrThrow();
    }

    private Result<UserInfo> _saveUser(final UserMod userData, final PrivilegeType privilegeType) {

        final ActivityType activityType = (userData.uuid == null)
                ? ActivityType.CREATE
                : ActivityType.MODIFY;

        return this.authorizationGrantService
                .checkGrantOnEntity(userData, privilegeType)
                .flatMap(this.userDao::save)
                .flatMap(userInfo -> this.userActivityLogDAO.log(activityType, userInfo))
                .flatMap(userInfo -> revokePassword(userData, userInfo));
    }

    private Result<UserInfo> revokePassword(final UserMod userData, final UserInfo userInfo) {
        // handle password change; revoke access tokens if password has changed
        if (userData.passwordChangeRequest() && userData.newPasswordMatch()) {
            this.applicationEventPublisher.publishEvent(
                    new RevokeTokenEndpoint.RevokeTokenEvent(this, userInfo.username));
        }
        return Result.of(userInfo);
    }

    private Collection<UserInfo> getAll(final UserFilter userFilter) {
        if (this.authorizationGrantService.hasBasePrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY)) {

            return this.userDao
                    .all(userFilter)
                    .getOrThrow();

        } else {

            return this.userDao.all(
                    userFilter,
                    this.authorizationGrantService.getGrantFilter(
                            EntityType.USER,
                            PrivilegeType.READ_ONLY))
                    .getOrThrow();
        }
    }

    private UserFilter createUserFilter(
            final Long institutionId,
            final Boolean active,
            final String name,
            final String username,
            final String email,
            final String locale) {

        return (institutionId != null || active != null || name != null ||
                username != null || email != null || locale != null)
                        ? new UserFilter(institutionId, name, username, email, active, locale)
                        : null;
    }

}
