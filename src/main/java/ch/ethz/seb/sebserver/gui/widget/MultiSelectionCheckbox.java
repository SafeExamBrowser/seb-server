/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageUtils;

public final class MultiSelectionCheckbox extends Composite implements Selection {

    private static final long serialVersionUID = -8507565817745610126L;

    private Listener listener = null;
    private final Map<String, Button> checkboxes;

    MultiSelectionCheckbox(final Composite parent) {
        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        this.checkboxes = new LinkedHashMap<>();
    }

    @Override
    public Type type() {
        return Type.MULTI_CHECKBOX;
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        final String selectionValue = getSelectionValue();
        this.checkboxes.clear();
        PageUtils.clearComposite(this);

        for (final Tuple<String> tuple : mapping) {
            final Button button = new Button(this, SWT.CHECK);
            button.setText(tuple._2);
            final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
            button.setLayoutData(gridData);
            button.setData(OPTION_VALUE, tuple._1);
            button.addListener(SWT.Selection, event -> {
                if (this.listener != null) {
                    this.listener.handleEvent(event);
                }
            });
            this.checkboxes.put(tuple._1, button);
        }

        if (StringUtils.isNoneBlank(selectionValue)) {
            select(selectionValue);
        }
    }

    @Override
    public void applyToolTipsForItems(final List<Tuple<String>> mapping) {
        mapping
                .stream()
                .filter(tuple -> StringUtils.isNoneBlank(tuple._2))
                .forEach(tuple -> {
                    final Button button = this.checkboxes.get(tuple._1);
                    if (button != null) {
                        button.setToolTipText(tuple._2);
                    }
                });
    }

    @Override
    public void select(final String keys) {
        clear();
        if (StringUtils.isBlank(keys)) {
            return;
        }

        Arrays.asList(StringUtils.split(keys, Constants.LIST_SEPARATOR))
                .stream()
                .forEach(key -> {
                    final Button button = this.checkboxes.get(key);
                    if (button != null) {
                        button.setSelection(true);
                    }
                });
    }

    @Override
    public String getSelectionValue() {
        return StringUtils.joinWith(
                Constants.LIST_SEPARATOR,
                this.checkboxes
                        .values()
                        .stream()
                        .filter(button -> button.getSelection())
                        .map(button -> (String) button.getData(OPTION_VALUE))
                        .collect(Collectors.toList()).toArray());
    }

    @Override
    public void clear() {
        this.checkboxes
                .values()
                .stream()
                .forEach(button -> button.setSelection(false));
    }

    @Override
    public void setSelectionListener(final Listener listener) {
        this.listener = listener;
    }

}
