/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccountPage;
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
public class UserAccountList implements TemplateComposer {

    // localized text keys
    private static final LocTextKey EMPTY_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.empty");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.institution");
    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.info.pleaseSelect");
    private static final LocTextKey ACTIVE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.active");
    private static final LocTextKey LANG_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.language");
    private static final LocTextKey MAIL_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.email");
    private static final LocTextKey USER_NAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.username");
    private static final LocTextKey NAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.name");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.title");

    // filter attribute models
    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);
    private final TableFilterAttribute usernameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, UserInfo.FILTER_ATTR_USER_NAME);
    private final TableFilterAttribute mailFilter =
            new TableFilterAttribute(CriteriaType.TEXT, UserInfo.FILTER_ATTR_EMAIL);
    private final TableFilterAttribute languageFilter;
    private final TableFilterAttribute activityFilter;

    // dependencies
    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    protected UserAccountList(
            final PageService pageService,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = resourceService;
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.languageFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                UserInfo.FILTER_ATTR_LANGUAGE,
                this.resourceService::languageResources);

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

        final BooleanSupplier isSEBAdmin = () -> currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext.clearEntityKeys());

        // table
        final EntityTable<UserInfo> table = this.pageService.entityTableBuilder(
                restService.getRestCall(GetUserAccountPage.class))
                .withEmptyMessage(EMPTY_TEXT_KEY)
                .withPaging(this.pageSize)

                .withColumnIf(
                        isSEBAdmin,
                        () -> new ColumnDefinition<>(
                                Domain.USER.ATTR_INSTITUTION_ID,
                                INSTITUTION_TEXT_KEY,
                                userInstitutionNameFunction(this.resourceService))
                                        .withFilter(this.institutionFilter)
                                        .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_NAME,
                        NAME_TEXT_KEY,
                        UserInfo::getName)
                                .withFilter(this.nameFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_USERNAME,
                        USER_NAME_TEXT_KEY,
                        UserInfo::getUsername)
                                .withFilter(this.usernameFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_EMAIL,
                        MAIL_TEXT_KEY,
                        UserInfo::getEmail)
                                .withFilter(this.mailFilter)
                                .sortable()
                                .widthProportion(3))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_LANGUAGE,
                        LANG_TEXT_KEY,
                        this::getLocaleDisplayText)
                                .withFilter(this.languageFilter)
                                .localized()
                                .sortable()
                                .widthProportion(1))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_ACTIVE,
                        ACTIVE_TEXT_KEY,
                        UserInfo::getActive)
                                .sortable()
                                .withFilter(this.activityFilter)
                                .widthProportion(1))

                .withDefaultAction(actionBuilder
                        .newAction(ActionDefinition.USER_ACCOUNT_VIEW_FROM_LIST)
                        .create())
                .compose(content);

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.USER);
        actionBuilder

                .newAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(userGrant::iw)

                .newAction(ActionDefinition.USER_ACCOUNT_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> table.hasAnyContent())

                .newAction(ActionDefinition.USER_ACCOUNT_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelection, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> userGrant.im() && table.hasAnyContent());
    }

    private String getLocaleDisplayText(final UserInfo userInfo) {
        return (userInfo.language != null)
                ? userInfo.language.getDisplayLanguage(this.pageService.getI18nSupport().getCurrentLocale())
                : Constants.EMPTY_NOTE;
    }

    private static Function<UserInfo, String> userInstitutionNameFunction(final ResourceService resourceService) {
        final Function<String, String> institutionNameFunction = resourceService.getInstitutionNameFunction();
        return userInfo -> institutionNameFunction.apply(String.valueOf(userInfo.institutionId));
    }

}
