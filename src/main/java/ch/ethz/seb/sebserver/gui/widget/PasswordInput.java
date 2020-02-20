/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class PasswordInput extends Composite {

    public static final LocTextKey PLAIN_TEXT_VIEW_TOOLTIP_KEY =
            new LocTextKey("sebserver.overall.action.showPassword.tooltip");


    private final Composite inputAnchor;
    private final Label visibilityButton;

    private Text passwordInput = null;
    private boolean isPlainText = true;
    private boolean isEditable = true;

    public PasswordInput(final Composite parent, final WidgetFactory widgetFactory) {
        super(parent, SWT.NONE);

        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.setLayout(gridLayout);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        inputAnchor = new Composite(this, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        inputAnchor.setLayout(gridLayout);
        inputAnchor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        visibilityButton = widgetFactory.imageButton(
                WidgetFactory.ImageIcon.VISIBILITY,
                this,
                PLAIN_TEXT_VIEW_TOOLTIP_KEY,
                event -> changePasswordView());
        GridData ld = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
        ld.heightHint = 22;
        ld.horizontalIndent = 5;
        visibilityButton.setLayoutData(ld);

        changePasswordView();

    }

    private void changePasswordView() {
        final String value = (this.passwordInput != null) ? this.passwordInput.getText() : null;
        final boolean buildPassword = this.isPlainText;

        if (this.passwordInput != null) {
            PageService.clearComposite(this.inputAnchor);
        }

        Text passwordInput = new Text(
                inputAnchor,
                SWT.LEFT | SWT.BORDER | (buildPassword ? SWT.PASSWORD : SWT.NONE));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        passwordInput.setLayoutData(gridData);
        passwordInput.setText(value != null ? value : StringUtils.EMPTY);
        if (!buildPassword) {
            passwordInput.setEditable(false);
        } else {
            passwordInput.setEditable(isEditable);
            passwordInput.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.CONFIG_INPUT_READONLY.key);
            if (!isEditable) {
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

        this.passwordInput = passwordInput;
        this.isPlainText = !this.isPlainText;

        super.layout(true, true);
    }

    private void changeEvent(int eventType, Event event) {
        if (!this.visibilityButton.isEnabled() && !StringUtils.endsWith(
                this.passwordInput.getText(),
                Constants.IMPORTED_PASSWORD_MARKER)) {

            visibilityButton.setEnabled(true);
        }
        super.notifyListeners(eventType, event);
    }

    public void setValue(CharSequence value) {
        if (passwordInput != null) {
            passwordInput.setText(value != null ? value.toString() : StringUtils.EMPTY);
            if (StringUtils.endsWith(value, Constants.IMPORTED_PASSWORD_MARKER)) {
                this.visibilityButton.setEnabled(false);
            }
        }
    }

    public CharSequence getValue() {
        if (passwordInput != null) {
            return passwordInput.getText();
        }

        return null;
    }


    public void setEditable(boolean editable) {
        this.isEditable = editable;
        this.isPlainText = !this.isPlainText;
        this.changePasswordView();
    }
}
