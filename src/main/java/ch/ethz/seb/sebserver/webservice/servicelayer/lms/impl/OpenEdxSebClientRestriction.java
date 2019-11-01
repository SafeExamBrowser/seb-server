/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenEdxSebClientRestriction {

    @JsonProperty("CONFIG_KEYS")
    public final Collection<String> configKeys;

    @JsonProperty("BROWSER_KEYS")
    public final Collection<String> browserExamKeys;

    @JsonProperty("WHITELIST_PATHS")
    public final Collection<String> whiteListPaths;

    @JsonProperty("BLACKLIST_CHAPTERS")
    public final Collection<String> blacklistChapters;

    @JsonProperty("SEB_PERMISSION_COMPONENTS")
    public final Collection<String> permissionComponents;

    @JsonProperty("USER_BANNING_ENABLED")
    public final boolean banningEnabled;

    @JsonCreator
    protected OpenEdxSebClientRestriction(
            @JsonProperty("CONFIG_KEYS") final Collection<String> configKeys,
            @JsonProperty("BROWSER_KEYS") final Collection<String> browserExamKeys,
            @JsonProperty("WHITELIST_PATHS") final Collection<String> whiteListPaths,
            @JsonProperty("BLACKLIST_CHAPTERS") final Collection<String> blacklistChapters,
            @JsonProperty("SEB_PERMISSION_COMPONENTS") final Collection<String> permissionComponents,
            @JsonProperty("USER_BANNING_ENABLED") final boolean banningEnabled) {

        this.configKeys = Utils.immutableCollectionOf(configKeys);
        this.browserExamKeys = Utils.immutableCollectionOf(browserExamKeys);
        this.whiteListPaths = Utils.immutableCollectionOf(whiteListPaths);
        this.blacklistChapters = Utils.immutableCollectionOf(blacklistChapters);
        this.permissionComponents = Utils.immutableCollectionOf(permissionComponents);
        this.banningEnabled = banningEnabled;
    }

    public Collection<String> getConfigKeys() {
        return this.configKeys;
    }

    public Collection<String> getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public Collection<String> getWhiteListPaths() {
        return this.whiteListPaths;
    }

    public Collection<String> getBlacklistChapters() {
        return this.blacklistChapters;
    }

    public Collection<String> getPermissionComponents() {
        return this.permissionComponents;
    }

    public boolean isBanningEnabled() {
        return this.banningEnabled;
    }
}
