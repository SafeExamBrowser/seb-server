/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class CompositeTableFieldBuilder extends AbstractTableFieldBuilder {

    private static final Logger log = LoggerFactory.getLogger(CompositeTableFieldBuilder.class);

    private static final String TABLE_ENTRY_NAME = "TABLE_ENTRY";
    private static final String TABLE_COLUMN_NAME_KEY = "TABLE_COLUMN_NAME";

    protected CompositeTableFieldBuilder(
            final RestService restService,
            final WidgetFactory widgetFactory) {

        super(restService, widgetFactory);
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return AttributeType.COMPOSITE_TABLE == attribute.type;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final I18nSupport i18nSupport = viewContext.getI18nSupport();
        final TableContext tableContext = createTableContext(attribute, viewContext);
        final Table table = createTable(parent, tableContext);

        final String resources = attribute.getResources();
        final String[] columnsAndRows = StringUtils.split(
                resources,
                Constants.EMBEDDED_LIST_SEPARATOR);
        final String[] columns = (columnsAndRows.length == 2)
                ? StringUtils.split(columnsAndRows[0], Constants.LIST_SEPARATOR)
                : new String[] { TABLE_ENTRY_NAME };
        final String[] rows = (columnsAndRows.length == 2)
                ? StringUtils.split(columnsAndRows[1], Constants.LIST_SEPARATOR)
                : StringUtils.split(columnsAndRows[0], Constants.LIST_SEPARATOR);

        final String attributeNameKey = ExamConfigurationService.attributeNameKey(attribute);
        for (int i = 0; i < columns.length; i++) {
            final TableColumn column = this.widgetFactory.tableColumnLocalized(
                    table,
                    new LocTextKey(attributeNameKey + "." + columns[i]),
                    new LocTextKey(attributeNameKey + "." + columns[i] + ".tootlip"));

            column.setData(TABLE_COLUMN_NAME_KEY, columns[i]);
            column.setWidth(100);
            column.setResizable(false);
            column.setMoveable(false);
        }

        for (int i = 0; i < rows.length; i++) {
            final TableItem item = new TableItem(table, SWT.NONE);
            for (int j = 0; j < columns.length; j++) {
                if (TABLE_ENTRY_NAME.equals(columns[j])) {
                    item.setText(j, i18nSupport.getText(
                            ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + rows[i],
                            rows[i]));
                }
            }
        }

        final CompositeTableInputField tableField = new CompositeTableInputField(
                tableContext,
                table,
                Arrays.asList(columns),
                Arrays.asList(rows));

        setSelectionListener(table, tableField);
        return tableField;
    }

    @Override
    protected void adaptColumnWidth(
            final Table table,
            final TableContext tableContext) {

        try {
            final int currentTableWidth = table.getClientArea().width - 50;
            final TableColumn[] columns = table.getColumns();
            final int widthUnit = currentTableWidth / (columns.length + 2);
            for (int i = 0; i < columns.length; i++) {
                final int factor = (TABLE_ENTRY_NAME.equals(columns[i].getData(TABLE_COLUMN_NAME_KEY))) ? 4 : 1;
                columns[i].setWidth(widthUnit * factor);
            }
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    static final class CompositeTableInputField extends AbstractTableInputField {

        final List<String> columns;
        final List<String> rows;
        final List<Map<Long, TableValue>> values;

        CompositeTableInputField(
                final TableContext tableContext,
                final Table control,
                final List<String> columns,
                final List<String> rows) {

            super(tableContext.attribute, tableContext.orientation, control, null, tableContext);
            this.values = new ArrayList<>();
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        void initValue(final List<TableValue> tableValues) {
            valuesFromIndexMap(this.values, createRowIndexMap(tableValues));
            for (int i = 0; i < this.values.size(); i++) {
                setRowValues(i, this.values.get(i));
            }
        }

        @Override
        protected void applyTableRowValues(final int index) {
            setRowValues(index, this.values.get(index));
        }

        private void setRowValues(final int index, final Map<Long, TableValue> map) {
            final TableItem rowItem = this.control.getItem(index);
            for (final TableValue val : map.values()) {
                final Orientation orientation = this.tableContext.getOrientation(val.attributeId);
                final String groupId = orientation.getGroupId();
                if (StringUtils.isNotBlank(groupId)) {
                    final int cellIndex = this.columns.indexOf(groupId);
                    if (cellIndex >= 0) {
                        setValueToCell(
                                this.tableContext,
                                rowItem,
                                cellIndex,
                                this.tableContext.getAttribute(val.attributeId),
                                val);
                    }
                }
            }
        }

        @Override
        protected void openForm(final int selectionIndex) {
            final String row = this.rows.get(selectionIndex);
            final Map<Long, TableValue> rowValues = this.values.get(selectionIndex);
            final TableRowFormBuilder builder = new TableRowFormBuilder(
                    this.tableContext,
                    rowValues,
                    row);

            final ModalInputDialog<Map<Long, TableValue>> dialog = new ModalInputDialog<Map<Long, TableValue>>(
                    this.control.getShell(),
                    this.tableContext.getWidgetFactory())
                            .setDialogWidth(500);

            if (this.tableContext.getViewContext().readonly) {
                dialog.open(
                        new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + row),
                        builder);
            } else {
                dialog.open(
                        new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + row),
                        (Consumer<Map<Long, TableValue>>) _rowValues -> applyFormValues(
                                this.values,
                                _rowValues,
                                selectionIndex),
                        () -> this.tableContext.getValueChangeListener()
                                .tableChanged(extractTableValue(this.values)),
                        builder);
            }
        }

        @Override
        protected Map<Integer, Map<Long, TableValue>> createRowIndexMap(final List<TableValue> tableValues) {
            final Map<Integer, Map<Long, TableValue>> indexMapping = new HashMap<>();
            for (final TableValue tableValue : tableValues) {
                final ConfigurationAttribute attribute = this.tableContext
                        .getViewContext()
                        .getAttribute(tableValue.attributeId);
                final String groupId = ConfigurationAttribute.getDependencyValue(
                        ConfigurationAttribute.DEPENDENCY_GROUP_ID,
                        attribute);
                final int index = this.rows.indexOf(groupId);
                final Map<Long, TableValue> rowValues = indexMapping.computeIfAbsent(
                        index,
                        key -> new HashMap<>());
                rowValues.put(tableValue.attributeId, tableValue);
            }
            return indexMapping;
        }

        @Override
        void clearTable() {
            // nothing to clear for this table type
        }

        @Override
        protected boolean isChildValue(
                final TableContext tableContext,
                final ConfigurationAttribute attribute,
                final ConfigurationValue value) {

            final boolean childValue = super.isChildValue(tableContext, attribute, value);
            if (childValue) {
                final ConfigurationAttribute attr = tableContext.getAttribute(value.attributeId);
                return ConfigurationAttribute.hasDependency(
                        ConfigurationAttribute.DEPENDENCY_GROUP_ID,
                        attr);
            }

            return childValue;
        }

    }

}
