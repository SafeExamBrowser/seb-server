/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.content.admin.InstitutionForm;
import ch.ethz.seb.sebserver.gui.content.admin.InstitutionList;
import ch.ethz.seb.sebserver.gui.content.admin.UserAccountChangePasswordForm;
import ch.ethz.seb.sebserver.gui.content.admin.UserAccountForm;
import ch.ethz.seb.sebserver.gui.content.admin.UserAccountList;
import ch.ethz.seb.sebserver.gui.content.admin.UserActivityLogs;
import ch.ethz.seb.sebserver.gui.content.configs.CertificateList;
import ch.ethz.seb.sebserver.gui.content.configs.ConfigTemplateAttributeForm;
import ch.ethz.seb.sebserver.gui.content.configs.ConfigTemplateForm;
import ch.ethz.seb.sebserver.gui.content.configs.ConfigTemplateList;
import ch.ethz.seb.sebserver.gui.content.configs.SEBClientConfigForm;
import ch.ethz.seb.sebserver.gui.content.configs.SEBClientConfigList;
import ch.ethz.seb.sebserver.gui.content.configs.SEBExamConfigForm;
import ch.ethz.seb.sebserver.gui.content.configs.SEBExamConfigList;
import ch.ethz.seb.sebserver.gui.content.configs.SEBSettingsForm;
import ch.ethz.seb.sebserver.gui.content.exam.ExamForm;
import ch.ethz.seb.sebserver.gui.content.exam.ExamList;
import ch.ethz.seb.sebserver.gui.content.exam.ExamTemplateForm;
import ch.ethz.seb.sebserver.gui.content.exam.ExamTemplateList;
import ch.ethz.seb.sebserver.gui.content.exam.IndicatorForm;
import ch.ethz.seb.sebserver.gui.content.exam.IndicatorTemplateForm;
import ch.ethz.seb.sebserver.gui.content.exam.LmsSetupForm;
import ch.ethz.seb.sebserver.gui.content.exam.LmsSetupList;
import ch.ethz.seb.sebserver.gui.content.exam.QuizLookupList;
import ch.ethz.seb.sebserver.gui.content.monitoring.MonitoringClientConnection;
import ch.ethz.seb.sebserver.gui.content.monitoring.MonitoringRunningExam;
import ch.ethz.seb.sebserver.gui.content.monitoring.MonitoringRunningExamList;
import ch.ethz.seb.sebserver.gui.content.monitoring.SEBClientEvents;
import ch.ethz.seb.sebserver.gui.service.page.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

public enum PageStateDefinitionImpl implements PageStateDefinition {

    INSTITUTION_LIST(Type.LIST_VIEW, InstitutionList.class, ActivityDefinition.INSTITUTION),
    INSTITUTION_VIEW(Type.FORM_VIEW, InstitutionForm.class, ActivityDefinition.INSTITUTION),
    INSTITUTION_EDIT(Type.FORM_EDIT, InstitutionForm.class, ActivityDefinition.INSTITUTION),

    USER_ACCOUNT_LIST(Type.LIST_VIEW, UserAccountList.class, ActivityDefinition.USER_ACCOUNT),
    USER_ACCOUNT_VIEW(Type.FORM_VIEW, UserAccountForm.class, ActivityDefinition.USER_ACCOUNT),
    USER_ACCOUNT_EDIT(Type.FORM_EDIT, UserAccountForm.class, ActivityDefinition.USER_ACCOUNT),
    USER_ACCOUNT_PASSWORD_CHANGE(Type.FORM_EDIT, UserAccountChangePasswordForm.class, ActivityDefinition.USER_ACCOUNT),

    LMS_SETUP_LIST(Type.LIST_VIEW, LmsSetupList.class, ActivityDefinition.LMS_SETUP),
    LMS_SETUP_VIEW(Type.FORM_VIEW, LmsSetupForm.class, ActivityDefinition.LMS_SETUP),
    LMS_SETUP_EDIT(Type.FORM_EDIT, LmsSetupForm.class, ActivityDefinition.LMS_SETUP),

    QUIZ_LIST(Type.LIST_VIEW, QuizLookupList.class, ActivityDefinition.QUIZ_DISCOVERY),

    EXAM_LIST(Type.LIST_VIEW, ExamList.class, ActivityDefinition.EXAM),
    EXAM_VIEW(Type.FORM_VIEW, ExamForm.class, ActivityDefinition.EXAM),
    EXAM_EDIT(Type.FORM_EDIT, ExamForm.class, ActivityDefinition.EXAM),
    INDICATOR_EDIT(Type.FORM_EDIT, IndicatorForm.class, ActivityDefinition.EXAM),

