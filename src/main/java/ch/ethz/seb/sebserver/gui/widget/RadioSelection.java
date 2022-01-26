/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.page.PageService;

public final class RadioSelection extends Composite implements Selection {

    private static final long serialVersionUID = 7937242481193100852L;

    private final String testKey;
    private Listener listener = null;
    private final Map<String, Button> radioButtons;

    RadioSelection(final Composite parent, final String testKey) {
        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);
        this.testKey = testKey;
        if (testKey != null) {
            WidgetFactory.setTestId(this, testKey);
        }

        this.radioButtons = new LinkedHashMap<>();
    }

    @Override
    public void setAriaLabel(final String label) {
        WidgetFactory.setARIALabel(this, label);
    }

    @Override
    public Type type() {
        return Type.RADIO;
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        final String selectionValue = getSelectionValue();
        this.radioButtons.clear();
        PageService.clearComposite(this);

        for (final Tuple<String> tuple : mapping) {
            final Button button = new Button(this, SWT.RADIO);
            button.setText(tuple._2);
            final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
            button.setLayoutData(gridData);
            button.setData(OPTION_VALUE, tuple._1);
            button.addListener(SWT.Selection, event -> {
                if (this.listener != null) {
                    this.listener.handleEvent(event);
                }
            });
            WidgetFactory.setTestId(button, (this.testKey != null) ? this.testKey + tuple._1 : tuple._1);
            WidgetFactory.setARIALabel(button, tuple._2);
            this.radioButtons.put(tuple._1, button);
        }

        if (StringUtils.isNotBlank(selectionValue)) {
            select(selectionValue);
        }
    }

    @Override
    public void applyToolTipsForItems(final List<Tuple<String>> mapping) {
        mapping
                .stream()
                .filter(tuple -> StringUtils.isNotBlank(tuple._2))
                .forEach(tuple -> {
                    final Button button = this.radioButtons.get(tuple._1);
                    if (button != null) {
                        button.setToolTipText(Utils.formatLineBreaks(tuple._2));
                    }
                });
    }

    @Override
    public void select(final String key) {
        clear();
        if (StringUtils.isNotBlank(key) && this.radioButtons.containsKey(key)) {
            this.radioButtons.get(key).setSelection(true);
        }
    }

    @Override
    public String getSelectionValue() {
        return this.radioButtons
                .values()
                .stream()
                .filter(Button::getSelection)
                .findFirst()
                .map(button -> (String) button.getData(OPTION_VALUE))
                .orElse(null);
    }

    @Override
    public void clear() {
        this.radioButtons
                .values()
                .forEach(button -> button.setSelection(false));

    }

    @Override
    public void setSelectionListener(final Listener listener) {
        this.listener = listener;
    }

}
