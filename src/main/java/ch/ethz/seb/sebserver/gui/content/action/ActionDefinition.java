/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.content.activity.PageStateDefinitionImpl;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

/** Enumeration of static action data for each action within the SEB Server GUI */
public enum ActionDefinition {

    INSTITUTION_VIEW_LIST(
            new LocTextKey("sebserver.institution.action.list"),
            PageStateDefinitionImpl.INSTITUTION_LIST),
    INSTITUTION_VIEW_FORM(
            new LocTextKey("sebserver.institution.action.form"),
            PageStateDefinitionImpl.INSTITUTION_VIEW),
    INSTITUTION_NEW(
            new LocTextKey("sebserver.institution.action.new"),
            ImageIcon.INSTITUTION,
            PageStateDefinitionImpl.INSTITUTION_EDIT,
            ActionCategory.LIST_VARIA),
    INSTITUTION_VIEW_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.INSTITUTION_LIST),
    INSTITUTION_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.INSTITUTION_EDIT,
            ActionCategory.INSTITUTION_LIST),

    INSTITUTION_MODIFY(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.INSTITUTION_EDIT,
            ActionCategory.FORM),
    INSTITUTION_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_SAVE(
            new LocTextKey("sebserver.institution.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_SAVE_AND_ACTIVATE(
            new LocTextKey("sebserver.institution.action.activate"),
            ImageIcon.ACTIVE,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_ACTIVATE(
            new LocTextKey("sebserver.institution.action.activate"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_DEACTIVATE(
            new LocTextKey("sebserver.institution.action.deactivate"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.INSTITUTION_VIEW,
            ActionCategory.FORM),
    INSTITUTION_TOGGLE_ACTIVITY(
            new LocTextKey("sebserver.overall.action.toggle-activity"),
            ImageIcon.SWITCH,
            PageStateDefinitionImpl.INSTITUTION_LIST,
            ActionCategory.INSTITUTION_LIST),

    USER_ACCOUNT_VIEW_LIST(
            new LocTextKey("sebserver.useraccount.action.list"),
            PageStateDefinitionImpl.USER_ACCOUNT_LIST),
    USER_ACCOUNT_VIEW_FORM(
            new LocTextKey("sebserver.useraccount.action.form"),
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW),
    USER_ACCOUNT_NEW(
            new LocTextKey("sebserver.useraccount.action.new"),
            ImageIcon.USER,
            PageStateDefinitionImpl.USER_ACCOUNT_EDIT,
            ActionCategory.LIST_VARIA),
    USER_ACCOUNT_VIEW_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.USER_ACCOUNT_EDIT,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_MODIFY(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.USER_ACCOUNT_EDIT,
            ActionCategory.FORM),
    USER_ACCOUNT_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_SAVE(
            new LocTextKey("sebserver.useraccount.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_SAVE_AND_ACTIVATE(
            new LocTextKey("sebserver.useraccount.action.activate"),
            ImageIcon.ACTIVE,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_ACTIVATE(
            new LocTextKey("sebserver.useraccount.action.activate"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_DEACTIVATE(
            new LocTextKey("sebserver.useraccount.action.deactivate"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),
    USER_ACCOUNT_TOGGLE_ACTIVITY(
            new LocTextKey("sebserver.overall.action.toggle-activity"),
            ImageIcon.SWITCH,
            PageStateDefinitionImpl.USER_ACCOUNT_LIST,
            ActionCategory.USER_ACCOUNT_LIST),
    USER_ACCOUNT_DELETE(
            new LocTextKey("sebserver.useraccount.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),

    USER_ACCOUNT_CHANGE_PASSWORD(
            new LocTextKey("sebserver.useraccount.action.change.password"),
            ImageIcon.SECURE,
            PageStateDefinitionImpl.USER_ACCOUNT_PASSWORD_CHANGE,
            ActionCategory.FORM),
    USER_ACCOUNT_CHANGE_PASSWORD_SAVE(
            new LocTextKey("sebserver.useraccount.action.change.password.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.USER_ACCOUNT_VIEW,
            ActionCategory.FORM),

    LMS_SETUP_VIEW_LIST(
            new LocTextKey("sebserver.lmssetup.action.list"),
            PageStateDefinitionImpl.LMS_SETUP_LIST),
    LMS_SETUP_NEW(
            new LocTextKey("sebserver.lmssetup.action.new"),
            ImageIcon.LMS_SETUP,
            PageStateDefinitionImpl.LMS_SETUP_EDIT,
            ActionCategory.LIST_VARIA),
    LMS_SETUP_VIEW_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.LMS_SETUP_LIST),
    LMS_SETUP_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.lmssetup.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.LMS_SETUP_EDIT,
            ActionCategory.LMS_SETUP_LIST),
    LMS_SETUP_MODIFY(
            new LocTextKey("sebserver.lmssetup.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.LMS_SETUP_EDIT,
            ActionCategory.FORM),
    LMS_SETUP_TEST(
            new LocTextKey("sebserver.lmssetup.action.test"),
            ImageIcon.TEST,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_TEST_EDIT(
            new LocTextKey("sebserver.lmssetup.action.test"),
            ImageIcon.TEST,
            PageStateDefinitionImpl.LMS_SETUP_EDIT,
            ActionCategory.FORM),
    LMS_SETUP_TEST_AND_SAVE(
            new LocTextKey("sebserver.lmssetup.action.testsave"),
            ImageIcon.TEST,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_SAVE(
            new LocTextKey("sebserver.lmssetup.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_SAVE_AND_ACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.activate"),
            ImageIcon.ACTIVE,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_ACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.activate"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_DEACTIVATE(
            new LocTextKey("sebserver.lmssetup.action.deactivate"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.LMS_SETUP_VIEW,
            ActionCategory.FORM),
    LMS_SETUP_TOGGLE_ACTIVITY(
            new LocTextKey("sebserver.overall.action.toggle-activity"),
            ImageIcon.SWITCH,
            PageStateDefinitionImpl.LMS_SETUP_LIST,
            ActionCategory.LMS_SETUP_LIST),

    QUIZ_DISCOVERY_VIEW_LIST(
            new LocTextKey("sebserver.quizdiscovery.action.list"),
            PageStateDefinitionImpl.QUIZ_LIST),
    QUIZ_DISCOVERY_SHOW_DETAILS(
            new LocTextKey("sebserver.quizdiscovery.action.details"),
            ImageIcon.SHOW,
            ActionCategory.QUIZ_LIST),
    QUIZ_DISCOVERY_EXAM_IMPORT(
            new LocTextKey("sebserver.quizdiscovery.action.import"),
            ImageIcon.IMPORT,
            PageStateDefinitionImpl.EXAM_EDIT,
            ActionCategory.QUIZ_LIST),

    EXAM_VIEW_LIST(
            new LocTextKey("sebserver.exam.action.list"),
            PageStateDefinitionImpl.EXAM_LIST),
    EXAM_IMPORT(
            new LocTextKey("sebserver.exam.action.import"),
            ImageIcon.IMPORT,
            PageStateDefinitionImpl.QUIZ_LIST),
    EXAM_VIEW_FROM_CONFIG(
            new LocTextKey("sebserver.exam.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.LIST_VARIA),
    EXAM_VIEW_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.EXAM_LIST),
    EXAM_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.EXAM_EDIT,
            ActionCategory.EXAM_LIST),
    EXAM_MODIFY(
            new LocTextKey("sebserver.exam.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.EXAM_EDIT,
            ActionCategory.FORM),
    EXAM_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_SAVE(
            new LocTextKey("sebserver.exam.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_ACTIVATE(
            new LocTextKey("sebserver.exam.action.activate"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_DEACTIVATE(
            new LocTextKey("sebserver.exam.action.deactivate"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_DELETE(
            new LocTextKey("sebserver.exam.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),

    EXAM_MODIFY_SEB_RESTRICTION_DETAILS(
            new LocTextKey("sebserver.exam.action.sebrestriction.details"),
            ImageIcon.RESTRICTION,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_ENABLE_SEB_RESTRICTION(
            new LocTextKey("sebserver.exam.action.sebrestriction.enable"),
            ImageIcon.UNLOCK,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_DISABLE_SEB_RESTRICTION(
            new LocTextKey("sebserver.exam.action.sebrestriction.disable"),
            ImageIcon.LOCK,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_PROCTORING_ON(
            new LocTextKey("sebserver.exam.proctoring.actions.open"),
            ImageIcon.VISIBILITY,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_PROCTORING_OFF(
            new LocTextKey("sebserver.exam.proctoring.actions.open"),
            ImageIcon.VISIBILITY_OFF,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),

    EXAM_CONFIGURATION_NEW(
            new LocTextKey("sebserver.exam.configuration.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.configuration.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_EXAM_CONFIG_VIEW_PROP(
            new LocTextKey("sebserver.exam.configuration.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_DELETE_FROM_LIST(
            new LocTextKey("sebserver.exam.configuration.action.list.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_EXPORT(
            new LocTextKey("sebserver.exam.configuration.action.export-config"),
            ImageIcon.EXPORT,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_GET_CONFIG_KEY(
            new LocTextKey("sebserver.exam.configuration.action.get-config-key"),
            ImageIcon.SECURE,
            ActionCategory.EXAM_CONFIG_MAPPING_LIST),
    EXAM_CONFIGURATION_SAVE(
            new LocTextKey("sebserver.exam.configuration.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_CONFIGURATION_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),

    EXAM_INDICATOR_NEW(
            new LocTextKey("sebserver.exam.indicator.action.list.new"),
            ImageIcon.INDICATOR,
            PageStateDefinitionImpl.INDICATOR_EDIT,
            ActionCategory.INDICATOR_LIST),
    EXAM_INDICATOR_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.INDICATOR_EDIT,
            ActionCategory.INDICATOR_LIST),
    EXAM_INDICATOR_DELETE_FROM_LIST(
            new LocTextKey("sebserver.exam.indicator.action.list.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.INDICATOR_LIST),
    EXAM_INDICATOR_SAVE(
            new LocTextKey("sebserver.exam.indicator.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_INDICATOR_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),
    EXAM_SEB_CLIENT_CONFIG_EXPORT(
            new LocTextKey("sebserver.exam.action.createClientToStartExam"),
            ImageIcon.EXPORT,
            PageStateDefinitionImpl.EXAM_VIEW,
            ActionCategory.FORM),

    EXAM_TEMPLATE_VIEW_LIST(
            new LocTextKey("sebserver.examtemplate.action.list"),
            PageStateDefinitionImpl.EXAM_TEMPLATE_LIST),
    EXAM_TEMPLATE_VIEW_FROM_LIST(
            new LocTextKey("sebserver.examtemplate.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.EXAM_TEMPLATE_LIST),
    EXAM_TEMPLATE_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.examtemplate.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.EXAM_TEMPLATE_EDIT,
            ActionCategory.EXAM_TEMPLATE_LIST),
    EXAM_TEMPLATE_NEW(
            new LocTextKey("sebserver.examtemplate.action.list.new"),
            ImageIcon.TEMPLATE,
            PageStateDefinitionImpl.EXAM_TEMPLATE_EDIT,
            ActionCategory.LIST_VARIA),
    EXAM_TEMPLATE_MODIFY(
            new LocTextKey("sebserver.examtemplate.form.action.edit"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.EXAM_TEMPLATE_EDIT,
            ActionCategory.FORM),
    EXAM_TEMPLATE_SAVE(
            new LocTextKey("sebserver.examtemplate.form.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),
    EXAM_TEMPLATE_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),
    EXAM_TEMPLATE_DELETE(
            new LocTextKey("sebserver.examtemplate.form.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.EXAM_TEMPLATE_LIST,
            ActionCategory.FORM),
    EXAM_TEMPLATE_PROCTORING_ON(
            new LocTextKey("sebserver.examtemplate.proctoring.actions.open"),
            ImageIcon.VISIBILITY,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),
    EXAM_TEMPLATE_PROCTORING_OFF(
            new LocTextKey("sebserver.examtemplate.proctoring.actions.open"),
            ImageIcon.VISIBILITY_OFF,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),

    INDICATOR_TEMPLATE_NEW(
            new LocTextKey("sebserver.examtemplate.indicator.action.list.new"),
            ImageIcon.INDICATOR,
            PageStateDefinitionImpl.INDICATOR_TEMPLATE_EDIT,
            ActionCategory.INDICATOR_TEMPLATE_LIST),
    INDICATOR_TEMPLATE_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.examtemplate.indicator.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.INDICATOR_TEMPLATE_EDIT,
            ActionCategory.INDICATOR_TEMPLATE_LIST),
    INDICATOR_TEMPLATE_DELETE_FROM_LIST(
            new LocTextKey("sebserver.examtemplate.indicator.action.list.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.INDICATOR_TEMPLATE_LIST),
    INDICATOR_TEMPLATE_SAVE(
            new LocTextKey("sebserver.examtemplate.indicator.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),
    INDICATOR_TEMPLATE_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.EXAM_TEMPLATE_VIEW,
            ActionCategory.FORM),

    SEB_CLIENT_CONFIG_LIST(
            new LocTextKey("sebserver.clientconfig.list.title"),
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_NEW(
            new LocTextKey("sebserver.clientconfig.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_EDIT,
            ActionCategory.LIST_VARIA),
    SEB_CLIENT_CONFIG_VIEW_FROM_LIST(
            new LocTextKey("sebserver.clientconfig.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.clientconfig.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_EDIT,
            ActionCategory.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_MODIFY(
            new LocTextKey("sebserver.clientconfig.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_EDIT,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_SAVE(
            new LocTextKey("sebserver.clientconfig.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_SAVE_AND_ACTIVATE(
            new LocTextKey("sebserver.clientconfig.action.activate"),
            ImageIcon.ACTIVE,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_ACTIVATE(
            new LocTextKey("sebserver.clientconfig.action.activate"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_DEACTIVATE(
            new LocTextKey("sebserver.clientconfig.action.deactivate"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),
    SEB_CLIENT_CONFIG_TOGGLE_ACTIVITY(
            new LocTextKey("sebserver.overall.action.toggle-activity"),
            ImageIcon.SWITCH,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_LIST,
            ActionCategory.SEB_CLIENT_CONFIG_LIST),
    SEB_CLIENT_CONFIG_EXPORT(
            new LocTextKey("sebserver.clientconfig.action.export"),
            ImageIcon.EXPORT,
            PageStateDefinitionImpl.SEB_CLIENT_CONFIG_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_LIST(
            new LocTextKey("sebserver.examconfig.action.list"),
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_NEW(
            new LocTextKey("sebserver.examconfig.action.list.new"),
            ImageIcon.NEW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_EDIT,
            ActionCategory.LIST_VARIA),
    SEB_EXAM_CONFIG_VIEW_PROP_FROM_LIST(
            new LocTextKey("sebserver.examconfig.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_VIEW_PROP(
            new LocTextKey("sebserver.examconfig.action.view.properties"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_IMPORT_TO_NEW_CONFIG(
            new LocTextKey("sebserver.examconfig.action.import-config"),
            ImageIcon.IMPORT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_LIST,
            ActionCategory.LIST_VARIA),

    SEB_EXAM_CONFIG_BULK_STATE_CHANGE(
            new LocTextKey("sebserver.examconfig.list.action.statechange"),
            ImageIcon.SWITCH,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_LIST,
            ActionCategory.SEB_EXAM_CONFIG_LIST),

    SEB_EXAM_CONFIG_BULK_RESET_TO_TEMPLATE(
            new LocTextKey("sebserver.examconfig.list.action.reset"),
            ImageIcon.EXPORT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_LIST,
            ActionCategory.SEB_EXAM_CONFIG_LIST),

    SEB_EXAM_CONFIG_MODIFY_PROP_FROM_LIST(
            new LocTextKey("sebserver.examconfig.action.list.modify.properties"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_EDIT,
            ActionCategory.SEB_EXAM_CONFIG_LIST),
    SEB_EXAM_CONFIG_PROP_MODIFY(
            new LocTextKey("sebserver.examconfig.action.modify.properties"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_MODIFY(
            new LocTextKey("sebserver.examconfig.action.modify"),
            ImageIcon.EDIT_SETTINGS,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_VIEW(
            new LocTextKey("sebserver.examconfig.action.view"),
            ImageIcon.EDIT_SETTINGS,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_DELETE(
            new LocTextKey("sebserver.examconfig.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_PROP_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_PROP_SAVE(
            new LocTextKey("sebserver.examconfig.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_PROP_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_EXPORT_PLAIN_XML(
            new LocTextKey("sebserver.examconfig.action.export.plainxml"),
            ImageIcon.EXPORT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_GET_CONFIG_KEY(
            new LocTextKey("sebserver.examconfig.action.get-config-key"),
            ImageIcon.SECURE,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_IMPORT_TO_EXISTING_CONFIG(
            new LocTextKey("sebserver.examconfig.action.import-settings"),
            ImageIcon.IMPORT,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_COPY_CONFIG_FROM_LIST(
            new LocTextKey("sebserver.examconfig.action.copy"),
            ImageIcon.COPY,
            ActionCategory.SEB_EXAM_CONFIG_LIST),

    SEB_EXAM_CONFIG_RESET_TO_TEMPLATE_SETTINGS(
            new LocTextKey("sebserver.examconfig.action.restore.template.settings"),
            ImageIcon.EXPORT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_COPY_CONFIG(
            new LocTextKey("sebserver.examconfig.action.copy"),
            ImageIcon.COPY,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_COPY_CONFIG_AS_TEMPLATE(
            new LocTextKey("sebserver.examconfig.action.copy-as-template"),
            ImageIcon.TEMPLATE,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_SAVE_TO_HISTORY(
            new LocTextKey("sebserver.examconfig.action.saveToHistory"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_UNDO(
            new LocTextKey("sebserver.examconfig.action.undo"),
            ImageIcon.UNDO,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_EDIT,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_TEMPLATE_LIST(
            new LocTextKey("sebserver.configtemplate.action.list"),
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_NEW(
            new LocTextKey("sebserver.configtemplate.action.list.new"),
            ImageIcon.TEMPLATE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_EDIT,
            ActionCategory.LIST_VARIA),
    SEB_EXAM_CONFIG_TEMPLATE_VIEW_FROM_LIST(
            new LocTextKey("sebserver.configtemplate.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.SEB_CONFIG_TEMPLATE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_VIEW(
            new LocTextKey("sebserver.configtemplate.action.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.configtemplate.action.list.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_EDIT,
            ActionCategory.SEB_CONFIG_TEMPLATE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_MODIFY(
            new LocTextKey("sebserver.configtemplate.action.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_CREATE_CONFIG(
            new LocTextKey("sebserver.configtemplate.action.create-config"),
            ImageIcon.NEW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_SAVE(
            new LocTextKey("sebserver.configtemplate.action.save"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_TEMPLATE_ATTR_EDIT(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.modify"),
            ImageIcon.EDIT,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_ATTRIBUTE_EDIT,
            ActionCategory.SEB_CONFIG_TEMPLATE_ATTRIBUTE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_SET_DEFAULT(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.setdefault"),
            ImageIcon.SAVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.SEB_CONFIG_TEMPLATE_ATTRIBUTE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_REMOVE_VIEW(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.removeview"),
            ImageIcon.REMOVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.SEB_CONFIG_TEMPLATE_ATTRIBUTE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_ATTACH_DEFAULT_VIEW(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.attach-default-view"),
            ImageIcon.ADD,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.SEB_CONFIG_TEMPLATE_ATTRIBUTE_LIST),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_REMOVE_VIEW(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.removeview"),
            ImageIcon.REMOVE,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_ATTRIBUTE_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_ATTACH_DEFAULT_VIEW(
            new LocTextKey("sebserver.configtemplate.attr.list.actions.attach-default-view"),
            ImageIcon.ADD,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_ATTRIBUTE_EDIT,
            ActionCategory.FORM),

    SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_SET_DEFAULT(
            new LocTextKey("sebserver.configtemplate.attr.action.setdefault"),
            ImageIcon.UNDO,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_ATTRIBUTE_EDIT,
            ActionCategory.FORM),
    SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_EDIT_TEMPLATE(
            new LocTextKey("sebserver.configtemplate.attr.action.template"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.SEB_EXAM_CONFIG_TEMPLATE_VIEW,
            ActionCategory.FORM),

    SEB_CERTIFICATE_LIST(
            new LocTextKey("sebserver.certificate.action.list"),
            PageStateDefinitionImpl.SEB_CERTIFICATE_LIST),
    SEB_CERTIFICATE_IMPORT(
            new LocTextKey("sebserver.certificate.action.import"),
            ImageIcon.IMPORT,
            PageStateDefinitionImpl.SEB_CERTIFICATE_LIST,
            ActionCategory.FORM),
    SEB_CERTIFICATE_REMOVE(
            new LocTextKey("sebserver.certificate.action.remove"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.SEB_CERTIFICATE_LIST,
            ActionCategory.FORM),

    RUNNING_EXAM_VIEW_LIST(
            new LocTextKey("sebserver.monitoring.action.list"),
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM_LIST),
    MONITOR_EXAM_FROM_LIST(
            new LocTextKey("sebserver.monitoring.exam.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.RUNNING_EXAM_LIST),
    MONITOR_EXAM_CLIENT_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.MONITORING_CLIENT_CONNECTION,
            ActionCategory.CLIENT_EVENT_LIST),
    MONITOR_EXAM_CLIENT_CONNECTION_QUIT(
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit"),
            ImageIcon.SEND_QUIT,
            PageStateDefinitionImpl.MONITORING_CLIENT_CONNECTION,
            ActionCategory.FORM),
    MONITOR_EXAM_CLIENT_CONNECTION_PROCTORING(
            new LocTextKey("sebserver.monitoring.exam.connection.action.proctoring"),
            ImageIcon.PROCTOR_SINGLE,
            PageStateDefinitionImpl.MONITORING_CLIENT_CONNECTION,
            ActionCategory.FORM),
    MONITOR_EXAM_CLIENT_CONNECTION_EXAM_ROOM_PROCTORING(
            new LocTextKey("sebserver.monitoring.exam.connection.action.proctoring.examroom"),
            ImageIcon.PROCTOR_ROOM,
            PageStateDefinitionImpl.MONITORING_CLIENT_CONNECTION,
            ActionCategory.FORM),
    MONITOR_EXAM_CLIENT_CONNECTION_CONFIRM_NOTIFICATION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.confirm.notification"),
            ImageIcon.YES,
            PageStateDefinitionImpl.MONITORING_CLIENT_CONNECTION,
            ActionCategory.EXAM_MONITORING_NOTIFICATION_LIST),

    MONITOR_EXAM_QUIT_SELECTED(
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.selected"),
            ImageIcon.SEND_QUIT,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.CLIENT_EVENT_LIST),
    MONITOR_EXAM_QUIT_ALL(
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.all"),
            ImageIcon.SEND_QUIT,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FORM),
    MONITOR_EXAM_BACK_TO_OVERVIEW(
            new LocTextKey("sebserver.monitoring.exam.action.detail.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FORM),

    MONITOR_EXAM_DISABLE_SELECTED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.disable"),
            ImageIcon.DISABLE,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.CLIENT_EVENT_LIST),

    MONITOR_EXAM_HIDE_REQUESTED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.hide.requested"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),
    MONITOR_EXAM_SHOW_REQUESTED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.show.requested"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),

    MONITOR_EXAM_HIDE_ACTIVE_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.hide.active"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),
    MONITOR_EXAM_SHOW_ACTIVE_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.show.active"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),

    MONITOR_EXAM_HIDE_CLOSED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.hide.closed"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),
    MONITOR_EXAM_SHOW_CLOSED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.show.closed"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),
    MONITOR_EXAM_HIDE_DISABLED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.hide.disabled"),
            ImageIcon.TOGGLE_OFF,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),
    MONITOR_EXAM_SHOW_DISABLED_CONNECTION(
            new LocTextKey("sebserver.monitoring.exam.connection.action.show.disabled"),
            ImageIcon.TOGGLE_ON,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FILTER),

    MONITORING_EXAM_SEARCH_CONNECTIONS(
            new LocTextKey("sebserver.monitoring.search.action"),
            ImageIcon.SEARCH,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.FORM),
    MONITORING_EXAM_SEARCH_VIEW_CONNECTION(
            new LocTextKey("sebserver.monitoring.search.action.view"),
            ImageIcon.SEARCH,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.CLIENT_EVENT_LIST),

    MONITOR_EXAM_NEW_PROCTOR_ROOM(
            new LocTextKey("sebserver.monitoring.exam.action.newroom"),
            ImageIcon.VISIBILITY,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.PROCTORING),
    MONITOR_EXAM_VIEW_PROCTOR_ROOM(
            new LocTextKey("sebserver.monitoring.exam.action.viewroom"),
            ImageIcon.PROCTOR_ROOM,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.PROCTORING),
    MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM(
            new LocTextKey("sebserver.monitoring.exam.action.proctoring.openTownhall"),
            ImageIcon.PROCTOR_ROOM,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.PROCTORING),
    MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM(
            new LocTextKey("sebserver.monitoring.exam.action.proctoring.closeTownhall"),
            ImageIcon.CANCEL,
            PageStateDefinitionImpl.MONITORING_RUNNING_EXAM,
            ActionCategory.PROCTORING),

    FINISHED_EXAM_VIEW_LIST(
            new LocTextKey("sebserver.finished.action.list"),
            PageStateDefinitionImpl.FINISHED_EXAM_LIST),
    VIEW_FINISHED_EXAM_FROM_LIST(
            new LocTextKey("sebserver.finished.exam.action.list.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.FINISHED_EXAM,
            ActionCategory.FINISHED_EXAM_LIST),
    VIEW_FINISHED_EXAM_CLIENT_CONNECTION(
            new LocTextKey("sebserver.finished.exam.connection.action.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.FINISHED_CLIENT_CONNECTION,
            ActionCategory.CLIENT_EVENT_LIST),
    FINISHED_EXAM_BACK_TO_OVERVIEW(
            new LocTextKey("sebserver.finished.exam.action.detail.view"),
            ImageIcon.SHOW,
            PageStateDefinitionImpl.FINISHED_EXAM,
            ActionCategory.FORM),

    LOGS_USER_ACTIVITY_LIST(
            new LocTextKey("sebserver.logs.activity.userlogs"),
            PageStateDefinitionImpl.USER_ACTIVITY_LOGS),
    LOGS_USER_ACTIVITY_SHOW_DETAILS(
            new LocTextKey("sebserver.logs.activity.userlogs.details"),
            ImageIcon.SHOW,
            ActionCategory.LOGS_USER_ACTIVITY_LIST),
    LOGS_USER_ACTIVITY_DELETE_ALL(
            new LocTextKey("sebserver.userlogs.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.USER_ACTIVITY_LOGS,
            ActionCategory.LOGS_USER_ACTIVITY_LIST),

    LOGS_SEB_CLIENT(
            new LocTextKey("sebserver.logs.activity.seblogs"),
            PageStateDefinitionImpl.SEB_CLIENT_LOGS),
    LOGS_SEB_CLIENT_SHOW_DETAILS(
            new LocTextKey("sebserver.logs.activity.seblogs.details"),
            ImageIcon.SHOW,
            ActionCategory.LOGS_SEB_CLIENT_LIST),
    LOGS_SEB_CLIENT_DELETE_ALL(
            new LocTextKey("sebserver.seblogs.action.delete"),
            ImageIcon.DELETE,
            PageStateDefinitionImpl.SEB_CLIENT_LOGS,
            ActionCategory.LOGS_SEB_CLIENT_LIST),
    LOGS_SEB_CLIENT_EXPORT_CSV(
            new LocTextKey("sebserver.seblogs.action.export.csv"),
            ImageIcon.EXPORT,
            ActionCategory.LOGS_SEB_CLIENT_LIST),

    ;

    public final LocTextKey title;
    public final ImageIcon icon;
    public final PageStateDefinition targetState;
    public final ActionCategory category;

    ActionDefinition(final LocTextKey title, final PageStateDefinition targetState) {
        this(title, null, targetState, ActionCategory.VARIA);
    }

    ActionDefinition(final LocTextKey title, final ImageIcon icon, final PageStateDefinition targetState) {
        this(title, icon, targetState, ActionCategory.VARIA);
    }

    ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final ActionCategory category) {

        this(title, icon, null, category);
    }

    ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final PageStateDefinition targetState,
            final ActionCategory category) {

        this.title = title;
        this.icon = icon;
        this.targetState = targetState;
        this.category = category;
    }

    public LocTextKey getTitle(final Object... args) {
        return new LocTextKey(this.title.name, args);
    }

}
