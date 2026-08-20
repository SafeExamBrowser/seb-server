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

    // not yet tabled
    EDIT_EXAM_SETTINGS,
    ARCHIVE_EXAM,
    DELETE_EXAM,
    APPLY_TEST_RUN,
    DISABLE_TEST_RUN,
    EXPORT_EXAM_CLIENT_CONFIG,
    VIEW_ASK_SETTINGS,
    EDIT_ASK_SETTINGS,
    EDIT_SCREEN_PROCTORING,
    EDIT_SEB_SETTINGS,
    EDIT_FULL_SEB_SETTINGS,
    EDIT_SUPERVISORS,
    EDIT_INDICATORS,
    EDIT_CLIENT_GROUPS,
    APPLY_SEB_RESTRICTION,
    SHOW_MONITORING,
    SHOW_FINISHED_EXAM_DATA,
    EXCLUDE_FROM_DELETION
}
