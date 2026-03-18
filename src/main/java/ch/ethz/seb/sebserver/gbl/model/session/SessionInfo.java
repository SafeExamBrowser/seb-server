package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionInfo (
        @JsonProperty("uuid") String uuid,
        @JsonProperty("clientName") String clientName,
        @JsonProperty("clientIp") String clientIP,
        @JsonProperty("clientMachineName") String clientMachineName,
        @JsonProperty("clientOsName") String clientOSName,
        @JsonProperty("clientVersion") String clientVersion,
        @JsonProperty("creationTime") Long creationTime,
        @JsonProperty("terminationTime") Long terminationTime
) {

    @JsonCreator
    public SessionInfo {}
}
