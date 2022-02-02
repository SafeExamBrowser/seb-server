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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
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
    boolean isColorBox = false;
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

    public TextFieldBuilder asHTML(final int minHeight) {
        this.isHTML = true;
        this.areaMinHeight = minHeight;
        return this;
    }

    public FieldBuilder<?> asHTML(final boolean html) {
        this.isHTML = html;
        return this;
    }

    public TextFieldBuilder asColorBox() {
        this.isColorBox = true;
        return this;
    }

    @Override
    void build(final FormBuilder builder) {
        final boolean readonly = builder.readonly || this.readonly;
        final Control titleLabel = createTitleLabel(builder.formParent, builder, this);
        final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);

        if (readonly && this.isHTML) {
            final Browser browser = new Browser(fieldGrid, SWT.NONE);
            final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
            gridData.minimumHeight = this.areaMinHeight;
            browser.setBackground(new Color(builder.formParent.getDisplay(), 250, 250, 250));
            browser.setLayoutData(gridData);
            if (StringUtils.isNoneBlank(this.value)) {
                browser.setText(createHTMLText(this.value));
            } else if (readonly) {
                browser.setText(Constants.EMPTY_NOTE);
            }
            builder.form.putReadonlyField(this.name, titleLabel, browser);

            if (builder.pageService.getFormTooltipMode() == PageService.FormTooltipMode.INPUT) {
                builder.pageService.getPolyglotPageService().injectI18nTooltip(
                        browser, this.tooltip);
            }
            return;
        }

        final String testKey = (this.label != null) ? this.label.name : this.name;
        final LocTextKey label = getARIALabel(builder);
        final Text textInput = (this.isNumber)
                ? builder.widgetFactory.numberInput(fieldGrid, this.numberCheck, readonly, testKey, label)
                : (this.isArea)
                        ? builder.widgetFactory.textAreaInput(fieldGrid, readonly, testKey, label)
                        : builder.widgetFactory.textInput(fieldGrid, this.isPassword, readonly, testKey, label);

        if (builder.pageService.getFormTooltipMode() == PageService.FormTooltipMode.INPUT) {
            builder.pageService.getPolyglotPageService().injectI18nTooltip(
                    textInput, this.tooltip);
        }

        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        if (this.isArea) {
            gridData.minimumHeight = this.areaMinHeight;
        } else if (this.isColorBox) {
            gridData.minimumHeight = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
            textInput.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.COLOR_BOX.key);
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

    private String createHTMLText(final String text) {
        return HTML_TEXT_BLOCK_START
                + text
                        .replace("<a", "<span")
                        .replace("</a", "</span")
                        .replace("<A", "<span")
                        .replace("</A", "</span")
                + HTML_TEXT_BLOCK_END;
    }

}