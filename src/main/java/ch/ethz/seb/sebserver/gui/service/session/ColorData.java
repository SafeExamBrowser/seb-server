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

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ColorData {

    final Color darkColor;
    final Color lightColor;
    final Color defaultColor;
    final Color color1;
    final Color color2;
    final Color color3;

    public ColorData(final Display display) {
        this.defaultColor = new Color(display, new RGB(220, 220, 220), 255);
        this.color1 = new Color(display, new RGB(255, 255, 255), 255);
        this.color2 = new Color(display, new RGB(255, 194, 14), 255);
        this.color3 = new Color(display, new RGB(237, 28, 36), 255);
        this.darkColor = new Color(display, Constants.BLACK_RGB);
        this.lightColor = new Color(display, Constants.WHITE_RGB);
    }

    Color getStatusColor(final ClientConnectionData connectionData) {
        if (connectionData == null || connectionData.clientConnection == null) {
            return this.defaultColor;
        }

        switch (connectionData.clientConnection.status) {
            case ACTIVE:
                return (connectionData.missingPing) ? this.color2 : this.color1;
            default:
                return this.defaultColor;
        }
    }

    Color getStatusTextColor(final Color statusColor) {
        return Utils.darkColorContrast(statusColor.getRGB()) ? this.darkColor : this.lightColor;
    }

    int statusWeight(final ClientConnectionData connectionData) {
        if (connectionData == null) {
            return 100;
        }

        switch (connectionData.clientConnection.status) {
            case CONNECTION_REQUESTED:
            case AUTHENTICATED:
                return 1;
            case ACTIVE:
                return (connectionData.missingPing) ? 0 : 2;
            case CLOSED:
                return 3;
            default:
                return 10;
        }
    }

}
