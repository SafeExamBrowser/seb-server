/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class GridTable extends Composite {

    private static final Logger log = LoggerFactory.getLogger(GridTable.class);

    public static final Set<AttributeType> SUPPORTED_TYPES = EnumSet.of(
            AttributeType.CHECKBOX,
            AttributeType.TEXT_FIELD);

    private static final int ACTION_COLUMN_WIDTH = 20;

    private final WidgetFactory widgetFactory;
    private final List<Column> columns;
    private final Label addAction;
    private final List<Row> rows;
    private final String locTextKeyPrefix;
    private Listener listener;

    public GridTable(
            final Composite parent,
            final List<ColumnDef> columnDefs,
            final String locTextKeyPrefix,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.NONE);

        this.widgetFactory = widgetFactory;
        this.locTextKeyPrefix = locTextKeyPrefix;
        final GridLayout gridLayout = new GridLayout(columnDefs.size() + 1, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        this.setLayout(gridLayout);
        // this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.columns = new ArrayList<>();
        for (final ColumnDef columnDef : columnDefs) {
            final Label label = new Label(this, SWT.NONE);
            label.setText("column");
//widgetFactory.labelLocalized(
//                    this,
//                    new LocTextKey(locTextKeyPrefix + columnDef.name));
            final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            //       gridData.widthHint = 50;
            label.setLayoutData(gridData);
            this.columns.add(new Column(columnDef, label, gridData));
        }

        this.addAction = widgetFactory.imageButton(
                ImageIcon.ADD_BOX,
                this,
                new LocTextKey(locTextKeyPrefix + "addAction"),
                this::addRow);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = ACTION_COLUMN_WIDTH;
        this.addAction.setLayoutData(gridData);

        this.rows = new ArrayList<>();
        this.addListener(SWT.Resize, this::adaptColumnWidth);
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    void addRow(final Event event) {
        final List<ControlAdapter> row = new ArrayList<>();
        for (final Column column : this.columns) {
            row.add(createCell(column, column.columnDef.defaultValue));
        }
        this.rows.add(new Row(row));

        this.adaptLayout();

        if (this.listener != null) {
            this.listener.handleEvent(new Event());
        }
    }

    public void adaptLayout() {
        this.getParent().getParent().layout(true, true);

        Composite parent = this.getParent();
        while (parent != null && !(parent instanceof ScrolledComposite)) {
            parent = parent.getParent();
        }

        System.out.println("********************** " + parent);
        if (parent != null) {
            ((ScrolledComposite) parent).setMinSize(this.getParent().getParent().computeSize(400, SWT.DEFAULT));
        }
    }

    void addRow(final String values) {
        if (StringUtils.isBlank(values)) {
            return;
        }

        final Map<String, String> nameValueMap = new HashMap<>();
        for (final String valueMap : StringUtils.split(values, Constants.EMBEDDED_LIST_SEPARATOR)) {
            final String[] nameValue = StringUtils.split(valueMap, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
            if (nameValue.length > 1) {
                nameValueMap.put(nameValue[0], nameValue[1]);
            } else {
                nameValueMap.put(nameValue[0], null);
            }
        }

        final List<ControlAdapter> row = new ArrayList<>();
        for (final Column column : this.columns) {
            row.add(createCell(column, nameValueMap.get(column.columnDef.name)));
        }
        this.rows.add(new Row(row));
    }

    void deleteRow(final Row row) {
        if (this.rows.remove(row)) {
            row.dispose();
        }

        this.adaptLayout();
    }

    public String getValue() {
        return StringUtils.join(
                this.rows
                        .stream()
                        .map(row -> row.getValue())
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR);
    }

    public void setValue(final String value) {
        clearTable();

        if (StringUtils.isBlank(value)) {
            return;
        }

        for (final String val : StringUtils.split(value, Constants.LIST_SEPARATOR)) {
            addRow(val);
        }

        this.adaptLayout();
    }

    private ControlAdapter createCell(final Column column, final String value) {
        switch (column.columnDef.type) {
            case CHECKBOX: {
                final CheckBox checkBox = new CheckBox(this, column.columnDef, this.listener);
                checkBox.setValue(value);
                return checkBox;
            }
            case TEXT_FIELD: {
                final TextField textField = new TextField(this, column.columnDef, this.listener);
                textField.setValue(value);
                return textField;
            }
            default: {
                return new Dummy(this, column.columnDef);
            }
        }
    }

    private void clearTable() {
        for (final Row row : this.rows) {
            row.dispose();
        }
        this.rows.clear();
    }

    private void adaptColumnWidth(final Event event) {
        try {
//            final int currentTableWidth = this.getClientArea().width;
//            final int dynWidth = currentTableWidth - ACTION_COLUMN_WIDTH;
//            final int colWidth = dynWidth / this.columns.size();
//            for (final Column column : this.columns) {
//                column.header.widthHint = 200;// colWidth;
//            }
            this.columns.get(0).header.widthHint = 50;
            this.columns.get(1).header.widthHint = 150;

        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    final class Row {
        final List<ControlAdapter> cells;
        final Label removeAction;

        protected Row(final List<ControlAdapter> cells) {
            this.cells = cells;
            this.removeAction = GridTable.this.widgetFactory.imageButton(
                    ImageIcon.REMOVE_BOX,
                    GridTable.this,
                    new LocTextKey(GridTable.this.locTextKeyPrefix + "addAction"),
                    event -> deleteRow(this));
            final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, true, false);
            this.removeAction.setLayoutData(gridData);
        }

        void dispose() {
            for (final ControlAdapter cell : this.cells) {
                cell.dispose();
            }
            this.removeAction.dispose();
        }

        String getValue() {
            return StringUtils.join(
                    this.cells
                            .stream()
                            .map(cell -> cell.columnDef().name + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR
                                    + cell.getValue())
                            .collect(Collectors.toList()),
                    Constants.EMBEDDED_LIST_SEPARATOR);
        }
    }

    public static final class ColumnDef {
        final int widthFactor;
        final String name;
        final AttributeType type;
        final String defaultValue;

        protected ColumnDef(
                final int widthFactor,
                final String name,
                final AttributeType type,
                final String defaultValue) {

            this.widthFactor = widthFactor;
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        // 2:argument:TEXT_FIELD
        public static final ColumnDef fromString(
                final String string,
                final Map<String, String> defaultValueMap) {

            if (StringUtils.isBlank(string)) {
                return null;
            }

            final String[] split = StringUtils.split(string, ':');

            final AttributeType attributeType = AttributeType.valueOf(split[2]);
            if (!SUPPORTED_TYPES.contains(attributeType)) {
                throw new UnsupportedOperationException(
                        "The AttributeType : " + attributeType + " is not supported yet");
            }

            return new ColumnDef(
                    Integer.parseInt(split[0]),
                    split[1],
                    attributeType,
                    defaultValueMap.get(split[1]));
        }
    }

    private static class Column {
        final ColumnDef columnDef;
        final Label label;
        final GridData header;

        protected Column(final ColumnDef columnDef, final Label label, final GridData header) {
            this.columnDef = columnDef;
            this.label = label;
            this.header = header;
        }
    }

    interface ControlAdapter {
        String getValue();

        void setValue(String value);

        void dispose();

        ColumnDef columnDef();
    }

    private class Dummy implements ControlAdapter {

        private final Label label;
        private final ColumnDef columnDef;

        Dummy(final Composite parent, final ColumnDef columnDef) {
            this.label = new Label(parent, SWT.NONE);
            this.label.setText("unsupported");
            this.columnDef = columnDef;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue(final String value) {
        }

        @Override
        public void dispose() {
            this.label.dispose();
        }

        @Override
        public ColumnDef columnDef() {
            return this.columnDef;
        }
    }

    private class CheckBox implements ControlAdapter {

        private final Button checkbox;
        private final ColumnDef columnDef;

        CheckBox(final Composite parent, final ColumnDef columnDef, final Listener listener) {
            this.checkbox = new Button(parent, SWT.CHECK);
            this.checkbox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            this.columnDef = columnDef;
            if (listener != null) {
                this.checkbox.addListener(SWT.Selection, listener);
            }
        }

        @Override
        public String getValue() {
            return this.checkbox.getSelection()
                    ? Constants.TRUE_STRING
                    : Constants.FALSE_STRING;
        }

        @Override
        public void setValue(final String value) {
            this.checkbox.setSelection(BooleanUtils.toBoolean(value));
        }

        @Override
        public void dispose() {
            this.checkbox.dispose();
        }

        @Override
        public ColumnDef columnDef() {
            return this.columnDef;
        }
    }

    private class TextField implements ControlAdapter {

        private final Text textField;
        private final ColumnDef columnDef;

        TextField(final Composite parent, final ColumnDef columnDef, final Listener listener) {
            this.textField = new Text(parent, SWT.LEFT | SWT.BORDER);
            this.textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            this.columnDef = columnDef;
            this.textField.addListener(SWT.FocusOut, listener);
            this.textField.addListener(SWT.Traverse, listener);
        }

        @Override
        public String getValue() {
            return this.textField.getText();
        }

        @Override
        public void setValue(final String value) {
            this.textField.setText((value != null) ? value : "");
        }

        @Override
        public void dispose() {
            this.textField.dispose();
        }

        @Override
        public ColumnDef columnDef() {
            return this.columnDef;
        }
    }

}
