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

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigCreationInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.CopyConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;

final class SEBExamConfigCreationPopup {

    static final LocTextKey FORM_COPY_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.copy.dialog");
    static final LocTextKey FORM_COPY_AS_TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.action.copy-as-template.dialog");
    static final LocTextKey FORM_CREATE_FROM_TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.action.create-config.dialog");

    static Function<PageAction, PageAction> configCreationFunction(
            final PageService pageService,
            final PageContext pageContext) {

        final boolean copyAsTemplate = BooleanUtils.toBoolean(
                pageContext.getAttribute(PageContext.AttributeKeys.COPY_AS_TEMPLATE));
        final boolean createFromTemplate = BooleanUtils.toBoolean(
                pageContext.getAttribute(PageContext.AttributeKeys.CREATE_FROM_TEMPLATE));

        return action -> {

            final ModalInputDialog<FormHandle<ConfigCreationInfo>> dialog =
                    new ModalInputDialog<FormHandle<ConfigCreationInfo>>(
                            action.pageContext().getParent().getShell(),
                            pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final CreationFormContext formContext = new CreationFormContext(
                    pageService,
                    pageContext,
                    copyAsTemplate,
                    createFromTemplate);

            final Predicate<FormHandle<ConfigCreationInfo>> doCopy = formHandle -> doCreate(
                    pageService,
                    pageContext,
                    copyAsTemplate,
                    createFromTemplate,
                    formHandle);

            final LocTextKey title = (copyAsTemplate)
                    ? FORM_COPY_AS_TEMPLATE_TEXT_KEY
                    : (createFromTemplate)
                            ? FORM_CREATE_FROM_TEMPLATE_TEXT_KEY
                            : FORM_COPY_TEXT_KEY;

            dialog.open(
                    title,
                    doCopy,
                    Utils.EMPTY_EXECUTION,
                    formContext);

            return action;
        };
    }

    private static boolean doCreate(
            final PageService pageService,
            final PageContext pageContext,
            final boolean copyAsTemplate,
            final boolean createFromTemplate,
            final FormHandle<ConfigCreationInfo> formHandle) {

        // create either a new configuration form template or from other configuration
        final Class<? extends RestCall<ConfigurationNode>> restCall = (createFromTemplate)
                ? NewExamConfig.class
                : CopyConfiguration.class;

        final ConfigurationNode newConfig = pageService
                .getRestService()
                .getBuilder(restCall)
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .getOr(null);

        if (newConfig == null) {
            return false;
        }

        // view either new template or configuration
        final PageAction viewCopy = (copyAsTemplate)
                ? pageService.pageActionBuilder(pageContext)
                        .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_VIEW)
                        .withEntityKey(new EntityKey(newConfig.id, EntityType.CONFIGURATION_NODE))
                        .create()
                : pageService.pageActionBuilder(pageContext)
                        .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP)
                        .withEntityKey(new EntityKey(newConfig.id, EntityType.CONFIGURATION_NODE))
                        .create();

        pageService.executePageAction(viewCopy);

        return true;
    }

    private static final class CreationFormContext implements ModalInputDialogComposer<FormHandle<ConfigCreationInfo>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final boolean copyAsTemplate;
        private final boolean createFromTemplate;

        protected CreationFormContext(
                final PageService pageService,
                final PageContext pageContext,
                final boolean copyAsTemplate,
                final boolean createFromTemplate) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.copyAsTemplate = copyAsTemplate;
            this.createFromTemplate = createFromTemplate;
        }

        @Override
        public Supplier<FormHandle<ConfigCreationInfo>> compose(final Composite parent) {

            final Composite grid = this.pageService.getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final EntityKey entityKey = this.pageContext.getEntityKey();
            final FormHandle<ConfigCreationInfo> formHandle = this.pageService.formBuilder(
                    this.pageContext.copyOf(grid))
                    .readonly(false)
                    .putStaticValueIf(
                            () -> !this.createFromTemplate,
                            Domain.CONFIGURATION_NODE.ATTR_ID,
                            entityKey.getModelId())
                    .putStaticValue(
                            Domain.CONFIGURATION_NODE.ATTR_TYPE,
                            (this.copyAsTemplate)
                                    ? ConfigurationType.TEMPLATE.name()
                                    : ConfigurationType.EXAM_CONFIG.name())
                    .putStaticValueIf(
                            () -> this.createFromTemplate,
                            Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                            entityKey.getModelId())
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_NAME,
                            SEBExamConfigForm.FORM_NAME_TEXT_KEY))
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                            SEBExamConfigForm.FORM_DESCRIPTION_TEXT_KEY)
                            .asArea())
                    .addFieldIf(
                            () -> !this.copyAsTemplate && !this.createFromTemplate,
                            () -> FormBuilder.checkbox(
                                    ConfigCreationInfo.ATTR_COPY_WITH_HISTORY,
                                    SEBExamConfigForm.FORM_HISTORY_TEXT_KEY))
                    .build();

            return () -> formHandle;
        }

    }

}
