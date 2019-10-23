/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.ImportExamConfig;
import ch.ethz.seb.sebserver.gui.widget.FileUploadSelection;

public final class SebExamConfigImport {

    static Function<PageAction, PageAction> importConfigFunction(final PageService pageService) {
        return action -> {

            final ModalInputDialog<FormHandle<ConfigurationNode>> dialog =
                    new ModalInputDialog<FormHandle<ConfigurationNode>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(600);

            final ImportFormContext importFormContext = new ImportFormContext(
                    pageService,
                    action.pageContext());

            dialog.open(
                    SebExamConfigPropForm.FORM_IMPORT_TEXT_KEY,
                    formHandle -> doImport(
                            pageService,
                            formHandle),
                    importFormContext::cancelUpload,
                    importFormContext);

            return action;
        };
    }

    private static final void doImport(
            final PageService pageService,
            final FormHandle<ConfigurationNode> formHandle) {

        final Form form = formHandle.getForm();
        final EntityKey entityKey = formHandle.getContext().getEntityKey();
        final Control fieldControl = form.getFieldControl(API.IMPORT_FILE_ATTR_NAME);
        final PageContext context = formHandle.getContext();
        if (fieldControl != null && fieldControl instanceof FileUploadSelection) {
            final FileUploadSelection fileUpload = (FileUploadSelection) fieldControl;
            final InputStream inputStream = fileUpload.getInputStream();
            if (inputStream != null) {
                final Configuration configuration = pageService.getRestService()
                        .getBuilder(ImportExamConfig.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .withHeader(
                                API.IMPORT_PASSWORD_ATTR_NAME,
                                form.getFieldValue(API.IMPORT_PASSWORD_ATTR_NAME))
                        .withBody(inputStream)
                        .call()
                        .get(e -> {
                            fileUpload.close();
                            return context.notifyError(e);
                        });

                if (configuration != null) {
                    context.publishInfo(SebExamConfigPropForm.FORM_IMPORT_CONFIRM_TEXT_KEY);
                }
            } else {
                formHandle.getContext().publishPageMessage(
                        new LocTextKey("sebserver.error.unexpected"),
                        new LocTextKey("Please selecte a valid SEB Exam Configuration File"));
            }
        }
    }

    private static final class ImportFormContext implements ModalInputDialogComposer<FormHandle<ConfigurationNode>> {

        private final PageService pageService;
        private final PageContext pageContext;

        private Form form = null;

        protected ImportFormContext(final PageService pageService, final PageContext pageContext) {
            this.pageService = pageService;
            this.pageContext = pageContext;
        }

        @Override
        public Supplier<FormHandle<ConfigurationNode>> compose(final Composite parent) {

            final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(parent), 4)
                    .readonly(false)
                    .addField(FormBuilder.fileUpload(
                            API.IMPORT_FILE_ATTR_NAME,
                            SebExamConfigPropForm.FORM_IMPORT_SELECT_TEXT_KEY,
                            null,
                            API.SEB_FILE_EXTENSION))
                    .addField(FormBuilder.text(
                            API.IMPORT_PASSWORD_ATTR_NAME,
                            SebExamConfigPropForm.FORM_IMPORT_PASSWORD_TEXT_KEY,
                            "").asPasswordField())
                    .build();

            this.form = formHandle.getForm();
            return () -> formHandle;
        }

        void cancelUpload() {
            if (this.form != null) {
                final Control fieldControl = this.form.getFieldControl(API.IMPORT_FILE_ATTR_NAME);
                if (fieldControl != null && fieldControl instanceof FileUploadSelection) {
                    ((FileUploadSelection) fieldControl).close();
                }
            }
        }
    }

}
