/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;

public class EntityTable<ROW extends Entity> extends Composite {

    private static final long serialVersionUID = -4931198225547108993L;

    private static final Logger log = LoggerFactory.getLogger(EntityTable.class);

    static final String COLUMN_DEFINITION = "COLUMN_DEFINITION";
    static final String TABLE_ROW_DATA = "TABLE_ROW_DATA";

    transient final WidgetFactory widgetFactory;
    transient final RestCall<Page<ROW>> restCall;

    transient final List<ColumnDefinition<ROW>> columns;
    transient final List<TableRowAction> actions;

    private final TableFilter<ROW> filter;
    private final Table table;
    private final TableNavigator navigator;

    private int pageNumber = 1;
    private int pageSize;
    private String sortColumn = null;
    private SortOrder sortOrder = SortOrder.ASCENDING;

    private boolean columnsWithSameWidth = true;

    EntityTable(
            final int type,
            final Composite parent,
            final RestCall<Page<ROW>> restCall,
            final WidgetFactory widgetFactory,
            final List<ColumnDefinition<ROW>> columns,
            final List<TableRowAction> actions,
            final int pageSize) {

        super(parent, type);
        this.widgetFactory = widgetFactory;
        this.restCall = restCall;
        this.columns = Utils.immutableListOf(columns);
        this.actions = Utils.immutableListOf(actions);

        super.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);

        gridData.heightHint = (pageSize + 1) * 40;
        super.setLayoutData(gridData);

        this.pageSize = pageSize;
        this.filter = columns
                .stream()
                .map(column -> column.filterAttribute)
                .filter(Objects::nonNull)
                .findFirst()
                .isPresent() ? new TableFilter<>(this) : null;

