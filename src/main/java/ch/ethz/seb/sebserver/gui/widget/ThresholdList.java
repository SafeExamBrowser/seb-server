/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.Selection.Type;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class ThresholdList extends Composite {

    private static final Logger log = LoggerFactory.getLogger(ThresholdList.class);
    private static final long serialVersionUID = -2305091471607040280L;
    private static final int ACTION_COLUMN_WIDTH = 20;

    private static final LocTextKey valueTextKey = new LocTextKey("sebserver.exam.indicator.thresholds.list.value");
    private static final LocTextKey colorTextKey = new LocTextKey("sebserver.exam.indicator.thresholds.list.color");
    private static final LocTextKey addTextKey = new LocTextKey("sebserver.exam.indicator.thresholds.list.add");
    private static final LocTextKey removeTextKey = new LocTextKey("sebserver.exam.indicator.thresholds.list.remove");

    private final WidgetFactory widgetFactory;
    private final List<Entry> thresholds = new ArrayList<>();

    private final GridData valueCell;
    private final GridData colorCell;
    private final GridData actionCell;

    ThresholdList(final Composite parent, final WidgetFactory widgetFactory) {
        super(parent, SWT.NONE);
        this.widgetFactory = widgetFactory;

        final GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        this.addListener(SWT.Resize, this::adaptColumnWidth);

        final Label valueTitle = widgetFactory.labelLocalized(this, CustomVariant.TITLE_LABEL, valueTextKey);
        this.valueCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        valueTitle.setLayoutData(this.valueCell);

        final Label colorTitle = widgetFactory.labelLocalized(this, CustomVariant.TITLE_LABEL, colorTextKey);
        this.colorCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        colorTitle.setLayoutData(this.colorCell);

        final Label imageButton = widgetFactory.imageButton(
                ImageIcon.ADD_BOX,
                this,
                addTextKey,
                this::addThreshold);
        this.actionCell = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        imageButton.setLayoutData(this.actionCell);
    }

    public void setThresholds(final Collection<Threshold> thresholds) {
        clearList();
        if (thresholds != null) {
            thresholds
                    .stream()
                    .forEach(this::addThreshold);
        }
    }

    public Collection<Threshold> getThresholds() {
        removeInvalidListEntries();
        return this.thresholds
                .stream()
                .map(entry -> new Threshold(entry.getValue(), entry.getColor()))
                .collect(Collectors.toList());
    }

    private void removeInvalidListEntries() {
        this.thresholds
                .stream()
                .filter(entry -> entry.getValue() == null || StringUtils.isBlank(entry.getColor()))
                .collect(Collectors.toList())
                .stream()
                .forEach(entry -> removeThreshold(entry));
    }

    private void clearList() {
        this.thresholds.stream()
                .forEach(e -> e.dispose());
        this.thresholds.clear();
    }

    private void addThreshold(final Event event) {
        addThreshold((Threshold) null);
    }

    private void addThreshold(final Threshold threshold) {
        final Text valueInput = this.widgetFactory.numberInput(this, s -> Double.parseDouble(s));
        final GridData valueCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        valueInput.setLayoutData(valueCell);

        final Selection selector = this.widgetFactory.selectionLocalized(Type.COLOR, this, null);
        final GridData selectorCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        selectorCell.horizontalIndent = 2;
        selector.adaptToControl().setLayoutData(selectorCell);

        final Label imageButton = this.widgetFactory.imageButton(
                ImageIcon.REMOVE_BOX,
                this,
                removeTextKey,
                null);
        final GridData actionCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        imageButton.setLayoutData(actionCell);

        if (threshold != null) {
            if (threshold.value != null) {
                valueInput.setText(threshold.value.toString());
            }
            if (threshold.color != null) {
                selector.select(threshold.color);
            }
        }

        final Entry entry = new Entry(valueInput, selector, imageButton);
        this.thresholds.add(entry);

        this.getParent().layout();
    }

    private void removeThreshold(final Entry entry) {
        if (this.thresholds.remove(entry)) {
            entry.dispose();
        }

        this.getParent().layout();
    }

    private void adaptColumnWidth(final Event event) {
        try {
            // TODO
            final int currentTableWidth = this.getClientArea().width;
            final int dynWidth = currentTableWidth - ACTION_COLUMN_WIDTH;
            final int colWidth = dynWidth / 2;
            this.valueCell.widthHint = colWidth;
            this.colorCell.widthHint = colWidth;
            this.layout();
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    private final class Entry {
        final Text valueInput;
        final Selection colorSelector;
        final Label removeButton;

        Entry(final Text valueInput, final Selection colorSelector, final Label removeButton) {
            super();
            this.valueInput = valueInput;
            this.colorSelector = colorSelector;
            this.removeButton = removeButton;
            removeButton.addListener(SWT.MouseDown, event -> removeThreshold(this));
        }

        void dispose() {
            this.valueInput.dispose();
            this.colorSelector.adaptToControl().dispose();
            this.removeButton.dispose();
        }

        Double getValue() {
            if (this.valueInput == null || StringUtils.isBlank(this.valueInput.getText())) {
                return null;
            }

            return Double.parseDouble(this.valueInput.getText());
        }

        String getColor() {
            if (this.colorSelector == null) {
                return null;
            }

            return this.colorSelector.getSelectionValue();
        }
    }

}
