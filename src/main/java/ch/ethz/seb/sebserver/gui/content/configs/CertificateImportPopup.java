/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.cert.AddCertificate;
import ch.ethz.seb.sebserver.gui.widget.FileUploadSelection;

@Lazy
@Component
@GuiProfile
public class CertificateImportPopup {

    private final static PageMessageException MISSING_PASSWORD = new PageMessageException(
            new LocTextKey("sebserver.certificate.action.import.missing-password"));
    private final static LocTextKey IMPORT_POPUP_TITLE = new LocTextKey("sebserver.certificate.action.import");

    private final PageService pageService;

    protected CertificateImportPopup(final PageService pageService) {
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

            final ModalInputDialog<FormHandle<CertificateInfo>> dialog =
                    new ModalInputDialog<FormHandle<CertificateInfo>>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final ImportFormContext importFormContext = new ImportFormContext(
                    this.pageService,
                    context);

            dialog.open(
                    IMPORT_POPUP_TITLE,
                    (Predicate<FormHandle<CertificateInfo>>) formHandle -> doImport(
                            formHandle,
                            newConfig),
                    importFormContext::cancelUpload,
                    importFormContext);

            return action;
        };
    }

    private boolean doImport(
            final FormHandle<CertificateInfo> formHandle,
            final boolean newConfig) {

        try {
            final Form form = formHandle.getForm();
            final Control fieldControl = form.getFieldInput(API.IMPORT_FILE_ATTR_NAME);
            final PageContext context = formHandle.getContext();

            if (!(fieldControl instanceof FileUploadSelection)) {
                return false;
            }

            final FileUploadSelection fileUpload = (FileUploadSelection) fieldControl;
            if (!checkInput(formHandle, fileUpload, form)) {
                return false;
            }

            final InputStream inputStream = fileUpload.getInputStream();
            if (inputStream != null) {
                final Result<CertificateInfo> result = this.pageService.getRestService()
                        .getBuilder(AddCertificate.class)
                        .withHeader(
                                API.IMPORT_FILE_ATTR_NAME,
                                fileUpload.getFileName())
                        .withHeader(
                                API.IMPORT_PASSWORD_ATTR_NAME,
                                form.getFieldValue(API.IMPORT_PASSWORD_ATTR_NAME))
                        .withBody(inputStream)
                        .call();

                if (!result.hasError()) {
                    context.publishInfo(CertificateList.FORM_IMPORT_CONFIRM_TEXT_KEY);
                } else {
                    handleImportError(formHandle, result);
                }

            } else {
                formHandle.getContext().publishPageMessage(
                        CertificateList.FORM_IMPORT_ERRPR_TITLE,
                        CertificateList.FORM_IMPORT_ERROR_FILE_SELECTION);
            }

            reloadPage(newConfig, context);
            return true;
        } catch (final Exception e) {
            reloadPage(newConfig, formHandle.getContext());
            formHandle.getContext().notifyError(CertificateList.TITLE_TEXT_KEY, e);
            return true;
        }

    }

    private boolean checkInput(
            final FormHandle<CertificateInfo> formHandle,
            final FileUploadSelection fileSelection,
            final Form form) {

        if (!fileSelection.hasFileSelection()) {
            form.setFieldError(
                    API.IMPORT_FILE_ATTR_NAME,
                    this.pageService
                            .getI18nSupport()
                            .getText(CertificateList.FORM_IMPORT_NO_SELECT_TEXT_KEY));
            return false;
        }

        return true;
    }

    private void reloadPage(final boolean newConfig, final PageContext context) {
        this.pageService.firePageEvent(
                new ActionEvent(this.pageService.pageActionBuilder(context)
                        .newAction(ActionDefinition.SEB_CERTIFICATE_LIST)
                        .create()),
                context);
    }

    private void handleImportError(
            final FormHandle<CertificateInfo> formHandle,
            final Result<CertificateInfo> result) {

        final Exception error = result.getError();
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
                    CertificateList.TITLE_TEXT_KEY,
                    result.getError());
        }
    }

    private final class ImportFormContext implements ModalInputDialogComposer<FormHandle<CertificateInfo>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private Form form = null;

        protected ImportFormContext(
                final PageService pageService,
                final PageContext pageContext) {

            this.pageService = pageService;
            this.pageContext = pageContext;
        }

        @Override
        public Supplier<FormHandle<CertificateInfo>> compose(final Composite parent) {

            final Composite grid = this.pageService.getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final FormHandle<CertificateInfo> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(grid))
                    .readonly(false)
                    .addField(FormBuilder.fileUpload(
                            API.IMPORT_FILE_ATTR_NAME,
                            CertificateList.FORM_IMPORT_SELECT_TEXT_KEY,
                            null,
                            CertificateFileType.getAllExtensions()))

//                    .addField(FormBuilder.text(
//                            CertificateInfo.ATTR_ALIAS,
//                            CertificateList.FORM_ALIAS_TEXT_KEY))

                    .addField(FormBuilder.text(
                            API.IMPORT_PASSWORD_ATTR_NAME,
                            CertificateList.FORM_IMPORT_PASSWORD_TEXT_KEY,
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

}
