/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.USER_ACTIVITY_LOG_ENDPOINT)
public class UserActivityLogController {

    private final UserActivityLogDAO userActivityLogDAO;
    private final AuthorizationService authorization;
    private final PaginationService paginationService;

    public UserActivityLogController(
            final UserActivityLogDAO userActivityLogDAO,
            final AuthorizationService authorization,
            final PaginationService paginationService) {

        this.userActivityLogDAO = userActivityLogDAO;
        this.authorization = authorization;
        this.paginationService = paginationService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorization
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    /** Rest endpoint to get a Page UserActivityLog.
     *
     * GET /{api}/{entity-type-endpoint-name}
     *
     * GET /admin-api/v1/useractivity
     * GET /admin-api/v1/useractivity?page_number=2&page_size=10&sort=-name
     * GET /admin-api/v1/useractivity?name=seb&active=true
     *
     * @param institutionId The institution identifier of the request.
     *            Default is the institution identifier of the institution of the current user
     * @param pageNumber the number of the page that is requested
     * @param pageSize the size of the page that is requested
     * @param sort the sort parameter to sort the list of entities before paging
     *            the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
     *            descending sort order
     * @param allRequestParams a MultiValueMap of all request parameter that is used for filtering
     * @return Page of domain-model-entities of specified type */
    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<UserActivityLog> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        // at least current user must have read access for specified entity type within its own institution
        checkBaseReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams);
        filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        return this.paginationService.<UserActivityLog> getPage(
                pageNumber,
                pageSize,
                sort,
                UserActivityLogRecordDynamicSqlSupport.userActivityLogRecord.name(),
                () -> this.userActivityLogDAO.allMatching(filterMap))
                .getOrThrow();
    }

    private void checkBaseReadPrivilege(final Long institutionId) {
        this.authorization.check(
                PrivilegeType.READ,
                EntityType.USER_ACTIVITY_LOG,
                institutionId);
    }

}
