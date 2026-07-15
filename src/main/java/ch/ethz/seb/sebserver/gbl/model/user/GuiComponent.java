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
    NAVIGATION_OVERVIEW,
    HOME,

    SETTINGS,
    INSTITUTIONS,
    USER_ACCOUNTS,
    CONNECTION_CONFIGS,
    LMS_SETUPS,
    CERTIFICATES,

    EXAM_TEMPLATE,
    PREPARE_EXAM,
    ADD_EXAM_WITH_URL,

    EXAMS,

    RUNNING_EXAMS,
    SCREEN_PROCTORING,
    SCREEN_PROCTORING_SEARCH,
    SCREEN_PROCTORING_APPLICATION_SEARCH,

    ANALYZE_EXAMS,
    ARCHIVE_EXAMS,
    SCHEDULED_DELETION
}
