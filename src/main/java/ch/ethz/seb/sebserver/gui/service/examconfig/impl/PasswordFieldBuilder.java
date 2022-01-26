/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gui.form.FieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.PasswordInput;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class PasswordFieldBuilder implements InputFieldBuilder {

    private static final String SEBSERVER_FORM_CONFIRM_LABEL = "sebserver.form.confirm.label";

    private static final LocTextKey VAL_CONFIRM_PWD_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.props.validation.password.confirm");

    private final Cryptor cryptor;
    private final WidgetFactory widgetFactory;

    public PasswordFieldBuilder(
            final WidgetFactory widgetFactory,
            final Cryptor cryptor) {

        this.cryptor = cryptor;
        this.widgetFactory = widgetFactory;
    }

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

        final LocTextKey attributeNameLocKey = ExamConfigurationService.attributeNameLocKey(attribute);
        final PasswordInput passwordInput = new PasswordInput(
                innerGrid,
                this.widgetFactory,
                attributeNameLocKey);
        final GridData passwordInputLD = new GridData(SWT.FILL, SWT.FILL, true, true);
        passwordInput.setLayoutData(passwordInputLD);

        final LocTextKey confirmNameLocKey =
                new LocTextKey(
                        SEBSERVER_FORM_CONFIRM_LABEL,
                        viewContext.i18nSupport.getText(attributeNameLocKey));
        final PasswordInput confirmInput = new PasswordInput(
                innerGrid,
                this.widgetFactory,
                confirmNameLocKey);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.verticalIndent = 14;
        confirmInput.setLayoutData(gridData);
        innerGrid.setData("isPlainText", false);

        final PasswordInputField passwordInputField = new PasswordInputField(
                attribute,
                orientation,
                passwordInput,
                confirmInput,
                FieldBuilder.createErrorLabel(innerGrid),
                this.cryptor);

        if (viewContext.readonly) {
            passwordInput.setEditable(false);
            passwordInputLD.heightHint = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
            confirmInput.setEditable(false);
            confirmInput.setData(RWT.CUSTOM_VARIANT, CustomVariant.CONFIG_INPUT_READONLY.key);
            gridData.heightHint = WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
        } else {
            final Listener valueChangeEventListener = event -> {
                passwordInputField.clearError();

                final CharSequence pwd = passwordInput.getValue();
                final CharSequence confirm = confirmInput.getValue();

                if (passwordInputField.initValue != null && passwordInputField.initValue.contentEquals(pwd)) {
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

    static final class PasswordInputField extends AbstractInputField<PasswordInput> {

        private final PasswordInput confirm;
        private final Cryptor cryptor;

        PasswordInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final PasswordInput control,
                final PasswordInput confirm,
                final Label errorLabel,
                final Cryptor cryptor) {

            super(attribute, orientation, control, errorLabel);
            this.confirm = confirm;
            this.cryptor = cryptor;
        }

        @Override
        protected void setValueToControl(final String value) {
            if (StringUtils.isNotBlank(value)) {
                final CharSequence pwd = this.cryptor
                        .decrypt(value)
                        .getOrThrow();
                this.control.setValue(pwd.toString());
                this.confirm.setValue(pwd.toString());
            } else {
                this.control.setValue(StringUtils.EMPTY);
                this.confirm.setValue(StringUtils.EMPTY);
            }
        }

        @Override
        public String getValue() {
            final CharSequence pwd = this.control.getValue();
            if (StringUtils.isNotBlank(pwd)) {
                return this.cryptor
                        .encrypt(pwd)
                        .getOrThrow()
                        .toString();
            }

            return StringUtils.EMPTY;
        }

    }

}
