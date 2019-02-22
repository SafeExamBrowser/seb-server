/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.widget.MultiSelection;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.SingleSelection;

public final class SelectionFieldBuilder extends FieldBuilder {

    final Supplier<List<Tuple<String>>> itemsSupplier;
    Consumer<Form> selectionListener = null;
    boolean multi = false;

    SelectionFieldBuilder(
            final String name,
            final String label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        super(name, label, value);
        this.itemsSupplier = itemsSupplier;
    }

    public SelectionFieldBuilder withSelectionListener(final Consumer<Form> selectionListener) {
        this.selectionListener = selectionListener;
        return this;
    }

    public SelectionFieldBuilder asMultiSelection() {
        this.multi = true;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
        if (builder.readonly) {
            buildReadOnly(builder, lab);
        } else {
            buildInput(builder, lab);
        }
    }

    private void buildInput(final FormBuilder builder, final Label lab) {
        final Selection selection = (this.multi)
                ? builder.widgetFactory.multiSelectionLocalized(builder.formParent, this.itemsSupplier)
                : builder.widgetFactory.singleSelectionLocalized(builder.formParent, this.itemsSupplier);

        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
        ((Control) selection).setLayoutData(gridData);
        selection.select(this.value);
        if (this.multi) {
            builder.form.putField(this.name, lab, (MultiSelection) selection);
        } else {
            builder.form.putField(this.name, lab, (SingleSelection) selection);
        }
        if (this.selectionListener != null) {
            ((Control) selection).addListener(SWT.Selection, e -> {
                this.selectionListener.accept(builder.form);
            });
        }
    }

    /* Build the read-only representation of the selection field */
    private void buildReadOnly(final FormBuilder builder, final Label lab) {
        builder.form.putField(
                this.name, lab,
                builder.valueLabel(
                        builder.formParent,
                        getSelectionValue(this.value, this.multi),
                        this.spanInput));
    }

    /*
     * For Single selection just the selected value, for multi selection a comma
     * separated list of values within a String value.
     *
     * @param key the key or keys, in case of multi selection a comma separated String of keys
     *
     * @param multi indicates multi seleciton
     *
     * @return selected value or comma separated String list of selected values
     */
    private String getSelectionValue(final String key, final boolean multi) {
        if (multi) {
            final Collection<String> keys = Arrays.asList(StringUtils.split(key, Constants.LIST_SEPARATOR));
            return StringUtils.join(this.itemsSupplier.get().stream()
                    .filter(tuple -> keys.contains(tuple._1))
                    .map(tuple -> tuple._2)
                    .collect(Collectors.toList()),
                    Constants.LIST_SEPARATOR);
        } else {
            return this.itemsSupplier.get().stream()
                    .filter(tuple -> key.equals(tuple._1))
                    .findFirst()
                    .map(tuple -> tuple._2)
                    .orElse(null);
        }
    }
}