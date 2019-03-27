/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;
import ch.ethz.seb.sebserver.gui.widget.Message;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LoginPage implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    private final AuthorizationContextHolder authorizationContextHolder;
    private final WidgetFactory widgetFactory;
    private final I18nSupport i18nSupport;

    public LoginPage(
            final AuthorizationContextHolder authorizationContextHolder,
            final WidgetFactory widgetFactory,
            final I18nSupport i18nSupport) {

        this.authorizationContextHolder = authorizationContextHolder;
        this.widgetFactory = widgetFactory;
        this.i18nSupport = i18nSupport;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite parent = pageContext.getParent();

        final Composite loginGroup = new Composite(parent, SWT.NONE);
        final GridLayout rowLayout = new GridLayout();
        rowLayout.marginWidth = 20;
        rowLayout.marginRight = 100;
        loginGroup.setLayout(rowLayout);
        loginGroup.setData(RWT.CUSTOM_VARIANT, "login");

        final Label name = this.widgetFactory.labelLocalized(loginGroup, "sebserver.login.username");
        name.setLayoutData(new GridData(300, -1));
        name.setAlignment(SWT.BOTTOM);
        final Text loginName = this.widgetFactory.textInput(loginGroup);
        loginName.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        final Label pwd = this.widgetFactory.labelLocalized(loginGroup, "sebserver.login.pwd");
        pwd.setLayoutData(gridData);
        final Text loginPassword = this.widgetFactory.passwordInput(loginGroup);
        loginPassword.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        final Button button = this.widgetFactory.buttonLocalized(loginGroup, "sebserver.login.login");
        gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        button.setLayoutData(gridData);

        final SEBServerAuthorizationContext authorizationContext = this.authorizationContextHolder
                .getAuthorizationContext(RWT.getUISession().getHttpSession());

        button.addListener(SWT.Selection, event -> {
            final String username = loginName.getText();
            try {

                final boolean loggedIn = authorizationContext.login(
                        username,
                        loginPassword.getText());

                if (loggedIn) {
                    // Set users locale on page after successful login
                    this.i18nSupport.setSessionLocale(
                            authorizationContext
                                    .getLoggedInUser()
                                    .getOrThrow().language);

                    pageContext.forwardToMainPage();

                } else {
                    loginError(pageContext, "sebserver.login.failed.message");
                }
            } catch (final Exception e) {
                log.error("Unexpected error while trying to login with user: {}", username, e);
                loginError(pageContext, "Unexpected Error. Please call an Administrator");
            }
        });
        loginName.addListener(SWT.KeyDown, event -> {
            if (event.character == '\n' || event.character == '\r') {
                loginPassword.setFocus();
            }
        });
        loginPassword.addListener(SWT.KeyDown, event -> {
            if (event.character == '\n' || event.character == '\r') {
                button.setFocus();
            }
        });

    }

    private void loginError(
            final PageContext pageContext,
            final String message) {

        pageContext.logout();
        final MessageBox error = new Message(
                pageContext.getShell(),
                this.i18nSupport.getText("sebserver.login.failed.title"),
                this.i18nSupport.getText(message, message),
                SWT.ERROR);
        error.open(null);
    }

}
