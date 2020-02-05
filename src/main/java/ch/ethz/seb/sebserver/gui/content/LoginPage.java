/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.DefaultRegisterPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;
import ch.ethz.seb.sebserver.gui.widget.Message;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LoginPage implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    private final PageService pageService;
    private final AuthorizationContextHolder authorizationContextHolder;
    private final WidgetFactory widgetFactory;
    private final I18nSupport i18nSupport;
    private final DefaultRegisterPage defaultRegisterPage;
    private final boolean registreringEnabled;

    public LoginPage(
            final PageService pageService,
            final DefaultRegisterPage defaultRegisterPage,
            @Value("${sebserver.gui.self-registering:false}") final Boolean registreringEnabled) {

        this.pageService = pageService;
        this.authorizationContextHolder = pageService.getAuthorizationContextHolder();
        this.widgetFactory = pageService.getWidgetFactory();
        this.i18nSupport = pageService.getI18nSupport();
        this.defaultRegisterPage = defaultRegisterPage;
        this.registreringEnabled = BooleanUtils.toBoolean(registreringEnabled);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite parent = pageContext.getParent();
        WidgetFactory.setTestId(parent, "login-page");
        WidgetFactory.setARIARole(parent, "composite");

        final Composite loginGroup = new Composite(parent, SWT.NONE);
        final GridLayout rowLayout = new GridLayout();
        rowLayout.marginWidth = 20;
        rowLayout.marginRight = 100;
        loginGroup.setLayout(rowLayout);
        loginGroup.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN.key);

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

        final SEBServerAuthorizationContext authorizationContext = this.authorizationContextHolder
                .getAuthorizationContext(RWT.getUISession().getHttpSession());

        final Composite buttons = new Composite(loginGroup, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        buttons.setLayout(new GridLayout(2, false));
        buttons.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN_BACK.key);

        final Button loginButton = this.widgetFactory.buttonLocalized(buttons, "sebserver.login.login");
        gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        loginButton.setLayoutData(gridData);
        loginButton.addListener(SWT.Selection, event -> {
            login(
                    pageContext,
                    loginName.getText(),
                    loginPassword.getText(),
                    authorizationContext);
        });
        loginName.addListener(SWT.KeyDown, event -> {
            if (event.character == '\n' || event.character == '\r') {
                if (StringUtils.isNotBlank(loginPassword.getText())) {
                    login(
                            pageContext,
                            loginName.getText(),
                            loginPassword.getText(),
                            authorizationContext);
                } else {
                    loginPassword.setFocus();
                }
            }
        });
        loginPassword.addListener(SWT.KeyDown, event -> {
            if (event.character == '\n' || event.character == '\r') {
                if (StringUtils.isNotBlank(loginName.getText())) {
                    login(
                            pageContext,
                            loginName.getText(),
                            loginPassword.getText(),
                            authorizationContext);
                } else {
                    loginName.setFocus();
                }
            }
        });

        if (this.registreringEnabled) {
            final Button registerButton = this.widgetFactory.buttonLocalized(buttons, "sebserver.login.register");
            gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
            gridData.verticalIndent = 10;
            registerButton.setLayoutData(gridData);
            registerButton.addListener(SWT.Selection, event -> {
                pageContext.forwardToPage(this.defaultRegisterPage);
            });
        }
    }

    private void login(
            final PageContext pageContext,
            final String loginName,
            final CharSequence loginPassword,
            final SEBServerAuthorizationContext authorizationContext) {

        final String username = loginName;
        try {

            final boolean loggedIn = authorizationContext.login(
                    username,
                    loginPassword);

            if (loggedIn) {
                // Set users locale on page after successful login
                try {
                    RWT.getUISession()
                            .getHttpSession()
                            .setAttribute(I18nSupport.ATTR_CURRENT_SESSION_LOCALE, authorizationContext
                                    .getLoggedInUser()
                                    .getOrThrow().language);

                } catch (final IllegalStateException e) {
                    log.error("Set current locale for session failed: ", e);
                }

                RWT.setLocale(this.i18nSupport.getUsersFormatLocale());

                pageContext.forwardToMainPage();

            } else {
                loginError(pageContext, "sebserver.login.failed.message");
            }
        } catch (final Exception e) {
            log.error("Unexpected error while trying to login with user: {}", username, e);
            loginError(pageContext, "Unexpected Error. Please call an Administrator");
        }
    }

    private void loginError(
            final PageContext pageContext,
            final String message) {

        this.pageService.logout(pageContext);
        final MessageBox error = new Message(
                pageContext.getShell(),
                this.i18nSupport.getText("sebserver.login.failed.title"),
                this.i18nSupport.getText(message, message),
                SWT.ERROR,
                this.i18nSupport);
        error.open(null);
    }

}
