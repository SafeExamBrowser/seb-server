/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.configs.SEBExamConfigForm;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteExamConfigMapping;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingsPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamFormConfigs implements TemplateComposer {

    private final static LocTextKey CONFIG_LIST_TITLE_KEY =
            new LocTextKey("sebserver.exam.configuration.list.title");
    private final static LocTextKey CONFIG_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.exam.configuration.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONFIG_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.name");
    private final static LocTextKey CONFIG_DESCRIPTION_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.description");
    private final static LocTextKey CONFIG_STATUS_COLUMN_KEY =
            new LocTextKey("sebserver.exam.configuration.list.column.status");
    private final static LocTextKey CONFIG_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.configuration.list.pleaseSelect");
    private static final LocTextKey CONFIG_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.exam.configuration.list.empty");
    private final static LocTextKey CONFIRM_MESSAGE_REMOVE_CONFIG =
            new LocTextKey("sebserver.exam.confirm.remove-config");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final ExamToConfigBindingForm examToConfigBindingForm;
    private final WidgetFactory widgetFactory;
    private final RestService restService;

    protected ExamFormConfigs(
            final PageService pageService,
            final ExamSEBRestrictionSettings examSEBRestrictionSettings,
            final ExamToConfigBindingForm examToConfigBindingForm,
            final DownloadService downloadService) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.examToConfigBindingForm = examToConfigBindingForm;
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = pageService.getRestService();
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = pageContext.getParent();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean editable = BooleanUtils.toBoolean(
                pageContext.getAttribute(ExamForm.ATTR_EDITABLE));
        final boolean readGrant = BooleanUtils.toBoolean(
                pageContext.getAttribute(ExamForm.ATTR_READ_GRANT));
        final ExamStatus examStatus = ExamStatus.valueOf(
                pageContext.getAttribute(ExamForm.ATTR_EXAM_STATUS));
        final boolean isExamRunning = examStatus == ExamStatus.RUNNING;

        // List of SEB Configuration
        this.widgetFactory.addFormSubContextHeader(
                content,
                CONFIG_LIST_TITLE_KEY,
                CONFIG_LIST_TITLE_TOOLTIP_KEY);

        final EntityTable<ExamConfigurationMap> configurationTable =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetExamConfigMappingsPage.class))
                        .withRestCallAdapter(builder -> builder.withQueryParam(
                                ExamConfigurationMap.FILTER_ATTR_EXAM_ID,
                                entityKey.modelId))
                        .withEmptyMessage(CONFIG_EMPTY_LIST_MESSAGE)
                        .withPaging(1)
                        .hideNavigation()
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_NAME,
                                CONFIG_NAME_COLUMN_KEY,
                                ExamConfigurationMap::getConfigName)
                                        .widthProportion(2))
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                CONFIG_DESCRIPTION_COLUMN_KEY,
                                ExamConfigurationMap::getConfigDescription)
                                        .widthProportion(4))
                        .withColumn(new ColumnDefinition<ExamConfigurationMap>(
                                Domain.CONFIGURATION_NODE.ATTR_STATUS,
                                CONFIG_STATUS_COLUMN_KEY,
                                this.resourceService::localizedExamConfigStatusName)
                                        .widthProportion(1))
                        .withDefaultActionIf(
                                () -> readGrant,
                                this::viewExamConfigPageAction)

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP,
                                ActionDefinition.EXAM_CONFIGURATION_DELETE_FROM_LIST,
                                ActionDefinition.EXAM_CONFIGURATION_EXPORT,
                                ActionDefinition.EXAM_CONFIGURATION_GET_CONFIG_KEY))

                        .compose(pageContext.copyOf(content));

        final EntityKey configMapKey = (configurationTable.hasAnyContent())
                ? new EntityKey(
                        configurationTable.getFirstRowData().configurationNodeId,
                        EntityType.CONFIGURATION_NODE)
                : null;

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));

        actionBuilder

                .newAction(ActionDefinition.EXAM_CONFIGURATION_NEW)
                .withParentEntityKey(entityKey)
                .withExec(this.examToConfigBindingForm.bindFunction())
                .noEventPropagation()
                .publishIf(() -> editable && !configurationTable.hasAnyContent())

                .newAction(ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP)
                .withParentEntityKey(entityKey)
                .withEntityKey(configMapKey)
                .publishIf(() -> readGrant && configurationTable.hasAnyContent(), false)

                .newAction(ActionDefinition.EXAM_CONFIGURATION_DELETE_FROM_LIST)
                .withEntityKey(entityKey)
                .withSelect(
                        getConfigMappingSelection(configurationTable),
                        this::deleteExamConfigMapping,
                        CONFIG_EMPTY_SELECTION_TEXT_KEY)
                .withConfirm(() -> {
                    if (isExamRunning) {
                        return CONFIRM_MESSAGE_REMOVE_CONFIG;
                    }
                    return null;
                })
                .publishIf(() -> editable && configurationTable.hasAnyContent() && editable, false)

                .newAction(ActionDefinition.EXAM_CONFIGURATION_GET_CONFIG_KEY)
                .withSelect(
                        getConfigSelection(configurationTable),
                        this::getExamConfigKey,
                        CONFIG_EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> readGrant && configurationTable.hasAnyContent(), false);

    }

    private Supplier<Set<EntityKey>> getConfigMappingSelection(
            final EntityTable<ExamConfigurationMap> configurationTable) {
        return () -> {
            final ExamConfigurationMap firstRowData = configurationTable.getFirstRowData();
            if (firstRowData == null) {
                return Collections.emptySet();
            } else {
                return new HashSet<>(Arrays.asList(firstRowData.getEntityKey()));
            }
        };
    }

    private Supplier<Set<EntityKey>> getConfigSelection(final EntityTable<ExamConfigurationMap> configurationTable) {
        return () -> {
            final ExamConfigurationMap firstRowData = configurationTable.getFirstRowData();
            if (firstRowData == null) {
                return Collections.emptySet();
            } else {
                return new HashSet<>(Arrays.asList(new EntityKey(
                        firstRowData.configurationNodeId,
                        EntityType.CONFIGURATION_NODE)));
            }
        };
    }

    private PageAction deleteExamConfigMapping(final PageAction action) {
        final EntityKey examConfigMappingKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteExamConfigMapping.class)
                .withURIVariable(API.PARAM_MODEL_ID, examConfigMappingKey.modelId)
                .call()
                .onError(error -> action.pageContext().notifyRemoveError(EntityType.EXAM_CONFIGURATION_MAP, error));
        return action;
    }

    private PageAction getExamConfigKey(final PageAction action) {
        final EntityKey examConfigMappingKey = action.getSingleSelection();
        if (examConfigMappingKey != null) {
            action.withEntityKey(examConfigMappingKey);
            return SEBExamConfigForm
                    .getConfigKeyFunction(this.pageService)
                    .apply(action);
        }

        return action;
    }

    private PageAction viewExamConfigPageAction(final EntityTable<ExamConfigurationMap> table) {

        return this.pageService.pageActionBuilder(table.getPageContext()
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA))
                .newAction(ActionDefinition.EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP)
                .withSelectionSupplier(() -> {
                    final ExamConfigurationMap selectedROWData = table.getSingleSelectedROWData();
                    final HashSet<EntityKey> result = new HashSet<>();
                    if (selectedROWData != null) {
                        result.add(new EntityKey(
                                selectedROWData.configurationNodeId,
                                EntityType.CONFIGURATION_NODE));
                    }
                    return result;
                })
                .create();
    }

}
