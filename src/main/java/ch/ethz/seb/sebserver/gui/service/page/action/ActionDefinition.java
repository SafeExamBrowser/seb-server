/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory.ImageIcon;

public enum ActionDefinition {

    INSTITUTION_NEW(
            "sebserver.institution.action.new",
            ImageIcon.NEW),

    INSTITUTION_VIEW(
            "sebserver.institution.action.view",
            ImageIcon.SHOW),

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

    ;

    public final String name;
    public final ImageIcon icon;

    private ActionDefinition(final String name, final ImageIcon icon) {
        this.name = name;
        this.icon = icon;
    }

}
