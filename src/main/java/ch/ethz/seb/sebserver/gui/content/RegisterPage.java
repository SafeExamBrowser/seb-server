/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.InstitutionalAuthenticationEntryPoint;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionInfo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.RegisterNewUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.WebserviceURIService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class RegisterPage implements TemplateComposer {

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
    static final LocTextKey FORM_SURNAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.surname");
    static final LocTextKey FORM_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.institution");
    static final LocTextKey FORM_LANG_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.form.language");
    static final LocTextKey ACTION_CREATE =
            new LocTextKey("sebserver.login.register.do");
    static final LocTextKey ACTION_CANCEL =
            new LocTextKey("sebserver.overall.action.cancel");
    static final LocTextKey MESSAGE_SUCCESS_TILE =
            new LocTextKey("sebserver.page.message");
    static final LocTextKey MESSAGE_SUCCESS_TEXT =
            new LocTextKey("sebserver.login.register.success");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
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
        this.multilingual = BooleanUtils.toBoolean(multilingual);

        this.restTemplate = new RestTemplate();

        final ClientHttpRequestFactory clientHttpRequestFactory = clientHttpRequestFactoryService
                .getClientHttpRequestFactory()
                .getOrThrow();

        this.restTemplate.setRequestFactory(clientHttpRequestFactory);
    }

    @Override
    public void compose(final PageContext pageContext) {

        final Composite outer = new Composite(pageContext.getParent(), SWT.NONE);
        final GridLayout outerLayout = new GridLayout();
        outerLayout.marginLeft = 50;
        outerLayout.marginRight = 50;
        outer.setLayout(outerLayout);
        outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite parent = PageService.createManagedVScrolledComposite(
                outer,
                scrolledComposite -> {
                    final Composite result = new Composite(scrolledComposite, SWT.NONE);
                    result.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
                    final GridLayout contentOutlaying = new GridLayout();
                    contentOutlaying.marginHeight = 0;
                    contentOutlaying.marginWidth = 0;
                    result.setLayout(contentOutlaying);
                    result.setData(RWT.CUSTOM_VARIANT, "register");
                    return result;
                },
                false);

        final String institutionName = InstitutionalAuthenticationEntryPoint
                .extractInstitutionalEndpoint();

        final List<EntityName> institutions = this.pageService
                .getRestService()
                .getBuilder(GetInstitutionInfo.class)
                .withRestTemplate(this.restTemplate)
                .withURIVariable(API.INFO_PARAM_INST_SUFFIX, institutionName)
                .call()
                .getOrThrow();

        final boolean definedInstitution = institutions.size() == 1;
        final String institutionId = (definedInstitution) ? institutions.get(0).modelId : null;
        final Supplier<List<Tuple<String>>> instResources = () -> institutions
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(ResourceService.RESOURCE_COMPARATOR)
                .collect(Collectors.toList());

        this.widgetFactory.labelLocalizedTitle(parent, TITLE_TEXT_KEY);

        // The UserAccount form
        final FormHandle<UserInfo> registerForm = this.pageService.formBuilder(
                pageContext.copyOf(parent))
                .readonly(false)
                .putStaticValueIf(
                        () -> !this.multilingual,
                        Domain.USER.ATTR_LANGUAGE,
                        Locale.ENGLISH.getLanguage())
                .putStaticValueIf(
                        () -> definedInstitution,
                        Domain.USER.ATTR_INSTITUTION_ID,
                        institutionId)
                .addField(FormBuilder.singleSelection(
                        Domain.USER.ATTR_INSTITUTION_ID,
                        FORM_INSTITUTION_TEXT_KEY,
                        institutionId,
                        instResources)
                        .readonly(definedInstitution)
                        .mandatory(!definedInstitution))
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_NAME,
                        FORM_NAME_TEXT_KEY)
                        .mandatory())
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_SURNAME,
                        FORM_SURNAME_TEXT_KEY)
                        .mandatory())
                .addField(FormBuilder.text(
                        Domain.USER.ATTR_USERNAME,
                        FORM_USERNAME_TEXT_KEY)
                        .mandatory())
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
                        this.resourceService::timeZoneResources)
                        .mandatory())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_NEW_PASSWORD,
                        FORM_PASSWORD_TEXT_KEY)
                        .asPasswordField()
                        .mandatory())
                .addField(FormBuilder.text(
                        PasswordChange.ATTR_NAME_CONFIRM_NEW_PASSWORD,
                        FORM_PASSWORD_CONFIRM_TEXT_KEY)
                        .asPasswordField()
                        .mandatory())

                .build();

        final Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 20;
        gridLayout.marginTop = 0;
        gridLayout.marginBottom = 20;
        buttons.setLayout(gridLayout);
        buttons.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.LOGIN_BACK.key);

        final Button registerButton = this.widgetFactory.buttonLocalized(buttons, ACTION_CREATE);
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        registerButton.setLayoutData(gridData);
        registerButton.addListener(SWT.Selection, event -> {

            registerForm.getForm().clearErrors();
            final Result<UserInfo> result = this.pageService
                    .getRestService()
                    .getBuilder(RegisterNewUser.class)
                    .withRestTemplate(this.restTemplate)
                    .withFormBinding(registerForm.getForm())
                    .call()
                    .onError(registerForm::handleError);

            if (result.hasError()) {
                return;
            }

            pageContext.forwardToLoginPage();
            pageContext.publishPageMessage(MESSAGE_SUCCESS_TILE, MESSAGE_SUCCESS_TEXT);

        });

        final Button cancelButton = this.widgetFactory.buttonLocalized(buttons, ACTION_CANCEL);
        gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        gridData.verticalIndent = 10;
        cancelButton.setLayoutData(gridData);
        cancelButton.addListener(SWT.Selection, event -> pageContext.forwardToLoginPage());

    }

}
