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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplateProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamTemplateProctoringSettings;

@Lazy
@Component
@GuiProfile
public class ExamTemplateProctoringSettingsPopup {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateProctoringSettingsPopup.class);

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
    private final static LocTextKey SEB_PROCTORING_FORM_APPKEY =
            new LocTextKey("sebserver.exam.proctoring.form.appkey");
    private final static LocTextKey SEB_PROCTORING_FORM_SECRET =
            new LocTextKey("sebserver.exam.proctoring.form.secret");
    private final static LocTextKey SEB_PROCTORING_FORM_SDKKEY =
            new LocTextKey("sebserver.exam.proctoring.form.sdkkey");
    private final static LocTextKey SEB_PROCTORING_FORM_SDKSECRET =
            new LocTextKey("sebserver.exam.proctoring.form.sdksecret");
    private final static LocTextKey SEB_PROCTORING_FORM_USE_ZOOM_APP_CLIENT =
            new LocTextKey("sebserver.exam.proctoring.form.useZoomAppClient");

    private final static LocTextKey SEB_PROCTORING_FORM_FEATURES =
            new LocTextKey("sebserver.exam.proctoring.form.features");

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
                                    .setDialogWidth(740)
                                    .setDialogHeight(400);

            final SEBProctoringPropertiesForm bindFormContext = new SEBProctoringPropertiesForm(
                    pageService,
                    pageContext);

            final Predicate<FormHandle<?>> doBind = formHandle -> doSaveSettings(
                    pageService,
                    pageContext,
                    formHandle);

            if (modifyGrant) {
                dialog.open(
                        SEB_PROCTORING_FORM_TITLE,
                        doBind,
                        Utils.EMPTY_EXECUTION,
                        bindFormContext);
            } else {
                dialog.open(
                        SEB_PROCTORING_FORM_TITLE,
                        pageContext,
                        pc -> bindFormContext.compose(pc.getParent()));
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

        final boolean saveOk = !pageService
                .getRestService()
                .getBuilder(SaveExamTemplateProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withBody(examProctoring)
                .call()
                .onError(formHandle::handleError)
                .hasError();

        if (saveOk) {
            final PageAction action = pageService.pageActionBuilder(pageContext)
                    .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
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

        protected SEBProctoringPropertiesForm(
                final PageService pageService,
                final PageContext pageContext) {

            this.pageService = pageService;
            this.pageContext = pageContext;

        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final RestService restService = this.pageService.getRestService();
            final ResourceService resourceService = this.pageService.getResourceService();
            final EntityKey entityKey = this.pageContext.getEntityKey();
            final boolean isReadonly = BooleanUtils.toBoolean(
                    this.pageContext.getAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY));

            final Composite content = this.pageService
                    .getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final ProctoringServiceSettings proctoringSettings = restService
                    .getBuilder(GetExamTemplateProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final PageContext formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            final FormHandle<ProctoringServiceSettings> formHandle = this.pageService.formBuilder(
                    formContext)
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
                            String.valueOf(proctoringSettings.enableProctoring)))

                    .addField(FormBuilder.singleSelection(
                            ProctoringServiceSettings.ATTR_SERVER_TYPE,
                            SEB_PROCTORING_FORM_TYPE,
                            proctoringSettings.serverType.name(),
                            resourceService::examProctoringTypeResources))

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_SERVER_URL,
                            SEB_PROCTORING_FORM_URL,
                            proctoringSettings.serverURL)
                            .mandatory())

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_APP_KEY,
                            SEB_PROCTORING_FORM_APPKEY,
                            proctoringSettings.appKey)
                            .mandatory())
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringServiceSettings.ATTR_APP_SECRET,
                            SEB_PROCTORING_FORM_SECRET,
                            (proctoringSettings.appSecret != null)
                                    ? String.valueOf(proctoringSettings.appSecret)
                                    : null)
                            .mandatory())

                    .addField(FormBuilder.text(
                            ProctoringServiceSettings.ATTR_SDK_KEY,
                            SEB_PROCTORING_FORM_SDKKEY,
                            proctoringSettings.sdkKey))
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringServiceSettings.ATTR_SDK_SECRET,
                            SEB_PROCTORING_FORM_SDKSECRET,
                            (proctoringSettings.sdkSecret != null)
                                    ? String.valueOf(proctoringSettings.sdkSecret)
                                    : null))

                    .withDefaultSpanInput(1)
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

            if (proctoringSettings.serviceInUse) {
                formHandle.getForm().getFieldInput(ProctoringServiceSettings.ATTR_SERVER_TYPE).setEnabled(false);
                formHandle.getForm().getFieldInput(ProctoringServiceSettings.ATTR_SERVER_URL).setEnabled(false);
            }

            return () -> formHandle;
        }
    }

}
