/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.table;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;

public class EntityTable<ROW extends Entity> extends Composite {

    private static final Logger log = LoggerFactory.getLogger(EntityTable.class);

    private static final long serialVersionUID = -4931198225547108993L;

    public static final String TABLE_ROW_DATA = "TABLE_ROW_DATA";

    private transient final WidgetFactory widgetFactory;

    private transient final RestCall<Page<ROW>> restCall;
    private transient final List<ColumnDefinition<ROW>> columns;
    private transient final List<TableRowAction> actions;

    private transient final TableFilter<ROW> filter;
    private transient final Table table;
    private transient final TableNavigator navigator;

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
            final int pageSize,
            final boolean withFilter) {

        super(parent, type);
        this.widgetFactory = widgetFactory;
        this.restCall = restCall;
        this.columns = Utils.immutableListOf(columns);
        this.actions = Utils.immutableListOf(actions);

        super.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);

        gridData.heightHint = (pageSize + 1) * 40;
        super.setLayoutData(gridData);

        this.pageSize = pageSize;
        this.filter = (withFilter) ? new TableFilter<>(this) : null;

        this.table = widgetFactory.tableLocalized(this);
        this.table.setLayout(new GridLayout(columns.size(), true));
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.heightHint = (pageSize + 1) * 25;
        this.table.setLayoutData(gridData);
        this.table.addListener(SWT.Resize, this::adaptColumnWidth);

        //this.table.setLayoutData(new GridData(GridData.FILL_BOTH));
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
        // TODO remove all rows, set current page to 0, call rest to get entities and build rows and navigation again
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

    public Collection<String> getSelection() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(selection)
                .stream()
                .map(this::getRowDataId)
                .collect(Collectors.toList());
    }

    private void createTableColumns() {
        for (final ColumnDefinition<ROW> column : this.columns) {
            final TableColumn tableColumn = this.widgetFactory.tableColumnLocalized(
                    this.table,
                    column.displayName,
                    column.tooltip);

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
                .withFilterAttributes(this.filter)
                .call()
                .map(this::createTableRowsFromPage)
                .map(this.navigator::update)
                .onErrorDo(t -> {
                    // TODO error handling
                });

        this.layout();
    }

    private Page<ROW> createTableRowsFromPage(final Page<ROW> page) {
        for (final ROW row : page.content) {
            final TableItem item = new TableItem(this.table, SWT.NONE);
            item.setData(TABLE_ROW_DATA, row);
            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {
                final Object value = column.valueSupplier.apply(row);
                if (value instanceof Boolean) {
                    // TODO set an image or HTML with checkbox
                    item.setText(index, String.valueOf(value));
                } else {
                    item.setText(index, String.valueOf(value));
                }
                index++;
            }
            if (this.actions != null) {
                // TODO
            }
        }

        return page;
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
                tableColumn.setWidth(currentTableWidth / 100 * percentage);

                index++;
            }

        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
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
