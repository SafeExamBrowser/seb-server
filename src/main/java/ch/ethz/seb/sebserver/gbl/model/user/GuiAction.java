/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** Identifier of one GUI action (button, switch or workflow step) the GUI can gate by role.
 *
 * The values mirror the GUIAction enum of the SEB Server GUI and must stay stable once
 * released; they grow as the GUI adds gated actions. */
@Schema(name = "GuiAction", description = "Identifier of one GUI action (button, switch or workflow step).")
public enum GuiAction {
    // 02 Institution
    // (no actions — the INSTITUTIONS page gate covers all institution controls)

    // 03 Assessment Tool
    // (no actions — the ASSESSMENT_TOOLS page gate covers all assessment-tool controls)

    // 04 Connection Configuration
    // (no actions — the CONNECTION_CONFIGURATIONS page gate covers all connection-configuration controls)

    // 05 Certificate
    // (no actions — the CERTIFICATES page gate covers all certificate controls)

    // 06 User Account
    SHOW_INSTITUTION_COLUMN,
    OFFER_SERVER_ADMIN_ROLE,
    CHOOSE_INSTITUTION,

    // 07 Exam
    EXCLUDE_FROM_DELETION,
    EDIT_FULL_SEB_SETTINGS,
    EDIT_RESTRICTED_SEB_SETTINGS,
    EDIT_BASIC_SETTINGS,
    EDIT_SCREEN_PROCTORING,
    EDIT_SEB_KEYS,
    EDIT_SUPERVISORS,
    EDIT_CLIENT_GROUPS,
    APPLY_DISABLE_TEST_RUN,
    APPLY_SEB_LOCK,
    DOWNLOAD_EXAM_CONNECTION,
    DELETE_EXAM,

    // 08 Analyze
    // (no actions — the ANALYZE_EXAMS page gate covers all analyze controls)

    // 09 Archive
    // (no actions — the ARCHIVE_EXAMS page gate covers all archive controls)

    // 10 Exam Template
    // (no actions — the exam-template page gates cover all exam-template controls)

    // 11 Scheduled Deletion
    // (no actions — the scheduled-deletion page gates cover all scheduled-deletion controls)

    // not yet tabled
    EDIT_INDICATORS,
    SHOW_FINISHED_EXAM_DATA
}
