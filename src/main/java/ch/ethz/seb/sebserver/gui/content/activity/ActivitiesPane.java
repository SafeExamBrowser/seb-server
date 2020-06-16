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
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageState;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActivitiesPane implements TemplateComposer {

    private static final String SKIP_EXPAND = "SKIP_EXPAND";

    private static final String ATTR_ACTIVITY_SELECTION = "ACTIVITY_SELECTION";
    private static final LocTextKey TITLE_KEY = new LocTextKey("sebserver.activitiespane.title");

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

        final boolean isSupporterOnly = userInfo.hasRole(UserRole.EXAM_SUPPORTER) &&
                !userInfo.hasAnyRole(UserRole.EXAM_ADMIN, UserRole.INSTITUTIONAL_ADMIN, UserRole.SEB_SERVER_ADMIN);

        if (this.pageService.getI18nSupport().hasText(TITLE_KEY)) {
            final Label activities = this.widgetFactory.labelLocalized(
                    pageContext.getParent(),
                    CustomVariant.TEXT_H2,
                    TITLE_KEY);
            final GridData activitiesGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            activitiesGridData.horizontalIndent = 20;
            activities.setLayoutData(activitiesGridData);
        }

        final Tree navigation = this.widgetFactory.treeLocalized(
                pageContext.getParent(),
                SWT.SINGLE | SWT.FULL_SELECTION);
        final GridData navigationGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        //navigationGridData.horizontalIndent = 10;
        navigation.setLayoutData(navigationGridData);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext);

        //--------------------------------------------------------------------------------------
        // ---- SEB ADMIN ----------------------------------------------------------------------

        final boolean isServerOrInstAdmin = this.currentUser.get()
                .hasAnyRole(UserRole.SEB_SERVER_ADMIN, UserRole.INSTITUTIONAL_ADMIN);

        // SEB Server Administration
        final TreeItem sebAdmin = this.widgetFactory.treeItemLocalized(
                navigation,
                ActivityDefinition.SEB_ADMINISTRATION.displayName);

        // Institution
        // If current user has SEB Server Admin role, show the Institution list
        if (userInfo.hasRole(UserRole.SEB_SERVER_ADMIN)) {
            // institutions (list) as root
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    sebAdmin,
                    ActivityDefinition.INSTITUTION.displayName);
            injectActivitySelection(
                    institutions,
                    actionBuilder
                            .newAction(ActionDefinition.INSTITUTION_VIEW_LIST)
                            .create());

        } else if (userInfo.hasRole(UserRole.INSTITUTIONAL_ADMIN)) {
            // otherwise show the form of the institution for current user
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    sebAdmin,
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
        if (isServerOrInstAdmin) {

            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    sebAdmin,
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

        // User Activity Logs
        final boolean viewUserActivityLogs = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.USER_ACTIVITY_LOG);
        if (viewUserActivityLogs) {
            final TreeItem activityLogs = this.widgetFactory.treeItemLocalized(
                    sebAdmin,
                    ActivityDefinition.USER_ACTIVITY_LOGS.displayName);
            injectActivitySelection(
                    activityLogs,
                    actionBuilder
                            .newAction(ActionDefinition.LOGS_USER_ACTIVITY_LIST)
                            .create());
        }

        if (sebAdmin.getItemCount() > 0) {
            sebAdmin.setExpanded(this.currentUser.get().hasAnyRole(
                    UserRole.SEB_SERVER_ADMIN,
                    UserRole.INSTITUTIONAL_ADMIN));
        } else {
            sebAdmin.dispose();
        }

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

        if ((clientConfigRead || examConfigRead) && !isSupporterOnly) {
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

            // SEB Exam Config Template
            if (examConfigRead) {
                final TreeItem examConfigTemplate = this.widgetFactory.treeItemLocalized(
                        sebConfigs,
                        ActivityDefinition.SEB_EXAM_CONFIG_TEMPLATE.displayName);
                injectActivitySelection(
                        examConfigTemplate,
                        actionBuilder
                                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_LIST)
                                .create());
            }

            sebConfigs.setExpanded(this.currentUser.get().hasAnyRole(UserRole.EXAM_ADMIN));
        }

        // ---- SEB CONFIGURATION --------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------
        // ---- EXAM ADMINISTRATION ------------------------------------------------------------

        final boolean lmsRead = this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.LMS_SETUP);
        final boolean examRead = userInfo.hasAnyRole(UserRole.EXAM_SUPPORTER, UserRole.EXAM_ADMIN) ||
                this.currentUser.hasInstitutionalPrivilege(PrivilegeType.READ, EntityType.EXAM);
        final boolean examWrite = this.currentUser.hasInstitutionalPrivilege(PrivilegeType.WRITE, EntityType.EXAM);

        // Exam Administration
        final TreeItem examAdmin = this.widgetFactory.treeItemLocalized(
                navigation,
                ActivityDefinition.EXAM_ADMINISTRATION.displayName);

        if (examRead || lmsRead) {
            // LMS Setup
            if (lmsRead && !isSupporterOnly) {
                final TreeItem lmsSetup = this.widgetFactory.treeItemLocalized(
                        examAdmin,
                        ActivityDefinition.LMS_SETUP.displayName);
                injectActivitySelection(
                        lmsSetup,
                        actionBuilder
                                .newAction(ActionDefinition.LMS_SETUP_VIEW_LIST)
                                .create());
            }

            if (examRead) {

                if (examWrite) {
                    // Quiz Discovery
                    final TreeItem quizDiscovery = this.widgetFactory.treeItemLocalized(
                            examAdmin,
                            ActivityDefinition.QUIZ_DISCOVERY.displayName);
                    injectActivitySelection(
                            quizDiscovery,
                            actionBuilder
                                    .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                                    .create());
                }

                // Exam
                final TreeItem exam = this.widgetFactory.treeItemLocalized(
                        examAdmin,
                        ActivityDefinition.EXAM.displayName);
                injectActivitySelection(
                        exam,
                        actionBuilder
                                .newAction(ActionDefinition.EXAM_VIEW_LIST)
                                .create());
            }

            examAdmin.setExpanded(this.currentUser.get().hasAnyRole(UserRole.EXAM_ADMIN));
        }

        // ---- EXAM ADMINISTRATION ------------------------------------------------------------
        //--------------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------
        // ---- MONITORING ---------------------------------------------------------------------

        final boolean isSupporter = this.currentUser.get().hasAnyRole(UserRole.EXAM_SUPPORTER);
        final boolean viewSEBClientLogs = this.currentUser.hasInstitutionalPrivilege(
                PrivilegeType.READ,
                EntityType.EXAM) ||
                this.currentUser.get().hasRole(UserRole.EXAM_SUPPORTER);

        if (isSupporter || viewSEBClientLogs) {
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
            if (viewSEBClientLogs) {
                final TreeItem sebLogs = (isSupporter)
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

            if (monitoring.getItemCount() > 0) {
                monitoring.setExpanded(
                        this.currentUser
                                .get()
                                .hasAnyRole(UserRole.EXAM_SUPPORTER));
            } else {
                monitoring.dispose();
            }
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
            final TreeItem item = getDefaultSelectionFor(navigation, this.currentUser);
            final TreeItem actionItem = getActionItem(item);
            final PageAction activityAction = getActivitySelection(actionItem);
            this.pageService.executePageAction(activityAction);
        } else {
            final TreeItem item = findItemByActionDefinition(
                    navigation.getItems(),
                    state.activityAnchor());
            if (item != null) {
                final PageAction action = getActivitySelection(item);
                this.pageService.executePageAction(action, result -> navigation.select(item));
            }
        }
    }

    private TreeItem getDefaultSelectionFor(final Tree navigation, final CurrentUser currentUser2) {
        if (this.currentUser.get().hasAnyRole(UserRole.SEB_SERVER_ADMIN, UserRole.INSTITUTIONAL_ADMIN)) {
            return navigation.getItem(0);
        } else if (this.currentUser.get().hasAnyRole(UserRole.EXAM_ADMIN)) {
            return findItemByActionDefinition(
                    navigation.getItems(),
                    ActivityDefinition.SEB_EXAM_CONFIG);
        } else if (this.currentUser.get().hasAnyRole(UserRole.EXAM_SUPPORTER)) {
            return findItemByActionDefinition(
                    navigation.getItems(),
                    ActivityDefinition.MONITORING_EXAMS);
        } else {
            return navigation.getItem(0);
        }
    }

    private void selectCurrentItem(final Tree navigation, final TreeItem item) {
        final PageState currentState = this.pageService.getCurrentState();
        if (currentState == null) {
            return;
        }
        final TreeItem currentItem = findItemByActionDefinition(
                item.getItems(),
                currentState.definition.activityAnchor());
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
        if (currentState != null && currentState.definition == action.definition.targetState) {
            return;
        }

        this.pageService.executePageAction(
                action,
                resultAction -> {
                    if (resultAction.hasError()) {
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

    private static boolean isInSubTree(final TreeItem treeItem, final TreeItem currentSelection) {
        if (treeItem == null) {
            return false;
        }

        final TreeItem[] items = treeItem.getItems();
        if (ArrayUtils.isEmpty(items)) {
            return false;
        }

        for (final TreeItem item : items) {
            if (item.equals(currentSelection)) {
                return true;
            }
        }

        return false;
    }

    private static TreeItem findItemByActionDefinition(
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

    private static TreeItem getActionItem(final TreeItem item) {
        final PageAction action = (PageAction) item.getData(ATTR_ACTIVITY_SELECTION);
        if (action == null && item.getItemCount() > 0) {
            final TreeItem firstChild = item.getItem(0);
            if (firstChild != null) {
                return firstChild;
            }
        }

        return item;
    }

    private static PageAction getActivitySelection(final TreeItem item) {
        return (PageAction) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    private static void injectActivitySelection(final TreeItem item, final PageAction action) {
        item.setData(ATTR_ACTIVITY_SELECTION, action);
    }

    private static final class ActivitiesActionEventListener implements ActionEventListener {
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
                this.navigation.deselectAll();
                this.navigation.select(item);
            }
        }
    }

}
