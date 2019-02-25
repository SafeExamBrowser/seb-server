/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.function.BooleanSupplier;

public abstract class FieldBuilder {
    int spanLabel = -1;
    int spanInput = -1;
    int spanEmptyCell = -1;
    boolean autoEmptyCellSeparation = false;
    String group = null;
    BooleanSupplier condition = null;
    boolean readonly = false;
    boolean visible = true;

    final String name;
    final String label;
    final String value;

    protected FieldBuilder(final String name, final String label, final String value) {
        this.name = name;
        this.label = label;
        this.value = value;
    }

    public FieldBuilder withLabelSpan(final int span) {
        this.spanLabel = span;
        return this;
    }

    public FieldBuilder withInputSpan(final int span) {
        this.spanInput = span;
        return this;
    }

    public FieldBuilder withEmptyCellSpan(final int span) {
        this.spanEmptyCell = span;
        return this;
    }

    public FieldBuilder withEmptyCellSeparation(final boolean separation) {
        this.autoEmptyCellSeparation = separation;
        return this;
    }

    public FieldBuilder withGroup(final String group) {
        this.group = group;
        return this;
    }

    public FieldBuilder withCondition(final BooleanSupplier condition) {
        this.condition = condition;
        return this;
    }

    public FieldBuilder readonly(final boolean readonly) {
        this.readonly = readonly;
        return this;
    }

    public FieldBuilder visibleIf(final boolean visible) {
        this.visible = visible;
        return this;
    }

    public FieldBuilder readonlyIf(final BooleanSupplier readonly) {
        this.readonly = readonly != null && readonly.getAsBoolean();
        return this;
    }

    abstract void build(FormBuilder builder);

}