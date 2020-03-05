/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public abstract class AbstractTableFieldBuilder implements InputFieldBuilder {

    private static final Logger log = LoggerFactory.getLogger(AbstractTableFieldBuilder.class);

    private static final int ROW_HEIGHT = 20;
    private static final int NAV_HEIGHT = 40;
    private static final int TABLE_WIDTH_SPACE = 50;

    protected final RestService restService;
    protected final WidgetFactory widgetFactory;
    protected InputFieldBuilderSupplier inputFieldBuilderSupplier;

    protected AbstractTableFieldBuilder(
            final RestService restService,
            final WidgetFactory widgetFactory) {

        this.restService = restService;
        this.widgetFactory = widgetFactory;
    }

    @Override
    public void init(final InputFieldBuilderSupplier inputFieldBuilderSupplier) {
        this.inputFieldBuilderSupplier = inputFieldBuilderSupplier;
    }

    protected Table createTable(final Composite parent, final TableContext tableContext) {
        final Table table = new Table(parent, SWT.NONE | SWT.V_SCROLL);
        table.setLayout(new GridLayout());
        final GridData gridData = new GridData(
                SWT.FILL, SWT.FILL,
                true, false,
                tableContext.orientation.width(),
                tableContext.orientation.height());

        gridData.heightHint = tableContext.orientation.height() * ROW_HEIGHT + NAV_HEIGHT;
        table.setLayoutData(gridData);
        table.setHeaderVisible(true);
        table.addListener(SWT.Resize, event -> adaptColumnWidth(table, tableContext));
        return table;
    }

    protected TableContext createTableContext(final ConfigurationAttribute attribute, final ViewContext viewContext) {
        return new TableContext(
                this.inputFieldBuilderSupplier,
                this.widgetFactory,
                attribute,
                viewContext);
    }

    protected void setSelectionListener(final Table table, final AbstractTableInputField tableField) {
        table.addListener(SWT.MouseDoubleClick, event -> {
            final int selectionIndex = table.getSelectionIndex();
            if (selectionIndex >= 0) {
                tableField.openForm(selectionIndex);
            }
        });
    }

    protected void adaptColumnWidth(
            final Table table,
            final TableContext tableContext) {

        try {
            final boolean readonly = tableContext.getViewContext().readonly;
            final int currentTableWidth = table.getClientArea().width - TABLE_WIDTH_SPACE;
            final TableColumn[] columns = table.getColumns();
            final List<Orientation> orientations = tableContext
                    .getColumnAttributes()
                    .stream()
                    .map(attr -> tableContext.getOrientation(attr.id))
                    .collect(Collectors.toList());
            final Integer div = orientations
                    .stream()
                    .map(o -> o.width)
                    .reduce(0, Integer::sum);
            final int widthUnit = currentTableWidth / div;
            for (int i = 0; i < columns.length - ((readonly) ? 0 : 2); i++) {
                columns[i].setWidth(widthUnit * orientations.get(i).width);
            }
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    protected static void setValueToCell(
            final TableContext tableContext,
            final TableItem item,
            final int cellIndex,
            final ConfigurationAttribute attribute,
            final TableValue tableValue) {

        switch (attribute.type) {
            case CHECKBOX: {
                item.setImage(
                        cellIndex,
                        (BooleanUtils.toBoolean((tableValue != null) ? tableValue.value : null))
                                ? ImageIcon.YES.getImage(item.getDisplay())
                                : ImageIcon.NO.getImage(item.getDisplay()));
                break;
            }
            case SINGLE_SELECTION: {
                final String key = ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                        attribute.getName() + "." +
                        tableValue.value;
                item.setText(
                        cellIndex,
                        tableContext.i18nSupport().getText(key, getValue(attribute, tableValue)));
                break;
            }
            default: {
                item.setText(cellIndex, getValue(attribute, tableValue));
                break;
            }
        }
    }

    private static String getValue(
            final ConfigurationAttribute attribute,
            final TableValue tableValue) {

        if (tableValue == null) {
            if (StringUtils.isBlank(attribute.defaultValue)) {
                return Constants.EMPTY_NOTE;
            } else {
                return attribute.defaultValue;
            }
        } else {
            if (StringUtils.isBlank(tableValue.value)) {
                return Constants.EMPTY_NOTE;
            } else {
                return tableValue.value;
            }
        }
    }

    static abstract class AbstractTableInputField extends AbstractInputField<Table> {

        protected final TableContext tableContext;

        protected AbstractTableInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Table control,
                final Label errorLabel,
                final TableContext tableContext) {

            super(attribute, orientation, control, errorLabel);
            this.tableContext = tableContext;
        }

        @Override
        public ConfigurationValue initValue(final Collection<ConfigurationValue> values) {
            clearTable();
            // get all child values as TableValues
            final List<TableValue> tableValues = getChildValues(
                    this.tableContext,
                    this.attribute,
                    values);

            initValue(tableValues);
            return null;
        }

        abstract void initValue(final List<TableValue> tableValues);

        abstract void openForm(final int selectionIndex);

        abstract void applyTableRowValues(final int index);

        protected List<TableValue> getChildValues(
                final TableContext tableContext,
                final ConfigurationAttribute attribute,
                final Collection<ConfigurationValue> values) {

            return values.stream()
                    .filter(v -> isChildValue(tableContext, attribute, v))
                    .map(TableValue::of)
                    .collect(Collectors.toList());
        }

        protected boolean isChildValue(
                final TableContext tableContext,
                final ConfigurationAttribute attribute,
                final ConfigurationValue value) {

            if (!tableContext.getViewContext().attributeMapping.attributeIdMapping
                    .containsKey(value.attributeId)) {

                return false;
            }

            ConfigurationAttribute attr = tableContext.getAttribute(value.attributeId);
            if (attr == null) {
                return false;
            }
            while (attr.parentId != null) {
                if (attribute.id.equals(attr.parentId)) {
                    return true;
                }
                attr = tableContext.getAttribute(attr.parentId);
            }

            return false;
        }

        protected Map<Integer, Map<Long, TableValue>> createRowIndexMap(final List<TableValue> tableValues) {
            final Map<Integer, Map<Long, TableValue>> indexMapping = new HashMap<>();
            for (final TableValue tableValue : tableValues) {
                final Map<Long, TableValue> rowValues = indexMapping.computeIfAbsent(
                        tableValue.listIndex,
                        key -> new HashMap<>());
                rowValues.put(tableValue.attributeId, tableValue);
            }
            return indexMapping;
        }

        protected void valuesFromIndexMap(
                final List<Map<Long, TableValue>> values,
                final Map<Integer, Map<Long, TableValue>> indexMapping) {

            values.clear();
            final List<Integer> rows = new ArrayList<>(indexMapping.keySet());
            rows.sort(Integer::compareTo);
            rows
                    .forEach(i -> {
                        final Map<Long, TableValue> rowValues = indexMapping.get(i);
                        values.add(rowValues);
                    });
        }

        protected void applyFormValues(
                final List<Map<Long, TableValue>> values,
                final Map<Long, TableValue> rowValues,
                final int index) {

            if (!values.isEmpty()) {
                values.remove(index);
                values.add(index, rowValues);
                applyTableRowValues(index);
            }

            // send values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue(values));
        }

        protected ConfigurationTableValues extractTableValue(final List<Map<Long, TableValue>> values) {
            final List<TableValue> collect = values
                    .stream()
                    .flatMap(map -> map.values().stream())
                    .collect(Collectors.toList());

            return new ConfigurationTableValues(
                    this.tableContext.getInstitutionId(),
                    this.tableContext.getConfigurationId(),
                    this.attribute.id,
                    collect);
        }

        @Override
        public void setDefaultValue() {
            // NOTE this just empty the list for now
            // TODO do we need default values for lists?
            clearTable();
            final List<TableValue> values = new ArrayList<>();
            this.tableContext.getValueChangeListener().tableChanged(
                    new ConfigurationTableValues(
                            this.tableContext.getInstitutionId(),
                            this.tableContext.getConfigurationId(),
                            this.attribute.id,
                            values));
        }

        void clearTable() {
            this.control.setSelection(-1);
            if (this.control.getItemCount() > 0) {
                for (final TableItem item : this.control.getItems()) {
                    item.dispose();
                }
            }
        }

        @Override
        protected void setValueToControl(final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getValue() {
            throw new UnsupportedOperationException();
        }
    }

}
