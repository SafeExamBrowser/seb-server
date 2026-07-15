/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The effective GUI abilities of one user, merged over the user's roles.
 *
 * This is GUI presentation configuration only. The webservice authorizes every request
 * through its own privilege checks, independently of this data. */
@Schema(
        name = "GuiAbilities",
        description = "Effective GUI abilities of the current user, merged over the user's roles.")
public record GuiAbilities(
        @NotNull
        @ArraySchema(
                arraySchema = @Schema(
                        description = "GUI components the current user may see.",
                        requiredMode = Schema.RequiredMode.REQUIRED))
        @JsonProperty("components")
        List<GuiComponent> components,

        @NotNull
        @ArraySchema(
                arraySchema = @Schema(
                        description = "GUI actions the current user may perform.",
                        requiredMode = Schema.RequiredMode.REQUIRED))
        @JsonProperty("actions")
        List<GuiAction> actions) {

    @JsonCreator
    public GuiAbilities(
            @JsonProperty("components") final List<GuiComponent> components,
            @JsonProperty("actions") final List<GuiAction> actions) {

        this.components = List.copyOf(Objects.requireNonNull(components));
        this.actions = List.copyOf(Objects.requireNonNull(actions));
    }
}
