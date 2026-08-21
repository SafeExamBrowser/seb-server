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
    // 01 Navigation Overview
    NAVIGATION_OVERVIEW,

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

    // 08 Analyze
    ANALYZE_EXAMS,

    // 09 Archive
    ARCHIVE_EXAMS,

    // 10 Exam Template
    EXAM_TEMPLATES,
    CREATE_EXAM_TEMPLATE,
    EXAM_TEMPLATE_DETAIL,

    // 11 Scheduled Deletion
    SCHEDULED_DELETIONS,
    CREATE_SCHEDULED_DELETION,
    SCHEDULED_DELETION_REPORT,

    // 12 Profile
    PROFILE,

    // 13 Monitoring
    MONITORING,
    MONITORING_DETAIL,
    MONITORING_CLIENTS,
    MONITORING_CLIENT_DETAIL,

    // 14 Application Search
    SCREEN_PROCTORING_APPLICATION_SEARCH,

    // 15 Gallery
    GALLERY,

    // 16 SP Recording
    SCREEN_PROCTORING_RECORDING,

    // 17 SP Search
    SCREEN_PROCTORING_SEARCH,

    // not yet tabled
    HOME,

    SETTINGS
}
