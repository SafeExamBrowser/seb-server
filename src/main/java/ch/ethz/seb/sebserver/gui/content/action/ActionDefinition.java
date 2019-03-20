/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.content.ExamForm;
import ch.ethz.seb.sebserver.gui.content.ExamList;
import ch.ethz.seb.sebserver.gui.content.IndicatorForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionList;
import ch.ethz.seb.sebserver.gui.content.LmsSetupForm;
import ch.ethz.seb.sebserver.gui.content.LmsSetupList;
import ch.ethz.seb.sebserver.gui.content.QuizDiscoveryList;
import ch.ethz.seb.sebserver.gui.content.UserAccountChangePasswordForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountList;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.ActivateExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeactivateExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.ActivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.DeactivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.ActivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.DeactivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.DeactivateUserAccount;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

/** Enumeration of static action data for each action within the SEB Server GUI */
// TODO add category to allow easy positioning of actions later
public enum ActionDefinition {

    INSTITUTION_VIEW_LIST(
            new LocTextKey("sebserver.institution.action.list"),
            InstitutionList.class),
    INSTITUTION_VIEW_FORM(
            new LocTextKey("sebserver.institution.action.form"),
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_NEW(
            new LocTextKey("sebserver.institution.action.new"),
            ImageIcon.NEW,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            false),
    INSTITUTION_VIEW_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.view"),
            ImageIcon.SHOW,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.INSTITUTION_LIST),
    INSTITUTION_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.modify"),
            ImageIcon.EDIT,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.INSTITUTION_LIST,
            false),
    INSTITUTION_MODIFY(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.EDIT,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM,
            false),
    INSTITUTION_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM,
            true),
    INSTITUTION_SAVE(
            new LocTextKey("sebserver.institution.action.save"),
            ImageIcon.SAVE,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM),
    INSTITUTION_ACTIVATE(
            new LocTextKey("sebserver.institution.action.activate"),
            ImageIcon.INACTIVE,
            InstitutionForm.class,
            ActivateInstitution.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM),
    INSTITUTION_DEACTIVATE(
            new LocTextKey("sebserver.institution.action.deactivate"),
            ImageIcon.ACTIVE,
            InstitutionForm.class,
            DeactivateInstitution.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM),
    INSTITUTION_DELETE(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.DELETE,
            InstitutionList.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_EXPORT_SEB_CONFIG(
            new LocTextKey("sebserver.institution.action.export.sebconfig"),
            ImageIcon.SAVE,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST,
            ActionCategory.FORM),

    USER_ACCOUNT_VIEW_LIST(
            new LocTextKey("sebserver.useraccount.action.list"),
            UserAccountList.class),
    USER_ACCOUNT_VIEW_FORM(
            new LocTextKey("sebserver.useraccount.action.form"),
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_NEW(
            new LocTextKey("sebserver.useraccount.action.new"),
            ImageIcon.NEW,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST, false),
    USER_ACCOUNT_VIEW_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.view"),
            ImageIcon.SHOW,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.list.modify"),
            ImageIcon.EDIT,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.USER_ACCOUNT_LIST,
            false),
    USER_ACCOUNT_MODIFY(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.EDIT,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM,
            false),
    USER_ACCOUNT_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM,
            true),
    USER_ACCOUNT_SAVE(
            new LocTextKey("sebserver.useraccount.action.save"),
            ImageIcon.SAVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM),
    USER_ACCOUNT_ACTIVATE(
            new LocTextKey("sebserver.useraccount.action.activate"),
            ImageIcon.INACTIVE,
            UserAccountForm.class,
            ActivateUserAccount.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM),
    USER_ACCOUNT_DEACTIVATE(
            new LocTextKey("sebserver.useraccount.action.deactivate"),
            ImageIcon.ACTIVE,
            UserAccountForm.class,
            DeactivateUserAccount.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM),
    USER_ACCOUNT_DELETE(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.DELETE,
            UserAccountList.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_CHANGE_PASSOWRD(
            new LocTextKey("sebserver.useraccount.action.change.password"),
            ImageIcon.EDIT,
            UserAccountChangePasswordForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM,
            false),
    USER_ACCOUNT_CHANGE_PASSOWRD_SAVE(
            new LocTextKey("sebserver.useraccount.action.change.password.save"),
            ImageIcon.SAVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST,
            ActionCategory.FORM),

    LMS_SETUP_VIEW_LIST(
            new LocTextKey("sebserver.lmssetup.action.list"),
            LmsSetupList.class),
    LMS_SETUP_NEW(
            new LocTextKey("sebserver.lmssetup.action.new"),
            ImageIcon.NEW,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST, false),
    LMS_SETUP_VIEW_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.view"),
            ImageIcon.SHOW,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.LMS_SETUP_LIST),
    LMS_SETUP_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.modify"),
            ImageIcon.EDIT,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.LMS_SETUP_LIST,
            false),
    LMS_SETUP_MODIFY(
            new LocTextKey("sebserver.lmssetup.action.modify"),
            ImageIcon.EDIT,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM,
            false),
    LMS_SETUP_TEST(
            new LocTextKey("sebserver.lmssetup.action.test"),
            ImageIcon.TEST,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM),
    LMS_SETUP_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM,
            true),
    LMS_SETUP_SAVE(
            new LocTextKey("sebserver.lmssetup.action.save"),
            ImageIcon.SAVE,
            LmsSetupForm.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM),
    LMS_SETUP_ACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.activate"),
            ImageIcon.INACTIVE,
            LmsSetupForm.class,
            ActivateLmsSetup.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM),
    LMS_SETUP_DEACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.deactivate"),
            ImageIcon.ACTIVE,
            LmsSetupForm.class,
            DeactivateLmsSetup.class,
            LMS_SETUP_VIEW_LIST,
            ActionCategory.FORM),

    QUIZ_DISCOVERY_VIEW_LIST(
            new LocTextKey("sebserver.quizdiscovery.action.list"),
            QuizDiscoveryList.class),
    QUIZ_DISCOVERY_EXAM_IMPORT(
            new LocTextKey("sebserver.quizdiscovery.action.import"),
            ImageIcon.IMPORT,
            ExamForm.class,
            QUIZ_DISCOVERY_VIEW_LIST,
            ActionCategory.QUIZ_LIST,
            false),

    EXAM_VIEW_LIST(
            new LocTextKey("sebserver.exam.action.list"),
            ExamList.class),
    EXAM_IMPORT(
            new LocTextKey("sebserver.exam.action.import"),
            ImageIcon.IMPORT,
            QuizDiscoveryList.class,
            QUIZ_DISCOVERY_VIEW_LIST,
            false),
    EXAM_VIEW_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.view"),
            ImageIcon.SHOW,
            ExamForm.class,
            EXAM_VIEW_LIST,
            ActionCategory.EXAM_LIST),
    EXAM_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.modify"),
            ImageIcon.EDIT,
            ExamForm.class,
            EXAM_VIEW_LIST,
            ActionCategory.EXAM_LIST,
            false),
    EXAM_MODIFY(
            new LocTextKey("sebserver.exam.action.modify"),
            ImageIcon.EDIT,
            ExamForm.class,
            EXAM_VIEW_LIST,
            ActionCategory.FORM,
            false),
    EXAM_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            ExamForm.class,
            EXAM_VIEW_LIST,
            ActionCategory.FORM,
            true),
    EXAM_SAVE(
            new LocTextKey("sebserver.exam.action.save"),
            ImageIcon.SAVE,
            ExamForm.class,
            EXAM_VIEW_LIST,
            ActionCategory.FORM),
    EXAM_ACTIVATE(
            new LocTextKey("sebserver.exam.action.activate"),
            ImageIcon.INACTIVE,
            ExamForm.class,
            ActivateExam.class,
            EXAM_VIEW_LIST,
            ActionCategory.FORM),
    EXAM_DEACTIVATE(
            new LocTextKey("sebserver.exam.action.deactivate"),
            ImageIcon.ACTIVE,
            ExamForm.class,
            DeactivateExam.class,
            EXAM_VIEW_LIST,
            ActionCategory.FORM),

    EXAM_INDICATOR_NEW(
            new LocTextKey("sebserver.exam.indicator.action.list.new"),
            ImageIcon.NEW,
            LmsSetupForm.class,
            EXAM_VIEW_FROM_LIST,
            ActionCategory.FORM,
            false),
    EXAM_INDICATOR_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.modify"),
            ImageIcon.EDIT,
            IndicatorForm.class,
            EXAM_VIEW_FROM_LIST,
            ActionCategory.INDICATOR_LIST,
            false),
    EXAM_INDICATOR_DELETE_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.delete"),
            ImageIcon.DELETE,
            ExamForm.class,
            EXAM_VIEW_FROM_LIST,
            ActionCategory.INDICATOR_LIST,
            true),
    EXAM_INDICATOR_SAVE(
            new LocTextKey("sebserver.exam.indicator.action.list.save"),
            ImageIcon.SAVE,
            ExamForm.class,
            EXAM_VIEW_FROM_LIST,
            ActionCategory.FORM),
    EXAM_INDICATOR_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            ExamForm.class,
            EXAM_VIEW_FROM_LIST,
            ActionCategory.FORM,
            true),

    ;

    public final LocTextKey title;
    public final ImageIcon icon;
    public final Class<? extends TemplateComposer> contentPaneComposer;
    public final Class<? extends TemplateComposer> actionPaneComposer;
    public final Class<? extends RestCall<?>> restCallType;
    public final ActionDefinition activityAlias;
    public final ActionCategory category;
    public final Boolean readonly;

    private ActionDefinition(
            final LocTextKey title,
            final Class<? extends TemplateComposer> contentPaneComposer) {

        this(title, null, contentPaneComposer, ActionPane.class, null, null, null, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias) {

        this(title, null, contentPaneComposer, ActionPane.class, null, activityAlias, null, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias) {

        this(title, icon, contentPaneComposer, ActionPane.class, null, activityAlias, null, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias,
            final ActionCategory category) {

        this(title, icon, contentPaneComposer, ActionPane.class, null, activityAlias, category, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Class<? extends RestCall<?>> restCallType,
            final ActionDefinition activityAlias) {

        this(title, icon, contentPaneComposer, ActionPane.class, restCallType, activityAlias, null, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Class<? extends RestCall<?>> restCallType,
            final ActionDefinition activityAlias,
            final ActionCategory category) {

        this(title, icon, contentPaneComposer, ActionPane.class, restCallType, activityAlias, category, null);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias,
            final boolean readonly) {

        this(title, icon, contentPaneComposer, ActionPane.class, null, activityAlias, null, readonly);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias,
            final ActionCategory category,
            final boolean readonly) {

        this(title, icon, contentPaneComposer, ActionPane.class, null, activityAlias, category, readonly);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Class<? extends TemplateComposer> actionPaneComposer,
            final Class<? extends RestCall<?>> restCallType,
            final ActionDefinition activityAlias,
            final ActionCategory category,
            final Boolean readonly) {

        this.title = title;
        this.icon = icon;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = actionPaneComposer;
        this.restCallType = restCallType;
        this.activityAlias = activityAlias;
        this.category = (category != null) ? category : ActionCategory.VARIA;
        this.readonly = readonly;
    }

}
