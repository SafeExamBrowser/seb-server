/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringFullPageData {

    public static final String ATTR_CONNECTIONS_DATA = "monitoringConnectionData";
    public static final String ATTR_PROCTORING_DATA = "proctoringData";
    public static final String ATTR_SCREEN_PROCTORING_DATA = "screenProctoringData";

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID)
    public final Long examId;
    @JsonProperty(ATTR_CONNECTIONS_DATA)
    public final MonitoringSEBConnectionData monitoringConnectionData;
    @JsonProperty(ATTR_PROCTORING_DATA)
    public final Collection<RemoteProctoringRoom> proctoringData;
    @JsonProperty(ATTR_SCREEN_PROCTORING_DATA)
    final Collection<ScreenProctoringGroup> screenProctoringData;

    public MonitoringFullPageData(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(ATTR_CONNECTIONS_DATA) final MonitoringSEBConnectionData monitoringConnectionData,
            @JsonProperty(ATTR_PROCTORING_DATA) final Collection<RemoteProctoringRoom> proctoringData,
            @JsonProperty(ATTR_SCREEN_PROCTORING_DATA) final Collection<ScreenProctoringGroup> screenProctoringData) {

        this.examId = examId;
        this.monitoringConnectionData = monitoringConnectionData;
        this.proctoringData = proctoringData;
        this.screenProctoringData = screenProctoringData;
    }

    public Long getExamId() {
        return this.examId;
    }

    public MonitoringSEBConnectionData getMonitoringConnectionData() {
        return this.monitoringConnectionData;
    }

    public Collection<RemoteProctoringRoom> getProctoringData() {
        return this.proctoringData;
    }

    public Collection<ScreenProctoringGroup> getScreenProctoringData() {
        return this.screenProctoringData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.examId == null) ? 0 : this.examId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MonitoringFullPageData other = (MonitoringFullPageData) obj;
        if (this.examId == null) {
            if (other.examId != null)
                return false;
        } else if (!this.examId.equals(other.examId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MonitoringFullPageData [examId=");
        builder.append(this.examId);
        builder.append(", monitoringConnectionData=");
        builder.append(this.monitoringConnectionData);
        builder.append(", proctoringData=");
        builder.append(this.proctoringData);
        builder.append(", screenProctoringData=");
        builder.append(this.screenProctoringData);
        builder.append("]");
        return builder.toString();
    }

}
