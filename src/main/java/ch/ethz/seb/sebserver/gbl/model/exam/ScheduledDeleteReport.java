package ch.ethz.seb.sebserver.gbl.model.exam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduledDeleteReport(
        @JsonProperty("scheduledDelete") ScheduledDelete scheduledDelete,
        @JsonProperty("examDeletions") Collection<ScheduledDeleteViewInfo> examDeletions,
        @JsonProperty("spsOnlyDeletions") Collection<ScheduledDeleteViewInfo> spsOnlyDeletions) {

    @JsonCreator
    public ScheduledDeleteReport {

    }

    @Override
    public String toString() {
        return "ScheduledDeleteReport{" +
                "scheduledDelete=" + scheduledDelete +
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

        return new ScheduledDeleteReport(scheduledDelete, sebServerDeletions, spsOnlyDeletions);
    }

    private static Collection<String> extractGroupNames(final Map<String, String> spsInfos) {
        return spsInfos.entrySet().stream()
                .filter(entry -> {
                    final String key = entry.getKey();
                    return key != null && key.startsWith("group_") && key.endsWith("name");
                })
                .map(Map.Entry::getValue)
                .toList();
    }
}
