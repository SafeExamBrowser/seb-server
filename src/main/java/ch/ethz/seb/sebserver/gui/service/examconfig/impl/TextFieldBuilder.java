/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

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
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;

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

        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, orientation);

        final Text text;
        if (attribute.type == AttributeType.INTEGER ||
                attribute.type == AttributeType.DECIMAL) {

            text = new Text(innerGrid, SWT.RIGHT | SWT.BORDER);
        } else {
            text = new Text(innerGrid, SWT.LEFT | SWT.BORDER);
        }
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final TextInputField textInputField = new TextInputField(
                attribute,
                orientation,
                text,
                InputFieldBuilder.createErrorLabel(innerGrid));

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
            this.control.setText(value);
        }

        @Override
        public String getValue() {
            return this.control.getText();
        }
    }

}
