/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessageError;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodeNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportExamConfigOnExistingConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportNewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigHistory;
import ch.ethz.seb.sebserver.gui.widget.FileUploadSelection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SEBExamConfigImportPopup {

    private static final Logger log = LoggerFactory.getLogger(SEBExamConfigImportPopup.class);

    private static final LocTextKey EXAM_CONFIG_IMPORT_TEXT =
            new LocTextKey("sebserver.examconfig.action.import.config.text");
    private static final LocTextKey SEB_SETTINGS_IMPORT_TEXT =
            new LocTextKey("sebserver.examconfig.action.import.settings.text");
    private static final LocTextKey SEB_SETTINGS_IMPORT_AUTO_PUBLISH =
            new LocTextKey("sebserver.examconfig.action.import.auto-publish");
    private final static PageMessageException MISSING_PASSWORD = new PageMessageException(
            new LocTextKey("sebserver.examconfig.action.import.missing-password"));
    private static final LocTextKey MESSAGE_SAVE_INTEGRITY_VIOLATION =
            new LocTextKey("sebserver.examconfig.action.saveToHistory.integrity-violation");

    private final static String AUTO_PUBLISH_FLAG = "AUTO_PUBLISH_FLAG";

    private final PageService pageService;

    protected SEBExamConfigImportPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public Function<PageAction, PageAction> importFunction() {
        return importFunction(null);
    }

    public Function<PageAction, PageAction> importFunction(final Supplier<String> tabSelectionSupplier) {
        return action -> {

            final boolean newConfig = tabSelectionSupplier == null || tabSelectionSupplier.get() == null;
            final PageContext context = (tabSelectionSupplier != null)
                    ? action.pageContext()
                            .withAttribute(SEBSettingsForm.ATTR_VIEW_INDEX, tabSelectionSupplier.get())
                    : action.pageContext();

            final ModalInputDialog<FormHandle<ConfigurationNode>> dialog =
                    new ModalInputDialog<FormHandle<ConfigurationNode>>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final ImportFormContext importFormContext = new ImportFormContext(
                    this.pageService,
                    context,
                    newConfig);

            dialog.open(
                    newConfig
                            ? SEBExamConfigForm.FORM_IMPORT_CONFIG_TEXT_KEY
                            : SEBExamConfigForm.FORM_IMPORT_SETTINGS_TEXT_KEY,
                    (Predicate<FormHandle<ConfigurationNode>>) formHandle -> doImport(
                            formHandle,
                            newConfig),
                    importFormContext::cancelUpload,
                    importFormContext);

            return action;
        };
    }

    private boolean doImport(
            final FormHandle<ConfigurationNode> formHandle,
            final boolean newConfig) {

        try {
            final Form form = formHandle.getForm();
            final EntityKey entityKey = formHandle.getContext().getEntityKey();
            final Control fieldControl = form.getFieldInput(API.IMPORT_FILE_ATTR_NAME);
            final PageContext context = formHandle.getContext();

            if (!(fieldControl instanceof FileUploadSelection)) {
                return false;
            }

            if (!checkInput(formHandle, newConfig, form)) {
                return false;
            }

            final FileUploadSelection fileUpload = (FileUploadSelection) fieldControl;
            final InputStream inputStream = fileUpload.getInputStream();
            if (inputStream != null) {
                final RestCall<Configuration>.RestCallBuilder restCall = (newConfig)
                        ? this.pageService.getRestService()
                                .getBuilder(ImportNewExamConfig.class)
                        : this.pageService.getRestService()
                                .getBuilder(ImportExamConfigOnExistingConfig.class);

                restCall
                        .withHeader(
                                API.IMPORT_PASSWORD_ATTR_NAME,
                                form.getFieldValue(API.IMPORT_PASSWORD_ATTR_NAME))
                        .withBody(inputStream);

                if (newConfig) {
                    restCall
                            .withHeader(
                                    Domain.CONFIGURATION_NODE.ATTR_NAME,
                                    form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_NAME))
                            .withHeader(
                                    Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                    form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION))
                            .withHeader(
                                    Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                    form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID));
                } else {
                    restCall.withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId);
                }

                final Result<Configuration> importResult = restCall.call();
                if (!importResult.hasError()) {
                    context.publishInfo(SEBExamConfigForm.FORM_IMPORT_CONFIRM_TEXT_KEY);

                    // Auto publish?
                    if (!newConfig && BooleanUtils.toBoolean(form.getFieldValue(AUTO_PUBLISH_FLAG))) {
                        this.pageService.getRestService()
                                .getBuilder(SaveExamConfigHistory.class)
                                .withURIVariable(API.PARAM_MODEL_ID, importResult.get().getModelId())
                                .call()
                                .onError(error -> notifyErrorOnSave(error, context));
                    }
                } else {
                    handleImportError(formHandle, importResult);
                }

                reloadPage(newConfig, context);
                return true;
            } else {
                formHandle.getContext().publishPageMessage(
                        SEBExamConfigForm.FORM_IMPORT_ERROR_TITLE,
                        SEBExamConfigForm.FORM_IMPORT_ERROR_FILE_SELECTION);
            }

            return false;
        } catch (final Exception e) {
            reloadPage(newConfig, formHandle.getContext());
            formHandle.getContext().notifyError(SEBExamConfigForm.FORM_TITLE, e);
            return true;
        }
    }

    private void handleImportError(
            final FormHandle<ConfigurationNode> formHandle,
            final Result<Configuration> configuration) {

        final Exception error = configuration.getError();
        if (error instanceof RestCallError) {
            ((RestCallError) error)
                    .getAPIMessages()
                    .stream()
                    .findFirst()
                    .ifPresent(message -> {
                        if (APIMessage.ErrorMessage.MISSING_PASSWORD.isOf(message)) {
                            formHandle
                                    .getContext()
                                    .publishPageMessage(MISSING_PASSWORD);
                        } else {
                            formHandle
                                    .getContext()
                                    .notifyImportError(EntityType.CONFIGURATION_NODE, error);
                        }
                    });

        } else {
            formHandle.getContext().notifyError(
                    SEBExamConfigForm.FORM_TITLE,
                    configuration.getError());
        }
    }

    private void reloadPage(final boolean newConfig, final PageContext context) {
        final PageAction action = (newConfig)
                ? this.pageService.pageActionBuilder(context)
                        .newAction(ActionDefinition.SEB_EXAM_CONFIG_IMPORT_TO_NEW_CONFIG)
                        .create()
                : this.pageService.pageActionBuilder(context)
                        .newAction(ActionDefinition.SEB_EXAM_CONFIG_MODIFY)
                        .create();

        this.pageService.firePageEvent(
                new ActionEvent(action),
                action.pageContext());
    }

    private boolean checkInput(
            final FormHandle<ConfigurationNode> formHandle,
            final boolean newConfig,
            final Form form) {

        if (newConfig) {
            formHandle.process(name -> true, Form.FormFieldAccessor::resetError);
            final String fieldValue = form.getFieldValue(Domain.CONFIGURATION_NODE.ATTR_NAME);
            if (StringUtils.isBlank(fieldValue)) {
                form.setFieldError(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        this.pageService
                                .getI18nSupport()
                                .getText(SEBExamConfigForm.FIELD_VAL_NOT_NULL_KEY));
                return false;
            } else if (fieldValue.length() < 3 || fieldValue.length() > 255) {
                form.setFieldError(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        this.pageService
                                .getI18nSupport()
                                .getText(new LocTextKey("sebserver.form.validation.fieldError.size",
                                        null,
                                        null,
                                        null,
                                        3,
                                        255)));
                return false;
            } else {
                // check if name already exists
                try {
                    if (this.pageService.getRestService()
                            .getBuilder(GetExamConfigNodeNames.class)
                            .call()
                            .getOrThrow()
                            .stream()
                            .filter(n -> n.name.equals(fieldValue))
                            .findFirst()
                            .isPresent()) {

                        form.setFieldError(
                                Domain.CONFIGURATION_NODE.ATTR_NAME,
                                this.pageService
                                        .getI18nSupport()
                                        .getText(SEBExamConfigForm.FIELD_VAL_UNIQUE_NAME));
                        return false;
                    }
                } catch (final Exception e) {
                    log.error("Failed to verify unique name: {}", e.getMessage());
                }
            }
        }
        return true;
    }

    private final class ImportFormContext implements ModalInputDialogComposer<FormHandle<ConfigurationNode>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final boolean newConfig;

        private Form form = null;

        protected ImportFormContext(
                final PageService pageService,
                final PageContext pageContext,
                final boolean newConfig) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.newConfig = newConfig;
        }

        @Override
        public Supplier<FormHandle<ConfigurationNode>> compose(final Composite parent) {

            final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
            final Composite grid = widgetFactory.createPopupScrollComposite(parent);

            final Label info = widgetFactory.labelLocalized(
                    grid,
                    (this.newConfig) ? EXAM_CONFIG_IMPORT_TEXT : SEB_SETTINGS_IMPORT_TEXT,
                    true);
            final GridData gridData = new GridData(0, 0, this.newConfig, this.newConfig);
            gridData.horizontalIndent = 10;
            gridData.verticalIndent = 10;
            info.setLayoutData(gridData);

            final ResourceService resourceService = this.pageService.getResourceService();
            final List<Tuple<String>> examConfigTemplateResources = resourceService.getExamConfigTemplateResources();
            final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(grid))
                    .readonly(false)
                    .addField(FormBuilder.fileUpload(
                            API.IMPORT_FILE_ATTR_NAME,
                            SEBExamConfigForm.FORM_IMPORT_SELECT_TEXT_KEY,
                            null,
                            API.SEB_FILE_EXTENSION))

                    .addFieldIf(
                            () -> !this.newConfig,
                            () -> FormBuilder.checkbox(
                                    AUTO_PUBLISH_FLAG,
                                    SEB_SETTINGS_IMPORT_AUTO_PUBLISH,
                                    Constants.FALSE_STRING))

                    .addFieldIf(
                            () -> this.newConfig,
                            () -> FormBuilder.text(
                                    Domain.CONFIGURATION_NODE.ATTR_NAME,
                                    SEBExamConfigForm.FORM_NAME_TEXT_KEY))
                    .addFieldIf(
                            () -> this.newConfig,
                            () -> FormBuilder.text(
                                    Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                    SEBExamConfigForm.FORM_DESCRIPTION_TEXT_KEY)
                                    .asArea())
                    .addFieldIf(
                            () -> this.newConfig && !examConfigTemplateResources.isEmpty(),
                            () -> FormBuilder.singleSelection(
                                    Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                    SEBExamConfigForm.FORM_TEMPLATE_TEXT_KEY,
                                    null,
                                    resourceService::getExamConfigTemplateResources))

                    .addField(FormBuilder.text(
                            API.IMPORT_PASSWORD_ATTR_NAME,
                            SEBExamConfigForm.FORM_IMPORT_PASSWORD_TEXT_KEY,
                            "").asPasswordField())
                    .build();

            this.form = formHandle.getForm();
            return () -> formHandle;
        }

        void cancelUpload() {
            if (this.form != null) {
                final Control fieldControl = this.form.getFieldInput(API.IMPORT_FILE_ATTR_NAME);
                if (fieldControl instanceof FileUploadSelection) {
                    ((FileUploadSelection) fieldControl).close();
                }
            }
        }
    }

    private static void notifyErrorOnSave(final Exception error, final PageContext context) {
        if (error instanceof APIMessageError) {
            try {
                final Collection<APIMessage> errorMessages = ((APIMessageError) error).getAPIMessages();
                final APIMessage apiMessage = errorMessages.iterator().next();
                if (APIMessage.ErrorMessage.INTEGRITY_VALIDATION.isOf(apiMessage)) {
                    context.publishPageMessage(new PageMessageException(MESSAGE_SAVE_INTEGRITY_VIOLATION));
                } else {
                    context.notifyUnexpectedError(error);
                }
            } catch (final PageMessageException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(error);
            }
        }
    }

}
