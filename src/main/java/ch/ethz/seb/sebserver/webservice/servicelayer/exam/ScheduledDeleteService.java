package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDeleteReport;
import ch.ethz.seb.sebserver.gbl.util.Nullable;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

import java.util.Collection;

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

}
