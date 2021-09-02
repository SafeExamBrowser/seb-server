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
    SEB_ADMINISTRATION(new LocTextKey("sebserver.overall.activity.title.serveradmin")),
    INSTITUTION(new LocTextKey("sebserver.institution.action.list")),
    USER_ACCOUNT(new LocTextKey("sebserver.useraccount.action.list")),
    USER_ACTIVITY_LOGS(new LocTextKey("sebserver.logs.activity.userlogs")),
    LMS_SETUP(new LocTextKey("sebserver.lmssetup.action.list")),
    QUIZ_DISCOVERY(new LocTextKey("sebserver.quizdiscovery.action.list")),
    EXAM_ADMINISTRATION(new LocTextKey("sebserver.overall.activity.title.examadmin")),
    EXAM(new LocTextKey("sebserver.exam.action.list")),
    EXAM_TEMPLATE(new LocTextKey("sebserver.examtemplate.action.list")),
    SEB_CONFIGURATION(new LocTextKey("sebserver.overall.activity.title.sebconfig")),
    SEB_CLIENT_CONFIG(new LocTextKey("sebserver.clientconfig.list.title")),
    SEB_EXAM_CONFIG(new LocTextKey("sebserver.examconfig.action.list")),
    SEB_EXAM_CONFIG_TEMPLATE(new LocTextKey("sebserver.configtemplate.action.list")),
    SEB_CERTIFICATE_MANAGEMENT(new LocTextKey("sebserver.certificate.action.list")),
    MONITORING(new LocTextKey("sebserver.overall.activity.title.monitoring")),
    MONITORING_EXAMS(new LocTextKey("sebserver.monitoring.action.list")),
    SEB_CLIENT_LOGS(new LocTextKey("sebserver.logs.activity.seblogs"));

    public final LocTextKey displayName;

    ActivityDefinition(final LocTextKey displayName) {
        this.displayName = displayName;
    }

    @Override
    public LocTextKey displayName() {
        return this.displayName;
    }

}
