/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

@Lazy
@Component
@GuiProfile
public class TableFieldBuilder extends AbstractTableFieldBuilder {

    private static final String TOOLTIP_SUFFIX = ".tooltip";
    private static final String ADD_TOOLTIP_SUFFIX = ".add" + TOOLTIP_SUFFIX;
    private static final String REMOVE_TOOLTIP_SUFFIX = ".remove" + TOOLTIP_SUFFIX;

    protected TableFieldBuilder(
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

        return AttributeType.TABLE == attribute.type;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final TableContext tableContext = createTableContext(attribute, viewContext);
        final Table table = createTable(parent, tableContext);

        for (final ConfigurationAttribute columnAttribute : tableContext.getColumnAttributes()) {
            final TableColumn column = this.widgetFactory.tableColumnLocalized(
                    table,
                    new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                            columnAttribute.name),
                    new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                            columnAttribute.name +
                            TOOLTIP_SUFFIX));
            column.setWidth(100);
            column.setResizable(false);
            column.setMoveable(false);
        }

        final TableInputField tableField = new TableInputField(
                tableContext,
                table);
        if (!viewContext.readonly) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setImage(ImageIcon.ADD_BOX_WHITE.getImage(parent.getDisplay()));
            column.setToolTipText(Utils.formatLineBreaks(viewContext.i18nSupport.getText(
                    ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                            attribute.name +
                            ADD_TOOLTIP_SUFFIX,
                    "Add new")));
            column.setWidth(20);
            column.setResizable(false);
            column.setMoveable(false);

            column.addListener(SWT.Selection, event -> tableField.addRow());

            column = new TableColumn(table, SWT.NONE);
            column.setImage(ImageIcon.REMOVE_BOX_WHITE.getImage(parent.getDisplay()));
            column.setToolTipText(Utils.formatLineBreaks(viewContext.i18nSupport.getText(
                    ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                            attribute.name +
                            REMOVE_TOOLTIP_SUFFIX,
                    "Remove Selected")));
            column.setWidth(20);
            column.setResizable(false);
            column.setMoveable(false);

            column.addListener(SWT.Selection, event -> {
                final int selectionIndex = table.getSelectionIndex();
                if (selectionIndex >= 0) {
                    tableField.deleteRow(selectionIndex);
                }
            });
        }

        setSelectionListener(table, tableField);
        return tableField;
    }

    static final class TableInputField extends AbstractTableInputField {

        private final List<Map<Long, TableValue>> values;

        TableInputField(
                final TableContext tableContext,
                final Table control) {

            super(tableContext.attribute, tableContext.orientation, control, null, tableContext);
            this.values = new ArrayList<>();
        }

        @Override
        void initValue(final List<TableValue> tableValues) {
            valuesFromIndexMap(this.values, createRowIndexMap(tableValues));
            for (int i = 0; i < this.values.size(); i++) {
                addTableRow(i, this.values.get(i));
            }
        }

        private void deleteRow(final int selectionIndex) {
            this.control.remove(selectionIndex);
            this.values.remove(selectionIndex);
            // send new values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue(this.values));
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
            addTableRow(this.values.size() - 1, rowValues);
            this.control.layout();
            // send new values to web-service
            this.tableContext.getValueChangeListener()
                    .tableChanged(extractTableValue(this.values));
        }

        protected void addTableRow(final int index, final Map<Long, TableValue> rowValues) {
            new TableItem(this.control, SWT.NONE);
            applyTableRowValues(index);
        }

        @Override
        protected void applyTableRowValues(final int index) {
            final TableItem item = this.control.getItem(index);
            final Map<Long, TableValue> rowValues = this.values.get(index);

            int cellIndex = 0;
            for (final ConfigurationAttribute attr : this.tableContext.getColumnAttributes()) {
                if (rowValues.containsKey(attr.id)) {
                    final TableValue tableValue = rowValues.get(attr.id);
                    setValueToCell(this.tableContext, item, cellIndex, attr, tableValue);
                }
                cellIndex++;
            }
        }

        @Override
        protected void openForm(final int selectionIndex) {
            final Map<Long, TableValue> rowValues = this.values.get(selectionIndex);
            final TableRowFormBuilder builder = new TableRowFormBuilder(
                    this.tableContext,
                    rowValues,
                    selectionIndex);

            new ModalInputDialog<Map<Long, TableValue>>(
                    this.control.getShell(),
                    this.tableContext.getWidgetFactory())
                            .setDialogWidth(600)
                            .setDialogHeight(550)
                            .open(
                                    ExamConfigurationService.getTablePopupTitleKey(
                                            this.attribute,
                                            this.tableContext.getViewContext().i18nSupport),
                                    (Consumer<Map<Long, TableValue>>) _rowValues -> applyFormValues(
                                            this.values,
                                            _rowValues,
                                            selectionIndex),
                                    () -> this.tableContext.getValueChangeListener()
                                            .tableChanged(extractTableValue(this.values)),
                                    builder);
        }

    }

}
