/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageUtils;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SebClientConfigDownload;
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
public class SebClientConfigForm implements TemplateComposer {

    private static final LocTextKey FORM_TITLE_NEW =
            new LocTextKey("sebserver.clientconfig.form.title.new");
    private static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.clientconfig.form.title");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.name");
    private static final LocTextKey FORM_FALLBACK_URL_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.fallback-url");
    private static final LocTextKey FORM_DATE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.date");
    private static final LocTextKey FORM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.encryptSecret");
    private static final LocTextKey FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.form.encryptSecret.confirm");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final DownloadService downloadService;
    private final String downloadFileName;

    protected SebClientConfigForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            final DownloadService downloadService,
            @Value("${sebserver.gui.seb.client.config.download.filename}") final String downloadFileName) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.downloadService = downloadService;
        this.downloadFileName = downloadFileName;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();

        final UserInfo user = this.currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();

        final boolean isNew = entityKey == null;

        // get data or create new. Handle error if happen
        final SebClientConfig clientConfig = (isNew)
                ? SebClientConfig.createNew((parentEntityKey != null)
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

        // The SebClientConfig form
        final FormHandle<SebClientConfig> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_ID,
                        clientConfig.getModelId())
                .putStaticValue(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID,
                        String.valueOf(clientConfig.getInstitutionId()))
                .addField(FormBuilder.text(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        clientConfig.name))
                .addField(FormBuilder.text(
                        SebClientConfig.ATTR_FALLBACK_START_URL,
                        FORM_FALLBACK_URL_TEXT_KEY,
                        clientConfig.fallbackStartURL))
                .addFieldIf(() -> !isNew,
                        () -> FormBuilder.text(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE,
                                FORM_DATE_TEXT_KEY,
                                i18nSupport.formatDisplayDate(clientConfig.date))
                                .readonly(true))
                .addField(FormBuilder.text(
                        Domain.SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET,
                        FORM_ENCRYPT_SECRET_TEXT_KEY)
                        .asPasswordField())
                .addField(FormBuilder.text(
                        SebClientConfig.ATTR_CONFIRM_ENCRYPT_SECRET,
                        FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY)
                        .asPasswordField())
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewClientConfig.class)
                        : this.restService.getRestCall(SaveClientConfig.class));

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
                            SebClientConfigDownload.class,
                            this.downloadFileName);
                    urlLauncher.openURL(downloadURL);
                    return action;
                })
                .publishIf(() -> writeGrant && isReadonly && clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, DeactivateClientConfig.class)
                .withConfirm(PageUtils.confirmDeactivation(clientConfig, this.restService))
                .publishIf(() -> writeGrant && isReadonly && clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_ACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(this.restService, ActivateClientConfig.class)
                .publishIf(() -> writeGrant && isReadonly && !clientConfig.isActive())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

}
