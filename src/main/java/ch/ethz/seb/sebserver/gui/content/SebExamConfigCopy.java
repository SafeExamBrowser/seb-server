/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCopyInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.CopyConfiguration;

public final class SebExamConfigCopy {

    static Function<PageAction, PageAction> copyConfigFunction(
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

            final Predicate<FormHandle<ConfigCopyInfo>> doCopy = formHandle -> doCopy(
                    pageService,
                    pageContext,
                    formHandle);

            dialog.open(
                    SebExamConfigPropForm.FORM_COPY_TEXT_KEY,
                    doCopy,
                    Utils.EMPTY_EXECUTION,
                    formContext);

            return action;
        };
    }

    private static final boolean doCopy(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<ConfigCopyInfo> formHandle) {

        final ConfigurationNode newConfig = pageService.getRestService().getBuilder(CopyConfiguration.class)
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .getOr(null);

        if (newConfig == null) {
            return false;
        }

        final PageAction viewNewConfig = pageService.pageActionBuilder(pageContext)
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP)
                .withEntityKey(new EntityKey(newConfig.id, EntityType.CONFIGURATION_NODE))
                .create();

        pageService.executePageAction(viewNewConfig);

        return true;
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
                    .addField(FormBuilder.checkbox(
                            ConfigCopyInfo.ATTR_COPY_WITH_HISTORY,
                            SebExamConfigPropForm.FORM_HISTORY_TEXT_KEY))
                    .build();

            return () -> formHandle;
        }

    }

}
