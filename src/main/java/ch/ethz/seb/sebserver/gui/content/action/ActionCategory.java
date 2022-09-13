/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public enum ActionCategory {
    FORM(null, 0),
    LIST_VARIA(null, 10),
    INSTITUTION_LIST(new LocTextKey("sebserver.institution.list.actions"), 1),
    USER_ACCOUNT_LIST(new LocTextKey("sebserver.useraccount.list.actions"), 1),
    LMS_SETUP_LIST(new LocTextKey("sebserver.lmssetup.list.actions"), 1),
    QUIZ_LIST(new LocTextKey("sebserver.quizdiscovery.list.actions"), 1),
    EXAM_LIST(new LocTextKey("sebserver.exam.list.actions"), 1),
    EXAM_TEMPLATE_LIST(new LocTextKey("sebserver.examtemplate.list.actions"), 1),
    INDICATOR_TEMPLATE_LIST(new LocTextKey("sebserver.examtemplate.indicator.list.actions"), 1),
    CLIENT_GROUP_TEMPLATE_LIST(new LocTextKey("sebserver.examtemplate.clientgroup.list.actions"), 2),
    EXAM_CONFIG_MAPPING_LIST(new LocTextKey("sebserver.exam.configuration.list.actions"), 1),
    INDICATOR_LIST(new LocTextKey("sebserver.exam.indicator.list.actions"), 2),
    CLIENT_GROUP_LIST(new LocTextKey("sebserver.exam.clientgroup.list.actions"), 3),
    SEB_CLIENT_CONFIG_LIST(new LocTextKey("sebserver.clientconfig.list.actions"), 1),
    SEB_EXAM_CONFIG_LIST(new LocTextKey("sebserver.examconfig.list.actions"), 1),
    SEB_CONFIG_TEMPLATE_LIST(new LocTextKey("sebserver.configtemplate.list.actions"), 1),
    SEB_CONFIG_TEMPLATE_ATTRIBUTE_LIST(new LocTextKey("sebserver.configtemplate.attr.list.actions"), 1),
    RUNNING_EXAM_LIST(new LocTextKey("sebserver.monitoring.exam.list.actions"), 1),
    EXAM_MONITORING_NOTIFICATION_LIST(new LocTextKey(
            "sebserver.monitoring.exam.connection.notificationlist.actions"),
            1),
    EXAM_MONITORING_2(new LocTextKey(
            "sebserver.monitoring.exam.connection.actions.group2"),
            2),
    EXAM_MONITORING_3(new LocTextKey(
            "sebserver.monitoring.exam.connection.actions.group3"),
            3),
    CLIENT_EVENT_LIST(new LocTextKey("sebserver.monitoring.exam.connection.list.actions"), 1),
    LOGS_USER_ACTIVITY_LIST(new LocTextKey("sebserver.userlogs.list.actions"), 1),
    LOGS_SEB_CLIENT_LIST(new LocTextKey("sebserver.userlogs.list.actions"), 1),
    VARIA(new LocTextKey("sebserver.overall.action.category.varia"), 0),
    STATE_FILTER(new LocTextKey("sebserver.exam.monitoring.action.category.statefilter"), 40),
    GROUP_FILTER(new LocTextKey("sebserver.exam.monitoring.action.category.groupfilter"), 50),
    PROCTORING(new LocTextKey("sebserver.exam.overall.action.category.proctoring"), 60),

    FINISHED_EXAM_LIST(new LocTextKey("sebserver.finished.exam.list.actions"), 1);

    public final LocTextKey title;
    public final int slotPosition;

    ActionCategory(final LocTextKey title, final int slotPosition) {
        this.title = title;
        this.slotPosition = slotPosition;
    }

}
