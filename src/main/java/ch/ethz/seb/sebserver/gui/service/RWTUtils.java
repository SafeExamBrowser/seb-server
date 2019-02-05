/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public final class RWTUtils {

    public static final String TEXT_NAME_H2 = "h2";

    public static void clearComposite(final Composite parent) {
        if (parent == null) {
            return;
        }

        for (final Control control : parent.getChildren()) {
            control.dispose();
        }
    }

}
