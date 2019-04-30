/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.content.activity.PageStateDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageState;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

/** Enumeration of static action data for each action within the SEB Server GUI */
public enum ActionDefinition {

    INSTITUTION_VIEW_LIST(
            new LocTextKey("sebserver.institution.action.list"),
            PageStateDefinition.INSTITUTION_LIST),
    INSTITUTION_VIEW_FORM(
            new LocTextKey("sebserver.institution.action.form"),
            PageStateDefinition.INSTITUTION_VIEW),
    INSTITUTION_NEW(
            new LocTextKey("sebserver.institution.action.new"),
            ImageIcon.NEW,
            PageStateDefinition.INSTITUTION_EDIT),
    INSTITUTION_VIEW_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinition.INSTITUTION_VIEW,
            ActionCategory.INSTITUTION_LIST),
    INSTITUTION_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.INSTITUTION_EDIT,
            ActionCategory.INSTITUTION_LIST),
    INSTITUTION_MODIFY(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.INSTITUTION_EDIT,
            ActionCategory.FORM),
    INSTITUTION_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_SAVE(
            new LocTextKey("sebserver.institution.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_ACTIVATE(
            new LocTextKey("sebserver.institution.action.activate"),
            ImageIcon.INACTIVE,
            PageStateDefinition.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_DEACTIVATE(
            new LocTextKey("sebserver.institution.action.deactivate"),
            ImageIcon.ACTIVE,
            PageStateDefinition.INSTITUTION_VIEW,
            ActionCategory.FORM),

    USER_ACCOUNT_VIEW_LIST(
            new LocTextKey("sebserver.useraccount.action.list"),
            PageStateDefinition.USER_ACCOUNT_LIST),
    USER_ACCOUNT_VIEW_FORM(
            new LocTextKey("sebserver.useraccount.action.form"),
            PageStateDefinition.USER_ACCOUNT_VIEW),
    USER_ACCOUNT_NEW(
            new LocTextKey("sebserver.useraccount.action.new"),
            ImageIcon.NEW,
            PageStateDefinition.USER_ACCOUNT_EDIT),
    USER_ACCOUNT_VIEW_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.view"),
            ImageIcon.SHOW,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.USER_ACCOUNT_EDIT,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_MODIFY(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.USER_ACCOUNT_EDIT,
            ActionCategory.FORM),
    USER_ACCOUNT_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_SAVE(
            new LocTextKey("sebserver.useraccount.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_ACTIVATE(
            new LocTextKey("sebserver.useraccount.action.activate"),
            ImageIcon.INACTIVE,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_DEACTIVATE(
            new LocTextKey("sebserver.useraccount.action.deactivate"),
            ImageIcon.ACTIVE,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_CHANGE_PASSOWRD(
            new LocTextKey("sebserver.useraccount.action.change.password"),
            ImageIcon.EDIT,
            PageStateDefinition.USER_ACCOUNT_PASSWORD_CHANGE,
            ActionCategory.FORM),
    USER_ACCOUNT_CHANGE_PASSOWRD_SAVE(
            new LocTextKey("sebserver.useraccount.action.change.password.save"),
            ImageIcon.SAVE,
            PageStateDefinition.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),

    LMS_SETUP_VIEW_LIST(
            new LocTextKey("sebserver.lmssetup.action.list"),
            PageStateDefinition.LMS_SETUP_LIST),
    LMS_SETUP_NEW(
            new LocTextKey("sebserver.lmssetup.action.new"),
            ImageIcon.NEW,
            PageStateDefinition.LMS_SETUP_EDIT),
    LMS_SETUP_VIEW_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.LMS_SETUP_LIST),
    LMS_SETUP_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.LMS_SETUP_EDIT,
            ActionCategory.LMS_SETUP_LIST),
    LMS_SETUP_MODIFY(
            new LocTextKey("sebserver.lmssetup.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.LMS_SETUP_EDIT,
            ActionCategory.FORM),
    LMS_SETUP_SAVE_AND_TEST(
            new LocTextKey("sebserver.lmssetup.action.savetest"),
            ImageIcon.TEST,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_TEST_AND_SAVE(
            new LocTextKey("sebserver.lmssetup.action.testsave"),
            ImageIcon.TEST,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_SAVE(
            new LocTextKey("sebserver.lmssetup.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_ACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.activate"),
            ImageIcon.INACTIVE,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_DEACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.deactivate"),
            ImageIcon.ACTIVE,
            PageStateDefinition.LMS_SETUP_VIEW,
            ActionCategory.FORM),

    QUIZ_DISCOVERY_VIEW_LIST(
            new LocTextKey("sebserver.quizdiscovery.action.list"),
            PageStateDefinition.QUIZ_LIST),
    QUIZ_DISCOVERY_SHOW_DETAILS(
            new LocTextKey("sebserver.quizdiscovery.action.details"),
            ImageIcon.SHOW,
            null,
            ActionCategory.QUIZ_LIST),
    QUIZ_DISCOVERY_EXAM_IMPORT(
            new LocTextKey("sebserver.quizdiscovery.action.import"),
            ImageIcon.IMPORT,
            PageStateDefinition.EXAM_EDIT,
            ActionCategory.QUIZ_LIST),

    EXAM_VIEW_LIST(
            new LocTextKey("sebserver.exam.action.list"),
            PageStateDefinition.EXAM_LIST),
    EXAM_IMPORT(
            new LocTextKey("sebserver.exam.action.import"),
            ImageIcon.IMPORT,
            PageStateDefinition.QUIZ_LIST),
    EXAM_VIEW_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.EXAM_LIST),
    EXAM_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.EXAM_EDIT,
            ActionCategory.EXAM_LIST),
    EXAM_MODIFY(
            new LocTextKey("sebserver.exam.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.EXAM_EDIT,
            ActionCategory.FORM),
    EXAM_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_SAVE(
            new LocTextKey("sebserver.exam.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_ACTIVATE(
            new LocTextKey("sebserver.exam.action.activate"),
            ImageIcon.INACTIVE,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_DEACTIVATE(
            new LocTextKey("sebserver.exam.action.deactivate"),
            ImageIcon.ACTIVE,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),

    EXAM_INDICATOR_NEW(
            new LocTextKey("sebserver.exam.indicator.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinition.INDICATOR_EDIT),
    EXAM_INDICATOR_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.INDICATOR_EDIT,
            ActionCategory.INDICATOR_LIST),
    EXAM_INDICATOR_DELETE_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.delete"),
            ImageIcon.DELETE,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.INDICATOR_LIST),
    EXAM_INDICATOR_SAVE(
            new LocTextKey("sebserver.exam.indicator.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_INDICATOR_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.EXAM_VIEW,
            ActionCategory.FORM),

    SEB_CLIENT_CONFIG_LIST(
            new LocTextKey("sebserver.clientconfig.action.list"),
            PageStateDefinition.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_NEW(
            new LocTextKey("sebserver.clientconfig.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinition.SEB_CLIENT_CONFIG_EDIT),
    SEB_CLIENT_CONFIG_VIEW_FROM_LIST(
            new LocTextKey("sebserver.clientconfig.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.clientconfig.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.SEB_CLIENT_CONFIG_EDIT,
            ActionCategory.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_MODIFY(
            new LocTextKey("sebserver.clientconfig.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.SEB_CLIENT_CONFIG_EDIT,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_SAVE(
            new LocTextKey("sebserver.clientconfig.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_ACTIVATE(
            new LocTextKey("sebserver.clientconfig.action.activate"),
            ImageIcon.INACTIVE,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_DEACTIVATE(
            new LocTextKey("sebserver.clientconfig.action.deactivate"),
            ImageIcon.ACTIVE,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_EXPORT(
            new LocTextKey("sebserver.clientconfig.action.export"),
            ImageIcon.SAVE,
            PageStateDefinition.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_LIST(
            new LocTextKey("sebserver.examconfig.action.list"),
            PageStateDefinition.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_NEW(
            new LocTextKey("sebserver.examconfig.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinition.SEB_EXAM_CONFIG_EDIT),
    SEB_EXAM_CONFIG_VIEW_FROM_LIST(
            new LocTextKey("sebserver.examconfig.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinition.SEB_EXAM_CONFIG_VIEW,
            ActionCategory.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.examconfig.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.SEB_EXAM_CONFIG_EDIT,
            ActionCategory.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_MODIFY(
            new LocTextKey("sebserver.examconfig.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.SEB_EXAM_CONFIG_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinition.SEB_EXAM_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_SAVE(
            new LocTextKey("sebserver.examconfig.action.save"),
            ImageIcon.SAVE,
            PageStateDefinition.SEB_EXAM_CONFIG_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_MODIFY_PROPERTIES_FROM_LIST(
            new LocTextKey("sebserver.examconfig.props.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinition.SEB_EXAM_CONFIG_PROPERTIES_EDIT,
            ActionCategory.SEB_EXAM_CONFIG_LIST),

    ;

    public final LocTextKey title;
    public final ImageIcon icon;
    public final PageState targetState;
    public final ActionCategory category;

    private ActionDefinition(final LocTextKey title, final PageState targetState) {
        this(title, null, targetState, ActionCategory.VARIA);
    }

    private ActionDefinition(final LocTextKey title, final ImageIcon icon, final PageState targetState) {
        this(title, icon, targetState, ActionCategory.VARIA);
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final PageState targetState,
            final ActionCategory category) {

        this.title = title;
        this.icon = icon;
        this.targetState = targetState;
        this.category = category;
    }

}
