/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.activity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

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
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection.Activity;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.page.impl.MainPageState;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
public class ActivitiesPane implements TemplateComposer {

    private static final String ATTR_ACTIVITY_SELECTION = "ACTIVITY_SELECTION";

    private final WidgetFactory widgetFactory;
    private final RestService restService;
    private final CurrentUser currentUser;

    // TODO are those really needed?
    private final Map<ActionDefinition, ActivityActionHandler> activityActionHandler =
            new EnumMap<>(ActionDefinition.class);

    public ActivitiesPane(
            final WidgetFactory widgetFactory,
            final RestService restService,
            final CurrentUser currentUser,
            final Collection<ActivityActionHandler> activityActionHandler) {

        this.widgetFactory = widgetFactory;
        this.restService = restService;
        this.currentUser = currentUser;

        for (final ActivityActionHandler aah : activityActionHandler) {
            this.activityActionHandler.put(aah.handlesAction(), aah);
        }
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
        navigationGridData.horizontalIndent = 10;
        navigation.setLayoutData(navigationGridData);

        // Institution
        // If current user has SEB Server Admin role, show the Institution list
        if (userInfo.hasRole(UserRole.SEB_SERVER_ADMIN)) {
            // institutions (list) as root
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    Activity.INSTITUTION_LIST.title);
            injectActivitySelection(institutions, Activity.INSTITUTION_LIST.createSelection());

        } else {
            // otherwise show the form of the institution for current user
            final TreeItem institutions = this.widgetFactory.treeItemLocalized(
                    navigation,
                    Activity.INSTITUTION_FORM.title);
            injectActivitySelection(
                    institutions,
                    Activity.INSTITUTION_FORM.createSelection()
                            .withEntity(new EntityKey(userInfo.institutionId, EntityType.INSTITUTION))
                            .withAttribute(AttributeKeys.READ_ONLY, "true"));
        }

        // User Account
        // if current user has base read privilege for User Account, show list
        if (this.currentUser.hasPrivilege(PrivilegeType.READ_ONLY, EntityType.USER)) {
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    Activity.USER_ACCOUNT_LIST.title);
            injectActivitySelection(userAccounts, Activity.USER_ACCOUNT_LIST.createSelection());
        } else {
            // otherwise show the user account form for current user
            final TreeItem userAccounts = this.widgetFactory.treeItemLocalized(
                    navigation,
                    Activity.USER_ACCOUNT_FORM.title);
            injectActivitySelection(
                    userAccounts,
                    Activity.USER_ACCOUNT_FORM.createSelection()
                            .withEntity(this.currentUser.get().getEntityKey())
                            .withAttribute(AttributeKeys.READ_ONLY, "true"));
        }
//
//        final TreeItem configs = this.widgetFactory.treeItemLocalized(
//                navigation,
//                "org.sebserver.activities.sebconfigs");
//        ActivitySelection.set(configs, Activity.SEB_CONFIGS.createSelection());
//
//        final TreeItem config = this.widgetFactory.treeItemLocalized(
//                configs,
//                "org.sebserver.activities.sebconfig");
//        ActivitySelection.set(config, Activity.SEB_CONFIG.createSelection());
//
//        final TreeItem configTemplates = this.widgetFactory.treeItemLocalized(
//                configs,
//                "org.sebserver.activities.sebconfig.templates");
//        ActivitySelection.set(configTemplates, Activity.SEB_CONFIG_TEMPLATES.createSelection());
//
//        final TreeItem exams = this.widgetFactory.treeItemLocalized(
//                navigation,
//                "org.sebserver.activities.exam");
//        ActivitySelection.set(exams, Activity.EXAMS.createSelection());
//
//        final TreeItem monitoring = this.widgetFactory.treeItemLocalized(
//                navigation,
//                "org.sebserver.activities.monitoring");
//        ActivitySelection.set(monitoring, Activity.MONITORING.createSelection());
//
//        final TreeItem runningExams = this.widgetFactory.treeItemLocalized(
//                monitoring,
//                "org.sebserver.activities.runningExams");
//        ActivitySelection.set(runningExams, Activity.RUNNING_EXAMS.createSelection()
//                .withExpandFunction(this::runningExamExpand));
//        runningExams.setItemCount(1);
//
//        final TreeItem logs = this.widgetFactory.treeItemLocalized(
//                monitoring,
//                "org.sebserver.activities.logs");
//        ActivitySelection.set(logs, Activity.LOGS.createSelection());

        navigation.addListener(SWT.Expand, this::handleExpand);
        navigation.addListener(SWT.Selection, event -> handleSelection(pageContext, event));
        navigation.setData(
                PageEventListener.LISTENER_ATTRIBUTE_KEY,
                new ActionEventListener() {
                    @Override
                    public void notify(final ActionEvent event) {
//                        final ActivityActionHandler aah =
//                                ActivitiesPane.this.activityActionHandler.get(event.actionDefinition);
//                        if (aah != null) {
//                            aah.notifyAction(event, navigation, pageContext);
//                        }
                        // on case of an Action with ActivitySelection, reset the MainPageState
                        if (event.source instanceof ActivitySelection) {
                            final MainPageState mainPageState = MainPageState.get();
                            mainPageState.activitySelection = (ActivitySelection) event.source;
                        }
                    }
                });

        // page-selection on (re)load
        final MainPageState mainPageState = MainPageState.get();

        if (mainPageState.activitySelection == null ||
                mainPageState.activitySelection.activity == ActivitySelection.Activity.NONE) {
            mainPageState.activitySelection = getActivitySelection(navigation.getItem(0));
        }
        pageContext.publishPageEvent(
                new ActivitySelectionEvent(mainPageState.activitySelection));
        navigation.select(navigation.getItem(0));
    }

