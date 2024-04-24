/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetExamTemplateScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.SaveExamTemplateScreenProctoringSettings;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ScreenProctoringSettingsPopup {

    private static final Logger log = LoggerFactory.getLogger(ScreenProctoringSettingsPopup.class);

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.exam.sps.form.title");
    private final static LocTextKey FORM_INFO_TITLE =
            new LocTextKey("sebserver.exam.sps.form.info.title");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.exam.sps.form.info");
    private final static LocTextKey FORM_ENABLE =
            new LocTextKey("sebserver.exam.sps.form.enable");
    private final static LocTextKey FORM_URL =
            new LocTextKey("sebserver.exam.sps.form.url");
    private final static LocTextKey FORM_APPKEY_SPS =
            new LocTextKey("sebserver.exam.sps.form.appkey");
    private final static LocTextKey FORM_APPSECRET_SPS =
            new LocTextKey("sebserver.exam.sps.form.appsecret");
    private final static LocTextKey FORM_ACCOUNT_ID_SPS =
            new LocTextKey("sebserver.exam.sps.form.accountId");
    private final static LocTextKey FORM_ACCOUNT_SECRET_SPS =
            new LocTextKey("sebserver.exam.sps.form.accountSecret");
    private final static LocTextKey BUNDLED_ACTIVATION_ERROR =
            new LocTextKey("sebserver.exam.sps.form.saveSettings.error");
    private final static LocTextKey SAVE_TEXT_KEY =
            new LocTextKey("sebserver.exam.sps.form.saveSettings");

    Function<PageAction, PageAction> settingsFunction(final PageService pageService, final boolean modifyGrant) {
        return action -> {

            final PageContext pageContext = action.pageContext()
                    .withAttribute(
                            PageContext.AttributeKeys.FORCE_READ_ONLY,
                            (modifyGrant) ? Constants.FALSE_STRING : Constants.TRUE_STRING);

            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(860);

            if (modifyGrant) {

                final BiConsumer<Composite, Supplier<FormHandle<?>>> actionComposer = (composite, handle) -> {
                    final WidgetFactory widgetFactory = pageService.getWidgetFactory();

                    final Button save = widgetFactory.buttonLocalized(composite, SAVE_TEXT_KEY);
                    save.setLayoutData(new RowData());
                    save.addListener(SWT.Selection, event -> {
                        if (doSaveSettings(pageService, pageContext, handle.get())) {
                            dialog.close();
                        }
                    });

                };

                final ScreenProctoringPropertiesForm bindFormContext = new ScreenProctoringPropertiesForm(
                        pageService,
                        pageContext);

                dialog.openWithActions(
                        FORM_TITLE,
                        actionComposer,
                        Utils.EMPTY_EXECUTION,
                        bindFormContext);
            } else {
                dialog.open(
                        FORM_TITLE,
                        pageContext,
                        pc -> new ScreenProctoringPropertiesForm(
                                pageService,
                                pageContext)
                                        .compose(pc.getParent()));
            }

            return action;
        };
    }

    private boolean doSaveSettings(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<?> formHandle) {

        final boolean isReadonly = BooleanUtils.toBoolean(
                pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));
        if (isReadonly) {
            return true;
        }

        final EntityKey entityKey = pageContext.getEntityKey();
        ScreenProctoringSettings settings = null;
        try {
            final Form form = formHandle.getForm();
            form.clearErrors();

            final boolean enabled = BooleanUtils.toBoolean(
                    form.getFieldValue(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING));
            final String groupSizeString = form.getFieldValue(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE);
            final int groupSize = StringUtils.isNotBlank(groupSizeString) ? Integer.parseInt(groupSizeString) : 0;

            settings = new ScreenProctoringSettings(
                    Long.parseLong(entityKey.modelId),
                    enabled,
                    form.getFieldValue(ScreenProctoringSettings.ATTR_SPS_SERVICE_URL),
                    form.getFieldValue(ScreenProctoringSettings.ATTR_SPS_API_KEY),
                    form.getFieldValue(ScreenProctoringSettings.ATTR_SPS_API_SECRET),
                    form.getFieldValue(ScreenProctoringSettings.ATTR_SPS_ACCOUNT_ID),
                    form.getFieldValue(ScreenProctoringSettings.ATTR_SPS_ACCOUNT_PASSWORD),
                    CollectingStrategy.EXAM,
                    groupSize);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get settings from form: ", e);
        }

        if (settings == null) {
            return false;
        }

        final Result<ScreenProctoringSettings> saveRequest = pageService
                .getRestService()
                .getBuilder(
                        entityKey.entityType == EntityType.EXAM
                                ? SaveScreenProctoringSettings.class
                                : SaveExamTemplateScreenProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(settings)
                .call();

        final boolean saveOk = !saveRequest
                .onError(formHandle::handleError)
                .hasError();

        if (saveOk) {
            final PageAction action = pageService.pageActionBuilder(pageContext)
                    .newAction(
                            entityKey.entityType == EntityType.EXAM
                                    ? ActionDefinition.EXAM_VIEW_FROM_LIST
                                    : ActionDefinition.EXAM_TEMPLATE_VIEW_FROM_LIST)
                    .create();

            pageService.firePageEvent(
                    new ActionEvent(action),
                    action.pageContext());
            return true;
        } else {
            final String bundled = formHandle.getForm().getStaticValue(ScreenProctoringSettings.ATTR_SPS_BUNDLED);
            if (bundled != null) {
                pageContext.notifyError(BUNDLED_ACTIVATION_ERROR, null);
            } else {
                pageContext.notifyActivationError(EntityType.SCREEN_PROCTORING_GROUP, saveRequest.getError());
            }
        }

        return false;
    }

    private final class ScreenProctoringPropertiesForm
            implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;

        protected ScreenProctoringPropertiesForm(
                final PageService pageService,
                final PageContext pageContext) {

            this.pageService = pageService;
            this.pageContext = pageContext;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final RestService restService = this.pageService.getRestService();
            final EntityKey entityKey = this.pageContext.getEntityKey();

            final Composite content = this.pageService
                    .getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final PageContext formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            final ScreenProctoringSettings settings = restService
                    .getBuilder(
                            entityKey.entityType == EntityType.EXAM
                                    ? GetScreenProctoringSettings.class
                                    : GetExamTemplateScreenProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final boolean isReadonly = BooleanUtils.toBoolean(
                    this.pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));

            final FormHandle<Entity> form = this.pageService.formBuilder(formContext)
                    .putStaticValueIf(
                            () -> settings.bundled,
                            ScreenProctoringSettings.ATTR_SPS_BUNDLED,
                            Constants.TRUE_STRING)
                    .withDefaultSpanInput(5)
                    .withEmptyCellSeparation(true)
                    .withDefaultSpanEmptyCell(1)
                    .readonly(isReadonly)
                    .addField(FormBuilder.text(
                            "Info",
                            FORM_INFO_TITLE,
                            this.pageService.getI18nSupport().getText(FORM_INFO))
                            .asArea(80)
                            .asHTML()
                            .readonly(true))

                    .addField(FormBuilder.checkbox(
                            ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING,
                            FORM_ENABLE,
                            String.valueOf(settings.enableScreenProctoring)))

                    .addField(FormBuilder.text(
                            ScreenProctoringSettings.ATTR_SPS_SERVICE_URL,
                            FORM_URL,
                            settings.spsServiceURL)
                            .mandatory()
                            .readonly(settings.bundled))

                    .addField(FormBuilder.text(
                            ScreenProctoringSettings.ATTR_SPS_API_KEY,
                            FORM_APPKEY_SPS,
                            settings.spsAPIKey)
                            .readonly(settings.bundled))
                    .withEmptyCellSeparation(false)

                    .addFieldIf(
                            () -> !settings.bundled,
                            () -> FormBuilder.password(
                                    ScreenProctoringSettings.ATTR_SPS_API_SECRET,
                                    FORM_APPSECRET_SPS,
                                    (settings.spsAPISecret != null)
                                            ? String.valueOf(settings.spsAPISecret)
                                            : null))

                    .addField(FormBuilder.text(
                            ScreenProctoringSettings.ATTR_SPS_ACCOUNT_ID,
                            FORM_ACCOUNT_ID_SPS,
                            settings.spsAccountId)
                            .readonly(settings.bundled))
                    .withEmptyCellSeparation(false)
                    .addFieldIf(
                            () -> !settings.bundled,
                            () -> FormBuilder.password(
                                    ScreenProctoringSettings.ATTR_SPS_ACCOUNT_PASSWORD,
                                    FORM_ACCOUNT_SECRET_SPS,
                                    (settings.spsAccountPassword != null)
                                            ? String.valueOf(settings.spsAccountPassword)
                                            : null))

                    .build();

            return () -> form;
        }
    }

}
