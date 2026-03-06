package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduledDeleteInfo(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_ID) Long id,
        @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_SCHEDULED_DELETE_ID) Long scheduledDeleteId,
        @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_STATE) State state,
        @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_EXAM_UUID) String examUUID,
        @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_DELETION_INFO) Map<String, String> deletionInfo,
        @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_ERROR_INFO) String errorInfo) {

    public enum State {
        PENDING,
        RUNNING,
        DELETED,
        ERROR
    }

    public static final String ATTR_EXAM_NAME = "examName";
    public static final String ATTR_EXAM_START_TIME = "examStartTimestamp";
    public static final String ATTR_EXAM_OWNER = "examOwner";
    public static final String ATTR_NUM_OF_SESSIONS = "numberOfSessions";

    @JsonCreator
    public ScheduledDeleteInfo(
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_ID) final Long id,
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_SCHEDULED_DELETE_ID) final Long scheduledDeleteId,
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_STATE) final State state,
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_EXAM_UUID) final String examUUID,
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_DELETION_INFO) Map<String, String> deletionInfo,
            @JsonProperty(Domain.SCHEDULED_DELETE_INFO.ATTR_ERROR_INFO) final String errorInfo) {

        this.id = id;
        this.scheduledDeleteId = scheduledDeleteId;
        this.state = state;
        this.examUUID = examUUID;
        this.deletionInfo = deletionInfo;
        this.errorInfo = errorInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledDeleteInfo that = (ScheduledDeleteInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ScheduledDeleteInfo{" +
                "id=" + id +
                ", scheduledDeleteId=" + scheduledDeleteId +
                ", state=" + state +
                ", examUUID='" + examUUID + '\'' +
                ", deletionInfo='" + deletionInfo + '\'' +
                ", errorInfo='" + errorInfo + '\'' +
                '}';
    }
}
