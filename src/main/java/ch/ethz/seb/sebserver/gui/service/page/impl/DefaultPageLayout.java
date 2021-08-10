/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.widget.Message;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.AriaRole;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class DefaultPageLayout implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(DefaultPageLayout.class);

    private static final LocTextKey ABOUT_TEXT_KEY = new LocTextKey("sebserver.overall.about");
    private static final LocTextKey IMPRINT_TEXT_KEY = new LocTextKey("sebserver.overall.imprint");
    private static final LocTextKey HELP_TEXT_KEY = new LocTextKey("sebserver.overall.help");

    private static final LocTextKey ABOUT_MARKUP_TEXT_KEY = new LocTextKey("sebserver.overall.about.markup");
    private static final LocTextKey IMPRINT_MARKUP_TEXT_KEY = new LocTextKey("sebserver.overall.imprint.markup");
    private static final LocTextKey HELP_LINK_TEXT_KEY = new LocTextKey("sebserver.overall.help.link");

    public static final int LOGO_IMAGE_MAX_WIDTH = 400;
    public static final int LOGO_IMAGE_MAX_HEIGHT = 80;

    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;
    private final AuthorizationContextHolder authorizationContextHolder;
    private final PageService pageService;
    private final String sebServerVersion;
    private final boolean multilingual;

    public DefaultPageLayout(
            final PageService pageService,
            final Environment environment) {

        this.widgetFactory = pageService.getWidgetFactory();
        this.polyglotPageService = pageService.getPolyglotPageService();
        this.authorizationContextHolder = pageService.getAuthorizationContextHolder();
        this.pageService = pageService;
        this.sebServerVersion = environment.getProperty("sebserver.version", Constants.EMPTY_NOTE);
        this.multilingual = BooleanUtils.toBoolean(environment.getProperty("sebserver.gui.multilingual", "false"));
    }

    @Override
    public boolean validate(final PageContext pageContext) {
        return pageContext.hasAttribute(AttributeKeys.PAGE_TEMPLATE_COMPOSER_NAME);
    }

    @Override
    public void compose(final PageContext pageContext) {

        final GridLayout skeletonLayout = new GridLayout();
        skeletonLayout.marginBottom = 0;
        skeletonLayout.marginLeft = 0;
        skeletonLayout.marginRight = 0;
        skeletonLayout.marginTop = 0;
        skeletonLayout.marginHeight = 0;
        skeletonLayout.marginWidth = 0;
        skeletonLayout.verticalSpacing = 0;
        skeletonLayout.horizontalSpacing = 0;
        pageContext.getParent().setLayout(skeletonLayout);

        WidgetFactory.resetTabindex(pageContext.getParent());

        composeHeader(pageContext);
        composeLogoBar(pageContext);
        composeContent(pageContext);
        composeFooter(pageContext);

        this.polyglotPageService.setDefaultPageLocale(pageContext.getRoot());
    }

    private void composeHeader(final PageContext pageContext) {
        final Composite header = new Composite(pageContext.getParent(), SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginRight = 50;
        gridLayout.marginLeft = 50;
        header.setLayout(gridLayout);
        final GridData headerCell = new GridData(SWT.FILL, SWT.TOP, true, false);
        headerCell.minimumHeight = 40;
        headerCell.heightHint = 40;
        header.setLayoutData(headerCell);
        header.setData(RWT.CUSTOM_VARIANT, "header");

        final Composite headerRight = new Composite(header, SWT.NONE);
        headerRight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        final GridLayout headerRightGrid = new GridLayout(2, false);
        headerRightGrid.marginHeight = 0;
        headerRightGrid.marginWidth = 0;
        headerRightGrid.horizontalSpacing = 20;
        headerRight.setLayout(headerRightGrid);
        headerRight.setData(RWT.CUSTOM_VARIANT, "header");

        if (this.authorizationContextHolder.getAuthorizationContext().isLoggedIn()) {
            final Label username = new Label(headerRight, SWT.NONE);
            username.setData(RWT.CUSTOM_VARIANT, "header");
            username.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
            username.setText(this.authorizationContextHolder
                    .getAuthorizationContext()
                    .getLoggedInUser()
                    .get(t -> this.pageService.logoutOnError(t, pageContext)).username);

            final Button logout = this.widgetFactory.buttonLocalized(headerRight, "sebserver.logout");
            logout.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, true));
            logout.setData(RWT.CUSTOM_VARIANT, "header");
            logout.addListener(SWT.Selection, event -> {
                if (this.pageService.logout(pageContext)) {
                    // show successful logout message
                    final MessageBox logoutSuccess = new Message(
                            pageContext.getShell(),
                            this.polyglotPageService.getI18nSupport().getText("sebserver.logout"),
                            this.polyglotPageService.getI18nSupport().getText("sebserver.logout.success.message"),
                            SWT.ICON_INFORMATION,
                            pageContext.getI18nSupport());
                    logoutSuccess.open(null);
                }
            });
        }
    }

    private void composeLogoBar(final PageContext pageContext) {
        final Composite logoBar = new Composite(pageContext.getParent(), SWT.NONE);
        final GridData logoBarCell = new GridData(SWT.FILL, SWT.TOP, true, false);
        logoBarCell.minimumHeight = 80;
        logoBarCell.heightHint = 80;
        logoBarCell.horizontalIndent = 0;
        logoBar.setLayoutData(logoBarCell);
        logoBar.setData(RWT.CUSTOM_VARIANT, "logo");
        final GridLayout logoBarLayout = new GridLayout(2, true);
        logoBarLayout.horizontalSpacing = 0;
        logoBarLayout.verticalSpacing = 0;
        logoBarLayout.marginHeight = 0;
        logoBar.setLayout(logoBarLayout);

        final Composite logo = new Composite(logoBar, SWT.NONE);
        final GridData logoCell = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        logoCell.minimumHeight = LOGO_IMAGE_MAX_HEIGHT;
        logoCell.heightHint = LOGO_IMAGE_MAX_HEIGHT;
        logoCell.minimumWidth = LOGO_IMAGE_MAX_WIDTH;
        logoCell.horizontalIndent = 0;
        logo.setLayoutData(logoCell);

        // try to get institutional logo first. If no success, use default logo
        loadInstitutionalLogo(pageContext, logo);

        final Composite langSupport = new Composite(logoBar, SWT.NONE);
        final GridData langSupportCell = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        langSupportCell.heightHint = 20;
        logoCell.horizontalIndent = 50;
        langSupport.setLayoutData(langSupportCell);
        langSupport.setData(RWT.CUSTOM_VARIANT, "logo");
        final RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.spacing = 7;
        rowLayout.marginRight = 70;
        langSupport.setLayout(rowLayout);

        if (this.multilingual) {
            this.polyglotPageService.createLanguageSelector(pageContext.copyOf(langSupport));
        }
    }

    private void composeContent(final PageContext pageContext) {
        final Composite contentBackground = new Composite(pageContext.getParent(), SWT.NONE);
        contentBackground.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        contentBackground.setData(RWT.CUSTOM_VARIANT, "bgContent");
        final GridLayout innerGrid = new GridLayout();
        innerGrid.marginLeft = 55;
        innerGrid.marginRight = 55;
        innerGrid.marginHeight = 0;
        innerGrid.marginWidth = 0;

        contentBackground.setLayout(innerGrid);

        final Composite content = new Composite(contentBackground, SWT.NONE);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        content.setData(RWT.CUSTOM_VARIANT, "content");
        final GridLayout contentGrid = new GridLayout();
        contentGrid.marginHeight = 0;
        contentGrid.marginWidth = 0;
        content.setLayout(contentGrid);

        final Composite contentInner = new Composite(content, SWT.NONE);
        contentInner.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        contentInner.setLayout(gridLayout);

        final String contentComposerName = pageContext.getAttribute(
                AttributeKeys.PAGE_TEMPLATE_COMPOSER_NAME);
        pageContext.composerService().compose(
                contentComposerName,
                pageContext.copyOf(contentInner));
    }

    private void composeFooter(final PageContext pageContext) {
        final Composite footerBar = new Composite(pageContext.getParent(), SWT.NONE);
        final GridData footerCell = new GridData(SWT.FILL, SWT.BOTTOM, false, false);
        footerCell.minimumHeight = 30;
        footerCell.heightHint = 30;
        footerBar.setLayoutData(footerCell);
        footerBar.setData(RWT.CUSTOM_VARIANT, "bgFooter");
        final GridLayout innerBarGrid = new GridLayout();
        innerBarGrid.marginHeight = 0;
        innerBarGrid.marginWidth = 0;
        innerBarGrid.marginLeft = 50;
        innerBarGrid.marginRight = 50;
        footerBar.setLayout(innerBarGrid);

        final Composite footer = new Composite(footerBar, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        footer.setLayoutData(gridData);
        final GridLayout footerGrid = new GridLayout(2, false);
        footerGrid.marginHeight = 0;
        footerGrid.marginWidth = 0;
        footerGrid.horizontalSpacing = 0;
        footer.setLayout(footerGrid);
        footer.setData(RWT.CUSTOM_VARIANT, "footer");

        final Composite footerLeft = new Composite(footer, SWT.NONE);
        footerLeft.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
        footerLeft.setData(RWT.CUSTOM_VARIANT, "footer");
        RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.marginLeft = 20;
        rowLayout.spacing = 20;
        footerLeft.setLayout(rowLayout);

        final Composite footerRight = new Composite(footer, SWT.NONE);
        footerRight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        footerRight.setData(RWT.CUSTOM_VARIANT, "footer");
        rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.marginRight = 20;
        footerRight.setLayout(rowLayout);

        final I18nSupport i18nSupport = this.widgetFactory.getI18nSupport();
        if (StringUtils.isNoneBlank(i18nSupport.getText(IMPRINT_TEXT_KEY, ""))) {
            final Label imprint = this.widgetFactory.labelLocalized(
                    footerLeft,
                    CustomVariant.FOOTER,
                    IMPRINT_TEXT_KEY);

            imprint.addListener(SWT.MouseUp, event -> {
                try {
                    pageContext.publishPageMessage(IMPRINT_TEXT_KEY, IMPRINT_MARKUP_TEXT_KEY);
                } catch (final Exception e) {
                    log.error("Invalid markup for 'Imprint'", e);
                }
            });

            WidgetFactory.setARIARole(imprint, AriaRole.link);
        }
        if (StringUtils.isNoneBlank(i18nSupport.getText(ABOUT_TEXT_KEY, ""))) {
            final Label about = this.widgetFactory.labelLocalized(
                    footerLeft,
                    CustomVariant.FOOTER,
                    ABOUT_TEXT_KEY);

            about.addListener(SWT.MouseUp, event -> {
                try {
                    pageContext.publishPageMessage(ABOUT_TEXT_KEY, ABOUT_MARKUP_TEXT_KEY);
                } catch (final Exception e) {
                    log.error("Invalid markup for 'About'", e);
                }
            });

            WidgetFactory.setARIARole(about, AriaRole.link);
        }
        if (StringUtils.isNoneBlank(i18nSupport.getText(HELP_TEXT_KEY, ""))) {
            final Label help = this.widgetFactory.labelLocalized(
                    footerLeft,
                    CustomVariant.FOOTER,
                    HELP_TEXT_KEY);

            help.addListener(SWT.MouseUp, event -> {
                try {
                    final String link = i18nSupport.getText(HELP_LINK_TEXT_KEY, "");
                    if (StringUtils.isNoneBlank(link)) {
                        final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
                        urlLauncher.openURL(link);
                    }
                } catch (final Exception e) {
                    log.error("Invalid Help link", e);
                }
            });

            WidgetFactory.setARIARole(help, AriaRole.link);
        }
        this.widgetFactory.labelLocalized(
                footerRight,
                CustomVariant.FOOTER,
                new LocTextKey("sebserver.overall.version", this.sebServerVersion));
    }

    private void loadInstitutionalLogo(final PageContext pageContext, final Composite logo) {
        logo.setData(RWT.CUSTOM_VARIANT, "bgLogo");
        try {

            final String imageBase64 = (String) RWT.getUISession()
                    .getHttpSession()
                    .getAttribute(API.PARAM_LOGO_IMAGE);

            if (StringUtils.isBlank(imageBase64)) {
                return;
            }

            final Base64InputStream input = new Base64InputStream(
                    new ByteArrayInputStream(imageBase64.getBytes(StandardCharsets.UTF_8)),
                    false);

            final Display display = pageContext.getShell().getDisplay();
            final Image image = new Image(display, input);
            final Rectangle imageBounds = image.getBounds();
            final int width = Math.min(imageBounds.width, LOGO_IMAGE_MAX_WIDTH);
            final int height = Math.min(imageBounds.height, LOGO_IMAGE_MAX_HEIGHT);
            final ImageData imageData = image.getImageData().scaledTo(width, height);

            logo.setData(RWT.CUSTOM_VARIANT, "bgLogoNoImage");
            logo.setBackgroundImage(new Image(display, imageData));

        } catch (final Exception e) {
            log.warn("Get institutional logo failed: {}", e.getMessage());
        }
    }

}