//    private void runningExamExpand(final TreeItem item) {
//        item.removeAll();
//        final List<EntityName> runningExamNames = this.restService
//                .sebServerCall(GetRunningExamNames.class)
//                .onError(t -> {
//                    throw new RuntimeException(t);
//                });
//
//        if (runningExamNames != null) {
//            for (final EntityName runningExamName : runningExamNames) {
//                final TreeItem runningExams = this.widgetFactory.treeItemLocalized(
//                        item,
//                        runningExamName.name);
//                ActivitySelection.set(runningExams, Activity.RUNNING_EXAM.createSelection(runningExamName));
//            }
//        }
//    }

    private void handleExpand(final Event event) {
        final TreeItem treeItem = (TreeItem) event.item;

        System.out.println("opened: " + treeItem);

        final ActivitySelection activity = getActivitySelection(treeItem);
        if (activity != null) {
            activity.processExpand(treeItem);
        }
    }

    private void handleSelection(final PageContext composerCtx, final Event event) {
        final TreeItem treeItem = (TreeItem) event.item;

        System.out.println("selected: " + treeItem);

        final MainPageState mainPageState = MainPageState.get();
        final ActivitySelection activitySelection = getActivitySelection(treeItem);
        if (mainPageState.activitySelection == null) {
            mainPageState.activitySelection = Activity.NONE.createSelection();
        }
        if (!mainPageState.activitySelection.equals(activitySelection)) {
            mainPageState.activitySelection = activitySelection;
            composerCtx.publishPageEvent(
                    new ActivitySelectionEvent(mainPageState.activitySelection));
        }
    }

    static final TreeItem findItemByActivity(
            final TreeItem[] items,
            final Activity activity,
            final String objectId) {

        if (items == null) {
            return null;
        }

        for (final TreeItem item : items) {
            final ActivitySelection activitySelection = getActivitySelection(item);
            final String id = activitySelection.getEntityId();
            if (activitySelection != null && activitySelection.activity == activity &&
                    (id == null || (objectId != null && objectId.equals(id)))) {
                return item;
            }

            final TreeItem _item = findItemByActivity(item.getItems(), activity, objectId);
            if (_item != null) {
                return _item;
            }
        }

        return null;
    }

    static final TreeItem findItemByActivity(final TreeItem[] items, final Activity activity) {
        return findItemByActivity(items, activity, null);
    }

    static final void expand(final TreeItem item) {
        if (item == null) {
            return;
        }

        item.setExpanded(true);
        expand(item.getParentItem());
    }

    public static ActivitySelection getActivitySelection(final TreeItem item) {
        return (ActivitySelection) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    public static void injectActivitySelection(final TreeItem item, final ActivitySelection selection) {
        item.setData(ATTR_ACTIVITY_SELECTION, selection);
    }

}
