/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
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
    private static final LocTextKey SURNAME_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.column.surname");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.useraccount.list.title");
    private static final LocTextKey NO_EDIT_RIGHT_MESSAGE =
            new LocTextKey("sebserver.useraccount.info.notEditable");

    // filter attribute models
    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, Entity.FILTER_ATTR_NAME);
    private final TableFilterAttribute surnameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, UserInfo.FILTER_ATTR_SURNAME);
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
    private final boolean multilingual;

    protected UserAccountList(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize,
            @Value("${sebserver.gui.multilingual:false}") final Boolean ml) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;
        this.multilingual = BooleanUtils.isTrue(ml);

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
                .withDefaultSort(isSEBAdmin.getAsBoolean() ? Domain.USER.ATTR_INSTITUTION_ID : Domain.USER.ATTR_NAME)
                .withColumnIf(
                        isSEBAdmin,
                        () -> new ColumnDefinition<>(
                                Domain.USER.ATTR_INSTITUTION_ID,
                                INSTITUTION_TEXT_KEY,
                                userInstitutionNameFunction(this.resourceService))
                                        .withFilter(this.institutionFilter)
                                        .widthProportion(2)
                                        .sortable())

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_NAME,
                        NAME_TEXT_KEY,
                        UserInfo::getName)
                                .withFilter(this.nameFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.USER.ATTR_SURNAME,
                        SURNAME_TEXT_KEY,
                        UserInfo::getSurname)
                                .withFilter(this.surnameFilter)
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

                .withColumnIf(() -> this.multilingual,
                        () -> new ColumnDefinition<>(
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
                        this.pageService.getResourceService().<UserInfo> localizedActivityFunction())
                                .sortable()
                                .withFilter(this.activityFilter)
                                .widthProportion(1))

                .withDefaultAction(actionBuilder
                        .newAction(ActionDefinition.USER_ACCOUNT_VIEW_FROM_LIST)
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        ActionDefinition.USER_ACCOUNT_TOGGLE_ACTIVITY,
                        ActionDefinition.USER_ACCOUNT_ACTIVATE,
                        ActionDefinition.USER_ACCOUNT_DEACTIVATE,
                        pageContext,
                        ActionDefinition.USER_ACCOUNT_VIEW_FROM_LIST,
                        ActionDefinition.USER_ACCOUNT_MODIFY_FROM_LIST,
                        ActionDefinition.USER_ACCOUNT_TOGGLE_ACTIVITY))

                .compose(pageContext.copyOf(content));

        // propagate content actions to action-pane
        final GrantCheck userGrant = currentUser.grantCheck(EntityType.USER);
        actionBuilder

                .newAction(ActionDefinition.USER_ACCOUNT_NEW)
                .publishIf(userGrant::iw)

                .newAction(ActionDefinition.USER_ACCOUNT_VIEW_FROM_LIST)
                .withSelect(table::getMultiSelection, PageAction::applySingleSelectionAsEntityKey,
                        EMPTY_SELECTION_TEXT_KEY)
                .publish(false)

                .newAction(ActionDefinition.USER_ACCOUNT_MODIFY_FROM_LIST)
                .withSelect(table::getMultiSelection, this::editAction, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> userGrant.im(), false)

                .newAction(ActionDefinition.USER_ACCOUNT_TOGGLE_ACTIVITY)
                .withExec(this.pageService.activationToggleActionFunction(table, EMPTY_SELECTION_TEXT_KEY))
                .withConfirm(this.pageService.confirmDeactivation(table))
                .publishIf(() -> userGrant.im(), false);
    }

    private PageAction editAction(final PageAction pageAction) {
        if (!this.resourceService.getRestService()
                .getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, pageAction.getSingleSelection().modelId)
                .call()
                .map(user -> Privilege.hasRoleBasedUserAccountEditGrant(user,
                        this.resourceService.getCurrentUser().get()))
                .getOr(false)) {
            throw new PageMessageException(NO_EDIT_RIGHT_MESSAGE);
        }

        return PageAction.applySingleSelectionAsEntityKey(pageAction);
    }

    private String getLocaleDisplayText(final UserInfo userInfo) {
        return (userInfo.language != null)
                ? userInfo.language.getDisplayLanguage(this.pageService.getI18nSupport().getUsersLanguageLocale())
                : Constants.EMPTY_NOTE;
    }

    private static Function<UserInfo, String> userInstitutionNameFunction(final ResourceService resourceService) {
        final Function<String, String> institutionNameFunction = resourceService.getInstitutionNameFunction();
        return userInfo -> institutionNameFunction.apply(String.valueOf(userInfo.institutionId));
    }

}
