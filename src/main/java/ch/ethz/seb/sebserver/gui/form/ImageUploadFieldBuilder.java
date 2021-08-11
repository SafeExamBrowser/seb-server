/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;

public final class ImageUploadFieldBuilder extends FieldBuilder<String> {

    private int maxWidth = 100;
    private int maxHeight = 100;

    ImageUploadFieldBuilder(final String name, final LocTextKey label, final String value) {
        super(name, label, value);
    }

    public ImageUploadFieldBuilder withMaxWidth(final int width) {
        this.maxWidth = width;
        return this;
    }

    public ImageUploadFieldBuilder withMaxHeight(final int height) {
        this.maxHeight = height;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        final Control titleLabel = createTitleLabel(builder.formParent, builder, this);
        final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);
        final ImageUploadSelection imageUpload = builder.widgetFactory.imageUploadLocalized(
                fieldGrid,
                new LocTextKey("sebserver.overall.upload"),
                builder.readonly || this.readonly,
                this.maxWidth,
                this.maxHeight,
                getARIALabel(builder));
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        imageUpload.setLayoutData(gridData);
        imageUpload.setImageBase64(this.value);

        final Label errorLabel = createErrorLabel(fieldGrid);
        builder.form.putField(this.name, titleLabel, imageUpload, errorLabel);
        builder.setFieldVisible(this.visible, this.name);

        if (builder.pageService.getFormTooltipMode() == PageService.FormTooltipMode.INPUT) {
            builder.pageService.getPolyglotPageService().injectI18nTooltip(
                    imageUpload, this.tooltip);
        }
    }

}