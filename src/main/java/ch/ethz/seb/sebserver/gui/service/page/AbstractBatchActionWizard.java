/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Domain.BATCH_ACTION;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardPage;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.push.UpdateErrorHandler;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.batch.DoBatchAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.batch.GetBatchAction;

public abstract class AbstractBatchActionWizard {

    protected final String BATCH_ACTION_PAGE_NAME = "BATCH_ACTION_PAGE";
    protected final String BATCH_ACTION_RESULT_PAGE_NAME = "BATCH_ACTION_RESULT_PAGE";
    protected static final String SELECTED_OBJECTS_NAME = "FORM_SELECTED_OBJECTS";
    protected static final String FORM_FAILURE_NAME = "FAILURE";
    protected static final String FORM_SUCCESS_NAME = "SUCCESS";
    protected static final String FORM_PROGRESS_NAME = "PROGRESS";

    private final static LocTextKey FORM_SELECTED_OBJECTS =
            new LocTextKey("sebserver.overall.batchaction.selected");

    protected final PageService pageService;
    protected final ServerPushService serverPushService;

    protected AbstractBatchActionWizard(
            final PageService pageService,
            final ServerPushService serverPushService) {

        this.pageService = pageService;
        this.serverPushService = serverPushService;
    }

    protected abstract LocTextKey getTitle();

    protected abstract LocTextKey getBatchActionInfo();

    protected abstract LocTextKey getBatchActionTitle();

    protected abstract BatchActionType getBatchActionType();

    protected abstract Supplier<PageContext> createResultPageSupplier(
            final PageContext pageContext,
            final FormHandle<ConfigurationNode> formHandle);

    protected abstract void extendBatchActionRequest(
            PageContext pageContext,
            RestCall<BatchAction>.RestCallBuilder batchActionRequestBuilder);

    protected abstract FormBuilder buildSpecificFormFields(
            final PageContext formContext,
            final FormBuilder formHead,
            final boolean readonly);

    public Function<PageAction, PageAction> popupCreationFunction(final PageContext pageContext) {

        return action -> {
            final Set<EntityKey> multiSelection = action.getMultiSelection();
            if (multiSelection == null || multiSelection.isEmpty()) {
                return action;
            }

            final ModalInputWizard<PageContext> wizard =
                    new ModalInputWizard<PageContext>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setVeryLargeDialogWidth();

            final WizardPage<PageContext> page1 = new WizardPage<>(
                    this.BATCH_ACTION_PAGE_NAME,
                    true,
                    (prefPageContext, content) -> composeFormPage(content, pageContext, multiSelection),
                    new WizardAction<>(getBatchActionTitle(), this.BATCH_ACTION_RESULT_PAGE_NAME));

            final WizardPage<PageContext> page2 = new WizardPage<>(
                    this.BATCH_ACTION_RESULT_PAGE_NAME,
                    false,
                    (prefPageContext, content) -> composeResultPage(content, prefPageContext, multiSelection));

            wizard.open(getTitle(), Utils.EMPTY_EXECUTION, page1, page2);

            return action;
        };
    }

    public Supplier<PageContext> composeFormPage(
            final Composite parent,
            final PageContext pageContext,
            final Set<EntityKey> multiSelection) {

        final PageService pageService = this.pageService;
        final PageContext formContext = pageContext.copyOf(parent);

        final LocTextKey info = getBatchActionInfo();
        if (info != null) {
            pageService.getWidgetFactory().labelLocalized(parent, info, true);
        }

        final FormHandle<ConfigurationNode> formHandle = getFormHeadBuilder(
                pageService,
                formContext,
                multiSelection,
                false)
                        .build();

        return createResultPageSupplier(pageContext, formHandle);
    }

