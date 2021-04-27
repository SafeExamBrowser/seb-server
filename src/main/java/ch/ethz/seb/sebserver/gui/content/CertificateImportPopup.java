/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;

@Lazy
@Component
@GuiProfile
public class CertificateImportPopup {

    private static final Logger log = LoggerFactory.getLogger(SEBExamConfigImportPopup.class);

    private final static PageMessageException MISSING_PASSWORD = new PageMessageException(
            new LocTextKey("sebserver.certificate.action.import.missing-password"));

    private final PageService pageService;

    protected CertificateImportPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public Function<PageAction, PageAction> importFunction() {
        return importFunction(null);
    }

    public Function<PageAction, PageAction> importFunction(final Supplier<String> tabSelectionSupplier) {
        return action -> {

//            final boolean newConfig = tabSelectionSupplier == null || tabSelectionSupplier.get() == null;
//            final PageContext context = (tabSelectionSupplier != null)
//                    ? action.pageContext()
//                            .withAttribute(SEBSettingsForm.ATTR_VIEW_INDEX, tabSelectionSupplier.get())
//                    : action.pageContext();
//
//            final ModalInputDialog<FormHandle<ConfigurationNode>> dialog =
//                    new ModalInputDialog<FormHandle<ConfigurationNode>>(
//                            action.pageContext().getParent().getShell(),
//                            this.pageService.getWidgetFactory())
//                                    .setLargeDialogWidth();
//
//            final ImportFormContext importFormContext = new ImportFormContext(
//                    this.pageService,
//                    context,
//                    newConfig);
//
//            dialog.open(
//                    SEBExamConfigForm.FORM_IMPORT_TEXT_KEY,
//                    (Predicate<FormHandle<ConfigurationNode>>) formHandle -> doImport(
//                            formHandle,
//                            newConfig),
//                    importFormContext::cancelUpload,
//                    importFormContext);

            return action;
        };
    }

}
