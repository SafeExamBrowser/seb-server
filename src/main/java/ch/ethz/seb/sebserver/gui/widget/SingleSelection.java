/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class SingleSelection extends Combo implements Selection {

    private static final long serialVersionUID = 6522063655406404279L;

    final List<String> valueMapping;
    final List<String> keyMapping;
    final boolean isEditable;

    SingleSelection(final Composite parent, final int type, final String testKey) {
        super(parent, type);
        this.valueMapping = new ArrayList<>();
        this.keyMapping = new ArrayList<>();
        this.isEditable = type == SWT.NONE;
        if (testKey != null) {
            WidgetFactory.setTestId(this, testKey);
        }
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        final String selectionValue = getSelectionValue();
        this.valueMapping.clear();
        this.keyMapping.clear();
        this.valueMapping.addAll(mapping.stream()
                .map(t -> Utils.truncateText(t._2, 100))
                .collect(Collectors.toList()));
        this.keyMapping.addAll(mapping.stream()
                .map(t -> t._1)
                .collect(Collectors.toList()));
        super.setItems(this.valueMapping.toArray(new String[mapping.size()]));
        select(selectionValue);
    }

    @Override
    public void select(final String key) {
        if (this.isEditable) {
            super.setText(key);
            return;
        }
        final int selectionIndex = this.keyMapping.indexOf(key);
        if (selectionIndex < 0) {
            return;
        }

        super.select(selectionIndex);
    }

    @Override
    public String getSelectionValue() {
        if (this.isEditable) {
            return super.getText();
        }

        final int selectionIndex = super.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        }

        return this.keyMapping.get(selectionIndex);
    }

    @Override
    public String getSelectionReadableValue() {
        final int selectionIndex = super.getSelectionIndex();
        if (selectionIndex < 0) {
            return Constants.EMPTY_NOTE;
        }

        return this.valueMapping.get(selectionIndex);
    }

    @Override
    public void clear() {
        super.clearSelection();
        super.setItems(this.valueMapping.toArray(new String[0]));
    }

    @Override
    public Type type() {
        return Type.SINGLE;
    }

    @Override
    public void setSelectionListener(final Listener listener) {
        super.addListener(SWT.Selection, listener);
        if (this.isEditable) {
            super.addListener(SWT.FocusOut, listener);
            super.addListener(SWT.Traverse, listener);
        }
    }

    @Override
    public void setAriaLabel(final String label) {
        WidgetFactory.setARIALabel(this, label);
    }

}
