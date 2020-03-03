/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

public interface PageStateDefinition {

    enum Type {
        UNDEFINED,
        LIST_VIEW,
        FORM_VIEW,
        FORM_EDIT,
        FORM_IN_TIME_EDIT
    }

    String name();

    Type type();

    Class<? extends TemplateComposer> contentPaneComposer();

    Class<? extends TemplateComposer> actionPaneComposer();

    Activity activityAnchor();
}
