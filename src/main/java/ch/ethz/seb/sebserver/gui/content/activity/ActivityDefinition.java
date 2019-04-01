/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.activity;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.Activity;

public enum ActivityDefinition implements Activity {
    INSTITUTION(new LocTextKey("sebserver.institution.action.list")),
    USER_ACCOUNT(new LocTextKey("sebserver.useraccount.action.list")),
    LMS_SETUP(new LocTextKey("sebserver.lmssetup.action.list")),
    QUIZ_DISCOVERY(new LocTextKey("sebserver.quizdiscovery.action.list")),
    EXAM(new LocTextKey("sebserver.exam.action.list")),
    ;

    public final LocTextKey displayName;

    private ActivityDefinition(final LocTextKey displayName) {
        this.displayName = displayName;
    }

    @Override
    public LocTextKey displayName() {
        return this.displayName;
    }

}
