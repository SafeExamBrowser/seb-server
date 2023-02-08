/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ResetProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetExamTemplateProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.SaveExamTemplateProctoringSettings;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ProctoringSettingsPopup {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSettingsPopup.class);

    private final static LocTextKey SEB_PROCTORING_FORM_TITLE =
            new LocTextKey("sebserver.exam.proctoring.form.title");
    private final static LocTextKey SEB_PROCTORING_FORM_INFO =
            new LocTextKey("sebserver.exam.proctoring.form.info");
    private final static LocTextKey SEB_PROCTORING_FORM_INFO_TITLE =
            new LocTextKey("sebserver.exam.proctoring.form.info.title");
    private final static LocTextKey SEB_PROCTORING_FORM_ENABLE =
            new LocTextKey("sebserver.exam.proctoring.form.enabled");
    private final static LocTextKey SEB_PROCTORING_FORM_TYPE =
            new LocTextKey("sebserver.exam.proctoring.form.type");
    private final static LocTextKey SEB_PROCTORING_FORM_URL =
            new LocTextKey("sebserver.exam.proctoring.form.url");
    private final static LocTextKey SEB_PROCTORING_FORM_ROOM_SIZE =
            new LocTextKey("sebserver.exam.proctoring.form.collectingRoomSize");

    private final static LocTextKey SEB_PROCTORING_FORM_APPKEY_JITSI =
            new LocTextKey("sebserver.exam.proctoring.form.appkey.jitsi");
    private final static LocTextKey SEB_PROCTORING_FORM_SECRET_JITSI =
            new LocTextKey("sebserver.exam.proctoring.form.secret.jitsi");

    private final static LocTextKey SEB_PROCTORING_FORM_ACCOUNT_ID =
            new LocTextKey("sebserver.exam.proctoring.form.accountId");
    private final static LocTextKey SEB_PROCTORING_FORM_CLIENT_ID =
            new LocTextKey("sebserver.exam.proctoring.form.clientId");
    private final static LocTextKey SEB_PROCTORING_FORM_CLIENT_SECRET =
            new LocTextKey("sebserver.exam.proctoring.form.clientSecret");

    private final static LocTextKey SEB_PROCTORING_FORM_SDKKEY =
            new LocTextKey("sebserver.exam.proctoring.form.sdkkey");
    private final static LocTextKey SEB_PROCTORING_FORM_SDKSECRET =
            new LocTextKey("sebserver.exam.proctoring.form.sdksecret");
    private final static LocTextKey SEB_PROCTORING_FORM_USE_ZOOM_APP_CLIENT =
            new LocTextKey("sebserver.exam.proctoring.form.useZoomAppClient");

    private final static LocTextKey SEB_PROCTORING_FORM_FEATURES =
            new LocTextKey("sebserver.exam.proctoring.form.features");

    private final static LocTextKey SAVE_TEXT_KEY =
            new LocTextKey("sebserver.exam.proctoring.form.saveSettings");
    private final static LocTextKey RESET_TEXT_KEY =
            new LocTextKey("sebserver.exam.proctoring.form.resetSettings");
    private final static LocTextKey RESET_CONFIRM_KEY =
            new LocTextKey("sebserver.exam.proctoring.form.resetConfirm");
    private final static LocTextKey RESET_ACTIVE_CON_KEY =
            new LocTextKey("sebserver.exam.proctoring.form.resetActive");
    private final static LocTextKey RESET_SUCCESS_KEY =
            new LocTextKey("sebserver.exam.proctoring.form.resetOk");

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
                                    .setDialogWidth(860)
                                    .setDialogHeight(600);

            final ResetButtonHandler resetButtonHandler = new ResetButtonHandler();
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

                    final EntityKey entityKey = pageContext.getEntityKey();
                    if (entityKey.entityType == EntityType.EXAM) {
                        final Button reset = widgetFactory.buttonLocalized(composite, RESET_TEXT_KEY);
                        reset.setLayoutData(new RowData());
                        reset.addListener(SWT.Selection, event -> {
                            pageContext.applyConfirmDialog(RESET_CONFIRM_KEY, apply -> {
                                if (apply && doResetSettings(pageService, pageContext)) {
                                    dialog.close();
                                }
                            });
                        });
                        resetButtonHandler.set(reset);
                    }
                };

                final SEBProctoringPropertiesForm bindFormContext = new SEBProctoringPropertiesForm(
                        pageService,
                        pageContext,
                        resetButtonHandler);

                dialog.openWithActions(
                        SEB_PROCTORING_FORM_TITLE,
                        actionComposer,
                        Utils.EMPTY_EXECUTION,
                        bindFormContext);
            } else {
                dialog.open(
                        SEB_PROCTORING_FORM_TITLE,
                        pageContext,
                        pc -> new SEBProctoringPropertiesForm(
                                pageService,
                                pageContext,
                                resetButtonHandler).compose(pc.getParent()));
            }

            return action;
        };
    }

    private static final class ResetButtonHandler {

        Button resetBotton = null;
        boolean enabled = false;

        void set(final Button resetBotton) {
            this.resetBotton = resetBotton;
            resetBotton.setEnabled(this.enabled);
        }

        void enable(final boolean enable) {
            this.enabled = enable;
            if (this.resetBotton != null) {
                this.resetBotton.setEnabled(enable);
            }
        }
    }

    private boolean doResetSettings(
            final PageService pageService,
            final PageContext pageContext) {

        final EntityKey entityKey = pageContext.getEntityKey();

        return pageService.getRestService()
                .getBuilder(ResetProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .onError(error -> handleResetError(pageContext, entityKey, error))
                .onSuccess(settings -> pageContext.publishInfo(RESET_SUCCESS_KEY))
                .map(settings -> true)
                .getOr(false);
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
        ProctoringServiceSettings examProctoring = null;
        try {
            final Form form = formHandle.getForm();
            form.clearErrors();

            final boolean enabled = BooleanUtils.toBoolean(
                    form.getFieldValue(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING));
            final ProctoringServerType serverType = ProctoringServerType
                    .valueOf(form.getFieldValue(ProctoringServiceSettings.ATTR_SERVER_TYPE));

            final String features = form.getFieldValue(ProctoringServiceSettings.ATTR_ENABLED_FEATURES);
            final EnumSet<ProctoringFeature> featureFlags = (StringUtils.isNotBlank(features))
                    ? EnumSet.copyOf(Arrays.asList(StringUtils.split(features, Constants.LIST_SEPARATOR))
                            .stream()
                            .map(str -> ProctoringFeature.valueOf(str))
                            .collect(Collectors.toSet()))
                    : EnumSet.noneOf(ProctoringFeature.class);

            examProctoring = new ProctoringServiceSettings(
                    Long.parseLong(entityKey.modelId),
                    enabled,
                    serverType,
                    form.getFieldValue(ProctoringServiceSettings.ATTR_SERVER_URL),
                    Integer.parseInt(form.getFieldValue(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE)),
                    featureFlags,
                    false,
                    form.getFieldValue(ProctoringServiceSettings.ATTR_APP_KEY),
                    form.getFieldValue(ProctoringServiceSettings.ATTR_APP_SECRET),

                    form.getFieldValue(ProctoringServiceSettings.ATTR_ACCOUNT_ID),
                    form.getFieldValue(ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_ID),
                    form.getFieldValue(ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_SECRET),

                    form.getFieldValue(ProctoringServiceSettings.ATTR_SDK_KEY),
                    form.getFieldValue(ProctoringServiceSettings.ATTR_SDK_SECRET),

                    BooleanUtils.toBoolean(form.getFieldValue(
                            ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM)));

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get settings from form: ", e);
        }

        if (examProctoring == null) {
            return false;
        }

        final Result<ProctoringServiceSettings> settings = pageService
                .getRestService()
                .getBuilder(
                        entityKey.entityType == EntityType.EXAM
                                ? SaveExamProctoringSettings.class
                                : SaveExamTemplateProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(examProctoring)
                .call();

        final boolean saveOk = !settings
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
        }

        return false;
    }

    private final class SEBProctoringPropertiesForm
            implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final ResetButtonHandler resetButtonHandler;

        protected SEBProctoringPropertiesForm(
                final PageService pageService,
                final PageContext pageContext,
                final ResetButtonHandler resetButtonHandler) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.resetButtonHandler = resetButtonHandler;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final RestService restService = this.pageService.getRestService();
            final EntityKey entityKey = this.pageContext.getEntityKey();

            final Composite content = this.pageService
                    .getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final ProctoringServiceSettings proctoringSettings = restService
                    .getBuilder(
                            entityKey.entityType == EntityType.EXAM
                                    ? GetExamProctoringSettings.class
                                    : GetExamTemplateProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            this.resetButtonHandler.enable(proctoringSettings.serviceInUse);
            final FormHandleAnchor formHandleAnchor = new FormHandleAnchor();
            formHandleAnchor.formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            buildFormAccordingToService(
                    proctoringSettings,
                    proctoringSettings.serverType.name(),
                    formHandleAnchor);

            return () -> formHandleAnchor.formHandle;
        }

        private void buildFormAccordingToService(
                final ProctoringServiceSettings proctoringServiceSettings,
                final String serviceType,
                final FormHandleAnchor formHandleAnchor) {

            if (ProctoringServerType.JITSI_MEET.name().equals(serviceType)) {
                PageService.clearComposite(formHandleAnchor.formContext.getParent());
                formHandleAnchor.formHandle = buildFormForJitsi(proctoringServiceSettings, formHandleAnchor);
            } else if (ProctoringServerType.ZOOM.name().equals(serviceType)) {
                PageService.clearComposite(formHandleAnchor.formContext.getParent());
                formHandleAnchor.formHandle = buildFormForZoom(proctoringServiceSettings, formHandleAnchor);
            }

            if (proctoringServiceSettings.serviceInUse) {
                final Form form = formHandleAnchor.formHandle.getForm();
                form.getFieldInput(ProctoringServiceSettings.ATTR_SERVER_TYPE).setEnabled(false);
                form.getFieldInput(ProctoringServiceSettings.ATTR_SERVER_URL).setEnabled(false);
            }

            formHandleAnchor.formContext.getParent().getParent().getParent().layout(true, true);
        }

        private FormHandle<ProctoringServiceSettings> buildFormForJitsi(
                final ProctoringServiceSettings proctoringSettings,
                final FormHandleAnchor formHandleAnchor) {

            final ResourceService resourceService = this.pageService.getResourceService();
            final boolean isReadonly = BooleanUtils.toBoolean(
                    this.pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));

            final FormBuilder formBuilder = buildHeader(proctoringSettings, formHandleAnchor, isReadonly);

            formBuilder.addField(FormBuilder.singleSelection(
                    ProctoringServiceSettings.ATTR_SERVER_TYPE,
                    SEB_PROCTORING_FORM_TYPE,
                    ProctoringServerType.JITSI_MEET.name(),
                    resourceService::examProctoringTypeResources)
                    .withSelectionListener(form -> buildFormAccordingToService(
                            proctoringSettings,
                            form.getFieldValue(ProctoringServiceSettings.ATTR_SERVER_TYPE),
                            formHandleAnchor)))

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_SERVER_URL,
                            SEB_PROCTORING_FORM_URL,
                            proctoringSettings.serverURL)
                            .mandatory())

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_APP_KEY,
                            SEB_PROCTORING_FORM_APPKEY_JITSI,
                            proctoringSettings.appKey))
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringServiceSettings.ATTR_APP_SECRET,
                            SEB_PROCTORING_FORM_SECRET_JITSI,
                            (proctoringSettings.appSecret != null)
                                    ? String.valueOf(proctoringSettings.appSecret)
                                    : null));

            return buildFooter(proctoringSettings, resourceService, formBuilder);
        }

        private FormHandle<ProctoringServiceSettings> buildFormForZoom(
                final ProctoringServiceSettings proctoringSettings,
                final FormHandleAnchor formHandleAnchor) {

            final ResourceService resourceService = this.pageService.getResourceService();
            final boolean isReadonly = BooleanUtils.toBoolean(
                    this.pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));

            final FormBuilder formBuilder = buildHeader(proctoringSettings, formHandleAnchor, isReadonly);

            formBuilder
                    .addField(FormBuilder.singleSelection(
                            ProctoringServiceSettings.ATTR_SERVER_TYPE,
                            SEB_PROCTORING_FORM_TYPE,
                            ProctoringServerType.ZOOM.name(),
                            resourceService::examProctoringTypeResources)

                            .withSelectionListener(form -> buildFormAccordingToService(
                                    proctoringSettings,
                                    form.getFieldValue(ProctoringServiceSettings.ATTR_SERVER_TYPE),
                                    formHandleAnchor)))

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_SERVER_URL,
                            SEB_PROCTORING_FORM_URL,
                            proctoringSettings.serverURL)
                            .mandatory())

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_ACCOUNT_ID,
                            SEB_PROCTORING_FORM_ACCOUNT_ID,
                            proctoringSettings.accountId)
                            .mandatory())
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_ID,
                            SEB_PROCTORING_FORM_CLIENT_ID,
                            proctoringSettings.clientId)
                            .mandatory())
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_SECRET,
                            SEB_PROCTORING_FORM_CLIENT_SECRET,
                            (proctoringSettings.clientSecret != null)
                                    ? String.valueOf(proctoringSettings.clientSecret)
                                    : null)
                            .mandatory())

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_SDK_KEY,
                            SEB_PROCTORING_FORM_SDKKEY,
                            proctoringSettings.sdkKey)
                            .mandatory())
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringServiceSettings.ATTR_SDK_SECRET,
                            SEB_PROCTORING_FORM_SDKSECRET,
                            (proctoringSettings.sdkSecret != null)
                                    ? String.valueOf(proctoringSettings.sdkSecret)
                                    : null)
                            .mandatory());

            return buildFooter(proctoringSettings, resourceService, formBuilder);

        }

        private FormBuilder buildHeader(final ProctoringServiceSettings proctoringSettings,
                final FormHandleAnchor formHandleAnchor, final boolean isReadonly) {
            final FormBuilder formBuilder = this.pageService.formBuilder(
                    formHandleAnchor.formContext)
                    .withDefaultSpanInput(5)
                    .withEmptyCellSeparation(true)
                    .withDefaultSpanEmptyCell(1)
                    .readonly(isReadonly)

                    .addField(FormBuilder.text(
                            "Info",
                            SEB_PROCTORING_FORM_INFO_TITLE,
                            this.pageService.getI18nSupport().getText(SEB_PROCTORING_FORM_INFO))
                            .asArea(50)
                            .asHTML()
                            .readonly(true))

                    .addField(FormBuilder.checkbox(
                            ProctoringServiceSettings.ATTR_ENABLE_PROCTORING,
                            SEB_PROCTORING_FORM_ENABLE,
                            String.valueOf(proctoringSettings.enableProctoring)));
            return formBuilder;
        }

        private FormHandle<ProctoringServiceSettings> buildFooter(final ProctoringServiceSettings proctoringSettings,
                final ResourceService resourceService, final FormBuilder formBuilder) {
            return formBuilder.withDefaultSpanInput(1)
                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE,
                            SEB_PROCTORING_FORM_ROOM_SIZE,
                            String.valueOf(proctoringSettings.getCollectingRoomSize()))
                            .asNumber(numString -> Long.parseLong(numString)))
                    .withEmptyCellSeparation(true)
                    .withDefaultSpanEmptyCell(4)
                    .withDefaultSpanInput(5)

                    .addField(FormBuilder.checkbox(
                            ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM,
                            SEB_PROCTORING_FORM_USE_ZOOM_APP_CLIENT,
                            String.valueOf(proctoringSettings.useZoomAppClientForCollectingRoom)))
                    .withDefaultSpanInput(5)
                    .withEmptyCellSeparation(true)
                    .withDefaultSpanEmptyCell(1)

                    .addField(FormBuilder.multiCheckboxSelection(
                            ProctoringServiceSettings.ATTR_ENABLED_FEATURES,
                            SEB_PROCTORING_FORM_FEATURES,
                            StringUtils.join(proctoringSettings.enabledFeatures, Constants.LIST_SEPARATOR),
                            resourceService::examProctoringFeaturesResources))

                    .build();
        }
    }

    private void handleResetError(final PageContext pageContext, final EntityKey entityKey, final Exception error) {
        if (error.getMessage().contains("active connections") ||
                (error.getCause() != null &&
                        error.getCause().getMessage().contains("active connections"))) {
            pageContext.publishInfo(RESET_ACTIVE_CON_KEY);
        } else {
            log.error("Failed to rest proctoring settings for exam: {}", entityKey, error);
            pageContext.notifyUnexpectedError(error);
        }
    }

    private static final class FormHandleAnchor {
        FormHandle<ProctoringServiceSettings> formHandle;
        PageContext formContext;
    }

}
