/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsPublished {

    public static final String ATTR_SETTINGS_PUBLISHED = "settingsPublished";

    @NotNull
    @JsonProperty(ATTR_SETTINGS_PUBLISHED)
    public final Boolean pubished;

    @JsonCreator
    public SettingsPublished(@JsonProperty(ATTR_SETTINGS_PUBLISHED) final Boolean settingsPublished) {
        this.pubished = settingsPublished;
    }

}
