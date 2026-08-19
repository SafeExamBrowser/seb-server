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
                    GuiComponent.NAVIGATION_OVERVIEW,
                    GuiComponent.SETTINGS,
                    GuiComponent.USER_ACCOUNTS,
                    GuiComponent.INSTITUTIONS),

            UserRole.INSTITUTIONAL_ADMIN, Set.of(
                    GuiComponent.NAVIGATION_OVERVIEW,
                    GuiComponent.HOME,
                    GuiComponent.SETTINGS,
                    GuiComponent.USER_ACCOUNTS,
                    GuiComponent.CONNECTION_CONFIGS,
                    GuiComponent.LMS_SETUPS,
                    GuiComponent.CERTIFICATES,
                    GuiComponent.EXAM_TEMPLATE,
                    GuiComponent.EXAMS,
                    GuiComponent.ANALYZE_EXAMS,
                    GuiComponent.ARCHIVE_EXAMS,
                    GuiComponent.SCHEDULED_DELETION),

            UserRole.EXAM_ADMIN, Set.of(
                    GuiComponent.HOME,
                    GuiComponent.PREPARE_EXAM,
                    GuiComponent.ADD_EXAM_WITH_URL,
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH,
                    GuiComponent.ANALYZE_EXAMS,
                    GuiComponent.EXAMS),

            UserRole.EXAM_SUPPORTER, Set.of(
                    GuiComponent.HOME,
                    GuiComponent.EXAMS,
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH),

            UserRole.TEACHER, Set.of(
                    GuiComponent.EXAMS,
                    GuiComponent.RUNNING_EXAMS,
                    GuiComponent.SCREEN_PROCTORING,
                    GuiComponent.SCREEN_PROCTORING_SEARCH,
                    GuiComponent.SCREEN_PROCTORING_APPLICATION_SEARCH));

    static final Map<UserRole, Set<GuiAction>> ACTIONS_BY_ROLE = Map.of(

            UserRole.SEB_SERVER_ADMIN, Set.of(),

            UserRole.INSTITUTIONAL_ADMIN, Set.of(
                    GuiAction.ARCHIVE_EXAM,
                    GuiAction.DELETE_EXAM,
                    GuiAction.EDIT_FULL_SEB_SETTINGS, // TODO just for testing yet
                    GuiAction.VIEW_ASK_SETTINGS,
                    GuiAction.EXCLUDE_FROM_DELETION),

            UserRole.EXAM_ADMIN, Set.of(
                    GuiAction.EDIT_EXAM_SETTINGS,
                    GuiAction.ARCHIVE_EXAM,
                    GuiAction.DELETE_EXAM,
                    GuiAction.APPLY_TEST_RUN,
                    GuiAction.DISABLE_TEST_RUN,
                    GuiAction.EXPORT_EXAM_CLIENT_CONFIG,
                    GuiAction.VIEW_ASK_SETTINGS,
                    GuiAction.EDIT_ASK_SETTINGS,
                    GuiAction.EDIT_SCREEN_PROCTORING,
                    GuiAction.EDIT_SEB_SETTINGS,
                    GuiAction.EDIT_SUPERVISORS,
                    GuiAction.EDIT_INDICATORS,
                    GuiAction.EDIT_CLIENT_GROUPS,
                    GuiAction.APPLY_SEB_RESTRICTION,
                    GuiAction.SHOW_MONITORING,
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            // to clarify: EDIT_SCREEN_PROCTORING, EDIT_CLIENT_GROUPS, EDIT_SUPERVISORS
            UserRole.EXAM_SUPPORTER, Set.of(
                    GuiAction.EDIT_EXAM_SETTINGS,
                    GuiAction.APPLY_TEST_RUN,
                    GuiAction.DISABLE_TEST_RUN,
                    GuiAction.EXPORT_EXAM_CLIENT_CONFIG,
                    GuiAction.VIEW_ASK_SETTINGS,
                    GuiAction.EDIT_ASK_SETTINGS,
                    GuiAction.EDIT_SEB_SETTINGS,
                    GuiAction.APPLY_SEB_RESTRICTION,
                    GuiAction.SHOW_MONITORING,
                    GuiAction.SHOW_FINISHED_EXAM_DATA),

            UserRole.TEACHER, Set.of(
                    GuiAction.APPLY_TEST_RUN,
                    GuiAction.DISABLE_TEST_RUN,
                    GuiAction.VIEW_ASK_SETTINGS,
                    GuiAction.SHOW_MONITORING));

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