    public Supplier<PageContext> composeResultPage(
            final Composite parent,
            final PageContext pageContext,
            final Set<EntityKey> multiSelection) {

        try {

            final String ids = StringUtils.join(
                    multiSelection.stream().map(key -> key.modelId).collect(Collectors.toList()),
                    Constants.LIST_SEPARATOR_CHAR);

            final RestCall<BatchAction>.RestCallBuilder batchActionRequestBuilder = this.pageService
                    .getRestService()
                    .getBuilder(DoBatchAction.class)
                    .withFormParam(BATCH_ACTION.ATTR_ACTION_TYPE, getBatchActionType().name())
                    .withFormParam(BATCH_ACTION.ATTR_SOURCE_IDS, ids);

            extendBatchActionRequest(pageContext, batchActionRequestBuilder);

            final BatchAction batchAction = batchActionRequestBuilder
                    .call()
                    .getOrThrow();

            final ProgressUpdate progressUpdate = new ProgressUpdate(batchAction.getModelId());
            final PageContext formContext = pageContext.copyOf(parent);

            final LocTextKey info = getBatchActionInfo();
            if (info != null) {
                this.pageService.getWidgetFactory().labelLocalized(parent, info, true);
            }

            final FormHandle<ConfigurationNode> formHandle = getFormHeadBuilder(
                    this.pageService,
                    formContext,
                    multiSelection,
                    true)
                            .build();

            this.serverPushService.runServerPush(
                    new ServerPushContext(
                            parent,
                            context -> !progressUpdate.isFinished(),
                            new UpdateErrorHandler(
                                    this.pageService,
                                    formContext)),
                    1000,
                    context -> progressUpdate.update(),
                    context -> updateGUI(context, formContext, progressUpdate, formHandle.getForm()));
        } catch (final Exception e) {
            pageContext.notifyUnexpectedError(e);
            throw e;
        }

        return () -> pageContext;
    }

    protected void updateGUI(
            final ServerPushContext context,
            final PageContext formContext,
            final ProgressUpdate progressCall,
            final Form form) {

        if (!progressCall.isFinished()) {
            form.setFieldValue(
                    FORM_PROGRESS_NAME,
                    progressCall.batchAction.getProgress() + " %");
        } else {
            form.setFieldValue(
                    FORM_PROGRESS_NAME,
                    "100 %");
        }

        form.setFieldValue(
                FORM_SUCCESS_NAME,
                String.valueOf(progressCall.batchAction.successful.size()));
        form.setFieldValue(
                FORM_FAILURE_NAME,
                String.valueOf(progressCall.batchAction.failures.size()));

        formContext.getParent().layout(true, true);

        this.pageService.executePageAction(this.pageService.pageActionBuilder(formContext)
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_LIST)
                .create());
    }

    protected FormBuilder getFormHeadBuilder(
            final PageService pageService,
            final PageContext formContext,
            final Set<EntityKey> multiSelection,
            final boolean readonly) {

        final FormBuilder formBuilder = pageService
                .formBuilder(formContext)
                .addField(FormBuilder.text(
                        SELECTED_OBJECTS_NAME,
                        FORM_SELECTED_OBJECTS,
                        String.valueOf(multiSelection.size()))
                        .readonly(true));

        buildSpecificFormFields(formContext, formBuilder, readonly);
        return buildProgressFields(formBuilder, readonly);
    }

    protected FormBuilder buildProgressFields(final FormBuilder formHead, final boolean readonly) {
        return formHead
                .addField(FormBuilder.text(
                        FORM_PROGRESS_NAME,
                        new LocTextKey("Progress"),
                        "0 %")
                        .readonly(true).visibleIf(!readonly))

                .addField(FormBuilder.text(
                        FORM_SUCCESS_NAME,
                        new LocTextKey("Success"),
                        "0")
                        .asNumber()
                        .readonly(true).visibleIf(!readonly))

                .addField(FormBuilder.text(
                        FORM_FAILURE_NAME,
                        new LocTextKey("Failures"),
                        "0")
                        .asNumber()
                        .readonly(true).visibleIf(!readonly));
    }

    private final class ProgressUpdate {

        final RestCall<BatchAction>.RestCallBuilder progressCall;
        private BatchAction batchAction = null;

        ProgressUpdate(final String modelId) {
            this.progressCall = AbstractBatchActionWizard.this.pageService
                    .getRestService()
                    .getBuilder(GetBatchAction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, modelId);
        }

        void update() {
            this.batchAction = this.progressCall.call().getOrThrow();
        }

        boolean isFinished() {
            return this.batchAction != null && this.batchAction.isFinished();
        }
    }

}