    EXAM_TEMPLATE_LIST(Type.LIST_VIEW, ExamTemplateList.class, ActivityDefinition.EXAM_TEMPLATE),
    EXAM_TEMPLATE_VIEW(Type.LIST_VIEW, ExamTemplateForm.class, ActivityDefinition.EXAM_TEMPLATE),
    EXAM_TEMPLATE_EDIT(Type.FORM_EDIT, ExamTemplateForm.class, ActivityDefinition.EXAM_TEMPLATE),
    INDICATOR_TEMPLATE_EDIT(Type.FORM_EDIT, IndicatorTemplateForm.class, ActivityDefinition.EXAM_TEMPLATE),

    SEB_CLIENT_CONFIG_LIST(Type.LIST_VIEW, SEBClientConfigList.class, ActivityDefinition.SEB_CLIENT_CONFIG),
    SEB_CLIENT_CONFIG_VIEW(Type.FORM_VIEW, SEBClientConfigForm.class, ActivityDefinition.SEB_CLIENT_CONFIG),
    SEB_CLIENT_CONFIG_EDIT(Type.FORM_EDIT, SEBClientConfigForm.class, ActivityDefinition.SEB_CLIENT_CONFIG),

    SEB_EXAM_CONFIG_LIST(Type.LIST_VIEW, SEBExamConfigList.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_PROP_VIEW(Type.FORM_VIEW, SEBExamConfigForm.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_PROP_EDIT(Type.FORM_EDIT, SEBExamConfigForm.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_EDIT(Type.FORM_IN_TIME_EDIT, SEBSettingsForm.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_VIEW(Type.FORM_VIEW, SEBSettingsForm.class, ActivityDefinition.SEB_EXAM_CONFIG),

    SEB_EXAM_CONFIG_TEMPLATE_LIST(Type.LIST_VIEW, ConfigTemplateList.class,
            ActivityDefinition.SEB_EXAM_CONFIG_TEMPLATE),
    SEB_EXAM_CONFIG_TEMPLATE_VIEW(Type.FORM_VIEW, ConfigTemplateForm.class,
            ActivityDefinition.SEB_EXAM_CONFIG_TEMPLATE),
    SEB_EXAM_CONFIG_TEMPLATE_EDIT(Type.FORM_EDIT, ConfigTemplateForm.class,
            ActivityDefinition.SEB_EXAM_CONFIG_TEMPLATE),
    SEB_EXAM_CONFIG_TEMPLATE_ATTRIBUTE_EDIT(
            Type.FORM_EDIT,
            ConfigTemplateAttributeForm.class,
            ActivityDefinition.SEB_EXAM_CONFIG_TEMPLATE),

    SEB_CERTIFICATE_LIST(Type.LIST_VIEW, CertificateList.class,
            ActivityDefinition.SEB_CERTIFICATE_MANAGEMENT),

    MONITORING_RUNNING_EXAM_LIST(Type.LIST_VIEW, MonitoringRunningExamList.class, ActivityDefinition.MONITORING_EXAMS),
    MONITORING_RUNNING_EXAM(Type.FORM_VIEW, MonitoringRunningExam.class, ActivityDefinition.MONITORING_EXAMS),
    MONITORING_CLIENT_CONNECTION(Type.FORM_VIEW, MonitoringClientConnection.class, ActivityDefinition.MONITORING_EXAMS),

    USER_ACTIVITY_LOGS(Type.LIST_VIEW, UserActivityLogs.class, ActivityDefinition.USER_ACTIVITY_LOGS),
    SEB_CLIENT_LOGS(Type.LIST_VIEW, SEBClientEvents.class, ActivityDefinition.SEB_CLIENT_LOGS)

    ;

    public final Type type;
    public final Class<? extends TemplateComposer> contentPaneComposer;
    public final Class<? extends TemplateComposer> actionPaneComposer;
    public final Activity activityAnchor;

    PageStateDefinitionImpl(
            final Type type,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Activity activityAnchor) {

        this(type, contentPaneComposer, ActionPane.class, activityAnchor);
    }

    PageStateDefinitionImpl(
            final Type type,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Class<? extends TemplateComposer> actionPaneComposer,
            final Activity activityAnchor) {

        this.type = type;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = actionPaneComposer;
        this.activityAnchor = activityAnchor;
    }

    @Override
    public Type type() {
        return this.type;
    }

    @Override
    public Class<? extends TemplateComposer> contentPaneComposer() {
        return this.contentPaneComposer;
    }

    @Override
    public Class<? extends TemplateComposer> actionPaneComposer() {
        return this.actionPaneComposer;
    }

    @Override
    public Activity activityAnchor() {
        return this.activityAnchor;
    }

}
