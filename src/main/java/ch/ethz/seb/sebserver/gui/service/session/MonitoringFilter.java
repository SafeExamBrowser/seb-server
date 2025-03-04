/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionIssueStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringData;
import ch.ethz.seb.sebserver.gbl.model.session.ProctoringGroupMonitoringData;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringSEBConnectionData;

public interface MonitoringFilter {

    EnumSet<ConnectionStatus> getStatusFilter();

    String getStatusFilterParam();

    EnumSet<ConnectionIssueStatus> getIssueFilter();

    String getIssueFilterParam();

    boolean filterChanged();

    void resetFilterChanged();

    boolean isStatusHidden(ConnectionStatus status);

    void hideStatus(ConnectionStatus status);

    void showStatus(ConnectionStatus status);

    boolean hasClientGroupFilter();

    default boolean isClientGroupHidden(final ClientGroup clientGroup) {
        return isClientGroupHidden(clientGroup.id);
    }

    boolean isClientGroupHidden(Long clientGroupId);

    void hideClientGroup(Long clientGroupId);

    void showClientGroup(Long clientGroupId);

    boolean isIssueHidden(ConnectionIssueStatus connectionIssueStatus);

    void hideIssue(ConnectionIssueStatus connectionIssueStatus);

    void showIssue(ConnectionIssueStatus connectionIssueStatus);

    MonitoringFullPageData getMonitoringFullPageData();

    default MonitoringSEBConnectionData getMonitoringSEBConnectionData() {
        final MonitoringFullPageData monitoringFullPageData = getMonitoringFullPageData();
        if (monitoringFullPageData != null) {
            return monitoringFullPageData.monitoringConnectionData;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    default Collection<ClientMonitoringData> getConnectionData() {
        final MonitoringSEBConnectionData monitoringSEBConnectionData = getMonitoringSEBConnectionData();
        if (monitoringSEBConnectionData != null) {
            return (Collection<ClientMonitoringData>) monitoringSEBConnectionData.monitoringData;
        } else {
            return Collections.emptyList();
        }
    }

    default int getNumOfConnections(final ConnectionStatus status) {
        final MonitoringSEBConnectionData monitoringSEBConnectionData = getMonitoringSEBConnectionData();
        if (monitoringSEBConnectionData != null) {
            return monitoringSEBConnectionData.getNumberOfConnection(status);
        } else {
            return 0;
        }
    }

    default int getNumOfConnections(final Long clientGroupId) {
        final MonitoringSEBConnectionData monitoringSEBConnectionData = getMonitoringSEBConnectionData();
        if (monitoringSEBConnectionData != null) {
            return monitoringSEBConnectionData.getNumberOfConnection(clientGroupId);
        } else {
            return 0;
        }
    }

    default int getNumOfConnections(final ConnectionIssueStatus connectionIssueStatus) {
        final MonitoringSEBConnectionData monitoringSEBConnectionData = getMonitoringSEBConnectionData();
        if (monitoringSEBConnectionData != null) {
            return monitoringSEBConnectionData.getNumberOfConnection(connectionIssueStatus);
        } else {
            return 0;
        }
    }

    default Collection<RemoteProctoringRoom> proctoringData() {
        // not used anymore
        return null;
    }

    default Collection<ProctoringGroupMonitoringData> screenProctoringData() {
        final MonitoringFullPageData monitoringFullPageData = getMonitoringFullPageData();
        if (monitoringFullPageData != null) {
            return monitoringFullPageData.getScreenProctoringData();
        } else {
            return null;
        }
    }

}
