package ch.ethz.seb.sebserver.gbl.model.exam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

@Schema(name = "ScheduledDeleteViewInfo", description = "View information for scheduled deletion")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledDeleteViewInfo {


    @JsonProperty("examUUID")
    final String examUUID;

    @JsonProperty("examName")
    final String examName;

    @JsonProperty("examStartTime")
    final Long examStartTime;

    @JsonProperty("numberOfSessions")
    final String numberOfSessions;


    @JsonProperty("spsExamName")
    final String spsExamName;

    @JsonProperty("spsGroups")
    final Collection<String> spsGroups;

    @JsonCreator
    public ScheduledDeleteViewInfo(
            @JsonProperty("examUUID") final String examUUID,
            @JsonProperty("examName") final String examName,
            @JsonProperty("examStartTime") final Long examStartTime,
            @JsonProperty("numberOfSessions") final String numberOfSessions,
            @JsonProperty("spsExamName") final String spsExamName,
            @JsonProperty("spsGroupNames") final Collection<String> spsGroups) {

        this.examUUID = examUUID;
        this.examName = examName;
        this.examStartTime = examStartTime;
        this.numberOfSessions = numberOfSessions;
        this.spsExamName = spsExamName;
        this.spsGroups = spsGroups;
    }

    public ScheduledDeleteViewInfo(
            final String examUUID,
            final String examName,
            final Long examStartTime,
            final String numberOfSessions) {

        this.examUUID = examUUID;
        this.examName = examName;
        this.examStartTime = examStartTime;
        this.numberOfSessions = numberOfSessions;
        this.spsExamName = null;
        this.spsGroups = null;
    }

    public ScheduledDeleteViewInfo(
            final String spsExamName,
            final Collection<String> spsGroups) {

        this.examUUID = null;
        this.examName = null;
        this.examStartTime = null;
        this.numberOfSessions = null;
        this.spsExamName = spsExamName;
        this.spsGroups = spsGroups;
    }

    public boolean hasSEBServerData() {
        return examUUID != null;
    }

    public boolean hasSPSData() {
        return spsExamName != null;
    }
}
