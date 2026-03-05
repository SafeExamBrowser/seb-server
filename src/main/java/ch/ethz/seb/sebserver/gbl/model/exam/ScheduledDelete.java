package ch.ethz.seb.sebserver.gbl.model.exam;


import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduledDelete(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_ID) Long id,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SPS_ID) Long spsId,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_STATE) State state,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME) Long deleteDueTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SCHEDULE_TIME) Long scheduleTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_START_TIME) Long startTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_END_TIME) Long endTime,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_OWNER) String ownerUUID,
        @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_INSTITUTION_ID) Long institutionId,
        @JsonProperty(ATTR_INFO) Collection<ScheduledDeleteInfo> info) implements Entity {

    public static final String ATTR_INFO = "info";

    public enum State {
        PENDING,
        SPS_RUNNING,
        RUNNING,
        FINISHED
    }

    @JsonCreator
    public ScheduledDelete(
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_ID) final Long id,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SPS_ID) final Long spsId,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_STATE) final State state,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_DELETE_DUE_TIME) final Long deleteDueTime,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_SCHEDULE_TIME) final Long scheduleTime,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_START_TIME) final Long startTime,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_END_TIME) final Long endTime,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_OWNER) final String ownerUUID,
            @JsonProperty(Domain.SCHEDULED_DELETE.ATTR_INSTITUTION_ID) Long institutionId,
            @JsonProperty(ATTR_INFO) final Collection<ScheduledDeleteInfo> info) {

        this.id = id;
        this.spsId = spsId;
        this.state = state;
        this.deleteDueTime = deleteDueTime;
        this.scheduleTime = scheduleTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ownerUUID = ownerUUID;
        this.institutionId = institutionId;
        this.info = info;
    }


    @Override
    public String getModelId() {
        return String.valueOf(id);
    }

    @Override
    public String getName() {
        return Utils.formatDate(Utils.toDateTimeUTC(deleteDueTime));
    }

    @Override
    public EntityType entityType() {
        return EntityType.SCHEDULED_DELETE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledDelete that = (ScheduledDelete) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ScheduledDelete{" +
                "id=" + id +
                ", spsId=" + spsId +
                ", state=" + state +
                ", deleteDueTime=" + deleteDueTime +
                ", scheduleTime=" + scheduleTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", ownerUUID='" + ownerUUID + '\'' +
                ", institutionId='" + institutionId + '\'' +
                ", info=" + info +
                '}';
    }
}
