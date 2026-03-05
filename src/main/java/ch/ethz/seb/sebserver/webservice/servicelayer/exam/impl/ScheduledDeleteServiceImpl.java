package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteInfo;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteReport;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Nullable;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ScheduledDeleteService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ScreenProctoringAPIBinding;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Lazy
@Service
public class ScheduledDeleteServiceImpl implements ScheduledDeleteService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDeleteServiceImpl.class);

    private final ScheduledDeleteDAO scheduledDeleteDAO;
    private final ScreenProctoringAPIBinding screenProctoringAPIBinding;

    private final UserDAO userDAO;
    private final ExamDAO examDAO;
    private final ClientConnectionDAO clientConnectionDAO;
    private final UserService userService;

    public ScheduledDeleteServiceImpl(
            final ScheduledDeleteDAO scheduledDeleteDAO,
            final ScreenProctoringAPIBinding screenProctoringAPIBinding,
            final UserDAO userDAO,
            final ExamDAO examDAO,
            final ClientConnectionDAO clientConnectionDAO,
            final UserService userService) {

        this.scheduledDeleteDAO = scheduledDeleteDAO;
        this.screenProctoringAPIBinding = screenProctoringAPIBinding;
        this.userDAO = userDAO;
        this.examDAO = examDAO;
        this.clientConnectionDAO = clientConnectionDAO;
        this.userService = userService;
    }

    @Override
    public Result<ScheduledDeleteReport> createScheduledDelete(
            final Long deleteDueTimestampUTC,
            final Long scheduledTimestampUTC) {

        return Result.tryCatch(() -> {

            // first check if there is already a pending ScheduledDelete. If so, deny a new one to be created
            Nullable<ScheduledDelete> pending = scheduledDeleteDAO
                    .getPendingScheduledDelete()
                    .getOrThrow();

            if (!pending.isNull()) {
                throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of(
                        "There is already a pending ScheduledDelete. Only one is allowed"));
            }

            // prepare
            final SEBServerUser currentUser = userService.getCurrentUser();
            final Long institutionId = currentUser.institutionId();
            final DateTimeZone referenceTimezone = currentUser.getUserInfo().timeZone;
            final DateTimeZone refTimeZone = referenceTimezone != null ? referenceTimezone : DateTimeZone.UTC;

            // used timestamps (UTC)
            final Long scheduleTime = Utils.calcTimeToMidnight(scheduledTimestampUTC, referenceTimezone);
            final Long deleteTimeUTCAtStartOfDay =Utils.calcTimeAtStartOfDay(scheduledTimestampUTC, referenceTimezone);

            log.info("Schedule delete for dueTime: {} -- UTC: {} at: {} -- UTC: {}",
                    Utils.formatDate(new DateTime(deleteTimeUTCAtStartOfDay, refTimeZone)),
                    Utils.formatDate(new DateTime(deleteTimeUTCAtStartOfDay, DateTimeZone.UTC)),
                    Utils.formatDate(new DateTime(scheduleTime,refTimeZone)),
                    Utils.formatDate(new DateTime(scheduleTime,DateTimeZone.UTC)));

            return createScheduledDeleteInternal(deleteTimeUTCAtStartOfDay, scheduleTime);
        });
    }



    @Override
    public Result<ScheduledDeleteReport> getReportById(String reportId) {
        return Result.tryCatch(() -> {

            final Long pk = Long.parseLong(reportId);
            final ScheduledDelete sebServerDeletions = scheduledDeleteDAO.
                    byPK(pk)
                    .getOrThrow();

            final Long spsId = sebServerDeletions.spsId();
            ScheduledDelete spsDeletions = null;
            if (spsId != null) {
                spsDeletions = screenProctoringAPIBinding
                        .getScheduledDeleteById(spsId)
                        .getOrThrow();
            }

            return ScheduledDeleteReport.createFormInfos(
                    sebServerDeletions,
                    spsDeletions != null ? spsDeletions.info() : Collections.emptyList());
        });
    }

    @Override
    public Result<Collection<ScheduledDelete>> getAll(FilterMap filterMap) {
        return scheduledDeleteDAO.allMatching(filterMap);
    }

    @Override
    public Result<Nullable<ScheduledDeleteReport>> applyExcludeForDeletion(
            final Collection<String> examUUIDs,
            final boolean reset) {

        return Result.tryCatch(() -> {

            final Collection<EntityKey> changedExams = examDAO
                    .excludeFromDeletion(examUUIDs, reset)
                    .getOrThrow();

            if (changedExams.isEmpty()) {
                return Nullable.ofNull();
            }

            Nullable<ScheduledDelete> pending = scheduledDeleteDAO
                    .getPendingScheduledDelete()
                    .getOrThrow();

            if (pending.isNull()) {
                return Nullable.ofNull();
            }

            // we have a pending deletion, calculate a new one with
            // we need to delete the old ScheduledDelete fist and then create a new one
            final ScheduledDeleteReport result = deleteScheduledDeletion(pending.element.getModelId())
                    .map(key -> createScheduledDeleteInternal(
                            pending.element.deleteDueTime(),
                            pending.element.scheduleTime()
                    ))
                    .onError(error -> log.error("Failed to recreate ScheduledDelete: {} cause: {}",
                            pending.element,
                            error.getMessage()))
                    .getOrThrow();

            return new Nullable<>(result);
        });
    }

    @Override
    public Result<EntityKey> deleteScheduledDeletion(final String modelId) {
        return Result.tryCatch(() -> {

            final Long pk = Long.parseLong(modelId);
            final ScheduledDelete scheduledDeleteSEBServer = scheduledDeleteDAO
                    .byPK(pk)
                    .getOrThrow();

            log.info("Delete the following ScheduledDelete: {}", scheduledDeleteSEBServer.id());

            // if there is a SPS scheduled delete to also delete, delete it first
            Long spsId = scheduledDeleteSEBServer.spsId();
            if (spsId != null) {

                log.info("Request ScheduledDelete deletion on SPS with id: {}", spsId);

                screenProctoringAPIBinding
                        .deleteScheduledDelete(spsId)
                        .onError(error -> log.error(
                                "Failed to delete ScheduledDelete with id: {} on SPS. Cause: {}",
                                spsId,
                                error.getMessage()));
            }

            scheduledDeleteDAO
                    .delete(Collections.singleton(new EntityKey(pk, EntityType.SCHEDULED_DELETE)))
                    .getOrThrow();

            log.info("Successfully deleted the following ScheduledDelete: {}", scheduledDeleteSEBServer);

            return new EntityKey(pk, EntityType.SCHEDULED_DELETE);
        });
    }

    private ScheduledDeleteReport createScheduledDeleteInternal(
            final Long deleteTimeUTCAtStartOfDay,
            final Long scheduleTime) {

        final SEBServerUser currentUser = userService.getCurrentUser();
        final Long institutionId = currentUser.institutionId();

        // get all possibly involved Exams from all institutions and filter them
        final Map<String, Exam> includeExams = examDAO
                .getExamsForScheduledDeletion(deleteTimeUTCAtStartOfDay)
                .map(e -> e.stream().filter(exam -> !BooleanUtils.isTrue(exam.excludeFromDeletion)).toList())
                .getOrThrow()
                .stream()
                .filter(exam ->
                        Objects.equals(exam.institutionId, institutionId) &&
                        !BooleanUtils.isTrue(exam.excludeFromDeletion))
                .collect(Collectors.toMap(Exam::getExternalId, Function.identity()));

        // request scheduled deletion on SPS, compare and refine it with this data and store on SPS
        final ScheduledDelete requestedSPSDelete = screenProctoringAPIBinding
                .requestScheduledDelete(deleteTimeUTCAtStartOfDay, institutionId)
                .getOrThrow();

        // filter scheduled delete from SPS according to given includeExams and excludeExams maps we have
        final ArrayList<ScheduledDeleteInfo> spsDeleteInfos = filterSPSDeleteInfos(
                requestedSPSDelete.info(),
                includeExams,
                institutionId);

        // create Scheduled Delete for SPS
        final ScheduledDelete spsDelete = screenProctoringAPIBinding
                .createScheduledDelete(new ScheduledDelete(
                        null,
                        null,
                        requestedSPSDelete.state(),
                        deleteTimeUTCAtStartOfDay,
                        scheduleTime,
                        null,
                        null,
                        currentUser.uuid(),
                        institutionId,
                        spsDeleteInfos
                ))
                .getOrThrow();

        // create Scheduled Delete info for SEB Server
        final List<ScheduledDeleteInfo> deleteInfos = new ArrayList<>();
        includeExams.values().forEach(exam -> {
            Map<String, String> deleteInfo = new HashMap<>();
            deleteInfo.put(ScheduledDeleteInfo.ATTR_EXAM_NAME, exam.name);
            if (exam.startTime != null) {
                deleteInfo.put(ScheduledDeleteInfo.ATTR_EXAM_START_TIME, String.valueOf(exam.startTime.getMillis()));
            }
            deleteInfo.put(ScheduledDeleteInfo.ATTR_EXAM_OWNER, exam.owner);
            deleteInfo.put(
                    ScheduledDeleteInfo.ATTR_NUM_OF_SESSIONS,
                    String.valueOf(clientConnectionDAO.numberOfConnectionsOfExam(exam.id)));

            clientConnectionDAO
                    .getAllConnectionIdsForExam(exam.id)
                    .onSuccess(ids -> deleteInfo.put("numberOfSEBConnections", String.valueOf(ids.size())));

            deleteInfos.add(new ScheduledDeleteInfo(
                    null,
                    null,
                    ScheduledDeleteInfo.State.PENDING,
                    exam.externalId,
                    deleteInfo,
                    null
            ));
        });

        // create ScheduledDelete for SEB Server
        final ScheduledDelete sebServerDelete = scheduledDeleteDAO.createNew(
                new ScheduledDelete(
                        null,
                        spsDelete.id(),
                        ScheduledDelete.State.PENDING,
                        deleteTimeUTCAtStartOfDay,
                        scheduleTime,
                        null,
                        null,
                        currentUser.uuid(),
                        institutionId,
                        deleteInfos)
        ).getOrThrow();

        // provide full report as result
        return ScheduledDeleteReport.createFormInfos(sebServerDelete, spsDelete.info());
    }

    // This filters the given sourceSPSDeleteInfos on given includeExams and excludeExams
    // and also tries to verify correct institutional constraint for all deletions in the list.
    // If SPS data provide an institutional id it us used if not, it tries to get the institutionId from an
    // applied supporter user (for legacy data handling). If the institution cannot be verified anymore, skip the deletion
    ArrayList<ScheduledDeleteInfo> filterSPSDeleteInfos(
            final Collection<ScheduledDeleteInfo> sourceSPSDeleteInfos,
            final Map<String, Exam> includeExams,
            final Long institutionId) {

        final ArrayList<ScheduledDeleteInfo> spsDeleteInfos = new ArrayList<>();

        log.info("Filter given SPS deletion infos: {}", sourceSPSDeleteInfos);

        sourceSPSDeleteInfos.forEach(spsDeleteInfo -> {
            final String examUUID = spsDeleteInfo.examUUID();

            // if we have no exam UUID from SPS (means that this is only a group without exam) then skip
            if (examUUID == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip SPS deletion info due to missing examUUID: {}", spsDeleteInfo);
                }
                return;
            }

            // if exam exists on SEB Server and is in include list, add it also for SPS include list
            if (includeExams.containsKey(examUUID)) {
                if (log.isDebugEnabled()) {
                    log.debug("Add SPS deletion info, found uuid on include mapping: {}", spsDeleteInfo);
                }
                spsDeleteInfos.add(spsDeleteInfo);
                return;
            }

            // if the exam exists on SEB Server and was not in the inclusion list, exclude it also on SPS
            Result<Exam> existsResult = examDAO.byExternalIdLike(examUUID);
            if (!existsResult.hasError()) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip SPS deletion info, found uuid in SEB Server but was not in inclusion mapping: {}", spsDeleteInfo);
                }
                return;
            }

            final Map<String, String> spsExamInfo = spsDeleteInfo.deletionInfo();
            final String spsInstitutionId = spsExamInfo.get(Domain.EXAM.ATTR_INSTITUTION_ID);
            final String spsSupporter = spsExamInfo.get(Domain.EXAM.ATTR_SUPPORTER);

            // if we have a spsInstitutionId, we can check on that. It must be the same as the current user
            if (StringUtils.isNotBlank(spsInstitutionId) && Objects.equals(institutionId, Long.parseLong(spsInstitutionId))) {
                if (log.isDebugEnabled()) {
                    log.debug("Add SPS deletion info since institutionId matches user account institution: {}", spsDeleteInfo);
                }
                spsDeleteInfos.add(spsDeleteInfo);
                return;
            }

            // this is the case for legacy data where no spsInstitutionId is available, and we try to get the
            // origin institution id from supporter assigned to the SPS exam
            if (StringUtils.isNotBlank(spsSupporter)) {
                String[] split = StringUtils.split(spsSupporter, Constants.COMMA);
                if (split != null && split.length > 0) {
                    Result<UserInfo> userInfoResult = userDAO.byModelId(split[0]);
                    if (!userInfoResult.hasError()) {
                        UserInfo userInfo = userInfoResult.getOrThrow();
                        if (Objects.equals(userInfo.institutionId, institutionId)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Add SPS deletion info since institutionId matches user account institution of supporter: {}", spsDeleteInfo);
                            }
                            spsDeleteInfos.add(spsDeleteInfo);
                            return;
                        }
                    }
                }
            }

            // if we cannot verify institutional integrity for this SPS exam, we skip it
            if (log.isDebugEnabled()) {
                log.debug("Skip SPS deletion info because institutional integrity cannot be verified : {}", spsDeleteInfo);
            }

        });

        log.info("Filtered given SPS deletion infos to: {}", spsDeleteInfos);

        return spsDeleteInfos;
    }
}
