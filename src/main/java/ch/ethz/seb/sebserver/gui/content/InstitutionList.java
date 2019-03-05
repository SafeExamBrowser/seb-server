/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutions;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class InstitutionList implements TemplateComposer {

    private final WidgetFactory widgetFactory;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected InstitutionList(
            final WidgetFactory widgetFactory,
            final RestService restService,
            final CurrentUser currentUser) {

        this.widgetFactory = widgetFactory;
        this.restService = restService;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.institution.list.title"));

        // table
        final EntityTable<Institution> table =
                this.widgetFactory.entityTableBuilder(this.restService.getRestCall(GetInstitutions.class))
                        .withEmptyMessage(new LocTextKey("sebserver.institution.list.empty"))
                        .withPaging(3)
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_NAME,
                                new LocTextKey("sebserver.institution.list.column.name"),
                                entity -> entity.name,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_URL_SUFFIX,
                                new LocTextKey("sebserver.institution.list.column.urlSuffix"),
                                entity -> entity.urlSuffix,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_ACTIVE,
                                new LocTextKey("sebserver.institution.list.column.active"),
                                entity -> entity.active,
                                true))
                        .compose(content);

        // propagate content actions to action-pane
        final GrantCheck instGrant = this.currentUser.grantCheck(EntityType.INSTITUTION);
        final GrantCheck userGrant = this.currentUser.grantCheck(EntityType.USER);
        pageContext.clearEntityKeys()

                .createAction(ActionDefinition.INSTITUTION_NEW)
                .publishIf(instGrant::w)

                .createAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(userGrant::w)

                .createAction(ActionDefinition.INSTITUTION_VIEW_FROM_LIST)
                .withSelect(table::getSelection, Action.applySingleSelection("sebserver.institution.info.pleaseSelect"))
                .publishIf(() -> table.hasAnyContent())

                .createAction(ActionDefinition.INSTITUTION_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, Action.applySingleSelection("sebserver.institution.info.pleaseSelect"))
                .publishIf(() -> instGrant.m() && table.hasAnyContent());
        ;

    }

}
