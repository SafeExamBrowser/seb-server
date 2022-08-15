/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SEBExamConfigList implements TemplateComposer {

    private static final LocTextKey NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION =
            new LocTextKey("sebserver.examconfig.list.action.no.modify.privilege");
    private static final LocTextKey EMPTY_CONFIG_LIST_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.empty");
    private static final LocTextKey TITLE_CONFIGURATION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.title");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.institution");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.name");
    private static final LocTextKey DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.description");
    private static final LocTextKey STATUS_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.status");
    private static final LocTextKey TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.column.template");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.info.pleaseSelect");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);
    private final TableFilterAttribute descFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ConfigurationNode.FILTER_ATTR_DESCRIPTION);
    private final TableFilterAttribute statusFilter;
    private final TableFilterAttribute templateFilter;

    private final PageService pageService;
    private final SEBExamConfigImportPopup sebExamConfigImportPopup;
    private final SEBExamConfigCreationPopup sebExamConfigCreationPopup;
    private final SEBExamConfigBatchStateChangePopup sebExamConfigBatchStateChangePopup;
    private final SEBExamConfigBatchResetToTemplatePopup sebExamConfigBatchResetToTemplatePopup;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;
    private final int pageSize;

    protected SEBExamConfigList(
            final PageService pageService,
            final SEBExamConfigImportPopup sebExamConfigImportPopup,
            final SEBExamConfigCreationPopup sebExamConfigCreationPopup,
            final SEBExamConfigBatchStateChangePopup sebExamConfigBatchStateChangePopup,
            final SEBExamConfigBatchResetToTemplatePopup sebExamConfigBatchResetToTemplatePopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.sebExamConfigImportPopup = sebExamConfigImportPopup;
        this.sebExamConfigCreationPopup = sebExamConfigCreationPopup;
        this.sebExamConfigBatchStateChangePopup = sebExamConfigBatchStateChangePopup;
        this.sebExamConfigBatchResetToTemplatePopup = sebExamConfigBatchResetToTemplatePopup;
        this.currentUser = pageService.getCurrentUser();
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.statusFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ConfigurationNode.FILTER_ATTR_STATUS,
                this.resourceService::examConfigStatusFilterResources);

        this.templateFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ConfigurationNode.FILTER_ATTR_TEMPLATE_ID,
                this.resourceService::getExamConfigTemplateResourcesSelection);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_CONFIGURATION_TEXT_KEY);

        final boolean isSEBAdmin = this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final PageActionBuilder pageActionBuilder =
                this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // exam configuration table
        final EntityTable<ConfigurationNode> configTable =
                this.pageService.entityTableBuilder(GetExamConfigNodePage.class)
                        .withMultiSelection()
                        .withStaticFilter(
                                Domain.CONFIGURATION_NODE.ATTR_TYPE,
                                ConfigurationType.EXAM_CONFIG.name())
                        .withEmptyMessage(EMPTY_CONFIG_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withDefaultSort(isSEBAdmin
                                ? Domain.LMS_SETUP.ATTR_INSTITUTION_ID
                                : Domain.CONFIGURATION_NODE.ATTR_NAME)
                        .withColumnIf(
                                () -> isSEBAdmin,
                                () -> new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        this.resourceService::localizedExamConfigInstitutionName)
                                                .withFilter(this.institutionFilter)
                                                .sortable())
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_NAME,
                                NAME_TEXT_KEY,
                                ConfigurationNode::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())
                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                                DESCRIPTION_TEXT_KEY,
                                ConfigurationNode::getDescription)
                                        .withFilter(this.descFilter)
                                        .sortable())
                        .withColumn(new ColumnDefinition<ConfigurationNode>(
                                Domain.CONFIGURATION_NODE.ATTR_STATUS,
                                STATUS_TEXT_KEY,
                                this.resourceService::localizedExamConfigStatusName)
                                        .withFilter(this.statusFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.CONFIGURATION_NODE.ATTR_TEMPLATE_ID,
                                TEMPLATE_TEXT_KEY,
                                this.resourceService.examConfigTemplateNameFunction())
                                        .withFilter(this.templateFilter))

                        .withDefaultAction(pageActionBuilder
                                .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP_FROM_LIST)
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP_FROM_LIST,
                                ActionDefinition.SEB_EXAM_CONFIG_MODIFY_PROP_FROM_LIST,
                                ActionDefinition.SEB_EXAM_CONFIG_COPY_CONFIG_FROM_LIST,
                                ActionDefinition.SEB_EXAM_CONFIG_BULK_STATE_CHANGE,
                                ActionDefinition.SEB_EXAM_CONFIG_BULK_RESET_TO_TEMPLATE))

                        .compose(pageContext.copyOf(content));

        final GrantCheck examConfigGrant = this.currentUser.grantCheck(EntityType.CONFIGURATION_NODE);
        pageActionBuilder
                // Exam Configuration actions...
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_NEW)
                .publishIf(examConfigGrant::iw)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP_FROM_LIST)
                .withSelect(
                        configTable::getMultiSelection,
                        PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publish(false)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_MODIFY_PROP_FROM_LIST)
                .withSelect(
                        configTable.getGrantedSelection(this.currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> examConfigGrant.im(), false)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_COPY_CONFIG_FROM_LIST)
                .withSelect(
                        configTable.getGrantedSelection(this.currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        pageAction -> {
                            this.sebExamConfigCreationPopup.configCreationFunction(
                                    pageAction.pageContext()
                                            .withEntityKey(pageAction.getSingleSelection())
                                            .withAttribute(
                                                    PageContext.AttributeKeys.COPY_AS_TEMPLATE,
                                                    Constants.FALSE_STRING)
                                            .withAttribute(
                                                    PageContext.AttributeKeys.CREATE_FROM_TEMPLATE,
                                                    Constants.FALSE_STRING))
                                    .apply(pageAction);
                            return pageAction;
                        },
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> examConfigGrant.im(), false)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_BULK_STATE_CHANGE)
                .withSelect(
                        configTable::getMultiSelection,
                        this.sebExamConfigBatchStateChangePopup.popupCreationFunction(pageContext),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> examConfigGrant.im(), false)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_BULK_RESET_TO_TEMPLATE)
                .withSelect(
                        configTable::getMultiSelection,
                        this.sebExamConfigBatchResetToTemplatePopup.popupCreationFunction(pageContext),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> examConfigGrant.im(), false)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_IMPORT_TO_NEW_CONFIG)
                .withSelect(
                        configTable.getGrantedSelection(this.currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        this.sebExamConfigImportPopup.importFunction(),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(examConfigGrant::im);
    }

}
