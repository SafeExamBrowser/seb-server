/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** Identifier of one GUI component (view, page or navigation entry) the GUI can gate by role.
 *
 * The values mirror the GUIComponent enum of the SEB Server GUI and must stay stable once
 * released; they grow as the GUI adds gated components. */
@Schema(name = "GuiComponent", description = "Identifier of one GUI component (view, page or navigation entry).")
public enum GuiComponent {
    // 02 Institution
    INSTITUTIONS,
    EDIT_INSTITUTION,
    CREATE_INSTITUTION,

    // 03 Assessment Tool
    ASSESSMENT_TOOLS,
    EDIT_ASSESSMENT_TOOL,
    CREATE_ASSESSMENT_TOOL,

    // 04 Connection Configuration
    CONNECTION_CONFIGURATIONS,
    EDIT_CONNECTION_CONFIGURATION,
    CREATE_CONNECTION_CONFIGURATION,

    // 05 Certificate
    CERTIFICATES,

    // 06 User Account
    USER_ACCOUNTS,
    EDIT_USER_ACCOUNT,
    CREATE_USER_ACCOUNT,

    // 07 Exam
    EXAMS,
    ADD_EXAM_WITH_URL,
    CREATE_EXAM_WIZARD,
    EXAM_DETAIL,

    // 13 Monitoring
    MONITORING,

    // not yet tabled
    NAVIGATION_OVERVIEW,
    HOME,

    SETTINGS,

    EXAM_TEMPLATE,

    RUNNING_EXAMS,
    SCREEN_PROCTORING,
    SCREEN_PROCTORING_SEARCH,
    SCREEN_PROCTORING_APPLICATION_SEARCH,

    ANALYZE_EXAMS,
    ARCHIVE_EXAMS,
    SCHEDULED_DELETION
}
