/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.user.GuiAbilities;
import ch.ethz.seb.sebserver.gbl.model.user.GuiAction;
import ch.ethz.seb.sebserver.gbl.model.user.GuiComponent;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;

public class GuiAbilitiesDefinitionTest {

    @Test
    public void everyRoleHasADefinition() {
        for (final UserRole role : UserRole.values()) {
            assertNotNull(
                    "missing component definition for role " + role,
                    GuiAbilitiesDefinition.COMPONENTS_BY_ROLE.get(role));
            assertNotNull(
                    "missing action definition for role " + role,
                    GuiAbilitiesDefinition.ACTIONS_BY_ROLE.get(role));
        }
    }

    @Test
    public void everyComponentIsGrantedToAtLeastOneRole() {
        final EnumSet<GuiComponent> granted = EnumSet.noneOf(GuiComponent.class);
        GuiAbilitiesDefinition.COMPONENTS_BY_ROLE.values().forEach(granted::addAll);
        assertEquals(EnumSet.allOf(GuiComponent.class), granted);
    }

    @Test
    public void everyActionIsGrantedToAtLeastOneRole() {
        final EnumSet<GuiAction> granted = EnumSet.noneOf(GuiAction.class);
        GuiAbilitiesDefinition.ACTIONS_BY_ROLE.values().forEach(granted::addAll);
        assertEquals(EnumSet.allOf(GuiAction.class), granted);
    }

    @Test
    public void abilitiesForMergesOverAllGivenRoles() {
        final GuiAbilities abilities = GuiAbilitiesDefinition.abilitiesFor(
                EnumSet.of(UserRole.SEB_SERVER_ADMIN, UserRole.TEACHER));

        assertTrue(abilities.components().contains(GuiComponent.INSTITUTIONS));
        assertTrue(abilities.components().contains(GuiComponent.RUNNING_EXAMS));
        assertTrue(abilities.actions().contains(GuiAction.SHOW_MONITORING));
    }

    @Test
    public void abilitiesForWithoutRolesIsEmpty() {
        final GuiAbilities abilities = GuiAbilitiesDefinition.abilitiesFor(
                EnumSet.noneOf(UserRole.class));

        assertTrue(abilities.components().isEmpty());
        assertTrue(abilities.actions().isEmpty());
    }
}
