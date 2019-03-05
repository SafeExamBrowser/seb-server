/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.content.InstitutionForm;
import ch.ethz.seb.sebserver.gui.content.InstitutionList;
import ch.ethz.seb.sebserver.gui.content.LmsSetupForm;
import ch.ethz.seb.sebserver.gui.content.LmsSetupList;
import ch.ethz.seb.sebserver.gui.content.UserAccountChangePasswordForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountForm;
import ch.ethz.seb.sebserver.gui.content.UserAccountList;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
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
            INSTITUTION_VIEW_LIST, false),
    INSTITUTION_VIEW_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.view"),
            ImageIcon.SHOW,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.institution.action.list.modify"),
            ImageIcon.EDIT,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST, false),
    INSTITUTION_MODIFY(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.EDIT,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST, false),
    INSTITUTION_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_SAVE(
            new LocTextKey("sebserver.institution.action.save"),
            ImageIcon.SAVE,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_ACTIVATE(
            new LocTextKey("sebserver.institution.action.activate"),
            ImageIcon.INACTIVE,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_DEACTIVATE(
            new LocTextKey("sebserver.institution.action.deactivate"),
            ImageIcon.ACTIVE,
            InstitutionForm.class,
            INSTITUTION_VIEW_LIST),
    INSTITUTION_DELETE(
            new LocTextKey("sebserver.institution.action.modify"),
            ImageIcon.DELETE,
            InstitutionList.class,
            INSTITUTION_VIEW_LIST),

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
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_MODIFY_FROM_LIST(
            new LocTextKey("sebserver.useraccount.action.list.modify"),
            ImageIcon.EDIT,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST, false),
    USER_ACCOUNT_MODIFY(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.EDIT,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST, false),
    USER_ACCOUNT_CANCEL_MODIFY(
            new LocTextKey("sebserver.overall.action.modify.cancel"),
            ImageIcon.CANCEL,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_SAVE(
            new LocTextKey("sebserver.useraccount.action.save"),
            ImageIcon.SAVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_ACTIVATE(
            new LocTextKey("sebserver.useraccount.action.activate"),
            ImageIcon.INACTIVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_DEACTIVATE(
            new LocTextKey("sebserver.useraccount.action.deactivate"),
            ImageIcon.ACTIVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_DELETE(
            new LocTextKey("sebserver.useraccount.action.modify"),
            ImageIcon.DELETE,
            UserAccountList.class,
            USER_ACCOUNT_VIEW_LIST),
    USER_ACCOUNT_CHANGE_PASSOWRD(
            new LocTextKey("sebserver.useraccount.action.change.password"),
            ImageIcon.EDIT,
            UserAccountChangePasswordForm.class,
            USER_ACCOUNT_VIEW_LIST, false),
    USER_ACCOUNT_CHANGE_PASSOWRD_SAVE(
            new LocTextKey("sebserver.useraccount.action.change.password.save"),
            ImageIcon.SAVE,
            UserAccountForm.class,
            USER_ACCOUNT_VIEW_LIST),

    LMS_SETUP_VIEW_LIST(
            new LocTextKey("sebserver.lmssetup.list.title"),
            LmsSetupList.class),
    LMS_SETUP_VIEW_FORM(
            new LocTextKey("sebserver.useraccount.action.form"),
            LmsSetupForm.class,
            USER_ACCOUNT_VIEW_LIST),
            ;

    public final LocTextKey title;
    public final ImageIcon icon;
    public final Class<? extends TemplateComposer> contentPaneComposer;
    public final Class<? extends TemplateComposer> actionPaneComposer;
    public final ActionDefinition activityAlias;
    public final String category;
    public final boolean readonly;

    private ActionDefinition(
            final LocTextKey title,
            final Class<? extends TemplateComposer> contentPaneComposer) {

        this.title = title;
        this.icon = null;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = ActionPane.class;
        this.activityAlias = null;
        this.category = null;
        this.readonly = true;
    }

    private ActionDefinition(
            final LocTextKey title,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias) {

        this.title = title;
        this.icon = null;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = ActionPane.class;
        this.activityAlias = activityAlias;
        this.category = null;
        this.readonly = true;
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias) {

        this.title = title;
        this.icon = icon;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = ActionPane.class;
        this.activityAlias = activityAlias;
        this.category = null;
        this.readonly = true;
    }

    private ActionDefinition(
            final LocTextKey title,
            final ImageIcon icon,
            final Class<? extends TemplateComposer> contentPaneComposer,
            final ActionDefinition activityAlias,
            final boolean readonly) {

        this.title = title;
        this.icon = icon;
        this.contentPaneComposer = contentPaneComposer;
        this.actionPaneComposer = ActionPane.class;
        this.activityAlias = activityAlias;
        this.category = null;
        this.readonly = readonly;
    }

}
