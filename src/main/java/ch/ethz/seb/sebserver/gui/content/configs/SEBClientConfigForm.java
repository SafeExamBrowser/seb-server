/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.io.IOException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBClientConfigDownload;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.ActivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.DeactivateClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.NewClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.SaveClientConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SEBClientConfigForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SEBClientConfigForm.class);

    private static final LocTextKey FORM_TITLE_NEW =
            new LocTextKey("sebserver.clientconfig.form.title.new");
    private static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.clientconfig.form.title");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.name");

    private static final LocTextKey FORM_DATE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.date");
    private static final LocTextKey CLIENT_PURPOSE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebConfigPurpose");

    private static final LocTextKey PING_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.pinginterval");
    private static final LocTextKey VDI_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.vditype");
    private static final LocTextKey VDI_EXEC_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.vdi.executable");
    private static final LocTextKey VDI_PATH_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.vdi.path");
    private static final LocTextKey VDI_ARGS_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.vdi.args");

    private static final LocTextKey FALLBACK_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.fallback");
    private static final LocTextKey FALLBACK_URL_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.fallback-url");
    private static final LocTextKey FALLBACK_TIMEOUT_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebServerFallbackTimeout");
    private static final LocTextKey FALLBACK_ATTEMPTS_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebServerFallbackAttempts");
    private static final LocTextKey FALLBACK_ATTEMPT_INTERVAL_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebServerFallbackAttemptInterval");
    private static final LocTextKey FALLBACK_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebServerFallbackPasswordHash");
    private static final LocTextKey FALLBACK_PASSWORD_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.sebServerFallbackPasswordHash.confirm");
    private static final LocTextKey QUIT_PASSWORD_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.hashedQuitPassword");
    private static final LocTextKey QUIT_PASSWORD_CONFIRM_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.hashedQuitPassword.confirm");

    private static final LocTextKey FORM_ENCRYPT_CERT_KEY =
            new LocTextKey("sebserver.clientconfig.form.certificate");
    private static final LocTextKey FORM_ENCRYPT_CERTIFICATE_ASYM =
            new LocTextKey("sebserver.clientconfig.form.type.async");
    private static final LocTextKey FORM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.encryptSecret");
    private static final LocTextKey FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.encryptSecret.confirm");

    private static final String DEFAULT_PING_INTERVAL = String.valueOf(1000);
    private static final String FALLBACK_DEFAULT_TIME = String.valueOf(30 * Constants.SECOND_IN_MILLIS);
    private static final String FALLBACK_DEFAULT_ATTEMPTS = String.valueOf(5);
    private static final String FALLBACK_DEFAULT_ATTEMPT_INTERVAL = String.valueOf(2 * Constants.SECOND_IN_MILLIS);

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final DownloadService downloadService;
    private final String downloadFileName;
    private final Cryptor cryptor;

    protected SEBClientConfigForm(
            final PageService pageService,
            final DownloadService downloadService,
            final Cryptor cryptor,
            @Value("${sebserver.gui.seb.client.config.download.filename}") final String downloadFileName) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.cryptor = cryptor;
        this.currentUser = pageService.getCurrentUser();
        this.downloadService = downloadService;
        this.downloadFileName = downloadFileName;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final UserInfo user = this.currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();

        final boolean isNew = entityKey == null;

        // get data or create new. Handle error if happen
        final SEBClientConfig clientConfig = (isNew)
                ? SEBClientConfig.createNew((parentEntityKey != null)
                        ? Long.valueOf(parentEntityKey.modelId)
                        : user.institutionId)
                : this.restService
                        .getBuilder(GetClientConfig.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.SEB_CLIENT_CONFIGURATION, error))
                        .getOrThrow();

        final EntityGrantCheck entityGrant = this.currentUser.entityGrantCheck(clientConfig);
        final boolean writeGrant = entityGrant.w();
        final boolean modifyGrant = entityGrant.m();
        final boolean isReadonly = pageContext.isReadonly();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(clientConfig.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = (isNew)
                ? FORM_TITLE_NEW
                : FORM_TITLE;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final Composite formContent = widgetFactory.voidComposite(content);

        final FormHandleAnchor formHandleAnchor = new FormHandleAnchor();
        buildForm(clientConfig, formContext, formContent, formHandleAnchor);

        final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_EXPORT)
                .withEntityKey(entityKey)
                .withExec(action -> {
                    final String downloadURL = this.downloadService.createDownloadURL(
                            entityKey.modelId,
                            SEBClientConfigDownload.class,
                            this.downloadFileName);
                    urlLauncher.openURL(downloadURL);
                    return action;
                })
                .publishIf(() -> writeGrant && isReadonly && clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, DeactivateClientConfig.class)
                .withConfirm(this.pageService.confirmDeactivation(clientConfig))
                .publishIf(() -> writeGrant && isReadonly && clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_ACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, ActivateClientConfig.class)
                .publishIf(() -> writeGrant && isReadonly && !clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_SAVE)
                .withEntityKey(entityKey)
                .withExec(action -> formHandleAnchor.formHandle.processFormSave(action))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_SAVE_AND_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(action -> formHandleAnchor.formHandle.saveAndActivate(action))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly && !clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

    private void buildForm(
            final SEBClientConfig clientConfig,
            final PageContext formContext,
            final Composite formContent,
            final FormHandleAnchor formHandleAnchor) {

        final I18nSupport i18nSupport = this.pageService.getI18nSupport();
        final boolean isReadonly = formContext.isReadonly();
        final EntityKey entityKey = formContext.getEntityKey();
        final boolean isNew = entityKey == null;
        final boolean showVDIAttrs = clientConfig.vdiType == VDIType.VM_WARE;
        final boolean showFallbackAttrs = BooleanUtils.isTrue(clientConfig.fallback);

        final CharSequence pwd = (formHandleAnchor.formHandle == null)
                ? clientConfig.getEncryptSecret()
                : this.cryptor.encrypt(clientConfig.getEncryptSecret())
                        .getOrThrow();

        PageService.clearComposite(formContent);

        final FormBuilder formBuilder = this.pageService.formBuilder(
                formContext.copyOf(formContent))
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_ID,
                        clientConfig.getModelId())
                .putStaticValue(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID,
                        String.valueOf(clientConfig.getInstitutionId()))

                .addFieldIf(() -> !isNew,
                        () -> FormBuilder.text(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE,
                                FORM_DATE_TEXT_KEY,
                                i18nSupport.formatDisplayDateWithTimeZone(clientConfig.date))
                                .readonly(true))

                .addField(FormBuilder.text(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        clientConfig.name)
                        .mandatory(!isReadonly))

                .withDefaultSpanInput(3)
                .addField(FormBuilder.singleSelection(
                        SEBClientConfig.ATTR_CONFIG_PURPOSE,
                        CLIENT_PURPOSE_TEXT_KEY,
                        clientConfig.configPurpose != null
                                ? clientConfig.configPurpose.name()
                                : SEBClientConfig.ConfigPurpose.START_EXAM.name(),
                        () -> this.pageService.getResourceService().sebClientConfigPurposeResources())
                        .mandatory(!isReadonly))
                .withDefaultSpanEmptyCell(3)

                .addField(FormBuilder.password(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET,
                        FORM_ENCRYPT_SECRET_TEXT_KEY,
                        pwd))

                .withDefaultSpanEmptyCell(3)
                .addFieldIf(
                        () -> !isReadonly,
                        () -> FormBuilder.password(
                                SEBClientConfig.ATTR_ENCRYPT_SECRET_CONFIRM,
                                FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY,
                                pwd))

                .withDefaultSpanInput(2)
                .addField(FormBuilder.singleSelection(
                        SEBClientConfig.ATTR_ENCRYPT_CERTIFICATE_ALIAS,
                        FORM_ENCRYPT_CERT_KEY,
                        clientConfig.encryptCertificateAlias,
                        () -> this.pageService.getResourceService().identityCertificatesResources()))
                .withDefaultSpanInput(3)
                .withDefaultSpanLabel(1)
                .addField(FormBuilder.checkbox(
                        SEBClientConfig.ATTR_ENCRYPT_CERTIFICATE_ASYM,
                        FORM_ENCRYPT_CERTIFICATE_ASYM,
                        (BooleanUtils.isTrue(clientConfig.encryptCertificateAsym))
                                ? Constants.TRUE_STRING
                                : Constants.FALSE_STRING)
                        .withRightLabel()
                        .withEmptyCellSeparation(false))
                .withDefaultSpanEmptyCell(1)
                .withDefaultSpanLabel(2)
                .withDefaultSpanInput(2)
                .addField(FormBuilder.text(
                        SEBClientConfig.ATTR_PING_INTERVAL,
                        PING_TEXT_KEY,
                        clientConfig.sebServerPingTime != null
                                ? String.valueOf(clientConfig.sebServerPingTime)
                                : DEFAULT_PING_INTERVAL)
                        .asNumber(this::checkNaturalNumber)
                        .mandatory(!isReadonly))
                .withDefaultSpanEmptyCell(3)

                // VDI

                .withDefaultSpanInput(2)
                .addFieldIf(
                        () -> false, // TODO skipped for version 1.2 --> 1.3 or 1.4
                        () -> FormBuilder.singleSelection(
                                SEBClientConfig.ATTR_VDI_TYPE,
                                VDI_TYPE_TEXT_KEY,
                                clientConfig.vdiType != null
                                        ? clientConfig.vdiType.name()
                                        : SEBClientConfig.VDIType.NO.name(),
                                () -> this.pageService.getResourceService().vdiTypeResources())
                                .mandatory(!isReadonly))
                .withDefaultSpanEmptyCell(3);

        // VDI Attributes

        if (showVDIAttrs) {
            formBuilder.withDefaultSpanInput(2)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_VDI_EXECUTABLE,
                            VDI_EXEC_TEXT_KEY,
                            clientConfig.vdiExecutable)
                            .mandatory(!isReadonly))
                    .withDefaultSpanEmptyCell(3)

                    .withDefaultSpanInput(4)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_VDI_PATH,
                            VDI_PATH_TEXT_KEY,
                            clientConfig.vdiPath)
                            .mandatory(!isReadonly))
                    .withDefaultSpanEmptyCell(1)

                    .withDefaultSpanInput(4)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_VDI_ARGUMENTS,
                            VDI_ARGS_TEXT_KEY,
                            clientConfig.vdiArguments)
                            .asArea()
                            .mandatory(!isReadonly))
                    .withDefaultSpanEmptyCell(1);
        }

        // Fallback

        formBuilder.addField(FormBuilder.checkbox(
                SEBClientConfig.ATTR_FALLBACK,
                FALLBACK_TEXT_KEY,
                clientConfig.fallback != null
                        ? clientConfig.fallback.toString()
                        : Constants.FALSE_STRING));

        // Fallback Attributes

        if (showFallbackAttrs) {
            formBuilder.withDefaultSpanInput(5)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_FALLBACK_START_URL,
                            FALLBACK_URL_TEXT_KEY,
                            clientConfig.fallbackStartURL)
                            .mandatory(!isReadonly))

                    .withDefaultSpanEmptyCell(1)
                    .withDefaultSpanInput(2)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_FALLBACK_ATTEMPTS,
                            FALLBACK_ATTEMPTS_TEXT_KEY,
                            clientConfig.fallbackAttempts != null
                                    ? String.valueOf(clientConfig.fallbackAttempts)
                                    : FALLBACK_DEFAULT_ATTEMPTS)
                            .asNumber(this::checkNaturalNumber)
                            .mandatory(!isReadonly))

                    .withDefaultSpanEmptyCell(4)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL,
                            FALLBACK_ATTEMPT_INTERVAL_TEXT_KEY,
                            clientConfig.fallbackAttemptInterval != null
                                    ? String.valueOf(clientConfig.fallbackAttemptInterval)
                                    : FALLBACK_DEFAULT_ATTEMPT_INTERVAL)
                            .asNumber(this::checkNaturalNumber)
                            .mandatory(!isReadonly))

                    .withDefaultSpanEmptyCell(4)
                    .withDefaultSpanLabel(2)
                    .addField(FormBuilder.text(
                            SEBClientConfig.ATTR_FALLBACK_TIMEOUT,
                            FALLBACK_TIMEOUT_TEXT_KEY,
                            clientConfig.fallbackTimeout != null
                                    ? String.valueOf(clientConfig.fallbackTimeout)
                                    : FALLBACK_DEFAULT_TIME)
                            .asNumber(this::checkNaturalNumber)
                            .mandatory(!isReadonly))

                    .withDefaultSpanEmptyCell(4)
                    .withDefaultSpanInput(3)
                    .withDefaultSpanLabel(2)
                    .addField(FormBuilder.password(
                            SEBClientConfig.ATTR_FALLBACK_PASSWORD,
                            FALLBACK_PASSWORD_TEXT_KEY,
                            clientConfig.getFallbackPassword()))
                    .withDefaultSpanEmptyCell(2)
                    .addFieldIf(
                            () -> !isReadonly,
                            () -> FormBuilder.password(
                                    SEBClientConfig.ATTR_FALLBACK_PASSWORD_CONFIRM,
                                    FALLBACK_PASSWORD_CONFIRM_TEXT_KEY,
                                    clientConfig.getFallbackPasswordConfirm()))

                    .addField(FormBuilder.password(
                            SEBClientConfig.ATTR_QUIT_PASSWORD,
                            QUIT_PASSWORD_TEXT_KEY,
                            clientConfig.getQuitPassword()))

                    .withDefaultSpanEmptyCell(2)
                    .withDefaultSpanInput(3)
                    .withDefaultSpanLabel(2)

                    .addFieldIf(
                            () -> !isReadonly,
                            () -> FormBuilder.password(
                                    SEBClientConfig.ATTR_QUIT_PASSWORD_CONFIRM,
                                    QUIT_PASSWORD_CONFIRM_TEXT_KEY,
                                    clientConfig.getQuitPasswordConfirm()));
        }

        formHandleAnchor.formHandle = formBuilder.buildFor((isNew)
                ? this.restService.getRestCall(NewClientConfig.class)
                : this.restService.getRestCall(SaveClientConfig.class));

        final Listener selectionListener = event -> {
            try {

                final Form formBinding = formHandleAnchor.formHandle.getForm();
                final String formAsJson = formBinding.getFormAsJson();
                final SEBClientConfig newClientConfig = this.pageService.getJSONMapper()
                        .readValue(formAsJson, SEBClientConfig.class);
                buildForm(newClientConfig, formContext, formContent, formHandleAnchor);
            } catch (final IOException e) {
                log.error("Failed to create new SEBClientConfig from form data: ", e);
            }
        };

        if (!isReadonly) {
            final Control fallbackInput = formHandleAnchor.formHandle
                    .getForm()
                    .getFieldInput(SEBClientConfig.ATTR_FALLBACK);
            if (fallbackInput != null) {
                fallbackInput.addListener(SWT.Selection, selectionListener);
            }
            final Control vdiInput = formHandleAnchor.formHandle
                    .getForm()
                    .getFieldInput(SEBClientConfig.ATTR_VDI_TYPE);
            if (vdiInput != null) {
                vdiInput.addListener(SWT.Selection, selectionListener);
            }
        }

        formContent.layout();
        PageService.updateScrolledComposite(formContent);
    }

    private void checkNaturalNumber(final String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        final long num = Long.parseLong(value);
        if (num < 0) {
            throw new PageMessageException("Number must be positive");
        }
    }

    private static final class FormHandleAnchor {

        FormHandle<SEBClientConfig> formHandle;

    }

}
