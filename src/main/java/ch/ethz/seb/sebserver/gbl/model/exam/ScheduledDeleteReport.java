package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ScheduledDeleteReport", description = "Full report of a scheduled deletion, including the affected exams.")
public record ScheduledDeleteReport(
        @Schema(description = "Scheduled deletion identifier (PK).", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_ID) Long id,
        @Schema(description = "Identifier of the corresponding deletion on the screen proctoring service.", nullable = true) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SPS_ID) Long spsId,
        @Schema(description = "Processing state of the scheduled deletion.", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_STATE) ScheduledDelete.State state,
        @Schema(description = "Exams that ended before this time get deleted; unix timestamp in milliseconds.", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME) Long deleteDueTime,
        @Schema(description = "Time the deletion runs; unix timestamp in milliseconds.", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SCHEDULE_TIME) Long scheduleTime,
        @Schema(description = "Time the deletion run started; unix timestamp in milliseconds.", nullable = true) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_START_TIME) Long startTime,
        @Schema(description = "Time the deletion run finished; unix timestamp in milliseconds.", nullable = true) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_END_TIME) Long endTime,
        @Schema(description = "Institution identifier the scheduled deletion belongs to.") @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_INSTITUTION_ID) Long institutionId,
        @Schema(description = "Deletions of exams managed by this SEB Server.", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("examDeletions") Collection<ScheduledDeleteViewInfo> examDeletions,
        @Schema(description = "Deletions of exams that only exist on the screen proctoring service.", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty("spsOnlyDeletions") Collection<ScheduledDeleteViewInfo> spsOnlyDeletions) {

    @JsonCreator
    public ScheduledDeleteReport {

    }

    @Override
    public String toString() {
        return "ScheduledDeleteReport{" +
                "id=" + id +
                ", spsId=" + spsId +
                ", state=" + state +
                ", deleteDueTime=" + deleteDueTime +
                ", scheduleTime=" + scheduleTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", examDeletions=" + examDeletions +
                ", spsOnlyDeletions=" + spsOnlyDeletions +
                '}';
    }
}
