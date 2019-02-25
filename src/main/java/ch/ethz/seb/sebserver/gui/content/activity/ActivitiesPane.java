/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.impl.MainPageState;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActivitiesPane implements TemplateComposer {

    private static final String ATTR_ACTIVITY_SELECTION = "ACTIVITY_SELECTION";

    private final WidgetFactory widgetFactory;
    private final CurrentUser currentUser;

    public ActivitiesPane(
            final WidgetFactory widgetFactory,
            final CurrentUser currentUser) {

        this.widgetFactory = widgetFactory;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final UserInfo userInfo = this.currentUser
                .getOrHandleError(pageContext::logoutOnError);

        final Label activities = this.widgetFactory.labelLocalized(
                pageContext.getParent(),
                CustomVariant.TEXT_H2,
                new LocTextKey("sebserver.activitiespane.title"));
        final GridData activitiesGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        activitiesGridData.horizontalIndent = 20;
        activities.setLayoutData(activitiesGridData);

        final Tree navigation = this.widgetFactory.treeLocalized(
                pageContext.getParent(),
                SWT.SINGLE | SWT.FULL_SELECTION);
        final GridData navigationGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        //navigationGridData.horizontalIndent = 10;
        navigation.setLayoutData(navigationGridData);

        // Institution
        // If current user has SEB Server Admin role, show the Institution list
        if (userInfo.hasRole(UserRole.SEB_SERVER_ADMIN)) {
            // institutions (list) as root
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActionDefinition.INSTITUTION_VIEW_LIST.title);
            injectActivitySelection(
                    institutions,
                    pageContext.createAction(ActionDefinition.INSTITUTION_VIEW_LIST));

        } else {
            // otherwise show the form of the institution for current user
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActionDefinition.INSTITUTION_VIEW_FORM.title);
            injectActivitySelection(
                    institutions,
                    pageContext.createAction(ActionDefinition.INSTITUTION_VIEW_FORM)
                            .withEntity(userInfo.institutionId, EntityType.INSTITUTION)
                            .withAttribute(AttributeKeys.READ_ONLY, "true"));
        }

        // User Account
        // if current user has base read privilege for User Account, show list
        if (this.currentUser.hasPrivilege(PrivilegeType.READ_ONLY, EntityType.USER)) {
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActionDefinition.USER_ACCOUNT_VIEW_LIST.title);
            injectActivitySelection(
                    userAccounts,
                    pageContext.createAction(ActionDefinition.USER_ACCOUNT_VIEW_LIST));
        } else {
            // otherwise show the user account form for current user
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActionDefinition.USER_ACCOUNT_VIEW_FORM.title);
            injectActivitySelection(
                    userAccounts,
                    pageContext.createAction(ActionDefinition.USER_ACCOUNT_VIEW_FORM)
                            .withEntity(this.currentUser.get().getEntityKey())
                            .withAttribute(AttributeKeys.READ_ONLY, "true"));
        }

        navigation.addListener(SWT.Selection, event -> handleSelection(pageContext, event));
        navigation.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActionEventListener() {
                    @Override
                    public void notify(final ActionEvent event) {
                        final MainPageState mainPageState = MainPageState.get();
                        mainPageState.action = event.action;
                        if (!event.activity) {
                            final EntityKey entityKey = event.action.getEntityKey();
                            final String modelId = (entityKey != null) ? entityKey.modelId : null;
                            final TreeItem item = findItemByActionDefinition(
                                    navigation.getItems(),
                                    event.action.definition,
                                    modelId);
                            if (item != null) {
                                navigation.select(item);
                            }
                        }
                    }
                });

        // page-selection on (re)load
        final MainPageState mainPageState = MainPageState.get();

        if (mainPageState.action == null) {
            mainPageState.action = getActivitySelection(navigation.getItem(0));
        }
        pageContext.publishPageEvent(
                new ActionEvent(mainPageState.action, false));
        navigation.select(navigation.getItem(0));
    }

    private void handleSelection(final PageContext composerCtx, final Event event) {
        final TreeItem treeItem = (TreeItem) event.item;

        System.out.println("selected: " + treeItem);

        final MainPageState mainPageState = MainPageState.get();
        final Action action = getActivitySelection(treeItem);
        if (mainPageState.action.definition != action.definition) {
            mainPageState.action = action;
            composerCtx.publishPageEvent(
                    new ActionEvent(action, true));
        }
    }

    static final TreeItem findItemByActionDefinition(
            final TreeItem[] items,
            final ActionDefinition actionDefinition,
            final String modelId) {

        if (items == null) {
            return null;
        }

        for (final TreeItem item : items) {
            final Action action = getActivitySelection(item);
            final EntityKey entityKey = action.getEntityKey();
            if (action != null
                    && (action.definition == actionDefinition || action.definition == actionDefinition.activityAlias) &&
                    (entityKey == null || (modelId != null && modelId.equals(entityKey.modelId)))) {
                return item;
            }

            final TreeItem _item = findItemByActionDefinition(item.getItems(), actionDefinition, modelId);
            if (_item != null) {
                return _item;
            }
        }

        return null;
    }

    static final TreeItem findItemByActionDefinition(final TreeItem[] items, final ActionDefinition actionDefinition) {
        return findItemByActionDefinition(items, actionDefinition, null);
    }

    static final void expand(final TreeItem item) {
        if (item == null) {
            return;
        }

        item.setExpanded(true);
        expand(item.getParentItem());
    }

    public static Action getActivitySelection(final TreeItem item) {
        return (Action) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    public static void injectActivitySelection(final TreeItem item, final Action action) {
        item.setData(ATTR_ACTIVITY_SELECTION, action);
    }

}
