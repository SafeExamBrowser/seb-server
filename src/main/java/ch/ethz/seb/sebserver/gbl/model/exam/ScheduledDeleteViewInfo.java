package ch.ethz.seb.sebserver.gbl.model.exam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

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

    @JsonProperty("spsGroupNames")
    final Collection<String> spsGroupNames;

    @JsonCreator
    public ScheduledDeleteViewInfo(
            @JsonProperty("examUUID") final String examUUID,
            @JsonProperty("examName") final String examName,
            @JsonProperty("examStartTime") final Long examStartTime,
            @JsonProperty("numberOfSessions") final String numberOfSessions,
            @JsonProperty("spsExamName") final String spsExamName,
            @JsonProperty("spsGroupNames") final Collection<String> spsGroupNames) {

        this.examUUID = examUUID;
        this.examName = examName;
        this.examStartTime = examStartTime;
        this.numberOfSessions = numberOfSessions;
        this.spsExamName = spsExamName;
        this.spsGroupNames = spsGroupNames;
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
        this.spsGroupNames = null;
    }

    public ScheduledDeleteViewInfo(
            final String spsExamName,
            final Collection<String> spsGroupNames) {

        this.examUUID = null;
        this.examName = null;
        this.examStartTime = null;
        this.numberOfSessions = null;
        this.spsExamName = spsExamName;
        this.spsGroupNames = spsGroupNames;
    }



    public boolean hasSEBServerData() {
        return examUUID != null;
    }

    public boolean hasSPSData() {
        return spsExamName != null;
    }
}
