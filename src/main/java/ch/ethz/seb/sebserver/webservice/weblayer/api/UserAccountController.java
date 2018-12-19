/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserFilter;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.activation.EntityActivationEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
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
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserAccountController(
            final UserDAO userDao,
            final AuthorizationGrantService authorizationGrantService,
            final UserService userService,
            final UserActivityLogDAO userActivityLogDAO,
            final ApplicationEventPublisher applicationEventPublisher) {

        this.userDao = userDao;
        this.authorizationGrantService = authorizationGrantService;
        this.userService = userService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserInfo> getAll(
            @RequestParam(required = false) final Long institutionId,
            @RequestParam(required = false) final Boolean active,
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final String userName,
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) final String locale) {

        // fist check if current user has any privileges for this action
        this.authorizationGrantService.checkHasAnyPrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY);

        final UserFilter userFilter = ((institutionId != null) ||
                (active != null) ||
                (name != null) ||
                (userName != null) ||
                (email != null) ||
                (locale != null))
                        ? new UserFilter(institutionId, name, userName, email, active, locale)
                        : null;

        if (this.authorizationGrantService.hasBasePrivilege(
                EntityType.USER,
                PrivilegeType.READ_ONLY)) {

            return (userFilter != null)
                    ? this.userDao.all(userFilter).getOrThrow()
                    : this.userDao.allActive().getOrThrow();

        } else {

            final Predicate<UserInfo> grantFilter = this.authorizationGrantService.getGrantFilter(
                    EntityType.USER,
                    PrivilegeType.READ_ONLY);

            if (userFilter == null) {

                return this.userDao
                        .all(userInfo -> userInfo.active && grantFilter.test(userInfo))
                        .getOrThrow();

            } else {

                return this.userDao
                        .all(userFilter)
                        .getOrThrow()
                        .stream()
                        .filter(grantFilter)
                        .collect(Collectors.toList());
            }
        }
    }

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public UserInfo loggedInUser() {
        return this.userService
                .getCurrentUser()
                .getUserInfo();
    }

    @RequestMapping(value = "/{userUUID}", method = RequestMethod.GET)
    public UserInfo accountInfo(@PathVariable final String userUUID) {
        return this.userDao
                .byUuid(userUUID)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantOnEntity(
                        userInfo,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();

    }

    @RequestMapping(value = "/create", method = RequestMethod.PUT)
    public UserInfo createUser(@Valid @RequestBody final UserMod userData) {
        return _saveUser(userData, PrivilegeType.WRITE)
                .getOrThrow();
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public UserInfo saveUser(@Valid @RequestBody final UserMod userData) {
        return _saveUser(userData, PrivilegeType.MODIFY)
                .getOrThrow();

    }

    @RequestMapping(value = "/{userUUID}/activate", method = RequestMethod.POST)
    public UserInfo activateUser(@PathVariable final String userUUID) {
        return setActivity(userUUID, true);
    }

    @RequestMapping(value = "/{userUUID}/deactivate", method = RequestMethod.POST)
    public UserInfo deactivateUser(@PathVariable final String userUUID) {
        return setActivity(userUUID, false);
    }

    private UserInfo setActivity(final String userUUID, final boolean activity) {
        return this.userDao.byUuid(userUUID)
                .flatMap(userInfo -> this.authorizationGrantService.checkGrantOnEntity(userInfo, PrivilegeType.WRITE))
                .flatMap(userInfo -> this.userDao.setActive(userInfo.uuid, activity))
                .map(userInfo -> {
                    this.applicationEventPublisher.publishEvent(new EntityActivationEvent(userInfo, activity));
                    return userInfo;
                })
                .getOrThrow();
    }

    private Result<UserInfo> _saveUser(final UserMod userData, final PrivilegeType privilegeType) {

        final ActivityType actionType = (userData.uuid == null)
                ? ActivityType.CREATE
                : ActivityType.MODIFY;

        return this.authorizationGrantService
                .checkGrantOnEntity(userData, privilegeType)
                .flatMap(this.userDao::save)
                .flatMap(userInfo -> this.userActivityLogDAO.logUserActivity(actionType, userInfo))
                .flatMap(userInfo -> {
                    // handle password change; revoke access tokens if password has changed
                    if (userData.passwordChangeRequest() && userData.newPasswordMatch()) {
                        this.applicationEventPublisher.publishEvent(
                                new RevokeTokenEndpoint.RevokeTokenEvent(this, userInfo.userName));
                    }
                    return Result.of(userInfo);
                });
    }

}
