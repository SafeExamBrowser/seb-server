package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteInfo;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteReport;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteViewInfo;
import ch.ethz.seb.sebserver.gbl.model.session.SessionDeletionReport;
import ch.ethz.seb.sebserver.gbl.util.Nullable;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ScheduledDeleteService {

    /** Create a new scheduled delete task and marks all exams that are older than the given time
     * and are not marked as exclude from deletion (deletion_time < 0)
     * The task will be scheduled for midnight.
     *
     * @param deleteDueTimestampUTC the due time stamp (UTC) for that all older exams get deleted (mandatory)
     * @param scheduledTimestampUTC optional scheduled timestamp in milliseconds in UTC
     * @return Result that contains the ScheduledDeleteReport or points to an error when happened.*/
    Result<ScheduledDeleteReport> createScheduledDelete(Long deleteDueTimestampUTC, Long scheduledTimestampUTC);

    /** Get a full deletion report by id, including also all SPS deletions
     *
     * @param reportId The deletion report Id (PK)
     * @return Result containing the full deletion report or point to an error when happened */
    Result<ScheduledDeleteReport> getReportById(String reportId);

    /** Get all matching ScheduledDelete entities from SEB Server (no SPS data)
     *
     * @param filterMap Map with filter criteria
     * @return Result containing all matching ScheduledDelete or an error when happened */
    Result<Collection<ScheduledDelete>> getAll(FilterMap filterMap);

    /** Marks all Exams for given Exam uuid's back to ready for deletion if they are in correct state (Archived)
     *
     * @param examModelIds List of Exam uuid's (Pk's) to mark as ready for deletion
     * @param reset Indicates whether the exclusion for deletion shall be set or reset
     * @return Result containing all marked ExamKeys or an error when happened */
    Result<Nullable<ScheduledDeleteReport>> applyExcludeForDeletion(Collection<String> examModelIds, boolean reset);

    /** Use this to delete a scheduled deletion from SEB Server and also associated scheduled deletion on SPS if there is one
     *
     * @param modelId The id of the scheduled deletion to delete
     * @return Result with an EntityKey of the deleted ScheduledDelete or contains an error when happened*/
    Result<EntityKey> deleteScheduledDeletion(String modelId);

    /** Use this to get a report of all SPS sessions and SEB Server sessions (client connections)
     *  That get deleted with the given input name and due time
     *
     * @param searchName The session username search criteria. Is used wit wildcards on SQL
     * @param deleteDueTimestampUTC the user selected due time for deletion (all that are older than)
     *                              NOTE: this will be aligned with the users day start.
     * @return Result with SessionDeletionReport with all user sessions that will be deleted (SEB Server and SPS)*/
    Result<SessionDeletionReport> requestSessionDeletion(String searchName, Long deleteDueTimestampUTC);

    /** Use this to actually delete all SPS sessions and SEB Server sessions (client connections)
     *  for the given input name and due time
     *
     * @param searchName The session username search criteria. Is used wit wildcards on SQL
     * @param deleteDueTimestampUTC the user selected due time for deletion (all that are older than)
     *                              NOTE: this will be aligned with the users day start.
     * @param excludes If available defines all connection token ids that should be excluded.
     *                                 The ids are also applied to SPS session uuids (same as connectionToken)
     * @return Result with SessionDeletionReport with all user sessions has been deleted (SEB Server and SPS)*/
    Result<SessionDeletionReport> deleteUserSessions(
            String searchName,
            Long deleteDueTimestampUTC,
            Set<String> excludes);

    static ScheduledDeleteReport createFormInfos(
            final ScheduledDelete scheduledDelete,
            final Collection<ScheduledDeleteInfo> spsDeletions
    ) {

        final Collection<ScheduledDeleteInfo> examDeletions = scheduledDelete.info();
        final Map<String, ScheduledDeleteInfo> spsMap = spsDeletions.stream()
                .collect(Collectors.toMap(ScheduledDeleteInfo::examUUID, Function.identity()));

        final List<ScheduledDeleteViewInfo> sebServerDeletions = new ArrayList<>();
        final List<ScheduledDeleteViewInfo> spsOnlyDeletions = new ArrayList<>();

        examDeletions.forEach(sebServerData -> {
            final Map<String, String> infos = sebServerData.deletionInfo();
            final ScheduledDeleteInfo spsData = spsMap.remove(sebServerData.examUUID());
            if (spsData != null) {
                final Map<String, String> spsInfos = spsData.deletionInfo();
                // SEB Server and SPS Data
                final String startTime = infos.get(ScheduledDeleteInfo.ATTR_EXAM_START_TIME);
                sebServerDeletions.add(new ScheduledDeleteViewInfo(
                        sebServerData.examUUID(),
                        infos.get(ScheduledDeleteInfo.ATTR_EXAM_NAME),
                        startTime != null ? Long.parseLong(startTime) : null,
                        infos.get(ScheduledDeleteInfo.ATTR_NUM_OF_SESSIONS),
                        spsInfos.get("name"),
                        extractGroupNames(spsInfos),
                        sebServerData.errorInfo(),
                        sebServerData.getErrorType()));
            } else {
                // only SEB Server data available
                final String startTime = infos.get(ScheduledDeleteInfo.ATTR_EXAM_START_TIME);
                sebServerDeletions.add(new ScheduledDeleteViewInfo(
                        sebServerData.examUUID(),
                        infos.get(ScheduledDeleteInfo.ATTR_EXAM_NAME),
                        startTime != null ? Long.parseLong(startTime) : null,
                        infos.get(ScheduledDeleteInfo.ATTR_NUM_OF_SESSIONS),
                        sebServerData.errorInfo(),
                        sebServerData.getErrorType()));
            }
        });

        // remaining in spsMap has only SPS Data
        spsMap.values().forEach(spsData -> {
            final Map<String, String> spsInfos = spsData.deletionInfo();
            spsOnlyDeletions.add(new ScheduledDeleteViewInfo(
                    spsInfos.get("name"),
                    extractGroupNames(spsInfos),
                    spsData.errorInfo(),
                    spsData.getErrorType()));
        });

        return new ScheduledDeleteReport(
                scheduledDelete.id(),
                scheduledDelete.spsId(),
                scheduledDelete.state(),
                scheduledDelete.deleteDueTime(),
                scheduledDelete.scheduleTime(),
                scheduledDelete.startTime(),
                scheduledDelete.endTime(),
                scheduledDelete.institutionId(),
                sebServerDeletions,
                spsOnlyDeletions);
    }

    private static Collection<ScheduledDeleteViewInfo.GroupInfo> extractGroupNames(final Map<String, String> spsInfos) {
        final Set<String> groupKeys = spsInfos.keySet()
                .stream()
                .filter(key -> key.startsWith("group"))
                .map(key -> key.substring(0, key.lastIndexOf("_")))
                .collect(Collectors.toSet());

        return groupKeys.stream().map( key -> {
            try {
                return new ScheduledDeleteViewInfo.GroupInfo(
                        spsInfos.get(key + "_name"),
                        spsInfos.get(key + "_sessionCount"));
            } catch (Exception e) {
                try {
                    return new ScheduledDeleteViewInfo.GroupInfo(
                            spsInfos.get(key + "_name"),
                            "");
                } catch (Exception ee) {
                    return new ScheduledDeleteViewInfo.GroupInfo(
                            "",
                            "");
                }
            }
        } ).toList();
    }

}
