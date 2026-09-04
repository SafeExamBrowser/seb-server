/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.user.GuiAbilities;
import ch.ethz.seb.sebserver.gbl.model.user.GuiAction;
import ch.ethz.seb.sebserver.gbl.model.user.GuiComponent;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

/** Hand-maintained definition of the GUI abilities per user role.
 * <p>
 * This is GUI presentation configuration, not domain authorization: the webservice keeps
 * enforcing every request through its own privilege checks regardless of this data. */
public final class GuiAbilitiesDefinition {

    static final Map<UserRole, Set<GuiComponent>> COMPONENTS_BY_ROLE = Map.of(

            UserRole.SEB_SERVER_ADMIN, Set.of(
                    // 01 Navigation Overview
                    GuiComponent.NAVIGATION_OVERVIEW,

                    // 02 Institution
                    GuiComponent.INSTITUTIONS,
                    GuiComponent.EDIT_INSTITUTION,
                    GuiComponent.CREATE_INSTITUTION,

                    // 06 User Account
                    GuiComponent.USER_ACCOUNTS,
                    GuiComponent.EDIT_USER_ACCOUNT,
                    GuiComponent.CREATE_USER_ACCOUNT,

                    // 12 Profile
                    GuiComponent.PROFILE,

                    // not yet tabled
                    GuiComponent.SETTINGS),

            UserRole.INSTITUTIONAL_ADMIN, Set.of(
                    // 01 Navigation Overview
                    GuiComponent.NAVIGATION_OVERVIEW,

                    // 03 Assessment Tool
                    GuiComponent.ASSESSMENT_TOOLS,
                    GuiComponent.EDIT_ASSESSMENT_TOOL,
                    GuiComponent.CREATE_ASSESSMENT_TOOL,

                    // 04 Connection Configuration
                    GuiComponent.CONNECTION_CONFIGURATIONS,
                    GuiComponent.EDIT_CONNECTION_CONFIGURATION,
                    GuiComponent.CREATE_CONNECTION_CONFIGURATION,

                    // 05 Certificate
                    GuiComponent.CERTIFICATES,

                    // 06 User Account
                    GuiComponent.USER_ACCOUNTS,
                    GuiComponent.EDIT_USER_ACCOUNT,
                    GuiComponent.CREATE_USER_ACCOUNT,

                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.ADD_EXAM_WITH_URL,
                    GuiComponent.CREATE_EXAM_WIZARD,
                    GuiComponent.EXAM_DETAIL,

                    // 08 Analyze
                    GuiComponent.ANALYZE_EXAMS,

                    // 09 Archive
                    GuiComponent.ARCHIVE_EXAMS,

                    // 10 Exam Template
                    GuiComponent.EXAM_TEMPLATES,
                    GuiComponent.CREATE_EXAM_TEMPLATE,
                    GuiComponent.EXAM_TEMPLATE_DETAIL,

                    // 11 Scheduled Deletion
                    GuiComponent.SCHEDULED_DELETIONS,
                    GuiComponent.CREATE_SCHEDULED_DELETION,
                    GuiComponent.SCHEDULED_DELETION_REPORT,

                    // 12 Profile
                    GuiComponent.PROFILE,

                    // 13 Monitoring
                    GuiComponent.MONITORING,
                    GuiComponent.MONITORING_DETAIL,
                    GuiComponent.MONITORING_CLIENTS,
                    GuiComponent.MONITORING_CLIENT_DETAIL,

                    // 14 Application Search
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,

                    // 15 Gallery
                    GuiComponent.GALLERY,

                    // 16 SP Recording
                    GuiComponent.SCREEN_PROCTORING_RECORDING,

                    // 17 SP Search
                    GuiComponent.SCREEN_PROCTORING_SEARCH,

                    // not yet tabled
                    GuiComponent.HOME,
                    GuiComponent.SETTINGS),

            UserRole.EXAM_ADMIN, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.CREATE_EXAM_WIZARD,
                    GuiComponent.EXAM_DETAIL,

                    // 12 Profile
                    GuiComponent.PROFILE,

                    // 13 Monitoring
                    GuiComponent.MONITORING,
                    GuiComponent.MONITORING_DETAIL,
                    GuiComponent.MONITORING_CLIENTS,
                    GuiComponent.MONITORING_CLIENT_DETAIL,

                    // 14 Application Search
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,

                    // 15 Gallery
                    GuiComponent.GALLERY,

                    // 16 SP Recording
                    GuiComponent.SCREEN_PROCTORING_RECORDING,

                    // 17 SP Search
                    GuiComponent.SCREEN_PROCTORING_SEARCH,

                    // not yet tabled
                    GuiComponent.HOME),

            UserRole.EXAM_SUPPORTER, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.EXAM_DETAIL,

                    // 12 Profile
                    GuiComponent.PROFILE,

                    // 13 Monitoring
                    GuiComponent.MONITORING,
                    GuiComponent.MONITORING_DETAIL,
                    GuiComponent.MONITORING_CLIENTS,
                    GuiComponent.MONITORING_CLIENT_DETAIL,

                    // 14 Application Search
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,

                    // 15 Gallery
                    GuiComponent.GALLERY,

                    // 16 SP Recording
                    GuiComponent.SCREEN_PROCTORING_RECORDING,

                    // 17 SP Search
                    GuiComponent.SCREEN_PROCTORING_SEARCH,

