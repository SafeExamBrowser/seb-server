/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;

public class StatusData {

    final Color defaultColor;
    final Color color1;
    final Color color2;
    final Color color3;

    public StatusData(final Display display) {
        this.defaultColor = new Color(display, new RGB(255, 255, 255), 255);
        this.color1 = new Color(display, new RGB(0, 255, 0), 255);
        this.color2 = new Color(display, new RGB(249, 166, 2), 255);
        this.color3 = new Color(display, new RGB(255, 0, 0), 255);
    }

    Color getStatusColor(final ClientConnectionData connectionData) {
        if (connectionData == null || connectionData.clientConnection == null) {
            return this.defaultColor;
        }

        switch (connectionData.clientConnection.status) {
            case ESTABLISHED:
                return this.color1;
            case ABORTED:
                return this.color3;
            default:
                return this.color2;
        }
    }

    int statusWeight(final ClientConnectionData connectionData) {
        if (connectionData == null) {
            return 100;
        }

        switch (connectionData.clientConnection.status) {
            case ABORTED:
                return 0;
            case CONNECTION_REQUESTED:
            case AUTHENTICATED:
                return 1;
            case ESTABLISHED:
                return 2;
            case CLOSED:
                return 3;
            default:
                return 10;
        }
    }

}