        this.table = widgetFactory.tableLocalized(this);
        final GridLayout gridLayout = new GridLayout(columns.size(), true);
        this.table.setLayout(gridLayout);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.heightHint = (pageSize + 1) * 27;
        this.table.setLayoutData(gridData);
        this.table.addListener(SWT.Resize, this::adaptColumnWidth);
        @SuppressWarnings("unchecked")
        final Consumer<Table> locFunction = (Consumer<Table>) this.table.getData(POLYGLOT_WIDGET_FUNCTION_KEY);
        final Consumer<Table> newLocFunction = t -> {
            updateValues();
            locFunction.accept(t);
        };
        this.table.setData(POLYGLOT_WIDGET_FUNCTION_KEY, newLocFunction);

        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);

        this.navigator = new TableNavigator(this);

        createTableColumns();
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void selectPage(final int pageSelection) {
        // verify input
        this.pageNumber = pageSelection;
        if (this.pageNumber < 1) {
            this.pageNumber = 1;
        }

        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void applyFilter() {
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void applySort(final String columnName) {
        this.sortColumn = columnName;
        this.sortOrder = SortOrder.ASCENDING;

        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void changeSortOrder() {
        this.sortOrder = (this.sortOrder == SortOrder.ASCENDING)
                ? SortOrder.DESCENDING
                : SortOrder.ASCENDING;

        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public String getSingleSelection() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null || selection.length == 0) {
            return null;
        }

        return getRowDataId(selection[0]);
    }

    public Set<String> getSelection() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null) {
            return Collections.emptySet();
        }

        return Arrays.asList(selection)
                .stream()
                .map(this::getRowDataId)
                .collect(Collectors.toSet());
    }

    private void createTableColumns() {
        for (final ColumnDefinition<ROW> column : this.columns) {
            final TableColumn tableColumn = this.widgetFactory.tableColumnLocalized(
                    this.table,
                    column.displayName,
                    column.tooltip);

            tableColumn.addListener(SWT.Resize, this::adaptColumnWidthChange);
            tableColumn.setData(COLUMN_DEFINITION, column);

            if (column.sortable) {
                tableColumn.addListener(SWT.Selection, event -> {
                    if (!column.columnName.equals(this.sortColumn)) {
                        applySort(column.columnName);
                        this.table.setSortColumn(tableColumn);
                        this.table.setSortDirection(SWT.UP);
                    } else {
                        changeSortOrder();
                        this.table.setSortDirection(
                                (this.sortOrder == SortOrder.ASCENDING) ? SWT.UP : SWT.DOWN);
                    }
                });
            }

            if (column.widthPercent > 0) {
                this.columnsWithSameWidth = false;
            }
        }
    }

    private void updateTableRows(
            final int pageNumber,
            final int pageSize,
            final String sortColumn,
            final SortOrder sortOrder) {

        // first remove all rows if there are some
        this.table.removeAll();

        // get page data and create rows
        this.restCall.newBuilder()
                .withPaging(pageNumber, pageSize)
                .withSorting(sortColumn, sortOrder)
                .withQueryParams((this.filter != null) ? this.filter.getFilterParameter() : null)
                .call()
                .map(this::createTableRowsFromPage)
                .map(this.navigator::update)
                .onErrorDo(t -> {
                    // TODO error handling
                });

        this.layout(true, true);
    }

    private Page<ROW> createTableRowsFromPage(final Page<ROW> page) {
        for (final ROW row : page.content) {
            final TableItem item = new TableItem(this.table, SWT.NONE);
            item.setData(TABLE_ROW_DATA, row);
            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {
                setValueToCell(item, index, column.valueSupplier.apply(row));
                index++;
            }
            if (this.actions != null) {
                // TODO??
            }
        }

        return page;
    }

    private void updateValues() {
        final TableItem[] items = this.table.getItems();
        final TableColumn[] columns = this.table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            final ColumnDefinition<ROW> columnDefinition = this.columns.get(i);
            if (columnDefinition.localized) {
                for (int j = 0; j < items.length; j++) {
                    @SuppressWarnings("unchecked")
                    final ROW rowData = (ROW) items[j].getData(TABLE_ROW_DATA);
                    setValueToCell(items[j], i, columnDefinition.valueSupplier.apply(rowData));
                }
            }
        }
    }

    private void setValueToCell(final TableItem item, final int index, final Object value) {
        if (value instanceof Boolean) {
            addBooleanCell(item, index, value);
        } else {
            if (value != null) {
                item.setText(index, String.valueOf(value));
            } else {
                item.setText(index, Constants.EMPTY_NOTE);
            }
        }
    }

    private void addBooleanCell(final TableItem item, final int index, final Object value) {
        if ((Boolean) value) {
            item.setImage(index, ImageIcon.ACTIVE.getImage(item.getDisplay()));
        } else {
            item.setImage(index, ImageIcon.INACTIVE.getImage(item.getDisplay()));
        }
    }

    private void adaptColumnWidth(final Event event) {
        try {
            final int currentTableWidth = this.table.getParent().getClientArea().width;
            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {

                final int percentage = (this.columnsWithSameWidth)
                        ? 100 / this.columns.size()
                        : column.widthPercent;

                final TableColumn tableColumn = this.table.getColumn(index);
                final int newWidth = currentTableWidth / 100 * percentage;
                tableColumn.setWidth(newWidth);
                if (this.filter != null) {
                    this.filter.adaptColumnWidth(this.table.indexOf(tableColumn), newWidth);
                }

                index++;
            }

            // NOTE this.layout() triggers the navigation to disappear unexpectedly!?
            this.table.layout(true, true);

        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    private void adaptColumnWidthChange(final Event event) {
        final Widget widget = event.widget;
        if (widget instanceof TableColumn) {
            final TableColumn tableColumn = ((TableColumn) widget);
            if (this.filter != null &&
                    this.filter.adaptColumnWidth(
                            this.table.indexOf(tableColumn),
                            tableColumn.getWidth())) {

                this.layout(true, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ROW getRowData(final TableItem item) {
        return (ROW) item.getData(TABLE_ROW_DATA);
    }

    private String getRowDataId(final TableItem item) {
        return getRowData(item).getModelId();
    }

}
