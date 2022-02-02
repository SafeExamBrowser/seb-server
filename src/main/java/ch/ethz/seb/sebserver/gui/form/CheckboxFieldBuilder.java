/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;

public class CheckboxFieldBuilder extends FieldBuilder<String> {

    protected CheckboxFieldBuilder(
            final String name,
            final LocTextKey label,
            final String value) {

        super(name, label, value);
    }

    public FieldBuilder<?> withRightLabel() {
        this.rightLabel = true;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        final boolean readonly = builder.readonly || this.readonly;
        Control titleLabel = null;
        Composite fieldGrid;
        Button checkbox;
        if (this.rightLabel) {
            fieldGrid = createFieldGrid(builder.formParent, this.spanInput, false);
            checkbox = builder.widgetFactory.buttonLocalized(
                    fieldGrid,
                    SWT.CHECK,
                    this.label,
                    this.tooltip,
                    this.label.name,
                    getARIALabel(builder));
        } else {
            titleLabel = createTitleLabel(builder.formParent, builder, this);
            fieldGrid = createFieldGrid(builder.formParent, this.spanInput);
            checkbox = builder.widgetFactory.buttonLocalized(
                    fieldGrid,
                    SWT.CHECK,
                    null,
                    null,
                    this.label.name,
                    getARIALabel(builder));
        }

        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        checkbox.setLayoutData(gridData);
        checkbox.setSelection(BooleanUtils.toBoolean(this.value));

        if (readonly) {
            checkbox.setEnabled(false);
        }

        if (builder.pageService.getFormTooltipMode() == PageService.FormTooltipMode.INPUT) {
            builder.pageService.getPolyglotPageService().injectI18nTooltip(
                    checkbox, this.tooltip);
        }

        builder.form.putField(this.name, titleLabel, checkbox);
    }

}
