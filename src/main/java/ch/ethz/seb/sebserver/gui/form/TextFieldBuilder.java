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
import org.eclipse.swt.widgets.Text;

public final class TextFieldBuilder extends FieldBuilder<String> {

    boolean isPassword = false;
    boolean isNumber = false;
    boolean isArea = false;

    TextFieldBuilder(final String name, final String label, final String value) {
        super(name, label, value);
    }

    public TextFieldBuilder asPasswordField() {
        this.isPassword = true;
        return this;
    }

    public TextFieldBuilder asNumber() {
        this.isNumber = true;
        return this;
    }

    public TextFieldBuilder asArea() {
        this.isArea = true;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        if (this.isPassword && builder.readonly) {
            return;
        }

        final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
        if (builder.readonly || this.readonly) {
            builder.form.putField(this.name, lab,
                    builder.valueLabel(builder.formParent, this.value, this.spanInput));
            builder.setFieldVisible(this.visible, this.name);
        } else {
            final Text textInput = (this.isNumber)
                    ? builder.widgetFactory.numberInput(builder.formParent, null)
                    : (this.isArea)
                            ? builder.widgetFactory.textAreaInput(builder.formParent)
                            : builder.widgetFactory.textInput(builder.formParent, this.isPassword);

            final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
            textInput.setLayoutData(gridData);
            if (this.value != null) {
                textInput.setText(this.value);
            }
            builder.form.putField(this.name, lab, textInput);
            builder.setFieldVisible(this.visible, this.name);
        }
    }
}