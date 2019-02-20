/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.util.Tuple;

public class SingleSelection extends Combo {

    private static final long serialVersionUID = 6522063655406404279L;

    final List<String> valueMapping;
    final List<String> keyMapping;

    public SingleSelection(final Composite parent, final List<Tuple<String>> mapping) {
        super(parent, SWT.READ_ONLY);
        this.valueMapping = new ArrayList<>();
        this.keyMapping = new ArrayList<>();
        applyNewMapping(mapping);
    }

    protected void applyNewMapping(final List<Tuple<String>> mapping) {
        this.valueMapping.clear();
        this.keyMapping.clear();
        this.valueMapping.addAll(mapping.stream()
                .map(t -> t._2)
                .collect(Collectors.toList()));
        this.keyMapping.addAll(mapping.stream()
                .map(t -> t._1)
                .collect(Collectors.toList()));
        super.setItems(this.valueMapping.toArray(new String[mapping.size()]));
    }

    public void select(final String key) {
        final int selectionindex = this.keyMapping.indexOf(key);
        if (selectionindex < 0) {
            return;
        }

        super.select(selectionindex);
    }

    public String getSelectionValue() {
        final int selectionindex = super.getSelectionIndex();
        if (selectionindex < 0) {
            return null;
        }

        return this.keyMapping.get(selectionindex);
    }

}
