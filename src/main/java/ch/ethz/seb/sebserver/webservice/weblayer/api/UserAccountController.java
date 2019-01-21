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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.RevokeTokenEndpoint;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_USER_ACCOUNT)
public class UserAccountController extends ActivatableEntityController<UserInfo, UserMod> {

    private final UserDAO userDao;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserAccountController(
            final UserDAO userDao,
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final ApplicationEventPublisher applicationEventPublisher) {

        super(authorizationGrantService, bulkActionService, userDao, userActivityLogDAO, paginationService);
        this.userDao = userDao;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorizationGrantService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<UserInfo> getAll(
            @RequestParam(
                    name = UserFilter.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = UserFilter.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = UserFilter.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = UserFilter.FILTER_ATTR_USER_NAME, required = false) final String username,
            @RequestParam(name = UserFilter.FILTER_ATTR_EMAIL, required = false) final String email,
            @RequestParam(name = UserFilter.FILTER_ATTR_LOCALE, required = false) final String locale) {

        checkReadPrivilege(institutionId);

        this.paginationService.setDefaultLimit(UserRecordDynamicSqlSupport.userRecord);
        return getAll(createUserFilter(institutionId, active, name, username, email, locale));
    }

    @RequestMapping(path = "/page", method = RequestMethod.GET)
    public Page<UserInfo> getPage(
            @RequestParam(
                    name = UserFilter.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = UserFilter.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = UserFilter.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = UserFilter.FILTER_ATTR_USER_NAME, required = false) final String username,
            @RequestParam(name = UserFilter.FILTER_ATTR_EMAIL, required = false) final String email,
            @RequestParam(name = UserFilter.FILTER_ATTR_LOCALE, required = false) final String locale,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String sortBy,
            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final Page.SortOrder sortOrder) {

        checkReadPrivilege(institutionId);

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
        return this.authorizationGrantService
                .getUserService()
                .getCurrentUser()
                .getUserInfo();
    }

    @Override
    protected Result<UserInfo> notifySave(final UserMod userData, final UserInfo userInfo) {
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
                    .allMatching(userFilter)
                    .getOrThrow();

        } else {

            final Predicate<GrantEntity> grantFilter = this.authorizationGrantService.getGrantFilter(
                    EntityType.USER,
                    PrivilegeType.READ_ONLY);

            return this.userDao
                    .allMatching(userFilter)
                    .getOrThrow()
                    .stream()
                    .filter(grantFilter)
                    .collect(Collectors.toList());
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
