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
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.ImageUpload;

public final class ImageUploadFieldBuilder extends FieldBuilder<String> {

    ImageUploadFieldBuilder(final String name, final LocTextKey label, final String value) {
        super(name, label, value);
    }

    @Override
    void build(final FormBuilder builder) {
        final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
        final ImageUpload imageUpload = builder.widgetFactory.imageUploadLocalized(
                builder.formParent,
                new LocTextKey("sebserver.overall.upload"),
                builder.readonly || this.readonly);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
        imageUpload.setLayoutData(gridData);
        imageUpload.setImageBase64(this.value);
        builder.form.putField(this.name, lab, imageUpload);
        builder.setFieldVisible(this.visible, this.name);
    }

}