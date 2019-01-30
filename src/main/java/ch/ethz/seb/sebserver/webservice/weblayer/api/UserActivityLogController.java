/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.SEBServerRestEndpoints;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + SEBServerRestEndpoints.ENDPOINT_USER_ACTIVITY_LOG)
public class UserActivityLogController {

    private final UserActivityLogDAO userActivityLogDAO;
    private final AuthorizationGrantService authorizationGrantService;
    private final PaginationService paginationService;

    public UserActivityLogController(
            final UserActivityLogDAO userActivityLogDAO,
            final AuthorizationGrantService authorizationGrantService,
            final PaginationService paginationService) {

        this.userActivityLogDAO = userActivityLogDAO;
        this.authorizationGrantService = authorizationGrantService;
        this.paginationService = paginationService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorizationGrantService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<UserActivityLog> getPage(
            @RequestParam(
                    name = UserActivityLog.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = UserActivityLog.FILTER_ATTR_USER, required = false) final String userId,
            @RequestParam(name = UserActivityLog.FILTER_ATTR_FROM, required = false) final String from,
            @RequestParam(name = UserActivityLog.FILTER_ATTR_TO, required = false) final String to,
            @RequestParam(name = UserActivityLog.FILTER_ATTR_ACTIVITY_TYPES,
                    required = false) final String activityTypes,
            @RequestParam(name = UserActivityLog.FILTER_ATTR_ENTITY_TYPES, required = false) final String entityTypes,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        checkBaseReadPrivilege(institutionId);
        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                UserRecordDynamicSqlSupport.userRecord,
                () -> _getAll(institutionId, userId, from, to, activityTypes, entityTypes)).getOrThrow();
    }

//    @RequestMapping(path = "/page/{userId}", method = RequestMethod.GET)
//    public Page<UserActivityLog> getPageForUser(
//            @PathVariable final String userId,
//            @RequestParam(
//                    name = UserActivityLog.FILTER_ATTR_INSTITUTION,
//                    required = true,
//                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
//            @RequestParam(name = UserActivityLog.FILTER_ATTR_FROM, required = false) final String from,
//            @RequestParam(name = UserActivityLog.FILTER_ATTR_TO, required = false) final String to,
//            @RequestParam(name = UserActivityLog.FILTER_ATTR_ACTIVITY_TYPES,
//                    required = false) final String activityTypes,
//            @RequestParam(name = UserActivityLog.FILTER_ATTR_ENTITY_TYPES, required = false) final String entityTypes,
//            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
//            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
//            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String sortBy,
//            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final Page.SortOrder sortOrder) {
//
//        checkBaseReadPrivilege(institutionId);
//        return this.paginationService.getPage(
//                pageNumber,
//                pageSize,
//                sortBy,
//                sortOrder,
//                UserRecordDynamicSqlSupport.userRecord,
//                () -> _getAll(institutionId, userId, from, to, activityTypes, entityTypes));
//    }

    private Result<Collection<UserActivityLog>> _getAll(
            final Long institutionId,
            final String userId,
            final String from,
            final String to,
            final String activityTypes,
            final String entityTypes) {

        return Result.tryCatch(() -> {

            this.paginationService.setDefaultLimitIfNotSet();

            return this.userActivityLogDAO.all(
                    institutionId,
                    userId,
                    Utils.toMilliSeconds(from),
                    Utils.toMilliSeconds(to),
                    activityTypes,
                    entityTypes,
                    log -> true).getOrThrow();
        });
    }

    private void checkBaseReadPrivilege(final Long institutionId) {
        this.authorizationGrantService.checkPrivilege(
                EntityType.USER_ACTIVITY_LOG,
                PrivilegeType.READ_ONLY,
                institutionId);
    }

}
