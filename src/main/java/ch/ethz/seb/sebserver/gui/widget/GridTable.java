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
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
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
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class GridTable extends Composite {

    private static final long serialVersionUID = 8515260041931976458L;
    private static final Logger log = LoggerFactory.getLogger(GridTable.class);

    public static final Set<AttributeType> SUPPORTED_TYPES = EnumSet.of(
            AttributeType.CHECKBOX,
            AttributeType.TEXT_FIELD);

    private static final int ACTION_COLUMN_WIDTH = 20;

    private final WidgetFactory widgetFactory;
    private final List<Column> columns;
    private final Button addAction;
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
        gridLayout.marginHeight = 5;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        this.setLayout(gridLayout);

        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.columns = new ArrayList<>();
        for (final ColumnDef columnDef : columnDefs) {
            final Label label = widgetFactory.labelLocalized(
                    this,
                    new LocTextKey(locTextKeyPrefix + columnDef.name));
            final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            label.setLayoutData(gridData);
            this.columns.add(new Column(columnDef, gridData));
        }

        this.addAction = widgetFactory.imageButton(
                ImageIcon.ADD_BOX,
                this,
                new LocTextKey(locTextKeyPrefix + "addAction"),
                this::addRow);
        final GridData gridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
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
        PageService.updateScrolledComposite(this);
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
                        .map(Row::getValue)
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

            final int currentTableWidth = super.getClientArea().width - ACTION_COLUMN_WIDTH;
            final int widthUnit = currentTableWidth / this.columns
                    .stream()
                    .reduce(0,
                            (i, c2) -> i + c2.columnDef.widthFactor,
                            Integer::sum);

            this.columns
                    .forEach(c -> c.header.widthHint = c.columnDef.widthFactor * widthUnit);

            super.layout(true, true);
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    final class Row {
        final List<ControlAdapter> cells;
        final Button removeAction;

        protected Row(final List<ControlAdapter> cells) {
            this.cells = cells;
            this.removeAction = GridTable.this.widgetFactory.imageButton(
                    ImageIcon.REMOVE_BOX,
                    GridTable.this,
                    new LocTextKey(GridTable.this.locTextKeyPrefix + "removeAction"),
                    event -> deleteRow(this));
            final GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
            gridData.widthHint = ACTION_COLUMN_WIDTH;
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

        public static ColumnDef fromString(
                final String string,
                final Map<String, String> defaultValueMap) {

            if (StringUtils.isBlank(string)) {
                return null;
            }

            final String[] split = StringUtils.split(string, Constants.COMPLEX_VALUE_SEPARATOR);
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
        final GridData header;

        protected Column(final ColumnDef columnDef, final GridData header) {
            this.columnDef = columnDef;
            this.header = header;
        }
    }

    interface ControlAdapter {
        String getValue();

        void setValue(String value);

        void dispose();

        ColumnDef columnDef();
    }

    private static class Dummy implements ControlAdapter {

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

    private static class CheckBox implements ControlAdapter {

        private final Button checkboxButton;
        private final ColumnDef columnDef;

        CheckBox(final Composite parent, final ColumnDef columnDef, final Listener listener) {
            this.checkboxButton = new Button(parent, SWT.CHECK);
            this.checkboxButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            this.columnDef = columnDef;
            if (listener != null) {
                this.checkboxButton.addListener(SWT.Selection, listener);
            }
        }

        @Override
        public String getValue() {
            return this.checkboxButton.getSelection()
                    ? Constants.TRUE_STRING
                    : Constants.FALSE_STRING;
        }

        @Override
        public void setValue(final String value) {
            this.checkboxButton.setSelection(BooleanUtils.toBoolean(value));
        }

        @Override
        public void dispose() {
            this.checkboxButton.dispose();
        }

        @Override
        public ColumnDef columnDef() {
            return this.columnDef;
        }
    }

    private static class TextField implements ControlAdapter {

        private final Text _textField;
        private final ColumnDef columnDef;

        TextField(final Composite parent, final ColumnDef columnDef, final Listener listener) {
            this._textField = new Text(parent, SWT.LEFT | SWT.BORDER);
            this._textField.setData(RWT.CUSTOM_VARIANT, CustomVariant.CONFIG_INPUT_READONLY.key);
            this._textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            this.columnDef = columnDef;
            this._textField.addListener(SWT.FocusOut, listener);
            this._textField.addListener(SWT.Traverse, listener);
        }

        @Override
        public String getValue() {
            return this._textField.getText();
        }

        @Override
        public void setValue(final String value) {
            this._textField.setText((value != null) ? value : "");
        }

        @Override
        public void dispose() {
            this._textField.dispose();
        }

        @Override
        public ColumnDef columnDef() {
            return this.columnDef;
        }
    }

}
