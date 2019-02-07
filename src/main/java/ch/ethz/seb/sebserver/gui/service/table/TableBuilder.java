/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.table;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

/** <code>
 *  new TableBuilder<T>(RestCall)
 *      .withPaging(pageSize)
 *      .withColumn(new ColumnDefinition(
 *          columnName:String,
 *          displayName:LocTextKey,
 *          tooltip:LocTextKey,
 *          width:int,
 *          valueSupplier:Function<ROW, String>,
 *          sortable:boolean,
 *          columnFilter:TableColumnFilter))
 *      .withAction(action:TableRowAction)
 *      .withSelectableRows(boolean)
 *      .compose(parent:Composit, group:Composite);
 * </code> */
public class TableBuilder<ROW extends Entity> {

    private final WidgetFactory widgetFactory;
    final RestCall<Page<ROW>> restCall;
    final List<ColumnDefinition<ROW>> columns = new ArrayList<>();
    final List<TableRowAction> actions = new ArrayList<>();

    private int pageSize = -1;
    private int type = SWT.NONE;

    public TableBuilder(
            final WidgetFactory widgetFactory,
            final RestCall<Page<ROW>> restCall) {

        this.widgetFactory = widgetFactory;
        this.restCall = restCall;
    }

    public TableBuilder<ROW> withPaging(final int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public TableBuilder<ROW> withColumn(final ColumnDefinition<ROW> columnDef) {
        this.columns.add(columnDef);
        return this;
    }

    public TableBuilder<ROW> withAction(final TableRowAction action) {
        this.actions.add(action);
        return this;
    }

    public TableBuilder<ROW> withMultiselection() {
        this.type |= SWT.MULTI;
        return this;
    }

    public EntityTable<ROW> compose(final Composite parent) {
        final boolean withFilter = this.columns
                .stream()
                .filter(c -> c.filter != null)
                .findFirst()
                .isPresent();

        return new EntityTable<>(
                this.type,
                parent,
                this.restCall,
                this.widgetFactory,
                this.columns,
                this.actions,
                this.pageSize,
                withFilter);
    }

}
