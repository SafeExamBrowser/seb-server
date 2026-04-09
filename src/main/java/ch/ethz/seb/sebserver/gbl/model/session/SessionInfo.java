package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SessionInfo", description = "Session information")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionInfo (
        @JsonProperty("uuid") String uuid,
        @JsonProperty("clientName") String clientName,
        @JsonProperty("clientIp") String clientIP,
        @JsonProperty("clientMachineName") String clientMachineName,
        @JsonProperty("clientOsName") String clientOSName,
        @JsonProperty("clientVersion") String clientVersion,
        @JsonProperty("creationTime") Long creationTime,
        @JsonProperty("terminationTime") Long terminationTime,
        @JsonProperty("error") String error
) {

    @JsonCreator
    public SessionInfo {}

    public SessionInfo withError(final Exception e) {
        if (e == null) {
            return this;
        }

        return new SessionInfo(
                this.uuid(),
                this.clientName(),
                this.clientIP(),
                this.clientMachineName(),
                this.clientOSName(),
                this.clientVersion(),
                this.creationTime(),
                this.terminationTime(),
                e.getMessage());
    }
}
