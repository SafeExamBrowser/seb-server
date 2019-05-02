/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;

public class PassworFieldBuilder implements InputFieldBuilder {

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return AttributeType.PASSWORD_FIELD == attribute.type;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final Orientation orientation = viewContext.attributeMapping
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, orientation);

        final Text passwordInput = new Text(innerGrid, SWT.LEFT | SWT.BORDER | SWT.PASSWORD);
        passwordInput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        final Text confirmInput = new Text(innerGrid, SWT.LEFT | SWT.BORDER | SWT.PASSWORD);
        confirmInput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final PasswordInputField passwordInputField = new PasswordInputField(
                attribute,
                orientation,
                passwordInput,
                confirmInput,
                InputFieldBuilder.createErrorLabel(innerGrid));

        final Listener valueChangeEventListener = event -> {

            final String pwd = passwordInput.getText();
            final String confirm = confirmInput.getText();

            if (StringUtils.isBlank(pwd) && StringUtils.isBlank(confirm)) {
                return;
            }

            if (!pwd.equals(confirm)) {
                passwordInputField.showError("TODO confirm password message");
                return;
            }

            // TODO hash password

            passwordInputField.clearError();
            viewContext.getValueChangeListener().valueChanged(
                    viewContext,
                    attribute,
                    pwd,
                    passwordInputField.listIndex);
        };

        passwordInput.addListener(SWT.FocusOut, valueChangeEventListener);
        passwordInput.addListener(SWT.Traverse, valueChangeEventListener);
        confirmInput.addListener(SWT.FocusOut, valueChangeEventListener);
        confirmInput.addListener(SWT.Traverse, valueChangeEventListener);
        return passwordInputField;
    }

    static final class PasswordInputField extends AbstractInputField<Text> {

        private final Text confirm;

        PasswordInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Text control,
                final Text confirm,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
            this.confirm = confirm;
        }

        @Override
        protected void setDefaultValue() {
            // TODO clarify setting some "fake" input when a password is set (like in config tool)
            this.control.setText(this.initValue);
            this.confirm.setText(this.initValue);
        }

    }

}
