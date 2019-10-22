/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class PassworFieldBuilder implements InputFieldBuilder {

    private static final Logger log = LoggerFactory.getLogger(PassworFieldBuilder.class);

    private static final LocTextKey VAL_CONFIRM_PWD_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.props.validation.password.confirm");

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

        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final Text passwordInput = new Text(innerGrid, SWT.LEFT | SWT.BORDER | SWT.PASSWORD);
        final GridData passwordInputLD = new GridData(SWT.FILL, SWT.FILL, true, false);
        passwordInput.setLayoutData(passwordInputLD);
        final Text confirmInput = new Text(innerGrid, SWT.LEFT | SWT.BORDER | SWT.PASSWORD);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.verticalIndent = 14;
        confirmInput.setLayoutData(gridData);

        final PasswordInputField passwordInputField = new PasswordInputField(
                attribute,
                orientation,
                passwordInput,
                confirmInput,
                Form.createErrorLabel(innerGrid));

        if (viewContext.readonly) {
            passwordInput.setEditable(false);
            passwordInput.setData(RWT.CUSTOM_VARIANT, CustomVariant.CONFIG_INPUT_READONLY.key);
            passwordInputLD.heightHint = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
            confirmInput.setEditable(false);
            confirmInput.setData(RWT.CUSTOM_VARIANT, CustomVariant.CONFIG_INPUT_READONLY.key);
            gridData.heightHint = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
        } else {
            final Listener valueChangeEventListener = event -> {
                passwordInputField.clearError();

                final String pwd = passwordInput.getText();
                final String confirm = confirmInput.getText();

                if (passwordInputField.initValue != null && passwordInputField.initValue.equals(pwd)) {
                    return;
                }

                if (!pwd.equals(confirm)) {
                    passwordInputField.showError(viewContext
                            .getI18nSupport()
                            .getText(VAL_CONFIRM_PWD_TEXT_KEY));
                    return;
                }

                final String hashedPWD = passwordInputField.getValue();
                if (hashedPWD != null) {
                    viewContext.getValueChangeListener().valueChanged(
                            viewContext,
                            attribute,
                            hashedPWD,
                            passwordInputField.listIndex);
                }
            };

            passwordInput.addListener(SWT.FocusOut, valueChangeEventListener);
            passwordInput.addListener(SWT.Traverse, valueChangeEventListener);
            confirmInput.addListener(SWT.FocusOut, valueChangeEventListener);
            confirmInput.addListener(SWT.Traverse, valueChangeEventListener);
        }
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
        protected void setValueToControl(final String value) {
            // TODO clarify setting some "fake" input when a password is set (like in config tool)
            if (value != null) {
                this.control.setText(value);
                this.confirm.setText(value);
            }
        }

        @Override
        public String getValue() {
            String hashedPWD;
            try {
                hashedPWD = hashPassword(this.control.getText());
            } catch (final NoSuchAlgorithmException e) {
                log.error("Failed to hash password: ", e);
                showError("Failed to hash password");
                hashedPWD = null;
            }

            return hashedPWD;
        }

        private String hashPassword(final String pwd) throws NoSuchAlgorithmException {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] encodedhash = digest.digest(
                    pwd.getBytes(StandardCharsets.UTF_8));

            return Hex.encodeHexString(encodedhash);
        }

    }

}
