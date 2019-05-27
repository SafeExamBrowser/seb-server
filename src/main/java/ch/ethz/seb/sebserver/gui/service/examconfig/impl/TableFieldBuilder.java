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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

@Lazy
@Component
@GuiProfile
public class TableFieldBuilder implements InputFieldBuilder {

    private static final Logger log = LoggerFactory.getLogger(TableFieldBuilder.class);

    private static final String ROW_VALUE_KEY = "RowValues";

    private final RestService restService;
    private final WidgetFactory widgetFactory;
    private InputFieldBuilderSupplier inputFieldBuilderSupplier;

    protected TableFieldBuilder(
            final RestService restService,
            final WidgetFactory widgetFactory) {

        this.restService = restService;
        this.widgetFactory = widgetFactory;
    }

    @Override
    public void init(final InputFieldBuilderSupplier inputFieldBuilderSupplier) {
        this.inputFieldBuilderSupplier = inputFieldBuilderSupplier;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return AttributeType.TABLE == attribute.type;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final I18nSupport i18nSupport = viewContext.getI18nSupport();
        final TableContext tableContext = new TableContext(
                this.inputFieldBuilderSupplier,
                this.widgetFactory,
                this.restService,
                attribute,
                viewContext);

        final Table table = new Table(parent, SWT.NONE | SWT.H_SCROLL);
        table.setLayout(new GridLayout());
        final GridData gridData = new GridData(
                SWT.FILL, SWT.FILL,
                true, false,
                (tableContext.orientation != null) ? tableContext.orientation.width() : 1,
                (tableContext.orientation != null) ? tableContext.orientation.height() : 1);
        gridData.heightHint = tableContext.orientation.height * 20 + 40;
        table.setLayoutData(gridData);
        table.setHeaderVisible(true);
        table.addListener(SWT.Resize, event -> adaptColumnWidth(table, tableContext));

        for (final ConfigurationAttribute columnAttribute : tableContext.getColumnAttributes()) {
            final TableColumn column = new TableColumn(table, SWT.NONE);
            final String text = i18nSupport.getText(
                    ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                            columnAttribute.name,
                    columnAttribute.name);
            column.setText(text);
            column.setWidth(100);
            column.setResizable(false);
        }

        final TableInputField tableField = new TableInputField(
                tableContext,
                table);

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setImage(ImageIcon.ADD_BOX.getImage(parent.getDisplay()));

        column.setWidth(20);
        column.setResizable(false);
        column.setMoveable(false);

        column.addListener(SWT.Selection, event -> {
            tableField.addRow();
        });

        column = new TableColumn(table, SWT.NONE);
        column.setImage(ImageIcon.REMOVE_BOX.getImage(parent.getDisplay()));

        column.setWidth(20);
        column.setResizable(false);
        column.setMoveable(false);

        column.addListener(SWT.Selection, event -> {
            final int selectionIndex = table.getSelectionIndex();
            if (selectionIndex >= 0) {
                tableField.deleteRow(selectionIndex);
            }
        });

        table.addListener(SWT.MouseDoubleClick, event -> {
            final int selectionIndex = table.getSelectionIndex();
            if (selectionIndex >= 0) {
                tableField.openForm(selectionIndex);
            }
        });

        return tableField;
    }

