/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@Tag(name = "SEB Client Event", description = "SEB client event logs and export")
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_CLIENT_EVENT_ENDPOINT)
public class ClientEventController extends ReadonlyEntityController<ClientEvent, ClientEvent> {

    private final ExamDAO examDao;
    private final ClientEventDAO clientEventDAO;
    private final SEBClientEventAdminService sebClientEventAdminService;

    protected ClientEventController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final ClientEventDAO entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final ExamDAO examDao,
            final SEBClientEventAdminService sebClientEventAdminService) {

        super(authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDao = examDao;
        this.clientEventDAO = entityDAO;
        this.sebClientEventAdminService = sebClientEventAdminService;
    }

    @Operation(
            operationId = "getExtendedClientEvents",
            summary = "Get a page of ExtendedClientEvent. Sorting and filtering is applied before paging",
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
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ExtendedClientEvent> getExtendedPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> filterCriteria,
            final HttpServletRequest request) {

        // at least current user must have base read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(filterCriteria, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        return this.paginationService.getPage(
                pageNumber,
                pageSize,
                sort,
                getSQLTableOfEntity().tableNameAtRuntime(),
                () -> this.clientEventDAO.allMatchingExtended(filterMap, this::hasReadAccess))
                .getOrThrow();
    }

    @Operation(operationId = "deleteClientEvents", summary = "Hard delete a set of client events by model ID list")
    @ApiResponse(responseCode = "200", description = "Deleted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityProcessingReport hardDeleteAll(
            @RequestParam(name = API.PARAM_MODEL_ID_LIST) final List<String> ids,
            @RequestParam(name = API.PARAM_BULK_ACTION_ADD_INCLUDES, defaultValue = "false") final boolean addIncludes,
            @RequestParam(name = API.PARAM_BULK_ACTION_INCLUDES, required = false) final List<String> includes,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.checkWritePrivilege(institutionId);

        return this.sebClientEventAdminService
                .deleteAllClientEvents(ids)
                .getOrThrow();
    }

    @Operation(operationId = "exportClientEvents", summary = "Export SEB client events as a file download")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.SEB_CLIENT_EVENT_EXPORT_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportEvents(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = API.SEB_CLIENT_EVENT_EXPORT_TYPE, required = true) final ExportType type,
            @RequestParam(
                    name = API.SEB_CLIENT_EVENT_EXPORT_INCLUDE_CONNECTIONS,
                    required = false,
                    defaultValue = "true") final boolean includeConnectionDetails,
            @RequestParam(
                    name = API.SEB_CLIENT_EVENT_EXPORT_INCLUDE_EXAMS,
                    required = false,
                    defaultValue = "false") final boolean includeExamDetails,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        // at least current user must have base read access for specified entity type within its own institution
        checkReadPrivilege(institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams, request.getQueryString());
        populateFilterMap(filterMap, institutionId, sort);

        final ServletOutputStream outputStream = response.getOutputStream();
        PipedOutputStream pout;
        PipedInputStream pin;
        try {
            pout = new PipedOutputStream();
            pin = new PipedInputStream(pout);

            final SEBServerUser currentUser = this.authorization
                    .getUserService()
                    .getCurrentUser();

            this.sebClientEventAdminService.exportSEBClientLogs(
                    pout,
                    filterMap,
                    sort,
                    type,
                    includeConnectionDetails,
                    includeExamDetails,
                    currentUser);

            IOUtils.copyLarge(pin, outputStream);

            response.setStatus(HttpStatus.OK.value());

            outputStream.flush();

        } finally {
            outputStream.flush();
            outputStream.close();
        }
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
        return ClientEventRecordDynamicSqlSupport.clientEventRecord;
    }

    @Override
    protected Result<ClientEvent> checkReadAccess(final ClientEvent entity) {
        final EnumSet<UserRole> userRoles = this.authorization
                .getUserService()
                .getCurrentUser()
                .getUserRoles();
        final boolean isSupporterOnly = userRoles.size() == 1 &&
                (userRoles.contains(UserRole.EXAM_SUPPORTER) || userRoles.contains(UserRole.TEACHER));


        return Result.tryCatch(() -> {

            if (isSupporterOnly) {
                // check owner grant be getting exam
                return super.checkReadAccess(entity)
                        .getOrThrow();
            } else {
                // institutional read access
                return entity;
            }
        });
    }

    @Override
    protected boolean hasReadAccess(final ClientEvent entity) {
        return !checkReadAccess(entity).hasError();
    }

    @Override
    protected GrantEntity toGrantEntity(final ClientEvent entity) {
        return this.examDao
                .examGrantEntityByClientConnection(entity.connectionId)
                .get();
    }

    @Override
    protected void checkReadPrivilege(final Long institutionId) {
        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
        if (currentUser.institutionId().longValue() != institutionId.longValue()) {
            throw new PermissionDeniedException(
                    EntityType.CLIENT_EVENT,
                    PrivilegeType.READ,
                    currentUser.getUserInfo());
        }
    }

}
