/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;

public interface MonitoringEntry {

    ConnectionStatus getStatus();

    int incidentFlag();

    boolean hasMissingPing();

    boolean grantChecked();

    boolean grantDenied();

    boolean sebVersionDenied();

    boolean showNoGrantCheckApplied();

}