    private void adaptColumnWidth(
            final Table table,
            final TableContext tableContext) {

        try {
            final int currentTableWidth = table.getClientArea().width - 50;
            final TableColumn[] columns = table.getColumns();
            final List<Orientation> orientations = tableContext
                    .getColumnAttributes()
                    .stream()
                    .map(attr -> tableContext.getOrientation(attr.id))
                    .collect(Collectors.toList());
            final Integer div = orientations
                    .stream()
                    .map(o -> o.width)
                    .reduce(0, (acc, val) -> acc + val);
            final int widthUnit = currentTableWidth / div;
            for (int i = 0; i < columns.length - 2; i++) {
                columns[i].setWidth(widthUnit * orientations.get(i).width);
            }
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    static final class TableInputField extends AbstractInputField<Table> {

        private final TableContext tableContext;

        private List<Map<Long, TableValue>> values;

        TableInputField(
                final TableContext tableContext,
                final Table control) {

            super(tableContext.attribute, tableContext.orientation, control, null);
            this.tableContext = tableContext;
        }

        @Override
        public ConfigurationValue initValue(final Collection<ConfigurationValue> values) {
            clearTable();
            // get all child values as TableValues
            final List<TableValue> tableValues = values.stream()
                    .filter(this::isChildValue)
                    .map(TableValue::of)
                    .collect(Collectors.toList());

            initValue(tableValues);

            return null;
        }

        void initValue(final List<TableValue> tableValues) {
            final Map<Integer, Map<Long, TableValue>> _initValue = new HashMap<>();
            for (final TableValue tableValue : tableValues) {
                final Map<Long, TableValue> rowValues = _initValue.computeIfAbsent(
                        tableValue.listIndex,
                        key -> new HashMap<>());
                rowValues.put(tableValue.attributeId, tableValue);
            }

            final List<Integer> rows = new ArrayList<>(_initValue.keySet());
            rows.sort((i1, i2) -> i1.compareTo(i2));

            this.values = new ArrayList<>();
            rows
                    .stream()
                    .forEach(i -> {
                        final Map<Long, TableValue> rowValues = _initValue.get(i);
                        this.values.add(rowValues);
                        addTableRow(rowValues);
                    });
        }

        private boolean isChildValue(final ConfigurationValue value) {
            if (!this.tableContext.getViewContext().attributeMapping.attributeIdMapping
                    .containsKey(value.attributeId)) {

                return false;
            }

            ConfigurationAttribute attr = this.tableContext.getAttribute(value.attributeId);
            while (attr.parentId != null) {
                if (this.attribute.id.equals(attr.parentId)) {
                    return true;
                }
                attr = this.tableContext.getAttribute(attr.parentId);
            }

            return false;
        }

        private void deleteRow(final int selectionIndex) {
            this.control.remove(selectionIndex);
            this.values.remove(selectionIndex);
            // send new values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue());
        }

        private void addRow() {
            final int index = this.values.size();
            // create new values form default values
            final Map<Long, TableValue> rowValues = this.tableContext.getRowAttributes()
                    .stream()
                    .map(attr -> new TableValue(attr.id, index, attr.defaultValue))
                    .collect(Collectors.toMap(
                            tv -> tv.attributeId,
                            Function.identity()));

            this.values.add(rowValues);
            addTableRow(rowValues);
            this.control.layout();
            // send new values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue());
        }

        private void addTableRow(final Map<Long, TableValue> rowValues) {
            new TableItem(this.control, SWT.NONE);
            applyTableRowValues(this.values.size() - 1);

            // TODO try to add delete button within table row?
        }

        private void applyTableRowValues(final int index) {
            final TableItem item = this.control.getItem(index);
            final Map<Long, TableValue> rowValues = this.values.get(index);

            int cellIndex = 0;
            for (final ConfigurationAttribute attr : this.tableContext.getColumnAttributes()) {
                if (rowValues.containsKey(attr.id)) {
                    final TableValue tableValue = rowValues.get(attr.id);
                    if (attr.type == AttributeType.CHECKBOX) {

                        item.setImage(
                                cellIndex,
                                (BooleanUtils.toBoolean((tableValue != null) ? tableValue.value : null))
                                        ? ImageIcon.ACTIVE.getImage(this.control.getDisplay())
                                        : ImageIcon.INACTIVE.getImage(this.control.getDisplay()));
                    } else {
                        item.setText(
                                cellIndex,
                                this.tableContext.getRowValue(tableValue));
                    }
                }
                cellIndex++;
            }

            item.setData(ROW_VALUE_KEY, item);
        }

        private void openForm(final int selectionIndex) {
            final Map<Long, TableValue> rowValues = this.values.get(selectionIndex);
            final TableRowFormBuilder builder = new TableRowFormBuilder(
                    this.tableContext,
                    rowValues,
                    selectionIndex);

            new ModalInputDialog<Map<Long, TableValue>>(
                    this.control.getShell(),
                    this.tableContext.getWidgetFactory())
                            .setDialogWidth(500)
                            .open(
                                    ExamConfigurationService.getTablePopupTitleKey(
                                            this.attribute,
                                            this.tableContext.getViewContext().i18nSupport),
                                    values -> applyFormValues(values, selectionIndex),
                                    () -> this.tableContext.getValueChangeListener()
                                            .tableChanged(extractTableValue()),
                                    builder);
        }

        private void applyFormValues(final Map<Long, TableValue> values, final int index) {
            if (values != null && !values.isEmpty()) {
                this.values.remove(index);
                this.values.add(index, values);
                applyTableRowValues(index);
            }

            // send values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue());
        }

        private ConfigurationTableValues extractTableValue() {
            final List<TableValue> collect = this.values
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

        private void clearTable() {
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
