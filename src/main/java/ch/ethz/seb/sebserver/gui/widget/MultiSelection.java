/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class MultiSelection extends Composite implements Selection {

    private static final long serialVersionUID = 2730206903047681378L;
    private static final String OPTION_VALUE = "OPTION_VALUE";

    private final List<Label> labels = new ArrayList<>();
    private final List<Label> selected = new ArrayList<>();

    public MultiSelection(final Composite parent) {
        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        final String selectionValue = getSelectionValue();
        this.selected.clear();
        this.labels.clear();
        for (final Tuple<String> tuple : mapping) {
            final Label label = new Label(this, SWT.NONE);
            final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
            label.setLayoutData(gridData);
            label.setData(OPTION_VALUE, tuple._1);
            label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION.key);
            label.setText(tuple._2);
            label.addListener(SWT.MouseDown, event -> {
                final Label l = (Label) event.widget;
                if (this.selected.contains(l)) {
                    l.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION.key);
                    this.selected.remove(l);
                } else {
                    l.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTED.key);
                    this.selected.add(l);
                }
            });
            this.labels.add(label);
        }
        if (StringUtils.isNoneBlank(selectionValue)) {
            select(selectionValue);
        }
    }

    public void selectOne(final String key) {
        this.labels.stream()
                .filter(label -> key.equals(label.getData(OPTION_VALUE)))
                .findFirst()
                .ifPresent(label -> {
                    label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTED.key);
                    this.selected.add(label);
                });
    }

    public void deselect(final String key) {
        this.selected.stream()
                .filter(label -> key.equals(label.getData(OPTION_VALUE)))
                .findFirst()
                .ifPresent(label -> {
                    label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION.key);
                    this.selected.remove(label);
                });
    }

    public void deselectAll() {
        for (final Label label : this.selected) {
            label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION.key);
        }
        this.selected.clear();
    }

    @Override
    public void select(final String keys) {
        this.selected.clear();
        if (StringUtils.isNotBlank(keys)) {
            final List<String> split = Arrays.asList(StringUtils.split(keys, Constants.LIST_SEPARATOR));
            for (final Label label : this.labels) {
                if (split.contains(label.getData(OPTION_VALUE))) {
                    label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTED.key);
                    this.selected.add(label);
                }
            }
        }
    }

    @Override
    public String getSelectionValue() {
        if (this.selected.isEmpty()) {
            return null;
        }

        return StringUtils.join(
                this.selected
                        .stream()
                        .map(label -> (String) label.getData(OPTION_VALUE))
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);
    }

    @Override
    public void clear() {
        deselectAll();
    }

}
