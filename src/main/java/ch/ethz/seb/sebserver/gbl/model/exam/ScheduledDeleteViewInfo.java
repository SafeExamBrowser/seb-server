package ch.ethz.seb.sebserver.gbl.model.exam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledDeleteViewInfo {

    @JsonProperty("examUUID")
    public final String examUUID;

    @JsonProperty("examName")
    public final String examName;

    @JsonProperty("examStartTime")
    public final Long examStartTime;

    @JsonProperty("numberOfSessions")
    public final String numberOfSessions;

    @JsonProperty("spsExamName")
    public final String spsExamName;

    @JsonProperty("spsGroups")
    public final Collection<GroupInfo> spsGroups;

    @JsonProperty("error")
    public final String error;

    @JsonProperty("errorType")
    public final ScheduledDeleteInfo.ErrorType errorType;

    @JsonCreator
    public ScheduledDeleteViewInfo(
            @JsonProperty("examUUID") final String examUUID,
            @JsonProperty("examName") final String examName,
            @JsonProperty("examStartTime") final Long examStartTime,
            @JsonProperty("numberOfSessions") final String numberOfSessions,
            @JsonProperty("spsExamName") final String spsExamName,
            @JsonProperty("spsGroupNames") final Collection<GroupInfo> spsGroups,
            @JsonProperty("error") final String error,
            @JsonProperty("errorType") final ScheduledDeleteInfo.ErrorType errorType) {

        this.examUUID = examUUID;
        this.examName = examName;
        this.examStartTime = examStartTime;
        this.numberOfSessions = numberOfSessions;
        this.spsExamName = spsExamName;
        this.spsGroups = spsGroups;
        this.error = error;
        this.errorType = errorType;
    }

    public ScheduledDeleteViewInfo(
            final String examUUID,
            final String examName,
            final Long examStartTime,
            final String numberOfSessions,
            final String error,
            final ScheduledDeleteInfo.ErrorType errorType) {

        this.examUUID = examUUID;
        this.examName = examName;
        this.examStartTime = examStartTime;
        this.numberOfSessions = numberOfSessions;
        this.spsExamName = null;
        this.spsGroups = null;
        this.error = error;
        this.errorType = errorType;
    }

    public ScheduledDeleteViewInfo(
            final String spsExamName,
            final Collection<GroupInfo> spsGroups,
            final String error,
            final ScheduledDeleteInfo.ErrorType errorType) {

        this.examUUID = null;
        this.examName = null;
        this.examStartTime = null;
        this.numberOfSessions = null;
        this.spsExamName = spsExamName;
        this.spsGroups = spsGroups;
        this.error = error;
        this.errorType = errorType;
    }

    public boolean hasSEBServerData() {
        return examUUID != null;
    }

    public boolean hasSPSData() {
        return spsExamName != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupInfo(
            @JsonProperty("groupName") String groupName,
            @JsonProperty("numberOfSessions") String numberOfSessions) {
    }
}