                    // not yet tabled
                    GuiComponent.HOME),

            UserRole.TEACHER, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.EXAM_DETAIL,

                    // 12 Profile
                    GuiComponent.PROFILE,

                    // 13 Monitoring
                    GuiComponent.MONITORING,
                    GuiComponent.MONITORING_DETAIL,
                    GuiComponent.MONITORING_CLIENTS,
                    GuiComponent.MONITORING_CLIENT_DETAIL,

                    // 14 Application Search
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,

                    // 15 Gallery
                    GuiComponent.GALLERY,

                    // 16 SP Recording
                    GuiComponent.SCREEN_PROCTORING_RECORDING,

                    // 17 SP Search
                    GuiComponent.SCREEN_PROCTORING_SEARCH));

    static final Map<UserRole, Set<GuiAction>> ACTIONS_BY_ROLE = Map.of(

            // 01 Navigation Overview: no actions — items derive from their target page components
            // 02 Institution: no actions — the INSTITUTIONS page gate covers all controls
            UserRole.SEB_SERVER_ADMIN, Set.of(
                    // 06 User Account
                    GuiAction.SHOW_INSTITUTION_COLUMN,
                    GuiAction.OFFER_SERVER_ADMIN_ROLE,
                    GuiAction.CHOOSE_INSTITUTION,

                    // 12 Profile
                    GuiAction.EDIT_PROFILE_FIELDS,
                    GuiAction.CHANGE_OWN_PASSWORD),

            // 03 Assessment Tool: no actions — the ASSESSMENT_TOOLS page gate covers all controls
            // 04 Connection Configuration: no actions — the CONNECTION_CONFIGURATIONS page gate covers all controls
            // 05 Certificate: no actions — the CERTIFICATES page gate covers all controls
            // 08 Analyze: no actions — the ANALYZE_EXAMS page gate covers all controls
            // 09 Archive: no actions — the ARCHIVE_EXAMS page gate covers all controls
            // 10 Exam Template: no actions — the exam-template page gates cover all controls
            // 11 Scheduled Deletion: no actions — the scheduled-deletion page gates cover all controls
            // 14-17 Screen Proctoring: no actions — the page gates cover all controls
            UserRole.INSTITUTIONAL_ADMIN, Set.of(
                    // 07 Exam
                    GuiAction.EXCLUDE_FROM_DELETION,
                    GuiAction.EDIT_FULL_SEB_SETTINGS,
                    GuiAction.EDIT_BASIC_SETTINGS,
                    GuiAction.EDIT_SCREEN_PROCTORING,
                    GuiAction.SHOW_SEB_KEYS,
                    GuiAction.EDIT_SUPERVISORS,
                    GuiAction.EDIT_CLIENT_GROUPS,
                    GuiAction.APPLY_DISABLE_TEST_RUN,
                    GuiAction.APPLY_SEB_LOCK,
                    GuiAction.DOWNLOAD_EXAM_CONNECTION,
                    GuiAction.DELETE_EXAM,

                    // 12 Profile
                    GuiAction.EDIT_PROFILE_FIELDS,
                    GuiAction.CHANGE_OWN_PASSWORD,

                    // 13 Monitoring
                    GuiAction.QUIT_ALL_CLIENTS,
                    GuiAction.QUIT_CLIENTS),

            UserRole.EXAM_ADMIN, Set.of(
                    // 07 Exam
                    GuiAction.EDIT_RESTRICTED_SEB_SETTINGS,
                    GuiAction.EDIT_BASIC_SETTINGS,
                    GuiAction.SHOW_SEB_KEYS,
                    GuiAction.EDIT_SUPERVISORS,
                    GuiAction.EDIT_CLIENT_GROUPS,
                    GuiAction.APPLY_DISABLE_TEST_RUN,
                    GuiAction.APPLY_SEB_LOCK,
                    GuiAction.DOWNLOAD_EXAM_CONNECTION,
                    GuiAction.DELETE_EXAM,

                    // 12 Profile
                    GuiAction.EDIT_PROFILE_FIELDS,
                    GuiAction.CHANGE_OWN_PASSWORD,

                    // 13 Monitoring
                    GuiAction.QUIT_ALL_CLIENTS,
                    GuiAction.QUIT_CLIENTS,

                    // not yet tabled
                    GuiAction.EDIT_INDICATORS,
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            UserRole.EXAM_SUPPORTER, Set.of(
                    // 12 Profile
                    GuiAction.EDIT_PROFILE_FIELDS,
                    GuiAction.CHANGE_OWN_PASSWORD,

                    // not yet tabled
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            UserRole.TEACHER, Set.of(
                    // 07 Exam
                    GuiAction.EDIT_RESTRICTED_SEB_SETTINGS,
                    GuiAction.APPLY_DISABLE_TEST_RUN));

    private GuiAbilitiesDefinition() {
    }

    public static GuiAbilities abilitiesFor(final Collection<UserRole> userRoles) {
        final EnumSet<GuiComponent> components = EnumSet.noneOf(GuiComponent.class);
        final EnumSet<GuiAction> actions = EnumSet.noneOf(GuiAction.class);

        for (final UserRole role : userRoles) {
            components.addAll(COMPONENTS_BY_ROLE.getOrDefault(role, Set.of()));
            actions.addAll(ACTIONS_BY_ROLE.getOrDefault(role, Set.of()));
        }

        return new GuiAbilities(List.copyOf(components), List.copyOf(actions));
    }
}
