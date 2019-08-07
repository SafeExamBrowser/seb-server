/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

public final class ColumnDefinition<ROW extends Entity> {

    final String columnName;
    final LocTextKey displayName;
    final Function<ROW, ?> valueSupplier;

    private LocTextKey tooltip;
    private int widthProportion;
    private boolean sortable;
    private TableFilterAttribute filterAttribute;
    private boolean localized;
    private boolean withCellTooltip = false;

    public ColumnDefinition(final String columnName, final LocTextKey displayName) {
        this(columnName, displayName, null);
    }

    public ColumnDefinition(
            final String columnName,
            final LocTextKey displayName,
            final Function<ROW, ?> valueSupplier) {

        this(columnName, displayName, null, -1, valueSupplier, null, false, false);
    }

    private ColumnDefinition(
            final String columnName,
            final LocTextKey displayName,
            final LocTextKey tooltip,
            final int widthProportion,
            final Function<ROW, ?> valueSupplier,
            final TableFilterAttribute filterAttribute,
            final boolean sortable,
            final boolean localized) {

        this.columnName = columnName;
        this.displayName = displayName;
        this.tooltip = tooltip;
        this.widthProportion = widthProportion;
        this.valueSupplier = valueSupplier;
        this.filterAttribute = filterAttribute;
        this.sortable = sortable;
        this.localized = localized;
    }

    public ColumnDefinition<ROW> withCellTooltip() {
        this.withCellTooltip = true;
        return this;
    }

    public ColumnDefinition<ROW> withFilter(final TableFilterAttribute filterAttribute) {
        this.filterAttribute = filterAttribute;
        return this;
    }

    public ColumnDefinition<ROW> withTooltip(final LocTextKey tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ColumnDefinition<ROW> localized() {
        this.localized = true;
        return this;
    }

    public ColumnDefinition<ROW> sortable() {
        this.sortable = true;
        return this;
    }

    public ColumnDefinition<ROW> widthProportion(final int widthProportion) {
        this.widthProportion = widthProportion;
        return this;
    }

    public LocTextKey getTooltip() {
        return this.tooltip;
    }

    public int getWidthProportion() {
        return this.widthProportion;
    }

    public boolean isSortable() {
        return this.sortable;
    }

    public TableFilterAttribute getFilterAttribute() {
        return this.filterAttribute;
    }

    public boolean isLocalized() {
        return this.localized;
    }

    public boolean hasTooltip() {
        return this.withCellTooltip;
    }

    public static final class TableFilterAttribute {

        public final CriteriaType type;
        public final String columnName;
        public final String initValue;
        public final Supplier<List<Tuple<String>>> resourceSupplier;
        public final Function<EntityTable<?>, List<Tuple<String>>> resourceFunction;

        public TableFilterAttribute(final CriteriaType type, final String columnName) {
            this(type, columnName, "", (Supplier<List<Tuple<String>>>) null);
        }

        public TableFilterAttribute(
                final CriteriaType type,
                final String columnName,
                final Supplier<List<Tuple<String>>> resourceSupplier) {

            this(type, columnName, "", resourceSupplier);
        }

        public TableFilterAttribute(
                final CriteriaType type,
                final String columnName,
                final String initValue) {

            this(type, columnName, initValue, (Supplier<List<Tuple<String>>>) null);
        }

        public TableFilterAttribute(
                final CriteriaType type,
                final String columnName,
                final String initValue,
                final Supplier<List<Tuple<String>>> resourceSupplier) {

            this.type = type;
            this.columnName = columnName;
            this.initValue = initValue;
            this.resourceSupplier = resourceSupplier;
            this.resourceFunction = null;
        }

        public TableFilterAttribute(
                final CriteriaType type,
                final String columnName,
                final String initValue,
                final Function<EntityTable<?>, List<Tuple<String>>> resourceFunction) {

            this.type = type;
            this.columnName = columnName;
            this.initValue = initValue;
            this.resourceSupplier = null;
            this.resourceFunction = resourceFunction;
        }

    }

}
