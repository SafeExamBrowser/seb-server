/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

public class TableBuilder<ROW extends Entity> {

    private final PageService pageService;
    final RestCall<Page<ROW>> restCall;
    final List<ColumnDefinition<ROW>> columns = new ArrayList<>();
    LocTextKey emptyMessage;
    private Function<EntityTable<ROW>, PageAction> defaultActionFunction;
    private int pageSize = -1;
    private int type = SWT.NONE;
    private boolean hideNavigation = false;
    private Function<RestCall<Page<ROW>>.RestCallBuilder, RestCall<Page<ROW>>.RestCallBuilder> restCallAdapter;

    public TableBuilder(
            final PageService pageService,
            final RestCall<Page<ROW>> restCall) {

        this.pageService = pageService;
        this.restCall = restCall;
    }

    public TableBuilder<ROW> hideNavigation() {
        this.hideNavigation = true;
        return this;
    }

    public TableBuilder<ROW> withEmptyMessage(final LocTextKey emptyMessage) {
        this.emptyMessage = emptyMessage;
        return this;
    }

    public TableBuilder<ROW> withPaging(final int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public TableBuilder<ROW> withColumn(final ColumnDefinition<ROW> columnDefinition) {
        this.columns.add(columnDefinition);
        return this;
    }

    public TableBuilder<ROW> withColumnIf(
            final BooleanSupplier condition,
            final Supplier<ColumnDefinition<ROW>> columnDefSupplier) {

        if (condition != null && condition.getAsBoolean()) {
            this.columns.add(columnDefSupplier.get());
        }
        return this;
    }

    public TableBuilder<ROW> withRestCallAdapter(
            final Function<RestCall<Page<ROW>>.RestCallBuilder, RestCall<Page<ROW>>.RestCallBuilder> adapter) {
        this.restCallAdapter = adapter;
        return this;
    }

    public TableBuilder<ROW> withMultiselection() {
        this.type |= SWT.MULTI;
        return this;
    }

    public TableBuilder<ROW> withDefaultActionIf(
            final BooleanSupplier condition,
            final Supplier<PageAction> actionSupplier) {

        if (condition.getAsBoolean()) {
            return withDefaultAction(actionSupplier.get());
        }

        return this;
    }

    public TableBuilder<ROW> withDefaultAction(final PageAction action) {
        this.defaultActionFunction = table -> PageAction.copyOf(action);
        return this;
    }

    public TableBuilder<ROW> withDefaultActionIf(
            final BooleanSupplier condition,
            final Function<EntityTable<ROW>, PageAction> defaultActionFunction) {

        if (condition.getAsBoolean()) {
            return withDefaultAction(defaultActionFunction);
        }

        return this;
    }

    public TableBuilder<ROW> withDefaultAction(final Function<EntityTable<ROW>, PageAction> defaultActionFunction) {
        this.defaultActionFunction = defaultActionFunction;
        return this;
    }

    public EntityTable<ROW> compose(final PageContext pageContext) {
        return new EntityTable<>(
                this.type,
                pageContext,
                this.restCall,
                this.restCallAdapter,
                this.pageService,
                this.columns,
                this.pageSize,
                this.emptyMessage,
                this.defaultActionFunction,
                this.hideNavigation);
    }

}
