package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionDeletionReport(
        @JsonProperty(ATTR_SEARCH_NAME) String searchName,
        @JsonProperty(ATTR_DELETE_DUE_TIME) Long deleteDueTime,
        @JsonProperty("sebServerDeletions") Collection<SessionInfo> sebServerDeletions,
        @JsonProperty("spsDeletions") Collection<SessionDeletionInfo> spsDeletions
) {

    public static final String ATTR_SEARCH_NAME = "searchName";
    public static final String ATTR_DELETE_DUE_TIME = "deleteDueTime";
    public static final String ATTR_EXCLUDE_LIST = "excludes";

    @JsonCreator
    public SessionDeletionReport {}
}
