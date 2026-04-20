package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteReport;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ScheduledDeleteRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PermissionDeniedException;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ScheduledDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Objects;

@Tag(name = "Scheduled Delete", description = "Scheduled data deletion management")
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SCHEDULED_DELETE_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class ScheduledDeleteController {

    private final ScheduledDeleteService scheduledDeleteService;
    private final AuthorizationService authorizationService;
    private final PaginationService paginationService;
    private final UserActivityLogDAO userActivityLogDAO;

    public ScheduledDeleteController(
            final ScheduledDeleteService scheduledDeleteService,
            final AuthorizationService authorizationService,
            final PaginationService paginationService,
            final UserActivityLogDAO userActivityLogDAO) {

        this.scheduledDeleteService = scheduledDeleteService;
        this.authorizationService = authorizationService;
        this.paginationService = paginationService;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @Operation(operationId = "getScheduledDeletes", summary = "Get a page of scheduled delete entries")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ScheduledDelete> getPage(
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @Parameter(hidden = true) @RequestParam final MultiValueMap<String, String> filterCriteria,
            final HttpServletRequest request) {

        authorizationService.check(PrivilegeType.READ, EntityType.SCHEDULED_DELETE);
        final Long institutionId = authorizationService.getUserService().getCurrentUser().institutionId();
        final FilterMap filterMap = new FilterMap(filterCriteria, request.getQueryString());
        // if current user has no read access for specified entity type within other institution then its own institution,
        // then the current users institutionId is put as a SQL filter criteria attribute to extends query performance
        if (!this.authorizationService.hasGrant(PrivilegeType.READ, EntityType.SCHEDULED_DELETE)) {
            filterMap.putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));
        }

        final Page<ScheduledDelete> page = this.paginationService.getPage(
                        pageNumber,
                        pageSize,
                        sort,
                        ScheduledDeleteRecordDynamicSqlSupport.scheduledDeleteRecord.tableNameAtRuntime(),
                        () -> scheduledDeleteService.getAll(filterMap))
                .getOrThrow();

        return page;
    }

    @Operation(operationId = "getScheduledDelete", summary = "Get full scheduled delete report by model ID")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScheduledDeleteReport getFullReportById(@PathVariable final String modelId) {

        authorizationService.check(PrivilegeType.READ, EntityType.SCHEDULED_DELETE);
        return this.scheduledDeleteService
                .getReportById(modelId)
                .map(this::checkReadAccess)
                .getOrThrow();
    }

    @Operation(
        operationId = "scheduleSessionDeletion",
        summary = "Create a new scheduled delete entry",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = { @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) })
    )
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScheduledDeleteReport create(
            @RequestParam(name = Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME) final Long deleteDueTimestampUTC) {

        final Long institutionId = authorizationService.getUserService().getCurrentUser().institutionId();
        authorizationService.check(PrivilegeType.WRITE, EntityType.SCHEDULED_DELETE, institutionId);

        return this.scheduledDeleteService
                .createScheduledDelete(deleteDueTimestampUTC, null)
                .map(report -> {
                    userActivityLogDAO.logCreate(new EntityKey(report.id(), EntityType.SCHEDULED_DELETE));
                    return report;
                })
                .getOrThrow();
    }

    @Operation(operationId = "cancelScheduledDelete", summary = "Cancel and delete a scheduled delete entry by model ID")
    @ApiResponse(responseCode = "200", description = "Deleted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE ,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityKey delete(@PathVariable final String modelId) {

        final Long institutionId = authorizationService.getUserService().getCurrentUser().institutionId();
        authorizationService.check(PrivilegeType.WRITE, EntityType.SCHEDULED_DELETE, institutionId);

        return this.scheduledDeleteService
                .deleteScheduledDeletion(modelId)
                .map(report -> {
                    userActivityLogDAO.logDelete(report);
                    return report;
                })
                .getOrThrow();
    }

    private ScheduledDeleteReport checkReadAccess(final ScheduledDeleteReport scheduledDeleteReport) {
        SEBServerUser currentUser = authorizationService.getUserService().getCurrentUser();
        if (!Objects.equals(scheduledDeleteReport.institutionId(), currentUser.institutionId())) {
            throw new PermissionDeniedException(
                    EntityType.SCHEDULED_DELETE,
                    PrivilegeType.READ,
                    currentUser.getUserInfo());
        }

        return scheduledDeleteReport;
    }

    @Operation(
            operationId = "markExcludeScheduledDelete",
            summary = "Mark an exam as excluded from the scheduled delete"
    )
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.SCHEDULED_DELETE_MARK_EXCLUDE + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScheduledDeleteReport markExclude(@PathVariable final String modelId) {
        return markExclude(modelId, false);
    }

    @Operation(
            operationId = "unmarkIncludeScheduledDelete",
            summary = "Unmark an exam exclusion so it is included in the scheduled delete again"
    )
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.SCHEDULED_DELETE_UNMARK_INCLUDE + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ScheduledDeleteReport unmarkInclude(@PathVariable final String modelId) {
        return markExclude(modelId, true);
    }

    private ScheduledDeleteReport markExclude(String modelId, boolean reset) {
        final Long institutionId = authorizationService.getUserService().getCurrentUser().institutionId();
        authorizationService.check(PrivilegeType.WRITE, EntityType.SCHEDULED_DELETE, institutionId);

        return this.scheduledDeleteService
                .applyExcludeForDeletion(Collections.singletonList(modelId), reset)
                .getOrThrow()
                .getElement();
    }

}
