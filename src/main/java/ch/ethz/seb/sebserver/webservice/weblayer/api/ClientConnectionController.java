/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_CONNECTION_ENDPOINT)
public class ClientConnectionController extends ReadonlyEntityController<ClientConnection, ClientConnection> {

    private final SEBClientSessionService sebClientSessionService;

    protected ClientConnectionController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientConnectionDAO clientConnectionDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final SEBClientSessionService sebClientSessionServic) {

        super(authorization,
                bulkActionService,
                clientConnectionDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.sebClientSessionService = sebClientSessionServic;
    }

    @Override
    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ClientConnection> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> filterCriteria,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(filterCriteria, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        return super.getPage(institutionId, pageNumber, pageSize, sort, filterCriteria, request);
    }

    @Operation(
            summary = "Get a page of ClientConnectionData domain entity. Sorting and filtering is applied before paging",
            description = """
                    Sorting: the sort parameter to sort the list of entities before paging
                    the sort parameter is the name of the entity-model attribute to sort with a leading '-' sign for
                    descending sort order. Note that not all entity-model attribute are suited for sorting while the most
                    are.
                    </p>
                    Filter: The filter attributes accepted by this API depend on the actual entity model (domain object)
                    and are of the form [domain-attribute-name]=[filter-value]. E.g.: name=abc or type=EXAM. Usually
                    filter attributes of text type are treated as SQL wildcard with %[text]% to filter all text containing
                    a given text-snippet.""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) }),
            parameters = {
                    @Parameter(
                            name = Page.ATTR_PAGE_NUMBER,
                            description = "The number of the page to get from the whole list. If the page does not exists, the API retruns with the first page."),
                    @Parameter(
                            name = Page.ATTR_PAGE_SIZE,
                            description = "The size of the page to get."),
                    @Parameter(
                            name = Page.ATTR_SORT,
                            description = "the sort parameter to sort the list of entities before paging"),
                    @Parameter(
                            name = API.PARAM_INSTITUTION_ID,
                            description = "The institution identifier of the request.\n"
                                    + "Default is the institution identifier of the institution of the current user"),
                    @Parameter(
                            name = "filterCriteria",
                            description = "Additional filter criterias \n" +
                                    "For OpenAPI 3 input please use the form: {\"columnName\":\"filterValue\"}",
                            example = "{}",
                            required = false,
                            allowEmptyValue = false)
            })
    @RequestMapping(
            path = API.SEB_CLIENT_CONNECTION_DATA_ENDPOINT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ClientConnectionData> getClientConnectionDataPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> filterCriteria,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(filterCriteria, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        if (StringUtils.isNotBlank(sort) && sort.contains(ClientConnectionData.ATTR_INDICATOR_VALUE)) {

            final Collection<ClientConnectionData> allConnections = getAllData(filterMap)
                    .getOrThrow();

            return this.paginationService.buildPageFromList(
                    pageNumber,
                    pageSize,
                    sort,
                    allConnections,
                    c -> c.stream().sorted(new IndicatorValueComparator(sort)).collect(Collectors.toList()));
        } else {

            return this.paginationService.getPage(
                    pageNumber,
                    pageSize,
                    sort,
                    getSQLTableOfEntity().tableNameAtRuntime(),
                    () -> getAllData(filterMap))
                    .getOrThrow();
        }
    }

    @RequestMapping(
            path = API.SEB_CLIENT_CONNECTION_DATA_ENDPOINT + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ClientConnectionData getClientConnectionDataBy(@PathVariable final String modelId) {
        return this.sebClientSessionService
                .getIndicatorValues(super.getBy(modelId))
                .getOrThrow();
    }

    @Override
    public Collection<EntityDependency> getDependencies(
            final String modelId,
            final BulkActionType bulkActionType,
            final boolean addIncludes,
            final List<String> includes) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord;
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        checkRead(institutionId);
    }

    @Override
    protected Result<ClientConnection> checkReadAccess(final ClientConnection entity) {
        return Result.tryCatch(() -> {
            checkRead(entity.institutionId);
            return entity;
        });
    }

    @Override
    protected boolean hasReadAccess(final ClientConnection entity) {
        return checkReadAccess(entity).hasValue();
    }

    private void checkRead(final Long institution) {
        this.authorization.checkRole(
                institution,
                EntityType.CLIENT_EVENT,
                UserRole.EXAM_ADMIN, UserRole.EXAM_SUPPORTER, UserRole.TEACHER);
    }

    private Result<Collection<ClientConnectionData>> getAllData(final FilterMap filterMap) {
        return getAll(filterMap)
                .map(connections -> connections.stream()
                        .map(this.sebClientSessionService::getIndicatorValues)
                        .flatMap(Result::onErrorLogAndSkip)
                        .collect(Collectors.toList()));
    }

    private Predicate<ClientConnection> getClientConnectionFilter(final FilterMap filterMap) {
        final String infoFilter = filterMap.getString(ClientConnection.FILTER_ATTR_INFO);
        Predicate<ClientConnection> filter = Utils.truePredicate();
        if (StringUtils.isNotBlank(infoFilter)) {
            filter = c -> c.getInfo() == null || c.getInfo().contains(infoFilter);
        }
        return filter;
    }

    private Predicate<ClientConnectionData> getClientConnectionDataFilter(final FilterMap filterMap) {
        final Predicate<ClientConnection> clientConnectionFilter = getClientConnectionFilter(filterMap);
        return ccd -> clientConnectionFilter.test(ccd.clientConnection);
    }

    private static final class IndicatorValueComparator implements Comparator<ClientConnectionData> {

        //final ClientConnectionComparator clientConnectionComparator;
        final String sort;
        final Long iValuePK;
        final boolean descending;

        IndicatorValueComparator(final String sort) {
            this.sort = sort;
            iValuePK = Long.valueOf(StringUtils.split(sort, Constants.UNDERLINE)[1]);
            this.descending = PageSortOrder.getSortOrder(sort) == PageSortOrder.DESCENDING;
        }

        @Override
        public int compare(final ClientConnectionData cc1, final ClientConnectionData cc2) {
            final Double v1 = cc1.getIndicatorValue(iValuePK);
            final Double v2 = cc2.getIndicatorValue(iValuePK);
            final int result = ObjectUtils.compare(v1, v2);
            return (this.descending) ? -result : result;
        }
    }
}
