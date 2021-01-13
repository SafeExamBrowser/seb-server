/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveProctoringSettings;

@Lazy
@Component
@GuiProfile
public class ExamProctoringSettings {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringSettings.class);

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
        ProctoringSettings examProctoring = null;
        try {
            final Form form = formHandle.getForm();
            form.clearErrors();

            final boolean enabled = BooleanUtils.toBoolean(
                    form.getFieldValue(ProctoringSettings.ATTR_ENABLE_PROCTORING));
            final ProctoringServerType serverType = ProctoringServerType.valueOf(
                    form.getFieldValue(ProctoringSettings.ATTR_SERVER_TYPE));

            examProctoring = new ProctoringSettings(
                    Long.parseLong(entityKey.modelId),
                    enabled,
                    serverType,
                    form.getFieldValue(ProctoringSettings.ATTR_SERVER_URL),
                    Integer.parseInt(form.getFieldValue(ProctoringSettings.ATTR_COLLECTING_ROOM_SIZE)),
                    form.getFieldValue(ProctoringSettings.ATTR_APP_KEY),
                    form.getFieldValue(ProctoringSettings.ATTR_APP_SECRET));

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get settings from form: ", e);
        }

        if (examProctoring == null) {
            return false;
        }

        final boolean saveOk = !pageService
                .getRestService()
                .getBuilder(SaveProctoringSettings.class)
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

            final ProctoringSettings proctoringSettings = restService
                    .getBuilder(GetProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final PageContext formContext = this.pageContext
                    .copyOf(content)
                    .clearEntityKeys();

            final FormHandle<ProctoringSettings> formHandle = this.pageService.formBuilder(
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
                            ProctoringSettings.ATTR_ENABLE_PROCTORING,
                            SEB_PROCTORING_FORM_ENABLE,
                            String.valueOf(proctoringSettings.enableProctoring)))

                    .addField(FormBuilder.singleSelection(
                            ProctoringSettings.ATTR_SERVER_TYPE,
                            SEB_PROCTORING_FORM_TYPE,
                            proctoringSettings.serverType.name(),
                            resourceService::examProctoringTypeResources))

                    .addField(FormBuilder.text(
                            ProctoringSettings.ATTR_SERVER_URL,
                            SEB_PROCTORING_FORM_URL,
                            proctoringSettings.serverURL))
                    .withDefaultSpanInput(1)

                    .addField(FormBuilder.text(
                            ProctoringSettings.ATTR_COLLECTING_ROOM_SIZE,
                            SEB_PROCTORING_FORM_ROOM_SIZE,
                            String.valueOf(proctoringSettings.getCollectingRoomSize()))
                            .asNumber(numString -> Long.parseLong(numString)))
                    .withDefaultSpanInput(5)
                    .withDefaultSpanEmptyCell(4)

                    .addField(FormBuilder.text(
                            ProctoringSettings.ATTR_APP_KEY,
                            SEB_PROCTORING_FORM_APPKEY,
                            proctoringSettings.appKey))
                    .withEmptyCellSeparation(false)

                    .addField(FormBuilder.password(
                            ProctoringSettings.ATTR_APP_SECRET,
                            SEB_PROCTORING_FORM_SECRET,
                            (proctoringSettings.appSecret != null)
                                    ? String.valueOf(proctoringSettings.appSecret)
                                    : null))

                    .build();

            return () -> formHandle;
        }
    }

}
