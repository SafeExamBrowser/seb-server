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
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.PageState;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActivitiesPane implements TemplateComposer {

    private static final String ATTR_ACTIVITY_SELECTION = "ACTIVITY_SELECTION";

    private final WidgetFactory widgetFactory;
    private final CurrentUser currentUser;
    private final PageService pageService;

    public ActivitiesPane(
            final CurrentUser currentUser,
            final PageService pageService) {

        this.widgetFactory = pageService.getWidgetFactory();
        this.currentUser = currentUser;
        this.pageService = pageService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final UserInfo userInfo = this.currentUser
                .getOrHandleError(t -> this.pageService.logoutOnError(t, pageContext));

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

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext);

        // Institution
        // If current user has SEB Server Admin role, show the Institution list
        if (userInfo.hasRole(UserRole.SEB_SERVER_ADMIN)) {
            // institutions (list) as root
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.INSTITUTION.displayName);
            injectActivitySelection(
                    institutions,
                    actionBuilder
                            .newAction(ActionDefinition.INSTITUTION_VIEW_LIST)
                            .create());

        } else {
            // otherwise show the form of the institution for current user
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.INSTITUTION.displayName);
            injectActivitySelection(
                    institutions,
                    actionBuilder.newAction(ActionDefinition.INSTITUTION_VIEW_FORM)
                            .withEntityKey(userInfo.institutionId, EntityType.INSTITUTION)
                            .withAttribute(AttributeKeys.READ_ONLY, "true")
                            .create());
        }

        // User Account
        // if current user has role seb-server admin or institutional-admin, show list
        if (this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN) ||
                this.currentUser.get().hasRole(UserRole.INSTITUTIONAL_ADMIN)) {

            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.USER_ACCOUNT.displayName);
            injectActivitySelection(
                    userAccounts,
                    actionBuilder
                            .newAction(ActionDefinition.USER_ACCOUNT_VIEW_LIST)
                            .create());
        } else {
            // otherwise show the user account form for current user
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.USER_ACCOUNT.displayName);
            injectActivitySelection(
                    userAccounts,
                    actionBuilder.newAction(ActionDefinition.USER_ACCOUNT_VIEW_FORM)
                            .withEntityKey(this.currentUser.get().getEntityKey())
                            .withAttribute(AttributeKeys.READ_ONLY, "true")
                            .create());
        }

        // LMS Setup
        if (this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.LMS_SETUP)) {
            final TreeItem lmsSetup = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.LMS_SETUP.displayName);
            injectActivitySelection(
                    lmsSetup,
                    actionBuilder
                            .newAction(ActionDefinition.LMS_SETUP_VIEW_LIST)
                            .create());
        }

        // Exam (Quiz Discovery)
        if (this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.EXAM)) {

            // Quiz Discovery
            // TODO discussion if this should be visible on Activity Pane or just over the Exam activity and Import action
            final TreeItem quizDiscovery = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.QUIZ_DISCOVERY.displayName);
            injectActivitySelection(
                    quizDiscovery,
                    actionBuilder
                            .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                            .create());

            // Exam
            final TreeItem exam = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.EXAM.displayName);
            injectActivitySelection(
                    exam,
                    actionBuilder
                            .newAction(ActionDefinition.EXAM_VIEW_LIST)
                            .create());
        }

        // SEB Configurations
        final boolean clientConfigRead = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.SEB_CLIENT_CONFIGURATION);
        final boolean examConfigRead = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.CONFIGURATION_NODE);
        if (clientConfigRead || examConfigRead) {
            final TreeItem sebConfigs = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.SEB_CONFIGURATION.displayName);

            // SEB Client Config
            if (clientConfigRead) {
                final TreeItem clientConfig = this.widgetFactory.treeItemLocalized(
                        sebConfigs,
                        ActivityDefinition.SEB_CLIENT_CONFIG.displayName);
                injectActivitySelection(
                        clientConfig,
                        actionBuilder
                                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_LIST)
                                .create());
            }

            // SEB Exam Config
            if (examConfigRead) {
                final TreeItem examConfig = this.widgetFactory.treeItemLocalized(
                        sebConfigs,
                        ActivityDefinition.SEB_EXAM_CONFIG.displayName);
                injectActivitySelection(
                        examConfig,
                        actionBuilder
                                .newAction(ActionDefinition.SEB_EXAM_CONFIG_LIST)
                                .create());
            }
        }

        // Monitoring exams
        if (this.currentUser.get().hasRole(UserRole.EXAM_SUPPORTER)) {
            final TreeItem clientConfig = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.MONITORING_EXAMS.displayName);
            injectActivitySelection(
                    clientConfig,
                    actionBuilder
                            .newAction(ActionDefinition.RUNNING_EXAM_VIEW_LIST)
                            .create());
        }

        // TODO other activities

        // register page listener and initialize navigation data
        navigation.addListener(SWT.Selection, event -> handleSelection(pageContext, event));
        navigation.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActivitiesActionEventListener(navigation));

        // page-selection on (re)load
        final PageState state = this.pageService.getCurrentState();
        if (state == null) {
            final TreeItem item = navigation.getItem(0);
            final PageAction activityAction = getActivitySelection(item);
            this.pageService.executePageAction(activityAction);
        } else {
            final TreeItem item = findItemByActionDefinition(navigation.getItems(), state);
            if (item != null) {
                navigation.select(item);
            }
        }
    }

    private void handleSelection(final PageContext composerCtx, final Event event) {
        final Tree tree = (Tree) event.widget;
        final TreeItem treeItem = (TreeItem) event.item;
        final PageAction action = getActivitySelection(treeItem);
        // if there is no form action associated with the treeItem and the treeItem has sub items, toggle the item state
        if (action == null) {
            if (treeItem.getItemCount() > 0) {
                treeItem.setExpanded(!treeItem.getExpanded());
            }
            return;
        }
        this.pageService.executePageAction(
                action,
                result -> {
                    if (result.hasError()) {
                        tree.deselect(treeItem);
                        final PageState currentState = this.pageService.getCurrentState();
                        if (currentState != null) {
                            final TreeItem item = findItemByActionDefinition(tree.getItems(), currentState);
                            if (item != null) {
                                tree.select(item);
                            }
                        }
                    }
                });
    }

    private static final TreeItem findItemByActionDefinition(
            final TreeItem[] items,
            final Activity activity,
            final String modelId) {

        if (items == null) {
            return null;
        }

        for (final TreeItem item : items) {
            final PageAction action = getActivitySelection(item);
            if (action == null) {
                continue;
            }

            final Activity activityAnchor = action.definition.targetState.activityAnchor();
            final EntityKey entityKey = action.getEntityKey();
            if (activityAnchor.name().equals(activity.name()) &&
                    (entityKey == null || (modelId != null && modelId.equals(entityKey.modelId)))) {
                return item;
            }

            final TreeItem _item = findItemByActionDefinition(item.getItems(), activity, modelId);
            if (_item != null) {
                return _item;
            }
        }

        return null;
    }

    static final TreeItem findItemByActionDefinition(final TreeItem[] items, final PageState pageState) {
        return findItemByActionDefinition(items, pageState.activityAnchor(), null);
    }

    static final void expand(final TreeItem item) {
        if (item == null) {
            return;
        }

        item.setExpanded(true);
        expand(item.getParentItem());
    }

    public static PageAction getActivitySelection(final TreeItem item) {
        return (PageAction) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    public static void injectActivitySelection(final TreeItem item, final PageAction action) {
        item.setData(ATTR_ACTIVITY_SELECTION, action);
    }

    private final class ActivitiesActionEventListener implements ActionEventListener {
        private final Tree navigation;

        private ActivitiesActionEventListener(final Tree navigation) {
            this.navigation = navigation;
        }

        @Override
        public void notify(final ActionEvent event) {
            final EntityKey entityKey = event.action.getEntityKey();
            final String modelId = (entityKey != null) ? entityKey.modelId : null;
            final TreeItem item = findItemByActionDefinition(
                    this.navigation.getItems(),
                    event.action.definition.targetState.activityAnchor(),
                    modelId);
            if (item != null) {
                this.navigation.select(item);
            }
        }
    }

}
