/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;

@Lazy
@Component
@GuiProfile
public class TextFieldBuilder implements InputFieldBuilder {

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.TEXT_FIELD ||
                attribute.type == AttributeType.TEXT_AREA ||
                attribute.type == AttributeType.INTEGER ||
                attribute.type == AttributeType.DECIMAL;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final I18nSupport i18nSupport = viewContext.getI18nSupport();
        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final Text text;
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        switch (attribute.type) {
            case INTEGER:
            case DECIMAL: {
                text = new Text(innerGrid, SWT.RIGHT | SWT.BORDER);
                break;
            }
            case TEXT_AREA: {
                text = new Text(innerGrid, SWT.LEFT | SWT.BORDER | SWT.MULTI);
                gridData.heightHint = 50;
                break;
            }
            default: {
                text = new Text(innerGrid, SWT.LEFT | SWT.BORDER);
            }
        }
        text.setLayoutData(gridData);

        final LocTextKey toolTipKey = ExamConfigurationService.getToolTipKey(
                attribute,
                i18nSupport);
        if (toolTipKey != null) {
            final Consumer<Text> updateFunction = t -> t.setToolTipText(i18nSupport.getText(toolTipKey));
            text.setData(
                    PolyglotPageService.POLYGLOT_ITEM_TOOLTIP_DATA_KEY,
                    updateFunction);
            updateFunction.accept(text);
        }

        final TextInputField textInputField = new TextInputField(
                attribute,
                orientation,
                text,
                Form.createErrorLabel(innerGrid));

        final Listener valueChangeEventListener = event -> {
            textInputField.clearError();
            viewContext.getValueChangeListener().valueChanged(
                    viewContext,
                    attribute,
                    textInputField.getValue(),
                    textInputField.listIndex);
        };

        text.addListener(SWT.FocusOut, valueChangeEventListener);
        text.addListener(SWT.Traverse, valueChangeEventListener);
        return textInputField;
    }

    static final class TextInputField extends AbstractInputField<Text> {

        TextInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Text control,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
        }

        @Override
        protected void setValueToControl(final String value) {
            if (value == null) {
                this.control.setText(StringUtils.EMPTY);
                return;
            }

            this.control.setText(value);
        }

        @Override
        public String getValue() {
            return this.control.getText();
        }
    }

}
