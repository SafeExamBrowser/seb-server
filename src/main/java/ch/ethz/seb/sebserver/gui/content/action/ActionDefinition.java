/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public enum ActionDefinition {

    INSTITUTION_NEW(
            "sebserver.institution.action.new",
            ImageIcon.NEW),

    INSTITUTION_VIEW_FROM_LIST(
            "sebserver.institution.action.list.view",
            ImageIcon.SHOW),

    INSTITUTION_MODIFY_FROM__LIST(
            "sebserver.institution.action.list.modify",
            ImageIcon.EDIT),

    INSTITUTION_MODIFY(
            "sebserver.institution.action.modify",
            ImageIcon.EDIT),

    INSTITUTION_CANCEL_MODIFY(
            "sebserver.overall.action.modify.cancel",
            ImageIcon.CANCEL),

    INSTITUTION_SAVE(
            "sebserver.institution.action.save",
            ImageIcon.SAVE),

    INSTITUTION_ACTIVATE(
            "sebserver.institution.action.activate",
            ImageIcon.INACTIVE),

    INSTITUTION_DEACTIVATE(
            "sebserver.institution.action.deactivate",
            ImageIcon.ACTIVE),

    INSTITUTION_DELETE(
            "sebserver.institution.action.modify",
            ImageIcon.DELETE),

    USER_ACCOUNT_NEW(
            "sebserver.useraccount.action.new",
            ImageIcon.NEW),

    USER_ACCOUNT_VIEW(
            "sebserver.useraccount.action.view",
            ImageIcon.SHOW),

    USER_ACCOUNT_MODIFY(
            "sebserver.useraccount.action.modify",
            ImageIcon.EDIT),

    USER_ACCOUNT_CANCEL_MODIFY(
            "sebserver.overall.action.modify.cancel",
            ImageIcon.CANCEL),

    USER_ACCOUNT_SAVE(
            "sebserver.useraccount.action.save",
            ImageIcon.SAVE),

    USER_ACCOUNT_ACTIVATE(
            "sebserver.useraccount.action.activate",
            ImageIcon.INACTIVE),

    USER_ACCOUNT_DEACTIVATE(
            "sebserver.useraccount.action.deactivate",
            ImageIcon.ACTIVE),

    USER_ACCOUNT_DELETE(
            "sebserver.useraccount.action.modify",
            ImageIcon.DELETE),
            ;

    public final String name;
    public final ImageIcon icon;

    private ActionDefinition(final String name, final ImageIcon icon) {
        this.name = name;
        this.icon = icon;
    }

}
