/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
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

    private static final String SKIP_EXPAND = "SKIP_EXPAND";

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

        //--------------------------------------------------------------------------------------
        // ---- SEB ADMIN ----------------------------------------------------------------------

        // SEB Server Administration
        final TreeItem sebadmin = this.widgetFactory.treeItemLocalized(
                navigation,
                ActivityDefinition.SEB_ADMINISTRATION.displayName);

        // Institution
        // If current user has SEB Server Admin role, show the Institution list
        if (userInfo.hasRole(UserRole.SEB_SERVER_ADMIN)) {
            // institutions (list) as root
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    sebadmin,
                    ActivityDefinition.INSTITUTION.displayName);
            injectActivitySelection(
                    institutions,
                    actionBuilder
                            .newAction(ActionDefinition.INSTITUTION_VIEW_LIST)
                            .create());

        } else if (userInfo.hasRole(UserRole.INSTITUTIONAL_ADMIN)) {
            // otherwise show the form of the institution for current user
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    sebadmin,
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
        if (this.currentUser.get().hasAnyRole(UserRole.SEB_SERVER_ADMIN, UserRole.INSTITUTIONAL_ADMIN)) {

            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    sebadmin,
                    ActivityDefinition.USER_ACCOUNT.displayName);
            injectActivitySelection(
                    userAccounts,
                    actionBuilder
                            .newAction(ActionDefinition.USER_ACCOUNT_VIEW_LIST)
                            .create());
        } else {
            // otherwise show the user account form for current user
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    sebadmin,
                    ActivityDefinition.USER_ACCOUNT.displayName);
            injectActivitySelection(
                    userAccounts,
                    actionBuilder.newAction(ActionDefinition.USER_ACCOUNT_VIEW_FORM)
                            .withEntityKey(this.currentUser.get().getEntityKey())
                            .withAttribute(AttributeKeys.READ_ONLY, "true")
                            .create());
        }

        // User Activity Logs
        final boolean viewUserActivityLogs = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.USER_ACTIVITY_LOG);
        if (viewUserActivityLogs) {
            final TreeItem activityLogs = this.widgetFactory.treeItemLocalized(
                    sebadmin,
                    ActivityDefinition.USER_ACTIVITY_LOGS.displayName);
            injectActivitySelection(
                    activityLogs,
                    actionBuilder
                            .newAction(ActionDefinition.LOGS_USER_ACTIVITY_LIST)
                            .create());
        }

        sebadmin.setExpanded(true);
        // ---- SEB ADMIN ----------------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------
        // ---- SEB CONFIGURATION --------------------------------------------------------------

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
            //sebConfigs.setData(RWT.CUSTOM_VARIANT, CustomVariant.ACTIVITY_TREE_SECTION.key);

            // SEB Client Config
            if (clientConfigRead) {
                final TreeItem clientConfig = (sebConfigs != null)
                        ? this.widgetFactory.treeItemLocalized(
                                sebConfigs,
                                ActivityDefinition.SEB_CLIENT_CONFIG.displayName)
                        : this.widgetFactory.treeItemLocalized(
                                navigation,
                                ActivityDefinition.SEB_CLIENT_CONFIG.displayName);
                injectActivitySelection(
                        clientConfig,
                        actionBuilder
                                .newAction(ActionDefinition.SEB_CLIENT_CONFIG_LIST)
                                .create());
            }

            // SEB Exam Config
            if (examConfigRead) {
                final TreeItem examConfig = (sebConfigs != null)
                        ? this.widgetFactory.treeItemLocalized(
                                sebConfigs,
                                ActivityDefinition.SEB_EXAM_CONFIG.displayName)
                        : this.widgetFactory.treeItemLocalized(
                                navigation,
                                ActivityDefinition.SEB_EXAM_CONFIG.displayName);
                injectActivitySelection(
                        examConfig,
                        actionBuilder
                                .newAction(ActionDefinition.SEB_EXAM_CONFIG_LIST)
                                .create());
            }
            sebConfigs.setExpanded(true);
        }

        // ---- SEB CONFIGURATION --------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------
        // ---- EXAM ADMINISTRATION ------------------------------------------------------------

        final boolean lmsRead = this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.LMS_SETUP);
        final boolean examRead = this.currentUser.get().hasAnyRole(UserRole.EXAM_SUPPORTER, UserRole.EXAM_ADMIN) ||
                this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.EXAM);

        // Exam Administration
        final TreeItem examadmin = this.widgetFactory.treeItemLocalized(
                navigation,
                ActivityDefinition.EXAM_ADMINISTRATION.displayName);

        if (examRead || lmsRead) {
            // LMS Setup
            if (lmsRead) {
                final TreeItem lmsSetup = this.widgetFactory.treeItemLocalized(
                        examadmin,
                        ActivityDefinition.LMS_SETUP.displayName);
                injectActivitySelection(
                        lmsSetup,
                        actionBuilder
                                .newAction(ActionDefinition.LMS_SETUP_VIEW_LIST)
                                .create());
            }

            // Exam (Quiz Discovery)
            if (examRead) {

                // Quiz Discovery
                final TreeItem quizDiscovery = this.widgetFactory.treeItemLocalized(
                        examadmin,
                        ActivityDefinition.QUIZ_DISCOVERY.displayName);
                injectActivitySelection(
                        quizDiscovery,
                        actionBuilder
                                .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                                .create());

                // Exam
                final TreeItem exam = this.widgetFactory.treeItemLocalized(
                        examadmin,
                        ActivityDefinition.EXAM.displayName);
                injectActivitySelection(
                        exam,
                        actionBuilder
                                .newAction(ActionDefinition.EXAM_VIEW_LIST)
                                .create());
            }

            examadmin.setExpanded(true);
        }

        // ---- EXAM ADMINISTRATION ------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------
        // ---- MONITORING ---------------------------------------------------------------------

        final boolean isSupporter = this.currentUser.get().hasAnyRole(UserRole.EXAM_SUPPORTER);
        final boolean viewSebClientLogs = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.EXAM) ||
                this.currentUser.get().hasRole(UserRole.EXAM_SUPPORTER);

        if (isSupporter || viewSebClientLogs) {
            // Monitoring
            final TreeItem monitoring = this.widgetFactory.treeItemLocalized(
                    navigation,
                    ActivityDefinition.MONITORING.displayName);

            // Monitoring exams
            if (isSupporter) {
                final TreeItem clientConfig = this.widgetFactory.treeItemLocalized(
                        monitoring,
                        ActivityDefinition.MONITORING_EXAMS.displayName);
                injectActivitySelection(
                        clientConfig,
                        actionBuilder
                                .newAction(ActionDefinition.RUNNING_EXAM_VIEW_LIST)
                                .create());
            }

            // SEB Client Logs
            if (viewSebClientLogs) {
                final TreeItem sebLogs = (monitoring != null)
                        ? this.widgetFactory.treeItemLocalized(
                                monitoring,
                                ActivityDefinition.SEB_CLIENT_LOGS.displayName)
                        : this.widgetFactory.treeItemLocalized(
                                navigation,
                                ActivityDefinition.SEB_CLIENT_LOGS.displayName);
                injectActivitySelection(
                        sebLogs,
                        actionBuilder
                                .newAction(ActionDefinition.LOGS_SEB_CLIENT)
                                .create());
            }

            monitoring.setExpanded(true);
        }

        // ---- MONITORING ---------------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        // register page listener and initialize navigation data
        navigation.addListener(SWT.Selection, event -> handleSelection(pageContext, event));
        navigation.addListener(SWT.Expand, event -> {
            final TreeItem item = (TreeItem) event.item;
            selectCurrentItem(navigation, item);
        });
        navigation.addListener(SWT.Collapse, event -> {
            final Tree tree = (Tree) event.widget;
            tree.setData(SKIP_EXPAND, true);
        });
        navigation.addListener(SWT.MouseUp, event -> {
            final Tree tree = (Tree) event.widget;
            final TreeItem[] selection = tree.getSelection();
            if (ArrayUtils.isNotEmpty(selection)) {
                final TreeItem item = selection[0];
                final boolean skipExpand = BooleanUtils.isTrue((Boolean) tree.getData(SKIP_EXPAND));
                if (item.getItemCount() > 0 && !item.getExpanded() && !skipExpand) {
                    item.setExpanded(true);
                    handleParentSelection(tree, item);
                }
            }
            tree.setData(SKIP_EXPAND, false);
        });
        navigation.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActivitiesActionEventListener(navigation));

        // page-selection on (re)load
        final PageState state = this.pageService.getCurrentState();
        if (state == null) {
            final TreeItem item = navigation.getItem(0);
            final TreeItem actionItem = getActionItem(item);
            final PageAction activityAction = getActivitySelection(actionItem);
            this.pageService.executePageAction(activityAction);
        } else {
            final TreeItem item = findItemByActionDefinition(
                    navigation.getItems(),
                    state.activityAnchor());
            if (item != null) {
                final PageAction action = getActivitySelection(item);
                this.pageService.executePageAction(action, result -> {
                    navigation.select(item);
                });
            }
        }
    }

    private void selectCurrentItem(final Tree navigation, final TreeItem item) {
        final PageState currentState = this.pageService.getCurrentState();
        final TreeItem currentItem = findItemByActionDefinition(
                item.getItems(),
                currentState.activityAnchor());
        if (currentItem != null) {
            navigation.select(currentItem);
        }
    }

    private void handleSelection(final PageContext composerCtx, final Event event) {
        final Tree tree = (Tree) event.widget;
        final TreeItem treeItem = (TreeItem) event.item;

        if (treeItem.getItemCount() > 0 && !treeItem.getExpanded()) {
            return;
        }

        final PageAction action = getActivitySelection(treeItem);
        // if there is no form action associated with the treeItem and the treeItem has sub items, toggle the item state
        if (action == null) {
            handleParentSelection(tree, treeItem);
            return;
        }

        final PageState currentState = this.pageService.getCurrentState();
        if (currentState == action.definition.targetState) {
            return;
        }

        this.pageService.executePageAction(
                action,
                result -> {
                    if (result.hasError()) {
                        tree.deselect(treeItem);
                        if (currentState != null) {
                            final TreeItem item = findItemByActionDefinition(
                                    tree.getItems(),
                                    currentState.activityAnchor());
                            if (item != null) {
                                tree.select(item);
                            }
                        }
                    }
                });
    }

    private void handleParentSelection(final Tree tree, final TreeItem treeItem) {
        if (treeItem.getItemCount() > 0) {
            final PageState currentState = this.pageService.getCurrentState();
            final TreeItem currentSelection = findItemByActionDefinition(
                    tree.getItems(),
                    currentState.activityAnchor());
            if (currentSelection != null) {
                if (isInSubTree(treeItem, currentSelection)) {
                    tree.setSelection(currentSelection);
                } else {
                    selectFirstChild(tree, treeItem);
                }
            } else {
                tree.deselectAll();
            }
        }

        tree.layout();
    }

    private void selectFirstChild(final Tree tree, final TreeItem treeItem) {
        final TreeItem actionItem = ActivitiesPane.getActionItem(treeItem);
        final PageAction activitySelection = getActivitySelection(actionItem);
        this.pageService.executePageAction(activitySelection, result -> {
            if (!result.hasError()) {
                tree.setSelection(actionItem);
            }
        });
    }

    private static final boolean isInSubTree(final TreeItem treeItem, final TreeItem currentSelection) {
        if (treeItem == null) {
            return false;
        }

        final TreeItem[] items = treeItem.getItems();
        if (ArrayUtils.isEmpty(items)) {
            return false;
        }

        for (final TreeItem item : items) {
            if (item == currentSelection) {
                return true;
            }
        }

        return false;
    }

    private static final TreeItem findItemByActionDefinition(
            final TreeItem[] items,
            final Activity activity) {

        if (items == null) {
            return null;
        }

        for (final TreeItem item : items) {
            final PageAction action = getActivitySelection(item);
            if (action == null) {
                if (item.getItemCount() > 0) {
                    final TreeItem found = findItemByActionDefinition(item.getItems(), activity);
                    if (found != null) {
                        return found;
                    }
                }
                continue;
            }

            final Activity activityAnchor = action.definition.targetState.activityAnchor();
            if (activityAnchor.name().equals(activity.name())) {
                return item;
            }

            final TreeItem _item = findItemByActionDefinition(item.getItems(), activity);
            if (_item != null) {
                return _item;
            }
        }

        return null;
    }

    private static final TreeItem getActionItem(final TreeItem item) {
        final PageAction action = (PageAction) item.getData(ATTR_ACTIVITY_SELECTION);
        if (action == null && item.getItemCount() > 0) {
            final TreeItem firstChild = item.getItem(0);
            if (firstChild != null) {
                return firstChild;
            }
        }

        return item;
    }

    private static final PageAction getActivitySelection(final TreeItem item) {
        return (PageAction) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    private final static void injectActivitySelection(final TreeItem item, final PageAction action) {
        item.setData(ATTR_ACTIVITY_SELECTION, action);
    }

    private final class ActivitiesActionEventListener implements ActionEventListener {
        private final Tree navigation;

        private ActivitiesActionEventListener(final Tree navigation) {
            this.navigation = navigation;
        }

        @Override
        public void notify(final ActionEvent event) {
            final TreeItem item = findItemByActionDefinition(
                    this.navigation.getItems(),
                    event.action.definition.targetState.activityAnchor());
            if (item != null) {
                this.navigation.select(item);
            }
        }
    }

}
