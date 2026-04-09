package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Schema(name = "ScheduledDeleteReport", description = "Report of a scheduled deletion")
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

    public static ScheduledDeleteReport createFormInfos(
            final ScheduledDelete scheduledDelete,
            final Collection<ScheduledDeleteInfo> spsDeletions
    ) {

        final Collection<ScheduledDeleteInfo> examDeletions = scheduledDelete.info();
        final Map<String, ScheduledDeleteInfo> spsMap = spsDeletions.stream()
                .collect(Collectors.toMap(ScheduledDeleteInfo::examUUID, Function.identity()));

        final List<ScheduledDeleteViewInfo> sebServerDeletions = new ArrayList<>();
        final List<ScheduledDeleteViewInfo> spsOnlyDeletions = new ArrayList<>();

        examDeletions.forEach(sebServerData -> {
            final Map<String, String> infos = sebServerData.deletionInfo();
            final ScheduledDeleteInfo spsData = spsMap.remove(sebServerData.examUUID());
            if (spsData != null) {
                final Map<String, String> spsInfos = spsData.deletionInfo();
                // SEB Server and SPS Data
                final String startTime = infos.get(ScheduledDeleteInfo.ATTR_EXAM_START_TIME);
                sebServerDeletions.add(new ScheduledDeleteViewInfo(
                        sebServerData.examUUID(),
                        infos.get(ScheduledDeleteInfo.ATTR_EXAM_NAME),
                        startTime != null ? Long.parseLong(startTime) : null,
                        infos.get(ScheduledDeleteInfo.ATTR_NUM_OF_SESSIONS),
                        spsInfos.get("name"),
                        extractGroupNames(spsInfos)));
            } else {
                // only SEB Server data available
                final String startTime = infos.get(ScheduledDeleteInfo.ATTR_EXAM_START_TIME);
                sebServerDeletions.add(new ScheduledDeleteViewInfo(
                        sebServerData.examUUID(),
                        infos.get(ScheduledDeleteInfo.ATTR_EXAM_NAME),
                        startTime != null ? Long.parseLong(startTime) : null,
                        infos.get(ScheduledDeleteInfo.ATTR_NUM_OF_SESSIONS)));
            }
        });

        // remaining in spsMap has only SPS Data
        spsMap.values().forEach(spsData -> {
            final Map<String, String> spsInfos = spsData.deletionInfo();
            spsOnlyDeletions.add(new ScheduledDeleteViewInfo(
                    spsInfos.get("name"),
                    extractGroupNames(spsInfos)));
        });

        return new ScheduledDeleteReport(
                scheduledDelete.id(),
                scheduledDelete.spsId(),
                scheduledDelete.state(),
                scheduledDelete.deleteDueTime(),
                scheduledDelete.scheduleTime(),
                scheduledDelete.startTime(),
                scheduledDelete.endTime(),
                scheduledDelete.institutionId(),
                sebServerDeletions,
                spsOnlyDeletions);
    }


    private static Collection<String> extractGroupNames(final Map<String, String> spsInfos) {
        final Set<String> groupKeys = spsInfos.keySet()
                .stream()
                .filter(key -> key.startsWith("group"))
                .map(key -> key.substring(0, key.lastIndexOf("_")))
                .collect(Collectors.toSet());

        return groupKeys.stream().map( key -> {
            try {
                return spsInfos.get(key + "_name") + " / " + spsInfos.get(key + "_sessionCount");
            } catch (Exception e) {
                try {
                    return spsInfos.get(key + "_name");
                } catch (Exception ee) {
                    return "";
                }
            }
        } ).toList();
    }
}
