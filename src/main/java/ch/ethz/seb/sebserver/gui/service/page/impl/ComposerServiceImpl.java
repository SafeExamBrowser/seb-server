/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.RemoteProctoringView;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.IllegalUserSessionStateException;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;
import ch.ethz.seb.sebserver.gui.widget.Message;

@Lazy
@Service
@GuiProfile
public class ComposerServiceImpl implements ComposerService {

    private static final Logger log = LoggerFactory.getLogger(ComposerServiceImpl.class);

    public static final String TABINDEX_RESET_SCRIPT =
            "try {\n"
                    + "    document.body.setAttribute(\"tabindex\", \"0\");\n"
                    + "    var items = document.body.getElementsByTagName('div');\n"
                    + "    console.log('*** '+items.length);\n"
                    + "    for (i = 0; i < items.length; i++) {\n"
                    + "        if(items[i].hasAttribute(\"tabindex\")) {\n"
                    + "            items[i].setAttribute(\"tabindex\", \"0\");\n"
                    + "        }\n"
                    + "    }\n"
                    + "} catch (error) {\n"
                    + "  console.error(error);\n"
                    + "}";

    private final Class<? extends PageDefinition> loginPageType = DefaultLoginPage.class;
    private final Class<? extends PageDefinition> mainPageType = DefaultMainPage.class;

    final AuthorizationContextHolder authorizationContextHolder;
    private final I18nSupport i18nSupport;
    private final Map<String, TemplateComposer> composer;
    private final Map<String, PageDefinition> pages;

    public ComposerServiceImpl(
            final AuthorizationContextHolder authorizationContextHolder,
            final I18nSupport i18nSupport,
            final Collection<TemplateComposer> composer,
            final Collection<PageDefinition> pageDefinitions) {

        this.authorizationContextHolder = authorizationContextHolder;
        this.i18nSupport = i18nSupport;
        this.composer = composer
                .stream()
                .collect(Collectors.toMap(
                        comp -> comp.getClass().getName(),
                        Function.identity()));

        this.pages = pageDefinitions
                .stream()
                .collect(Collectors.toMap(
                        page -> page.getClass().getName(),
                        Function.identity()));
    }

    @Override
    public PageDefinition mainPage() {
        return this.pages.get(this.mainPageType.getName());
    }

    @Override
    public PageDefinition loginPage() {
        return this.pages.get(this.loginPageType.getName());
    }

    @Override
    public void loadProctoringView(final Composite parent) {
        final ProctoringWindowData currentProctoringWindowData = ProctoringGUIService.getCurrentProctoringWindowData();
        this.composer.values()
                .stream()
                .filter(c -> c instanceof RemoteProctoringView)
                .map(c -> (RemoteProctoringView) c)
                .filter(c -> c.serverType() == currentProctoringWindowData.connectionData.proctoringServerType)
                .findFirst()
                .ifPresent(c -> c.compose(createPageContext(parent)));
    }

    @Override
    public boolean validate(final String composerName, final PageContext pageContext) {
        if (!this.composer.containsKey(composerName)) {
            return false;
        }

        return this.composer
                .get(composerName)
                .validate(pageContext);
    }

    @Override
    public void compose(
            final Class<? extends TemplateComposer> composerType,
            final PageContext pageContext) {

        if (composerType != null && pageContext != null) {
            compose(composerType.getName(), pageContext);
        }
    }

    @Override
    public void compose(
            final String name,
            final PageContext pageContext) {

        // Check first if there is still a valid authorization context
        if (!this.authorizationContextHolder.getAuthorizationContext().isValid()) {
            return;
        }

        if (!this.composer.containsKey(name)) {
            log.error("No TemplateComposer with name: " + name + " found. Check Spring configuration and beans");
            return;
        }

        final TemplateComposer composer = this.composer.get(name);

        if (composer.validate(pageContext)) {

            PageService.clearComposite(pageContext.getParent());

            try {

                // apply tabindex reset script
                final JavaScriptExecutor executor = RWT.getClient().getService(JavaScriptExecutor.class);
                executor.execute(ComposerServiceImpl.TABINDEX_RESET_SCRIPT);

                composer.compose(pageContext);
                PageService.updateScrolledComposite(pageContext.getParent());

            } catch (final IllegalUserSessionStateException e) {
                log.warn("Illegal user session state detected... cleanup user session and forward to login page.");
                pageContext.forwardToLoginPage();
                final MessageBox logoutSuccess = new Message(
                        pageContext.getShell(),
                        this.i18nSupport.getText("sebserver.logout"),
                        Utils.formatLineBreaks(this.i18nSupport.getText("sebserver.logout.invalid-session.message")),
                        SWT.ICON_INFORMATION,
                        this.i18nSupport);
                logoutSuccess.open(null);
                return;
            } catch (final RuntimeException e) {
                log.warn("Failed to compose: {}, pageContext: {}", name, pageContext, e);
                pageContext.notifyError(new LocTextKey("sebserver.error.unexpected"), e);
            } catch (final Exception e) {
                log.error("Failed to compose: {}, pageContext: {}", name, pageContext, e);
            }

            try {
                pageContext.getParent().layout();
            } catch (final Exception e) {
                log.warn("Failed to layout new composition: {}, pageContext: {}", name, pageContext, e);
            }

        } else {
            log.error(
                    "Invalid or missing mandatory attributes to handle compose request of ViewComposer: {} pageContext: {}",
                    name,
                    pageContext);
        }

    }

    @Override
    public void composePage(
            final PageDefinition pageDefinition,
            final Composite root) {

        compose(
                pageDefinition.composer(),
                pageDefinition.applyPageContext(createPageContext(root)));
    }

    @Override
    public void composePage(
            final Class<? extends PageDefinition> pageType,
            final Composite root) {

        final String pageName = pageType.getName();
        if (!this.pages.containsKey(pageName)) {
            log.error("Unknown page with name: {}", pageName);
            return;
        }

        final PageDefinition pageDefinition = this.pages.get(pageName);
        compose(
                pageDefinition.composer(),
                pageDefinition.applyPageContext(createPageContext(root)));
    }

    @Override
    public void loadLoginPage(final Composite parent) {
        composePage(this.loginPageType, parent);
    }

    @Override
    public void loadMainPage(final Composite parent) {
        composePage(this.mainPageType, parent);
    }

    private PageContext createPageContext(final Composite root) {
        return new PageContextImpl(this.i18nSupport, this, root, root, null);
    }

}
