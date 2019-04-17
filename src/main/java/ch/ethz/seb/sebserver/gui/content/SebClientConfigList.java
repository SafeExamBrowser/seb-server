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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.clientconfig.GetClientConfigs;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class SebClientConfigList implements TemplateComposer {

    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.list.empty");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.list.title");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.list.column.institution");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.list.column.name");
    private static final LocTextKey ACTIVE_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.list.column.active");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.clientconfig.info.pleaseSelect");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);
    private final TableFilterAttribute dateFilter =
            new TableFilterAttribute(
                    CriteriaType.DATE,
                    SebClientConfig.FILTER_ATTR_CREATION_DATE,
                    DateTime.now(DateTimeZone.UTC).minusYears(1).toString(Constants.DEFAULT_DATE_TIME_FORMAT));

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;
    private final int pageSize;

    protected SebClientConfigList(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);
    }

    @Override
    public void compose(final PageContext pageContext) {

        final I18nSupport i18nSupport = this.pageService.getI18nSupport();

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final boolean isSEBAdmin = this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final PageActionBuilder pageActionBuilder =
                this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<SebClientConfig> table =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetClientConfigs.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withColumnIf(
                                () -> isSEBAdmin,
                                () -> new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        INSTITUTION_TEXT_KEY,
                                        clientConfigInstitutionNameFunction(this.resourceService),
                                        this.institutionFilter,
                                        false))
                        .withColumn(new ColumnDefinition<>(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                                NAME_TEXT_KEY,
                                entity -> entity.name,
                                this.nameFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE,
                                new LocTextKey(
                                        "sebserver.clientconfig.list.column.date",
                                        i18nSupport.getUsersTimeZoneTitleSuffix()),
                                entity -> entity.date,
                                this.dateFilter,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE,
                                ACTIVE_TEXT_KEY,
                                entity -> this.pageService
                                        .getResourceService()
                                        .localizedActivityResource().apply(entity.active),
                                true))
                        .withDefaultAction(pageActionBuilder
                                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_VIEW_FROM_LIST)
                                .create())
                        .compose(content);

        final GrantCheck clientConfigGrant = this.currentUser.grantCheck(EntityType.SEB_CLIENT_CONFIGURATION);

        pageActionBuilder

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_NEW)
                .publishIf(clientConfigGrant::w)

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> table.hasAnyContent())

                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> clientConfigGrant.m() && table.hasAnyContent());

    }

    private static Function<SebClientConfig, String> clientConfigInstitutionNameFunction(
            final ResourceService resourceService) {

        return config -> resourceService.getInstitutionNameFunction()
                .apply(String.valueOf(config.institutionId));
    }

}
