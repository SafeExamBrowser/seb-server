/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;

@Lazy
@Component
@GuiProfile
public class InstitutionList implements TemplateComposer {

    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.institution.list.empty");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.institution.list.title");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.institution.list.column.name");
    private static final LocTextKey URL_TEXT_KEY =
            new LocTextKey("sebserver.institution.list.column.urlSuffix");
    private static final LocTextKey ACTIVE_TEXT_KEY =
            new LocTextKey("sebserver.institution.list.column.active");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.institution.info.pleaseSelect");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final int pageSize;

    protected InstitutionList(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.pageSize = pageSize;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final PageActionBuilder pageActionBuilder =
                this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<Institution> table =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetInstitutionPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_NAME,
                                NAME_TEXT_KEY,
                                Institution::getName)
                                        .sortable())
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_URL_SUFFIX,
                                URL_TEXT_KEY,
                                Institution::getUrlSuffix)
                                        .sortable())
                        .withColumn(new ColumnDefinition<Institution>(
                                Domain.INSTITUTION.ATTR_ACTIVE,
                                ACTIVE_TEXT_KEY,
                                entity -> this.pageService
                                        .getResourceService()
                                        .localizedActivityResource().apply(entity.active))
                                                .sortable())
                        .withDefaultAction(pageActionBuilder
                                .newAction(ActionDefinition.INSTITUTION_VIEW_FROM_LIST)
                                .create())
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck instGrant = this.currentUser.grantCheck(EntityType.INSTITUTION);
        final GrantCheck userGrant = this.currentUser.grantCheck(EntityType.USER);

        pageActionBuilder

                .newAction(ActionDefinition.INSTITUTION_NEW)
                .publishIf(instGrant::w)

                .newAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(userGrant::w)

                .newAction(ActionDefinition.INSTITUTION_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> table.hasAnyContent())

                .newAction(ActionDefinition.INSTITUTION_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> instGrant.m() && table.hasAnyContent());
        ;
    }

}
