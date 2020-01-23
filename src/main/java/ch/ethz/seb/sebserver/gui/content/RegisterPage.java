/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.user.PasswordChange;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.InstitutionalAuthenticationEntryPoint;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionInfo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class RegisterPage implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(RegisterPage.class);

    static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.login.register.form.title");
    static final LocTextKey FORM_PASSWORD_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password.confirm");
    static final LocTextKey FORM_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.password");
    static final LocTextKey FORM_ROLES_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.roles");
    static final LocTextKey FORM_TIMEZONE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.timezone");
    static final LocTextKey FORM_MAIL_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.mail");
    static final LocTextKey FORM_USERNAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.username");
    static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.name");
    static final LocTextKey FORM_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.institution");
    static final LocTextKey FORM_LANG_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.language");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
    private final I18nSupport i18nSupport;
    private final WebserviceURIService webserviceURIService;
    private final RestTemplate restTemplate;
    private final boolean multilingual;

    protected RegisterPage(
            final PageService pageService,
            final WebserviceURIService webserviceURIService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            @Value("${sebserver.gui.multilingual:false}") final Boolean multilingual) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.widgetFactory = pageService.getWidgetFactory();
        this.i18nSupport = pageService.getI18nSupport();
        this.webserviceURIService = webserviceURIService;
        this.multilingual = BooleanUtils.toBoolean(multilingual);

        this.restTemplate = new RestTemplate();

        final ClientHttpRequestFactory clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory()
                .getOrThrow();

        this.restTemplate.setRequestFactory(clientHttpRequestFactory);
    }

    @Override
    public void compose(final PageContext pageContext) {

        final String institutionId = InstitutionalAuthenticationEntryPoint
                .extractInstitutionalEndpoint();

        final List<EntityName> institutions = this.pageService
                .getRestService()
                .getBuilder(GetInstitutionInfo.class)
                .withRestTemplate(this.restTemplate)
                .withURIVariable(API.INFO_PARAM_INST_SUFFIX, institutionId)
                .call()
                .getOrThrow();

        final boolean definedInstitution = institutions.size() == 1;
        final Supplier<List<Tuple<String>>> instResources = () -> institutions
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(ResourceService.RESOURCE_COMPARATOR)
                .collect(Collectors.toList());

        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);
        content.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN.key);

        // The UserAccount form
        final FormBuilder formBuilder = this.pageService.formBuilder(
                pageContext.copyOf(content))
                .readonly(false)
                .putStaticValueIf(
                        () -> !this.multilingual,
                        Domain.USER.ATTR_LANGUAGE,
                        "en")
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_INSTITUTION_ID,
                        FORM_INSTITUTION_TEXT_KEY,
                        (definedInstitution) ? institutions.get(0).modelId : null,
                        instResources)
                        .readonly(definedInstitution))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_NAME,
                        FORM_NAME_TEXT_KEY))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_USERNAME,
                        FORM_USERNAME_TEXT_KEY))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_EMAIL,
                        FORM_MAIL_TEXT_KEY))
                .addFieldIf(
                        () -> this.multilingual,
                        () -> FormBuilder.singleSelection(
                                Domain.USER.ATTR_LANGUAGE,
                                FORM_LANG_TEXT_KEY,
                                Constants.DEFAULT_LANG_CODE,
                                this.resourceService::languageResources))
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_TIMEZONE,
                        FORM_TIMEZONE_TEXT_KEY,
                        Constants.DEFAULT_TIME_ZONE_CODE,
                        this.resourceService::timeZoneResources))
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_NEW_PASSWORD,
                        FORM_PASSWORD_TEXT_KEY)
                        .asPasswordField())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                        FORM_PASSWORD_CONFIRM_TEXT_KEY)
                        .asPasswordField());

        //formBuilder.formParent.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN_BACK.key);
        formBuilder.build();

        final Composite buttons = new Composite(content, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        buttons.setLayout(new GridLayout(2, false));
        buttons.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN_BACK.key);

        final Button registerButton = this.widgetFactory.buttonLocalized(buttons, "sebserver.login.register");
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        registerButton.setLayoutData(gridData);
        registerButton.addListener(SWT.Selection, event -> {

        });

        final Button cancelButton = this.widgetFactory.buttonLocalized(buttons, "sebserver.overall.action.cancel");
        gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        cancelButton.setLayoutData(gridData);
        cancelButton.addListener(SWT.Selection, event -> {
            pageContext.forwardToLoginPage();
        });

    }

}
