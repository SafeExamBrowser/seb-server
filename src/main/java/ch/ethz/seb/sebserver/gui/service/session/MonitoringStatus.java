/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;

public interface MonitoringStatus {

    EnumSet<ConnectionStatus> getStatusFilter();

    String getStatusFilterParam();

    boolean statusFilterChanged();

    void resetStatusFilterChanged();

    boolean isStatusHidden(ConnectionStatus status);

    void hideStatus(ConnectionStatus status);

    void showStatus(ConnectionStatus status);

    MonitoringFullPageData getMonitoringFullPageData();

    default MonitoringSEBConnectionData getMonitoringSEBConnectionData() {
        final MonitoringFullPageData monitoringFullPageData = getMonitoringFullPageData();
        if (monitoringFullPageData != null) {
            return monitoringFullPageData.monitoringConnectionData;
        } else {
            return null;
        }
    }

    default Collection<ClientConnectionData> getConnectionData() {
        final MonitoringSEBConnectionData monitoringSEBConnectionData = getMonitoringSEBConnectionData();
        if (monitoringSEBConnectionData != null) {
            return monitoringSEBConnectionData.connections;
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

    default Collection<RemoteProctoringRoom> proctoringData() {
        final MonitoringFullPageData monitoringFullPageData = getMonitoringFullPageData();
        if (monitoringFullPageData != null) {
            return monitoringFullPageData.proctoringData;
        } else {
            return null;
        }
    }

}
