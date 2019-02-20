/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.content;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.action.UserAccountActions;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccounts;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.service.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.service.table.EntityTable;
import ch.ethz.seb.sebserver.gui.service.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class UserAccountList implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(UserAccountList.class);

    private final WidgetFactory widgetFactory;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final int pageSize;

    protected UserAccountList(
            final WidgetFactory widgetFactory,
            final RestService restService,
            final CurrentUser currentUser,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.widgetFactory = widgetFactory;
        this.restService = restService;
        this.currentUser = currentUser;
        this.pageSize = (pageSize != null) ? pageSize : 20;
    }

    @Override
    public void compose(final PageContext pageContext) {

        if (log.isDebugEnabled()) {
            log.debug("Compose User Account list within PageContext: {}", pageContext);
        }

        // content page layout with title
        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.useraccount.list.title"));

        // table
        final EntityTable<UserInfo> table =
                this.widgetFactory.entityTableBuilder(this.restService.getRestCall(GetUserAccounts.class))
                        .withPaging(this.pageSize)
                        .withColumn(new ColumnDefinition<>(
                                Domain.USER.ATTR_NAME,
                                new LocTextKey("sebserver.useraccount.list.column.name"),
                                entity -> entity.name,
                                new TableFilterAttribute(CriteriaType.TEXT, Domain.USER.ATTR_NAME),
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.USER.ATTR_USERNAME,
                                new LocTextKey("sebserver.useraccount.list.column.username"),
                                entity -> entity.username,
                                new TableFilterAttribute(CriteriaType.TEXT, Domain.USER.ATTR_USERNAME),
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.USER.ATTR_EMAIL,
                                new LocTextKey("sebserver.useraccount.list.column.email"),
                                entity -> entity.email,
                                new TableFilterAttribute(CriteriaType.TEXT, Domain.USER.ATTR_EMAIL),
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.USER.ATTR_LOCALE,
                                new LocTextKey("sebserver.useraccount.list.column.language"),
                                this::getLocaleDisplayText,
                                new TableFilterAttribute(CriteriaType.COUNTRY_SELECTION, Domain.USER.ATTR_LOCALE),
                                true, true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.USER.ATTR_ACTIVE,
                                new LocTextKey("sebserver.useraccount.list.column.active"),
                                entity -> entity.active,
                                true))
                        .compose(content);

        // propagate content actions to action-pane
        pageContext.createAction(ActionDefinition.USER_ACCOUNT_NEW)
                .withExec(UserAccountActions::newUserAccount)
                .publishIf(() -> this.currentUser.hasPrivilege(PrivilegeType.WRITE, EntityType.USER))
                .createAction(ActionDefinition.USER_ACCOUNT_VIEW)
                .withSelectionSupplier(table::getSelection)
                .withExec(UserAccountActions::viewUserAccountFromList)
                .publish()
                .createAction(ActionDefinition.USER_ACCOUNT_MODIFY)
                .withSelectionSupplier(table::getSelection)
                .withExec(UserAccountActions::editUserAccountFromList)
                .publishIf(() -> this.currentUser.hasPrivilege(PrivilegeType.MODIFY, EntityType.USER));
    }

    private String getLocaleDisplayText(final UserInfo userInfo) {
        return (userInfo.locale != null)
                ? userInfo.locale.getDisplayLanguage(this.widgetFactory.getI18nSupport().getCurrentLocale())
                : null;
    }

}
