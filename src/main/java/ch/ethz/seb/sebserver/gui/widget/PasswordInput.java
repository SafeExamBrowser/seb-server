/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;

public class PasswordInput extends Composite {

    private static final long serialVersionUID = 2228580383280478542L;

    public static final LocTextKey PLAIN_TEXT_VIEW_TOOLTIP_KEY =
            new LocTextKey("sebserver.overall.action.showPassword.tooltip");

    private final Composite inputAnchor;
    private final Button visibilityButton;

    private Text passwordInputField = null;
    private boolean isPlainText = true;
    private boolean isEditable = true;
    private String label = null;
    private String testKey = null;

    public PasswordInput(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final LocTextKey label) {

        super(parent, SWT.NONE);

        this.label = widgetFactory.getI18nSupport().getText(label);
        this.testKey = (label != null) ? label.name : null;
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.setLayout(gridLayout);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        this.inputAnchor = new Composite(this, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.inputAnchor.setLayout(gridLayout);
        this.inputAnchor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        this.visibilityButton = widgetFactory.imageButton(
                WidgetFactory.ImageIcon.VISIBILITY,
                this,
                PLAIN_TEXT_VIEW_TOOLTIP_KEY,
                event -> changePasswordView());
        final GridData ld = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
        ld.heightHint = 22;
        ld.horizontalIndent = 5;
        this.visibilityButton.setLayoutData(ld);

        changePasswordView();

    }

    private void changePasswordView() {
        final String value = (this.passwordInputField != null)
                ? this.passwordInputField.getText() != null
                        ? this.passwordInputField.getText().trim()
                        : null
                : null;
        final boolean buildPassword = this.isPlainText;

        if (this.passwordInputField != null) {
            PageService.clearComposite(this.inputAnchor);
        }

        final Text passwordInput = new Text(
                this.inputAnchor,
                SWT.LEFT | SWT.BORDER | (buildPassword ? SWT.PASSWORD : SWT.NONE));
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        passwordInput.setLayoutData(gridData);
        passwordInput.setText(value != null ? value : StringUtils.EMPTY);
        if (!buildPassword) {
            passwordInput.setEditable(false);
        } else {
            passwordInput.setEditable(this.isEditable);
            passwordInput.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.CONFIG_INPUT_READONLY.key);
            if (!this.isEditable) {
                gridData.heightHint = 21;
            }
        }

        if (buildPassword) {
            passwordInput.addListener(SWT.FocusOut, event -> changeEvent(SWT.FocusOut, event));
            passwordInput.addListener(SWT.Traverse, event -> changeEvent(SWT.Traverse, event));
            this.visibilityButton.setImage(WidgetFactory.ImageIcon.VISIBILITY.getImage(getDisplay()));
        } else {
            passwordInput.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.PLAIN_PWD.key);
            this.visibilityButton.setImage(WidgetFactory.ImageIcon.VISIBILITY_OFF.getImage(getDisplay()));
        }

        if (this.label != null) {
            WidgetFactory.setARIALabel(passwordInput, this.label);
            WidgetFactory.setTestId(passwordInput, this.testKey);
        }

        this.passwordInputField = passwordInput;
        this.isPlainText = !this.isPlainText;

        super.layout(true, true);
    }

    private void changeEvent(final int eventType, final Event event) {
        if (!this.visibilityButton.isEnabled() && !StringUtils.endsWith(
                this.passwordInputField.getText(),
                Constants.IMPORTED_PASSWORD_MARKER)) {

            this.visibilityButton.setEnabled(true);
        }
        super.notifyListeners(eventType, event);
    }

    public void setValue(final CharSequence value) {
        if (this.passwordInputField != null) {
            this.passwordInputField.setText(value != null
                    ? Utils.escapeHTML_XML_EcmaScript(value.toString())
                    : StringUtils.EMPTY);
            if (StringUtils.endsWith(value, Constants.IMPORTED_PASSWORD_MARKER)) {
                this.visibilityButton.setEnabled(false);
            }
        }
    }

    public CharSequence getValue() {
        if (this.passwordInputField != null) {
            return this.passwordInputField.getText();
        }

        return null;
    }

    public void setEditable(final boolean editable) {
        this.isEditable = editable;
        this.isPlainText = !this.isPlainText;
        this.changePasswordView();
    }
}
