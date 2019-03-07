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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.widget.MultiSelection;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.SingleSelection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

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
        if (builder.readonly || this.readonly) {
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
            builder.form.putField(this.name, lab, selection.<MultiSelection> getTypeInstance());
        } else {
            builder.form.putField(this.name, lab, selection.<SingleSelection> getTypeInstance());
        }
        if (this.selectionListener != null) {
            ((Control) selection).addListener(SWT.Selection, e -> {
                this.selectionListener.accept(builder.form);
            });
        }
        builder.setFieldVisible(this.visible, this.name);
    }

    /* Build the read-only representation of the selection field */
    private void buildReadOnly(final FormBuilder builder, final Label lab) {
        if (this.multi) {
            final Composite composite = new Composite(builder.formParent, SWT.NONE);
            final GridLayout gridLayout = new GridLayout(1, true);
            gridLayout.horizontalSpacing = 0;
            gridLayout.marginLeft = 0;
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            composite.setLayout(gridLayout);
            if (StringUtils.isBlank(this.value)) {
                createMuliSelectionReadonlyLabel(composite, Constants.EMPTY_NOTE);
            } else {
                final Collection<String> keys = Arrays.asList(StringUtils.split(this.value, Constants.LIST_SEPARATOR));
                this.itemsSupplier.get()
                        .stream()
                        .filter(tuple -> keys.contains(tuple._1))
                        .map(tuple -> tuple._1)
                        .forEach(v -> buildReadonlyLabel(composite, v, 0));
            }
        } else {
            builder.form.putField(
                    this.name,
                    lab,
                    buildReadonlyLabel(builder.formParent, this.value, this.spanInput));
            builder.setFieldVisible(this.visible, this.name);
        }
    }

    private Label buildReadonlyLabel(final Composite composite, final String valueKey, final int hspan) {
        final Label label = new Label(composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, hspan, 1);
        gridData.verticalIndent = 0;
        gridData.horizontalIndent = 0;
        label.setLayoutData(gridData);
        label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION_READONLY.key);

        final Supplier<String> valueSupplier = () -> this.itemsSupplier.get().stream()
                .filter(tuple -> valueKey.equals(tuple._1))
                .findFirst()
                .map(tuple -> tuple._2)
                .orElse(Constants.EMPTY_NOTE);
        final Consumer<Label> updateFunction = l -> l.setText(valueSupplier.get());

        label.setText(valueSupplier.get());
        label.setData(PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY, updateFunction);
        return label;
    }

    private void createMuliSelectionReadonlyLabel(final Composite composite, final String value) {
        final Label label = new Label(composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        label.setLayoutData(gridData);
        label.setData(RWT.CUSTOM_VARIANT, CustomVariant.SELECTION_READONLY.key);
        label.setText(value);
    }

}