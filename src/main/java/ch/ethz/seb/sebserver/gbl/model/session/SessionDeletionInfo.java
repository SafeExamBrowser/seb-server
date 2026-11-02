package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDeletionInfo(
        @JsonProperty("session") SessionInfo sessionInfo,
        @JsonProperty("groupName") String groupName,
        @JsonProperty("examName") String examName,
        @JsonProperty("examUUID") String examUUID,
        @JsonProperty("institutionId") Long institutionId,
        @JsonProperty("numberOfScreenshots") Long numberOfScreenshots
) {

    @JsonCreator
    public SessionDeletionInfo {}

    public SessionDeletionInfo withError(final Exception e) {
        if (e == null) {
            return this;
        }

        return new SessionDeletionInfo(
            this.sessionInfo.withError(e),
            this.groupName,
            this.examName,
            this.examUUID,
            this.institutionId,
            this.numberOfScreenshots
        );
    }
}
