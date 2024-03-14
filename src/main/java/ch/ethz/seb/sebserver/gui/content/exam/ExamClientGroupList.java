/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.DeleteClientGroup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.GetClientGroupPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamClientGroupList implements TemplateComposer {

    private final static LocTextKey CLIENT_GROUP_LIST_TITLE_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.title");
    private final static LocTextKey CLIENT_GROUP_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CLIENT_GROUP_TYPE_COLUMN_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.column.type");
    private final static LocTextKey CLIENT_GROUP_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.column.name");
    private final static LocTextKey CLIENT_GROUP_COLOR_COLUMN_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.column.color");
    private final static LocTextKey CLIENT_GROUP_DATA_COLUMN_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.column.data");
    private final static LocTextKey CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.list.pleaseSelect");
    private static final LocTextKey CLIENT_GROUP_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.exam.clientgroup.list.empty");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
    private final RestService restService;

    public ExamClientGroupList(final PageService pageService) {
        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = pageService.getRestService();
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = pageService.getCurrentUser();
        final Composite content = pageContext.getParent();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean editable = BooleanUtils.toBoolean(pageContext.getAttribute(ExamForm.ATTR_EDITABLE));
        final boolean isLight = pageService.isLightSetup();
        final boolean clientGroupsEnabled = currentUser.isFeatureEnabled(UserFeatures.Feature.EXAM_SEB_CLIENT_GROUPS);

        // List of ClientGroups
        this.widgetFactory.addFormSubContextHeader(
                content,
                CLIENT_GROUP_LIST_TITLE_KEY,
                CLIENT_GROUP_LIST_TITLE_TOOLTIP_KEY);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));

        final EntityTable<ClientGroup> clientGroupTable =
                this.pageService
                        .entityTableBuilder(this.restService.getRestCall(GetClientGroupPage.class))
                        .withRestCallAdapter(builder -> builder.withQueryParam(
                                ClientGroup.FILTER_ATTR_EXAM_ID,
                                entityKey.modelId))
                        .withEmptyMessage(CLIENT_GROUP_EMPTY_LIST_MESSAGE)
                        .withMarkup()
                        .withPaging(100)
                        .hideNavigation()

                        .withColumn(new ColumnDefinition<>(
                                Domain.CLIENT_GROUP.ATTR_NAME,
                                CLIENT_GROUP_NAME_COLUMN_KEY,
                                ClientGroup::getName)
                                        .widthProportion(2))

                        .withColumn(new ColumnDefinition<ClientGroup>(
                                Domain.CLIENT_GROUP.ATTR_TYPE,
                                CLIENT_GROUP_TYPE_COLUMN_KEY,
                                this.resourceService::clientGroupTypeName)
                                        .widthProportion(1))

                        .withColumn(new ColumnDefinition<ClientGroup>(
                                Domain.CLIENT_GROUP.ATTR_COLOR,
                                CLIENT_GROUP_COLOR_COLUMN_KEY,
                                WidgetFactory::getColorValueHTML)
                                        .asMarkup()
                                        .widthProportion(1))

                        .withColumn(new ColumnDefinition<ClientGroup>(
                                Domain.CLIENT_GROUP.ATTR_DATA,
                                CLIENT_GROUP_DATA_COLUMN_KEY,
                                this.widgetFactory::clientGroupDataToHTML)
                                        .asMarkup()
                                        .widthProportion(3))

                        .withDefaultActionIf(
                                () -> editable && clientGroupsEnabled,
                                () -> actionBuilder
                                        .newAction(ActionDefinition.EXAM_CLIENT_GROUP_MODIFY_FROM_LIST)
                                        .withParentEntityKey(entityKey)
                                        .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.EXAM_CLIENT_GROUP_MODIFY_FROM_LIST,
                                ActionDefinition.EXAM_CLIENT_GROUP_DELETE_FROM_LIST))

                        .compose(pageContext.copyOf(content));

        actionBuilder

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_MODIFY_FROM_LIST)
                .withParentEntityKey(entityKey)
                .withSelect(
                        clientGroupTable::getMultiSelection,
                        PageAction::applySingleSelectionAsEntityKey,
                        CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> clientGroupsEnabled && editable && clientGroupTable.hasAnyContent(), false)

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_DELETE_FROM_LIST)
                .withEntityKey(entityKey)
                .withSelect(
                        clientGroupTable::getMultiSelection,
                        this::deleteSelectedClientGroup,
                        CLIENT_GROUP_EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> clientGroupsEnabled && !isLight && editable && clientGroupTable.hasAnyContent(), false)

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_NEW)
                .withParentEntityKey(entityKey)
                .publishIf(() -> clientGroupsEnabled && editable);
    }

    private PageAction deleteSelectedClientGroup(final PageAction action) {
        final EntityKey clientGroupKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteClientGroup.class)
                .withURIVariable(API.PARAM_MODEL_ID, clientGroupKey.modelId)
                .call();
        return action;
    }

}
