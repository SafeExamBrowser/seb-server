/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

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
import ch.ethz.seb.sebserver.gbl.model.Domain;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_CONNECTION_ENDPOINT)
public class ClientConnectionController extends ReadonlyEntityController<ClientConnection, ClientConnection> {

    private final SEBClientConnectionService sebClientConnectionService;

    private static final Set<String> EXT_FILTER = new HashSet<>(Arrays.asList(ClientConnection.FILTER_ATTR_INFO));

    protected ClientConnectionController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientConnectionDAO clientConnectionDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final SEBClientConnectionService sebClientConnectionService) {

        super(authorization,
                bulkActionService,
                clientConnectionDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.sebClientConnectionService = sebClientConnectionService;
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
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        if (StringUtils.isNotBlank(sort) || filterMap.containsAny(EXT_FILTER)) {

            final Collection<ClientConnection> allConnections = getAll(filterMap)
                    .getOrThrow();

            return this.paginationService.buildPageFromList(
                    pageNumber,
                    pageSize,
                    sort,
                    allConnections,
                    pageClientConnectionFunction(filterMap, sort));

        } else {
            return super.getPage(institutionId, pageNumber, pageSize, sort, allRequestParams, request);
        }
    }

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
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        // at least current user must have read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        if (StringUtils.isNotBlank(sort) || filterMap.containsAny(EXT_FILTER)) {

            final Collection<ClientConnectionData> allConnections = getAllData(filterMap)
                    .getOrThrow();

            return this.paginationService.buildPageFromList(
                    pageNumber,
                    pageSize,
                    sort,
                    allConnections,
                    pageClientConnectionDataFunction(filterMap, sort));
        } else {

            return this.paginationService.getPage(
                    pageNumber,
                    pageSize,
                    sort,
                    getSQLTableOfEntity().name(),
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
        return this.sebClientConnectionService
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
                UserRole.EXAM_ADMIN,
                UserRole.EXAM_SUPPORTER);
    }

    private Result<Collection<ClientConnectionData>> getAllData(final FilterMap filterMap) {
        return getAll(filterMap)
                .map(connections -> connections.stream()
                        .map(this.sebClientConnectionService::getIndicatorValues)
                        .flatMap(Result::onErrorLogAndSkip)
                        .collect(Collectors.toList()));
    }

    private Function<Collection<ClientConnection>, List<ClientConnection>> pageClientConnectionFunction(
            final FilterMap filterMap,
            final String sort) {

        return connections -> {

            final List<ClientConnection> filtered = connections
                    .stream()
                    .filter(getClientConnectionFilter(filterMap))
                    .collect(Collectors.toList());

            if (StringUtils.isNotBlank(sort)) {
                filtered.sort(new ClientConnectionComparator(sort));
            }
            return filtered;
        };
    }

    private Function<Collection<ClientConnectionData>, List<ClientConnectionData>> pageClientConnectionDataFunction(
            final FilterMap filterMap,
            final String sort) {

        return connections -> {

            final List<ClientConnectionData> filtered = connections
                    .stream()
                    .filter(getClientConnectionDataFilter(filterMap))
                    .collect(Collectors.toList());

            if (StringUtils.isNotBlank(sort)) {
                filtered.sort(new ClientConnectionDataComparator(sort));
            }
            return filtered;
        };
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

    private static final class ClientConnectionComparator implements Comparator<ClientConnection> {

        final String sortColumn;
        final boolean descending;

        ClientConnectionComparator(final String sort) {
            this.sortColumn = PageSortOrder.decode(sort);
            this.descending = PageSortOrder.getSortOrder(sort) == PageSortOrder.DESCENDING;
        }

        @Override
        public int compare(final ClientConnection cc1, final ClientConnection cc2) {
            int result = 0;
            if (Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID.equals(this.sortColumn)) {
                result = cc1.userSessionId
                        .compareTo(cc2.userSessionId);
            } else if (ClientConnection.ATTR_INFO.equals(this.sortColumn)) {
                result = cc1.getInfo().compareTo(cc2.getInfo());
            } else if (Domain.CLIENT_CONNECTION.ATTR_STATUS.equals(this.sortColumn)) {
                result = cc1.getStatus()
                        .compareTo(cc2.getStatus());
            } else {
                result = cc1.userSessionId
                        .compareTo(cc2.userSessionId);
            }
            return (this.descending) ? -result : result;
        }
    }

    private static final class ClientConnectionDataComparator implements Comparator<ClientConnectionData> {

        final ClientConnectionComparator clientConnectionComparator;

        ClientConnectionDataComparator(final String sort) {
            this.clientConnectionComparator = new ClientConnectionComparator(sort);
        }

        @Override
        public int compare(final ClientConnectionData cc1, final ClientConnectionData cc2) {
            if (this.clientConnectionComparator.sortColumn.startsWith(ClientConnectionData.ATTR_INDICATOR_VALUE)) {
                try {
                    final Long iValuePK = Long.valueOf(StringUtils.split(
                            this.clientConnectionComparator.sortColumn,
                            Constants.UNDERLINE)[1]);
                    final Double indicatorValue1 = cc1.getIndicatorValue(iValuePK);
                    final Double indicatorValue2 = cc2.getIndicatorValue(iValuePK);
                    final int result = indicatorValue1.compareTo(indicatorValue2);
                    return (this.clientConnectionComparator.descending) ? -result : result;
                } catch (final Exception e) {
                    this.clientConnectionComparator.compare(cc1.clientConnection, cc2.clientConnection);
                }
            }

            return this.clientConnectionComparator.compare(cc1.clientConnection, cc2.clientConnection);
        }
    }
}
