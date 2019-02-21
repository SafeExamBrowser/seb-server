/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.activity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.TreeItem;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.page.content.InstitutionForm;
import ch.ethz.seb.sebserver.gui.service.page.content.InstitutionList;
import ch.ethz.seb.sebserver.gui.service.page.content.UserAccountForm;
import ch.ethz.seb.sebserver.gui.service.page.content.UserAccountList;
import ch.ethz.seb.sebserver.gui.service.page.impl.TODOTemplate;

public class ActivitySelection {

    public static final Consumer<TreeItem> EMPTY_FUNCTION = ti -> {
    };
    public static final Consumer<TreeItem> COLLAPSE_NONE_EMPTY = ti -> {
        ti.removeAll();
        ti.setItemCount(1);
    };

    public enum Activity {
        NONE(TODOTemplate.class, TODOTemplate.class),
        INSTITUTION_LIST(
                InstitutionList.class,
                ActionPane.class,
                new LocTextKey("sebserver.activities.institution")),
        INSTITUTION_FORM(
                InstitutionForm.class,
                ActionPane.class,
                new LocTextKey("sebserver.activities.institution")),

        USER_ACCOUNT_LIST(
                UserAccountList.class,
                ActionPane.class,
                new LocTextKey("sebserver.activities.useraccount")),

        USER_ACCOUNT_FORM(
                UserAccountForm.class,
                ActionPane.class,
                new LocTextKey("sebserver.activities.useraccount")),

//        USERS(UserAccountsForm.class, ActionPane.class),
//
//        EXAMS(ExamsListPage.class, ActionPane.class),
//        SEB_CONFIGS(SEBConfigurationForm.class, ActionPane.class),
//        SEB_CONFIG(SEBConfigurationPage.class, ActionPane.class),
//        SEB_CONFIG_TEMPLATES(TODOTemplate.class, ActionPane.class),
//        MONITORING(MonitoringForm.class, ActionPane.class),
//        RUNNING_EXAMS(RunningExamForm.class, ActionPane.class),
//        RUNNING_EXAM(RunningExamPage.class, ActionPane.class, AttributeKeys.EXAM_ID),
//        LOGS(TODOTemplate.class, ActionPane.class),
        ;

        public final LocTextKey title;
        public final Class<? extends TemplateComposer> contentPaneComposer;
        public final Class<? extends TemplateComposer> actionPaneComposer;
        //public final String modelIdAttribute;

        private Activity(
                final Class<? extends TemplateComposer> objectPaneComposer,
                final Class<? extends TemplateComposer> selectionPaneComposer,
                final LocTextKey title) {

            this.title = title;
            this.contentPaneComposer = objectPaneComposer;
            this.actionPaneComposer = selectionPaneComposer;
        }

        private Activity(
                final Class<? extends TemplateComposer> objectPaneComposer,
                final Class<? extends TemplateComposer> selectionPaneComposer) {

            this.title = null;
            this.contentPaneComposer = objectPaneComposer;
            this.actionPaneComposer = selectionPaneComposer;
        }

        public final ActivitySelection createSelection() {
            return new ActivitySelection(this);
        }
    }

    public final Activity activity;
    final Map<String, String> attributes;
    Consumer<TreeItem> expandFunction = EMPTY_FUNCTION;

    ActivitySelection(final Activity activity) {
        this.activity = activity;
        this.attributes = new HashMap<>();
    }

    public ActivitySelection withEntity(final EntityKey entityKey) {
        if (entityKey != null) {
            this.attributes.put(AttributeKeys.ENTITY_ID, entityKey.modelId);
            this.attributes.put(AttributeKeys.ENTITY_TYPE, entityKey.entityType.name());
        }

        return this;
    }

    public ActivitySelection withParentEntity(final EntityKey parentEntityKey) {
        if (parentEntityKey != null) {
            this.attributes.put(AttributeKeys.PARENT_ENTITY_ID, parentEntityKey.modelId);
            this.attributes.put(AttributeKeys.PARENT_ENTITY_TYPE, parentEntityKey.entityType.name());
        }

        return this;
    }

    public ActivitySelection withAttribute(final String name, final String value) {
        this.attributes.put(name, value);
        return this;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    public ActivitySelection withExpandFunction(final Consumer<TreeItem> expandFunction) {
        if (expandFunction == null) {
            this.expandFunction = EMPTY_FUNCTION;
        }
        this.expandFunction = expandFunction;
        return this;
    }

    public void processExpand(final TreeItem item) {
        this.expandFunction.accept(item);
    }

    public String getEntityId() {
        return this.attributes.get(AttributeKeys.ENTITY_ID);
    }

}
