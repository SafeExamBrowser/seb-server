package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduledDeleteReport(
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_ID) Long id,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SPS_ID) Long spsId,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_STATE) ScheduledDelete.State state,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME) Long deleteDueTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SCHEDULE_TIME) Long scheduleTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_START_TIME) Long startTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_END_TIME) Long endTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_INSTITUTION_ID) Long institutionId,
        @JsonProperty("examDeletions") Collection<ScheduledDeleteViewInfo> examDeletions,
        @JsonProperty("spsOnlyDeletions") Collection<ScheduledDeleteViewInfo> spsOnlyDeletions) {

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
