/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.List;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;

import ch.ethz.seb.sebserver.gbl.util.Tuple;

public interface Selection {

    enum Type {
        SINGLE,
        MULTI,
        MULTI_COMBO,
        COLOR,
    }

    Type type();

    void applyNewMapping(final List<Tuple<String>> mapping);

    void select(final String keys);

    String getSelectionValue();

    void clear();

    void setVisible(boolean visible);

    void setSelectionListener(Listener listener);

    default Control adaptToControl() {
        return (Control) this;
    }

    @SuppressWarnings("unchecked")
    default <T extends Selection> T getTypeInstance() {
        return (T) this;
    }

}
