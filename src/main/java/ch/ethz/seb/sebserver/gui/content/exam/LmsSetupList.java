/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetupPage;
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
public class LmsSetupList implements TemplateComposer {

    private static final LocTextKey NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION =
            new LocTextKey("sebserver.lmssetup.list.action.no.modify.privilege");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.info.pleaseSelect");
    private static final LocTextKey ACTIVITY_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.column.active");
    private static final LocTextKey TYPE_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.column.type");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.column.name");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.column.institution");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.empty");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.list.title");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);
    private final TableFilterAttribute typeFilter;
    private final TableFilterAttribute activityFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    protected LmsSetupList(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                LmsSetup.FILTER_ATTR_LMS_TYPE,
                this.resourceService::lmsTypeResources);

        this.activityFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                UserInfo.FILTER_ATTR_ACTIVE,
                this.resourceService::activityResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final boolean isSEBAdmin = currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<LmsSetup> table =
                this.pageService.entityTableBuilder(restService.getRestCall(GetLmsSetupPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withDefaultSort(isSEBAdmin ? Domain.LMS_SETUP.ATTR_INSTITUTION_ID : Domain.LMS_SETUP.ATTR_NAME)
                        .withColumnIf(
                                () -> isSEBAdmin,
                                () -> new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        lmsSetupInstitutionNameFunction(this.resourceService))
                                                .withFilter(this.institutionFilter)
                                                .sortable())
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_NAME,
                                NAME_TEXT_KEY,
                                LmsSetup::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_LMS_TYPE,
                                TYPE_TEXT_KEY,
                                this::lmsSetupTypeName)
                                        .withFilter(this.typeFilter)
                                        .localized()
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_ACTIVE,
                                ACTIVITY_TEXT_KEY,
                                this.pageService.getResourceService().<LmsSetup> localizedActivityFunction())
                                        .withFilter(this.activityFilter)
                                        .sortable())

                        .withDefaultAction(actionBuilder
                                .newAction(ActionDefinition.LMS_SETUP_VIEW_FROM_LIST)
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                ActionDefinition.LMS_SETUP_TOGGLE_ACTIVITY,
                                ActionDefinition.LMS_SETUP_ACTIVATE,
                                ActionDefinition.LMS_SETUP_DEACTIVATE,
                                pageContext,
                                ActionDefinition.LMS_SETUP_VIEW_FROM_LIST,
                                ActionDefinition.LMS_SETUP_MODIFY_FROM_LIST,
                                ActionDefinition.LMS_SETUP_TOGGLE_ACTIVITY))

                        .compose(pageContext.copyOf(content));

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.LMS_SETUP);
        actionBuilder

                .newAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(userGrant::iw)

                .newAction(ActionDefinition.LMS_SETUP_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelectionAsEntityKey, EMPTY_SELECTION_TEXT_KEY)
                .publish(false)

                .newAction(ActionDefinition.LMS_SETUP_MODIFY_FROM_LIST)
                .withSelect(
                        table.getGrantedSelection(currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        PageAction::applySingleSelectionAsEntityKey, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> userGrant.im(), false)

                .newAction(ActionDefinition.LMS_SETUP_TOGGLE_ACTIVITY)
                .withSelect(
                        table.getGrantedSelection(currentUser, NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION),
                        this.pageService.activationToggleActionFunction(
                                table,
                                EMPTY_SELECTION_TEXT_KEY,
                                action -> LmsSetupForm.testLmsSetup(action, null, restService)),
                        EMPTY_SELECTION_TEXT_KEY)
                .withConfirm(this.pageService.confirmDeactivation(table))
                .publishIf(() -> userGrant.iw(), false);

    }

    private String lmsSetupTypeName(final LmsSetup lmsSetup) {
        if (lmsSetup.lmsType == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText(ResourceService.LMSSETUP_TYPE_PREFIX + lmsSetup.lmsType.name());
    }

    private static Function<LmsSetup, String> lmsSetupInstitutionNameFunction(final ResourceService resourceService) {
        return lmsSetup -> resourceService.getInstitutionNameFunction()
                .apply(String.valueOf(lmsSetup.institutionId));
    }

}
