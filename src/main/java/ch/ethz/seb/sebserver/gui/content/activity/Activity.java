/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import ch.ethz.seb.sebserver.gui.content.InstitutionForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionList;
import ch.ethz.seb.sebserver.gui.content.UserAccountForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountList;
import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection;
import ch.ethz.seb.sebserver.gui.service.page.impl.TODOTemplate;

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