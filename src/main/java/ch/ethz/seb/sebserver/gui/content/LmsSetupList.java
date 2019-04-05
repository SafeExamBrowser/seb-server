/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetups;
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

    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    protected LmsSetupList(
            final PageService pageService,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = resourceService;
        this.pageSize = (pageSize != null) ? pageSize : 20;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Domain.LMS_SETUP.ATTR_LMS_TYPE,
                this.resourceService::lmsTypeResources);
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
                this.pageService.entityTableBuilder(restService.getRestCall(GetLmsSetups.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withColumnIf(
                                () -> isSEBAdmin,
                                () -> new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        lmsSetupInstitutionNameFunction(this.resourceService),
                                        this.institutionFilter,
                                        false))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_NAME,
                                NAME_TEXT_KEY,
                                entity -> entity.name,
                                (isSEBAdmin) ? this.nameFilter : null,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_LMS_TYPE,
                                TYPE_TEXT_KEY,
                                this::lmsSetupTypeName,
                                (isSEBAdmin) ? this.typeFilter : null,
                                false, true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_ACTIVE,
                                ACTIVITY_TEXT_KEY,
                                entity -> entity.active,
                                true))
                        .withDefaultAction(actionBuilder
                                .newAction(ActionDefinition.LMS_SETUP_VIEW_FROM_LIST)
                                .create())
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.LMS_SETUP);
        actionBuilder

                .newAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(userGrant::iw)

                .newAction(ActionDefinition.LMS_SETUP_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> table.hasAnyContent())

                .newAction(ActionDefinition.LMS_SETUP_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> userGrant.im() && table.hasAnyContent());

    }

    private String lmsSetupTypeName(final LmsSetup lmsSetup) {
        if (lmsSetup.lmsType == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText("sebserver.lmssetup.type." + lmsSetup.lmsType.name());
    }

    private static Function<LmsSetup, String> lmsSetupInstitutionNameFunction(final ResourceService resourceService) {
        return lmsSetup -> resourceService.getInstitutionNameFunction()
                .apply(String.valueOf(lmsSetup.institutionId));
    }

}
