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
 *
 * This is GUI presentation configuration, not domain authorization: the webservice keeps
 * enforcing every request through its own privilege checks regardless of this data. */
public final class GuiAbilitiesDefinition {

    static final Map<UserRole, Set<GuiComponent>> COMPONENTS_BY_ROLE = Map.of(

            UserRole.SEB_SERVER_ADMIN, Set.of(
                    // 02 Institution
                    GuiComponent.INSTITUTIONS,
                    GuiComponent.EDIT_INSTITUTION,
                    GuiComponent.CREATE_INSTITUTION,

                    // 06 User Account
                    GuiComponent.USER_ACCOUNTS,
                    GuiComponent.EDIT_USER_ACCOUNT,
                    GuiComponent.CREATE_USER_ACCOUNT,

                    // not yet tabled
                    GuiComponent.NAVIGATION_OVERVIEW,
                    GuiComponent.SETTINGS),

            UserRole.INSTITUTIONAL_ADMIN, Set.of(
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

                    // 13 Monitoring
                    GuiComponent.MONITORING,

                    // not yet tabled
                    GuiComponent.NAVIGATION_OVERVIEW,
                    GuiComponent.HOME,
                    GuiComponent.SETTINGS,
                    GuiComponent.EXAM_TEMPLATE,
                    GuiComponent.ANALYZE_EXAMS,
                    GuiComponent.ARCHIVE_EXAMS,
                    GuiComponent.SCHEDULED_DELETION),

            UserRole.EXAM_ADMIN, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.CREATE_EXAM_WIZARD,
                    GuiComponent.EXAM_DETAIL,

                    // 13 Monitoring
                    GuiComponent.MONITORING,

                    // not yet tabled
                    GuiComponent.HOME,
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,
                    GuiComponent.ANALYZE_EXAMS),

            UserRole.EXAM_SUPPORTER, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.EXAM_DETAIL,

                    // 13 Monitoring
                    GuiComponent.MONITORING,

                    // not yet tabled
                    GuiComponent.HOME,
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH),

            UserRole.TEACHER, Set.of(
                    // 07 Exam
                    GuiComponent.EXAMS,
                    GuiComponent.EXAM_DETAIL,

                    // 13 Monitoring
                    GuiComponent.MONITORING,

                    // not yet tabled
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH));

    static final Map<UserRole, Set<GuiAction>> ACTIONS_BY_ROLE = Map.of(

            // 02 Institution: no actions — the INSTITUTIONS page gate covers all controls
            UserRole.SEB_SERVER_ADMIN, Set.of(
                    // 06 User Account
                    GuiAction.SHOW_INSTITUTION_COLUMN,
                    GuiAction.OFFER_SERVER_ADMIN_ROLE,
                    GuiAction.CHOOSE_INSTITUTION),

            // 03 Assessment Tool: no actions — the ASSESSMENT_TOOLS page gate covers all controls
            // 04 Connection Configuration: no actions — the CONNECTION_CONFIGURATIONS page gate covers all controls
            // 05 Certificate: no actions — the CERTIFICATES page gate covers all controls
            UserRole.INSTITUTIONAL_ADMIN, Set.of(
                    // 07 Exam
                    GuiAction.EXCLUDE_FROM_DELETION,
                    GuiAction.EDIT_FULL_SEB_SETTINGS,
                    GuiAction.EDIT_BASIC_SETTINGS,
                    GuiAction.EDIT_SCREEN_PROCTORING,
                    GuiAction.EDIT_SEB_KEYS,
                    GuiAction.EDIT_SUPERVISORS,
                    GuiAction.EDIT_CLIENT_GROUPS,
                    GuiAction.APPLY_DISABLE_TEST_RUN,
                    GuiAction.APPLY_SEB_LOCK,
                    GuiAction.DOWNLOAD_EXAM_CONNECTION,
                    GuiAction.DELETE_EXAM,

                    // not yet tabled
                    GuiAction.ARCHIVE_EXAM),

            UserRole.EXAM_ADMIN, Set.of(
                    // 07 Exam
                    GuiAction.EDIT_RESTRICTED_SEB_SETTINGS,
                    GuiAction.EDIT_BASIC_SETTINGS,
                    GuiAction.EDIT_SEB_KEYS,
                    GuiAction.EDIT_SUPERVISORS,
                    GuiAction.EDIT_CLIENT_GROUPS,
                    GuiAction.APPLY_DISABLE_TEST_RUN,
                    GuiAction.APPLY_SEB_LOCK,
                    GuiAction.DOWNLOAD_EXAM_CONNECTION,
                    GuiAction.DELETE_EXAM,

                    // not yet tabled
                    GuiAction.EDIT_INDICATORS,
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            UserRole.EXAM_SUPPORTER, Set.of(
                    // not yet tabled
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            UserRole.TEACHER, Set.of(
                    // 07 Exam
                    GuiAction.EDIT_RESTRICTED_SEB_SETTINGS,
                    GuiAction.EDIT_CLIENT_GROUPS,
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
