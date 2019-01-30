/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.activity;

import java.util.function.Consumer;

import org.eclipse.swt.widgets.TreeItem;

import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.page.impl.TODOTemplate;

public class ActivitySelection {

    public static final Consumer<TreeItem> EMPTY_FUNCTION = ti -> {
    };
    public static final Consumer<TreeItem> COLLAPSE_NONE_EMPTY = ti -> {
        ti.removeAll();
        ti.setItemCount(1);
    };

    public enum Activity {
        NONE(TODOTemplate.class, TODOTemplate.class, (String) null),
        INSTITUTION_ROOT(
                TODOTemplate.class,
                ActionPane.class,
                new LocTextKey("sebserver.activities.inst")),
        INSTITUTION_NODE(
                TODOTemplate.class,
                ActionPane.class,
                AttributeKeys.INSTITUTION_ID),
//
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
        public final String objectIdentifierAttribute;

        private Activity(
                final Class<? extends TemplateComposer> objectPaneComposer,
                final Class<? extends TemplateComposer> selectionPaneComposer,
                final LocTextKey title) {

            this.title = title;
            this.contentPaneComposer = objectPaneComposer;
            this.actionPaneComposer = selectionPaneComposer;
            this.objectIdentifierAttribute = null;
        }

        private Activity(
                final Class<? extends TemplateComposer> objectPaneComposer,
                final Class<? extends TemplateComposer> selectionPaneComposer,
                final String objectIdentifierAttribute) {

            this.title = null;
            this.contentPaneComposer = objectPaneComposer;
            this.actionPaneComposer = selectionPaneComposer;
            this.objectIdentifierAttribute = objectIdentifierAttribute;
        }

        public final ActivitySelection createSelection() {
            return new ActivitySelection(this);
        }

        public final ActivitySelection createSelection(final EntityName entityName) {
            return new ActivitySelection(this, entityName);
        }
    }

    private static final String ATTR_ACTIVITY_SELECTION = "ACTIVITY_SELECTION";

    public final Activity activity;
    public final EntityName entityName;
    Consumer<TreeItem> expandFunction = EMPTY_FUNCTION;

    ActivitySelection(final Activity activity) {
        this(activity, null);
    }

    ActivitySelection(final Activity activity, final EntityName entityName) {
        this.activity = activity;
        this.entityName = entityName;
        this.expandFunction = EMPTY_FUNCTION;
    }

    public ActivitySelection withExpandFunction(final Consumer<TreeItem> expandFunction) {
        if (expandFunction == null) {
            this.expandFunction = EMPTY_FUNCTION;
        }
        this.expandFunction = expandFunction;
        return this;
    }

    public String getObjectIdentifier() {
        if (this.entityName == null) {
            return null;
        }

        return this.entityName.modelId;
    }

    public void processExpand(final TreeItem item) {
        this.expandFunction.accept(item);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.activity == null) ? 0 : this.activity.hashCode());
        result = prime * result + ((this.entityName == null) ? 0 : this.entityName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ActivitySelection other = (ActivitySelection) obj;
        if (this.activity != other.activity)
            return false;
        if (this.entityName == null) {
            if (other.entityName != null)
                return false;
        } else if (!this.entityName.equals(other.entityName))
            return false;
        return true;
    }

    public static ActivitySelection get(final TreeItem item) {
        return (ActivitySelection) item.getData(ATTR_ACTIVITY_SELECTION);
    }

    public static void inject(final TreeItem item, final ActivitySelection selection) {
        item.setData(ATTR_ACTIVITY_SELECTION, selection);
    }

}
