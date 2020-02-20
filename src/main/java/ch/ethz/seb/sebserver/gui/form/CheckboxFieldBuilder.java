/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CheckboxFieldBuilder extends FieldBuilder<String> {

    protected CheckboxFieldBuilder(
            final String name,
            final LocTextKey label,
            final String value) {

        super(name, label, value);
    }

    @Override
    void build(final FormBuilder builder) {
        final boolean readonly = builder.readonly || this.readonly;
        final Control titleLabel = createTitleLabel(builder.formParent, builder, this);
        final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);
        final Button checkbox = builder.widgetFactory.buttonLocalized(
                fieldGrid,
                SWT.CHECK,
                null, null);

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
