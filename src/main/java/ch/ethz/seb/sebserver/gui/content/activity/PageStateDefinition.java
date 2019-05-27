/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import ch.ethz.seb.sebserver.gui.content.ExamForm;
import ch.ethz.seb.sebserver.gui.content.ExamList;
import ch.ethz.seb.sebserver.gui.content.ExamSebConfigMapForm;
import ch.ethz.seb.sebserver.gui.content.IndicatorForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionList;
import ch.ethz.seb.sebserver.gui.content.LmsSetupForm;
import ch.ethz.seb.sebserver.gui.content.LmsSetupList;
import ch.ethz.seb.sebserver.gui.content.QuizDiscoveryList;
import ch.ethz.seb.sebserver.gui.content.SebClientConfigForm;
import ch.ethz.seb.sebserver.gui.content.SebClientConfigList;
import ch.ethz.seb.sebserver.gui.content.SebExamConfigForm;
import ch.ethz.seb.sebserver.gui.content.SebExamConfigList;
import ch.ethz.seb.sebserver.gui.content.SebExamConfigPropForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountChangePasswordForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountList;
import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.page.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageState;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

public enum PageStateDefinition implements PageState {

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

    QUIZ_LIST(Type.LIST_VIEW, QuizDiscoveryList.class, ActivityDefinition.QUIZ_DISCOVERY),

    EXAM_LIST(Type.LIST_VIEW, ExamList.class, ActivityDefinition.EXAM),
    EXAM_VIEW(Type.FORM_VIEW, ExamForm.class, ActivityDefinition.EXAM),
    EXAM_EDIT(Type.FORM_EDIT, ExamForm.class, ActivityDefinition.EXAM),
    EXAM_CONFIG_MAP_EDIT(Type.FORM_EDIT, ExamSebConfigMapForm.class, ActivityDefinition.EXAM),
    INDICATOR_EDIT(Type.FORM_EDIT, IndicatorForm.class, ActivityDefinition.EXAM),

    SEB_CLIENT_CONFIG_LIST(Type.LIST_VIEW, SebClientConfigList.class, ActivityDefinition.SEB_CLIENT_CONFIG),
    SEB_CLIENT_CONFIG_VIEW(Type.FORM_VIEW, SebClientConfigForm.class, ActivityDefinition.SEB_CLIENT_CONFIG),
    SEB_CLIENT_CONFIG_EDIT(Type.FORM_EDIT, SebClientConfigForm.class, ActivityDefinition.SEB_CLIENT_CONFIG),

    SEB_EXAM_CONFIG_LIST(Type.LIST_VIEW, SebExamConfigList.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_VIEW(Type.FORM_VIEW, SebExamConfigPropForm.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_PROP_EDIT(Type.FORM_EDIT, SebExamConfigPropForm.class, ActivityDefinition.SEB_EXAM_CONFIG),
    SEB_EXAM_CONFIG_EDIT(Type.FORM_VIEW, SebExamConfigForm.class, ActivityDefinition.SEB_EXAM_CONFIG),

    ;

    public final Type type;
    public final Class<? extends TemplateComposer> contentPaneComposer;
    public final Class<? extends TemplateComposer> actionPaneComposer;
    public final Activity activityAnchor;

    private PageStateDefinition(
            final Type type,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final Activity activityAnchor) {

        this(type, contentPaneComposer, ActionPane.class, activityAnchor);
    }

    private PageStateDefinition(
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
