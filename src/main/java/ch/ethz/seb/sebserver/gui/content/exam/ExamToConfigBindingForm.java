/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
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
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;

@Lazy
@Component
@GuiProfile
public class ExamToConfigBindingForm {

    private static final Logger log = LoggerFactory.getLogger(ExamToConfigBindingForm.class);

    private static final LocTextKey NEW_CONFIG_MAPPING_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.title.new");
    private static final LocTextKey CONFIG_MAPPING_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.title");
    private static final LocTextKey CONFIG_MAPPING_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.name");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.description");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.status");
    private static final LocTextKey FORM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.encryptSecret");
    private static final LocTextKey FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.form.encryptSecret.confirm");
    private final static LocTextKey CONFIG_ACTION_NO_CONFIG_MESSAGE =
            new LocTextKey("sebserver.exam.configuration.action.noconfig.message");

    private final PageService pageService;

    protected ExamToConfigBindingForm(final PageService pageService) {
        this.pageService = pageService;
    }

    Function<PageAction, PageAction> bindFunction() {
        return action -> {

            final PageContext pageContext = action.pageContext();
            final EntityKey entityKey = pageContext.getEntityKey();
            final boolean isNew = entityKey == null;

            if (isNew) {
                final boolean noConfigsAvailable = this.pageService.getResourceService()
                        .examConfigurationSelectionResources()
                        .isEmpty();

                if (noConfigsAvailable) {
                    throw new PageMessageException(CONFIG_ACTION_NO_CONFIG_MESSAGE);
                }
            }

            final ModalInputDialog<FormHandle<ExamConfigurationMap>> dialog =
                    new ModalInputDialog<FormHandle<ExamConfigurationMap>>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final BindFormContext bindFormContext = new BindFormContext(
                    this.pageService,
                    action.pageContext());

            final Predicate<FormHandle<ExamConfigurationMap>> doBind = formHandle -> doCreate(
                    this.pageService,
                    pageContext,
                    formHandle);

            // the default page layout
            final LocTextKey titleKey = (isNew)
                    ? NEW_CONFIG_MAPPING_TILE_TEXT_KEY
                    : CONFIG_MAPPING_TILE_TEXT_KEY;

            dialog.open(
                    titleKey,
                    doBind,
                    Utils.EMPTY_EXECUTION,
                    bindFormContext);

            return action;
        };
    }

    private boolean doCreate(
            final PageService pageService,
            final PageContext pageContext,
            final FormHandle<ExamConfigurationMap> formHandle) {

        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean isNew = entityKey == null;

        final Class<? extends RestCall<ExamConfigurationMap>> restCall = (isNew)
                ? NewExamConfigMapping.class
                : SaveExamConfigMapping.class;

        return !pageService
                .getRestService()
                .getBuilder(restCall)
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .map(mapping -> {
                    pageService.executePageAction(
                            pageService.pageActionBuilder(pageContext.clearEntityKeys())
                                    .newAction(ActionDefinition.EXAM_VIEW_FROM_LIST)
                                    .withEntityKey(pageContext.getParentEntityKey())
                                    .create());
                    return mapping;
                })
                .hasError();
    }

    private final class BindFormContext implements ModalInputDialogComposer<FormHandle<ExamConfigurationMap>> {

        private final PageService pageService;
        private final PageContext pageContext;

        protected BindFormContext(
                final PageService pageService,
                final PageContext pageContext) {

            this.pageService = pageService;
            this.pageContext = pageContext;

        }

        @Override
        public Supplier<FormHandle<ExamConfigurationMap>> compose(final Composite parent) {

            final Composite grid = this.pageService.getWidgetFactory()
                    .createPopupScrollComposite(parent);

            final RestService restService = this.pageService.getRestService();
            final ResourceService resourceService = this.pageService.getResourceService();

            final EntityKey entityKey = this.pageContext.getEntityKey();
            final EntityKey parentEntityKey = this.pageContext.getParentEntityKey();
            final boolean isNew = entityKey == null;

            final Exam exam = (isNew)
                    ? restService
                            .getBuilder(GetExam.class)
                            .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                            .call()
                            .onError(error -> this.pageContext.notifyLoadError(EntityType.EXAM, error))
                            .getOrThrow()
                    : null;

            // get data or create new. Handle error if happen
            final ExamConfigurationMap examConfigurationMap = (isNew)
                    ? ExamConfigurationMap.createNew(exam)
                    : restService
                            .getBuilder(GetExamConfigMapping.class)
                            .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                            .call()
                            .onError(error -> this.pageContext.notifyLoadError(
                                    EntityType.EXAM_CONFIGURATION_MAP,
                                    error))
                            .getOrThrow();

            // new PageContext with actual EntityKey
            final PageContext formContext = this.pageContext.withEntityKey(examConfigurationMap.getEntityKey());

            final FormHandle<ExamConfigurationMap> formHandle = this.pageService.formBuilder(
                    formContext.copyOf(grid))
                    .readonly(false)
                    .putStaticValueIf(() -> !isNew,
                            Domain.EXAM_CONFIGURATION_MAP.ATTR_ID,
                            examConfigurationMap.getModelId())
                    .putStaticValue(
                            Domain.EXAM_CONFIGURATION_MAP.ATTR_INSTITUTION_ID,
                            String.valueOf(examConfigurationMap.getInstitutionId()))
                    .putStaticValue(
                            Domain.EXAM_CONFIGURATION_MAP.ATTR_EXAM_ID,
                            String.valueOf(examConfigurationMap.examId))

                    .addField(FormBuilder.singleSelection(
                            Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID,
                            CONFIG_MAPPING_NAME_TEXT_KEY,
                            String.valueOf(examConfigurationMap.configurationNodeId),
                            resourceService::examConfigurationSelectionResources)
                            .withSelectionListener(form -> updateFormValuesFromConfigSelection(form, resourceService))
                            .mandatory())

                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                            FORM_DESCRIPTION_TEXT_KEY,
                            examConfigurationMap.configDescription)
                            .asArea()
                            .readonly(true))

                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_NODE.ATTR_STATUS,
                            FORM_STATUS_TEXT_KEY,
                            resourceService.localizedExamConfigStatusName(examConfigurationMap))
                            .readonly(true))

                    .addField(FormBuilder.password(
                            Domain.EXAM_CONFIGURATION_MAP.ATTR_ENCRYPT_SECRET,
                            FORM_ENCRYPT_SECRET_TEXT_KEY,
                            examConfigurationMap.encryptSecret))

                    .addField(FormBuilder.password(
                            ExamConfigurationMap.ATTR_CONFIRM_ENCRYPT_SECRET,
                            FORM_CONFIRM_ENCRYPT_SECRET_TEXT_KEY,
                            examConfigurationMap.encryptSecret))

                    .build();

            return () -> formHandle;
        }
    }

    private void updateFormValuesFromConfigSelection(final Form form, final ResourceService resourceService) {
        final String configId = form.getFieldValue(Domain.EXAM_CONFIGURATION_MAP.ATTR_CONFIGURATION_NODE_ID);
        if (StringUtils.isBlank(configId)) {
            form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION, null);
            form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS, null);
        } else {
            try {

                final ConfigurationNode configuration = resourceService
                        .getRestService()
                        .getBuilder(GetExamConfigNode.class)
                        .withURIVariable(API.PARAM_MODEL_ID, configId)
                        .call()
                        .getOrThrow();

                form.setFieldValue(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        configuration.description);
                form.setFieldValue(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        resourceService.localizedExamConfigStatusName(configuration));

            } catch (final Exception e) {
                log.error("Failed to update form values from SEB Configuration selection", e);
                form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION, null);
                form.setFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS, null);
            }
        }
    }

}
