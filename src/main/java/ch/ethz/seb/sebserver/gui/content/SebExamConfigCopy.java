/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCopyInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.CopyConfiguration;

public final class SebExamConfigCopy {

    static Function<PageAction, PageAction> importConfigFunction(
            final PageService pageService,
            final PageContext pageContext) {

        return action -> {

            final ModalInputDialog<FormHandle<ConfigCopyInfo>> dialog =
                    new ModalInputDialog<FormHandle<ConfigCopyInfo>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setDialogWidth(600);

            final CopyFormContext formContext = new CopyFormContext(
                    pageService,
                    action.pageContext());

            dialog.open(
                    SebExamConfigPropForm.FORM_COPY_TEXT_KEY,
                    formHandle -> doCopy(
                            pageService,
                            pageContext,
                            formHandle),
                    Utils.EMPTY_EXECUTION,
                    formContext);

            return action;
        };
    }

    private static final void doCopy(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<ConfigCopyInfo> formHandle) {

        final ConfigurationNode newConfig = pageService.getRestService().getBuilder(CopyConfiguration.class)
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .getOrThrow();

        final PageAction viewNewConfig = pageService.pageActionBuilder(pageContext.copy().clearAttributes())
                .withEntityKey(new EntityKey(newConfig.id, EntityType.CONFIGURATION_NODE))
                .create();

        pageService.executePageAction(viewNewConfig);
    }

    private static final class CopyFormContext implements ModalInputDialogComposer<FormHandle<ConfigCopyInfo>> {

        private final PageService pageService;
        private final PageContext pageContext;

        protected CopyFormContext(final PageService pageService, final PageContext pageContext) {
            this.pageService = pageService;
            this.pageContext = pageContext;
        }

        @Override
        public Supplier<FormHandle<ConfigCopyInfo>> compose(final Composite parent) {

            final EntityKey entityKey = this.pageContext.getEntityKey();
            final FormHandle<ConfigCopyInfo> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(parent), 4)
                    .readonly(false)
                    .putStaticValue(
                            Domain.CONFIGURATION_NODE.ATTR_ID,
                            entityKey.getModelId())
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_NAME,
                            SebExamConfigPropForm.FORM_NAME_TEXT_KEY))
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                            SebExamConfigPropForm.FORM_DESCRIPTION_TEXT_KEY)
                            .asArea())
                    .build();

            return () -> formHandle;
        }

    }

}
