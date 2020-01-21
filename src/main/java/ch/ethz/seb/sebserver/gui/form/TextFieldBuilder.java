/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public final class TextFieldBuilder extends FieldBuilder<String> {

    private static final String HTML_TEXT_BLOCK_START =
            "<span style=\"font: 12px Arial, Helvetica, sans-serif;color: #4a4a4a;\">";
    private static final String HTML_TEXT_BLOCK_END = "</span>";

    boolean isPassword = false;
    boolean isNumber = false;
    Consumer<String> numberCheck = null;
    boolean isArea = false;
    int areaMinHeight = WidgetFactory.TEXT_AREA_INPUT_MIN_HEIGHT;
    boolean isColorbox = false;
    boolean isHTML = false;

    TextFieldBuilder(final String name, final LocTextKey label, final String value) {
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

    public TextFieldBuilder asNumber(final Consumer<String> numberCheck) {
        this.isNumber = true;
        this.numberCheck = numberCheck;
        return this;
    }

    public TextFieldBuilder asArea(final int minHeight) {
        this.areaMinHeight = minHeight;
        return asArea();
    }

    public TextFieldBuilder asArea() {
        this.isArea = true;
        this.titleValign = SWT.CENTER;
        return this;
    }

    public TextFieldBuilder asHTML() {
        this.isHTML = true;
        return this;
    }

    public TextFieldBuilder asColorbox() {
        this.isColorbox = true;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        final boolean readonly = builder.readonly || this.readonly;
        final Label titleLabel = createTitleLabel(builder.formParent, builder, this);
        final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);

        if (readonly && this.isHTML) {
            final Browser browser = new Browser(fieldGrid, SWT.NONE);
            final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
            gridData.minimumHeight = this.areaMinHeight;
            browser.setLayoutData(gridData);
            if (StringUtils.isNoneBlank(this.value)) {
                browser.setText(HTML_TEXT_BLOCK_START + this.value + HTML_TEXT_BLOCK_END);
            } else if (readonly) {
                browser.setText(Constants.EMPTY_NOTE);
            }
            builder.form.putReadonlyField(this.name, titleLabel, browser);
            return;
        }

        final Text textInput = (this.isNumber)
                ? builder.widgetFactory.numberInput(fieldGrid, this.numberCheck, readonly)
                : (this.isArea)
                        ? builder.widgetFactory.textAreaInput(fieldGrid, readonly)
                        : builder.widgetFactory.textInput(fieldGrid, this.isPassword, readonly);

        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        if (this.isArea) {
            gridData.minimumHeight = this.areaMinHeight;
        } else if (this.isColorbox) {
            gridData.minimumHeight = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
            textInput.setData(RWT.CUSTOM_VARIANT, "colorbox");
        }
        textInput.setLayoutData(gridData);
        if (StringUtils.isNoneBlank(this.value)) {
            textInput.setText(this.value);
        } else if (readonly) {
            textInput.setText(Constants.EMPTY_NOTE);
        }

        if (readonly) {
            textInput.setEditable(false);
            builder.form.putReadonlyField(this.name, titleLabel, textInput);
        } else {
            final Label errorLabel = createErrorLabel(fieldGrid);
            builder.form.putField(this.name, titleLabel, textInput, errorLabel);
            builder.setFieldVisible(this.visible, this.name);
        }

    }

}