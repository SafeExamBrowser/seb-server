/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.table;

import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public final class ColumnDefinition<ROW extends Entity> {

    final String columnName;
    final LocTextKey displayName;
    final LocTextKey tooltip;
    final int widthPercent;
    final Function<ROW, Object> valueSupplier;
    final ColumnFilterDefinition filter;
    final boolean sortable;

    public ColumnDefinition(
            final String columnName,
            final LocTextKey displayName,
            final LocTextKey tooltip,
            final int widthPercent) {

        this(columnName, displayName, tooltip, widthPercent, null, null, false);
    }

    public ColumnDefinition(
            final String columnName,
            final LocTextKey displayName,
            final int widthPercent) {

        this(columnName, displayName, null, widthPercent, null, null, false);
    }

    public ColumnDefinition(
            final String columnName,
            final LocTextKey displayName,
            final LocTextKey tooltip,
            final int widthPercent,
            final Function<ROW, Object> valueSupplier,
            final ColumnFilterDefinition filter,
            final boolean sortable) {

        this.columnName = columnName;
        this.displayName = displayName;
        this.tooltip = tooltip;
        this.widthPercent = widthPercent;
        this.valueSupplier = valueSupplier;
        this.filter = filter;
        this.sortable = sortable;
    }
}
