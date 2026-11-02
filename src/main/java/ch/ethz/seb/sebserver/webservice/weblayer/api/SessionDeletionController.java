package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.session.SessionDeletionReport;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ScheduledDeleteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SESSION_DELETE_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class SessionDeletionController {

    private static final Logger log = LoggerFactory.getLogger(SessionDeletionController.class);

    private final ScheduledDeleteService scheduledDeleteService;
    private final AuthorizationService authorizationService;
    private final UserActivityLogDAO userActivityLogDAO;

    public SessionDeletionController(
            final ScheduledDeleteService scheduledDeleteService,
            final AuthorizationService authorizationService,
            final UserActivityLogDAO userActivityLogDAO) {

        this.scheduledDeleteService = scheduledDeleteService;
        this.authorizationService = authorizationService;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SessionDeletionReport getDeletionReport(
            @RequestParam(name = SessionDeletionReport.ATTR_SEARCH_NAME) final String searchName,
            @RequestParam(name = SessionDeletionReport.ATTR_DELETE_DUE_TIME, required = false) final Long deleteDueTimestampUTC) {


        authorizationService.check(PrivilegeType.READ, EntityType.CLIENT_CONNECTION);

        return scheduledDeleteService
                .requestSessionDeletion(searchName, deleteDueTimestampUTC)
                .getOrThrow();
    }

    @RequestMapping(
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SessionDeletionReport deleteSessions(
            @RequestParam(name = SessionDeletionReport.ATTR_SEARCH_NAME) final String searchName,
            @RequestParam(name = SessionDeletionReport.ATTR_DELETE_DUE_TIME, required = false) final Long deleteDueTimestampUTC,
            @RequestParam(name = SessionDeletionReport.ATTR_EXCLUDE_LIST, required = false) final List<String> excludes)  {

        authorizationService.check(PrivilegeType.READ, EntityType.CLIENT_CONNECTION);

        final Set<String> ex = (excludes != null && !excludes.isEmpty())
                ? new HashSet<>(excludes)
                : null;

        return scheduledDeleteService
                .deleteUserSessions(searchName, deleteDueTimestampUTC, ex)
                .map(report -> {
                    report.sebServerDeletions().forEach(info -> {
                        try {
                            userActivityLogDAO.log(UserLogActivityType.DELETE, EntityType.CLIENT_CONNECTION, info.uuid(), info.uuid());
                        } catch (Exception e) {
                            log.warn("Failed to log user session deletion: {} cause {}", info, e.getMessage());
                        }
                    });
                    return report;
                })
                .getOrThrow();
    }
}
