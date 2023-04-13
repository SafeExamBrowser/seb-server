/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class TextListInput extends Composite {

    private static final long serialVersionUID = 1361754245260627626L;

    private static final int ACTION_COLUMN_WIDTH = 20;

    private final LocTextKey nameKey;
    private final int initialSize;
    private final WidgetFactory widgetFactory;
    private final Button addAction;
    private final Composite content;
    private final List<Row> list = new ArrayList<>();
    private final String tooltipText;

    private Listener valueChangeEventListener = null;

    private boolean isEditable = true;

    public TextListInput(
            final Composite parent,
            final LocTextKey nameKey,
            final int initialSize,
            final WidgetFactory widgetFactory,
            final boolean editable) {

        super(parent, SWT.NONE);
        this.nameKey = nameKey;
        this.initialSize = initialSize;
        this.widgetFactory = widgetFactory;
        this.isEditable = editable;

        // main grid layout
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 5;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        this.setLayout(gridLayout);
        final GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.setLayoutData(gridData2);

        // build header
        final Composite header = new Composite(this, SWT.NONE);
        gridLayout = new GridLayout(2, false);
        header.setLayout(gridLayout);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        header.setLayoutData(gridData);

        final Label label = widgetFactory.labelLocalized(header, this.nameKey);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        label.setLayoutData(gridData);

        final LocTextKey toolTipKey = new LocTextKey(nameKey.name + ".tooltip");
        if (widgetFactory.getI18nSupport().hasText(toolTipKey)) {
            this.tooltipText = Utils.formatLineBreaks(widgetFactory.getI18nSupport().getText(toolTipKey));
            label.setToolTipText(this.tooltipText);
        } else {
            this.tooltipText = null;
        }

        this.addAction = widgetFactory.imageButton(
                ImageIcon.ADD_BOX,
                header,
                new LocTextKey(this.nameKey.name + ".addAction"),
                this::addRow);
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        gridData.widthHint = ACTION_COLUMN_WIDTH;
        this.addAction.setLayoutData(gridData);

        this.content = header;
    }

    public void addListener(final Listener valueChangeEventListener) {
        this.valueChangeEventListener = valueChangeEventListener;
        this.list.stream().forEach(row -> row.addListener());
    }

    void addRow(final Event event) {
        this.list.add(new Row(this.list.size(), this.tooltipText));
        this.content.getParent().getParent().layout(true, true);
    }

    public String getValue() {
        return this.list.stream()
                .map(row -> row.textInput.getText())
                .reduce("", (acc, val) -> {
                    if (StringUtils.isNotBlank(val)) {
                        if (StringUtils.isNotBlank(acc)) {
                            acc += Constants.LIST_SEPARATOR;
                        }
                        acc += val;
                    }
                    return acc;
                });
    }

    public void setValue(final String value) {
        if (StringUtils.isBlank(value)) {
            // clear rows
            new ArrayList<>(this.list).stream().forEach(row -> row.deleteRow());
            this.list.clear();
            // and fill with default empty
            for (int i = 0; i < this.initialSize; i++) {
                addRow(null);
            }
            return;
        }

        final String[] split = StringUtils.split(value, Constants.LIST_SEPARATOR);
        int gap = this.list.size() - split.length;
        while (gap < 0) {
            addRow(null);
            gap++;
        }

        for (int i = 0; i < split.length; i++) {
            this.list.get(i).textInput.setText(split[i]);
        }
    }

    public void setEditable(final boolean b) {
        this.isEditable = b;
        this.addAction.setEnabled(b);
        this.setValue(getValue());
    }

    private final class Row {

        public final Text textInput;
        public final Button deleteButton;

        public Row(final int index, final String tooltipText) {
            this.textInput = TextListInput.this.widgetFactory.textInput(
                    TextListInput.this.content,
                    TextListInput.this.nameKey);
            if (tooltipText != null) {
                this.textInput.setToolTipText(tooltipText);
            }
            GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
            this.textInput.setEditable(TextListInput.this.isEditable);
            if (TextListInput.this.isEditable) {
                this.textInput.setLayoutData(gridData);
                this.addListener();
                this.deleteButton = TextListInput.this.widgetFactory.imageButton(
                        ImageIcon.REMOVE_BOX,
                        TextListInput.this.content,
                        new LocTextKey(TextListInput.this.nameKey.name + ".removeAction"),
                        deleteEvent -> deleteRow());
                gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
                gridData.widthHint = ACTION_COLUMN_WIDTH;
                this.deleteButton.setLayoutData(gridData);
                this.deleteButton.setEnabled(TextListInput.this.isEditable);
            } else {
                this.deleteButton = null;
            }

        }

        private void addListener() {
            if (TextListInput.this.valueChangeEventListener != null) {
                this.textInput.addListener(SWT.FocusOut, TextListInput.this.valueChangeEventListener);
                this.textInput.addListener(SWT.Traverse, TextListInput.this.valueChangeEventListener);
            }
        }

        public void deleteRow() {
            TextListInput.this.list.remove(this);
            this.textInput.dispose();
            if (this.deleteButton != null) {
                this.deleteButton.dispose();
            }
            TextListInput.this.content.getParent().getParent().layout(true, true);
            if (TextListInput.this.valueChangeEventListener != null) {
                TextListInput.this.valueChangeEventListener.handleEvent(null);
            }
        }
    }

}
