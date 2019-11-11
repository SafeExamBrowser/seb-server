/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Collection;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class EdxCourseRestrictionAttributes {

    public enum PermissionComponents {
        ALWAYS_ALLOW_STUFF("AlwaysAllowStaff"),
        CHECK_BROWSER_EXAM_KEY("CheckSEBHashBrowserExamKey"),
        CHECK_CONFIG_KEY("CheckSEBHashConfigKey"),
        CHECK_BROWSER_EXAM_KEY_AND_CONFIG_KEY("CheckSEBHashBrowserExamKeyOrConfigKey");

        public final String name;

        private PermissionComponents(final String name) {
            this.name = name;
        }
    }

    public static final String ATTR_USER_BANNING_ENABLED = "userBanningEnabled";
    public static final String ATTR_SEB_PERMISSION_COMPONENTS = "permissionComponents";
    public static final String ATTR_BLACKLIST_CHAPTERS = "blacklistChapters";
    public static final String ATTR_WHITELIST_PATHS = "whitelistPaths";
    public static final String ATTR_BROWSER_EXAM_KEYS = "browserExamKeys";

    @JsonProperty(ATTR_WHITELIST_PATHS)
    public final Collection<String> whiteListPaths;

    @JsonProperty(ATTR_BLACKLIST_CHAPTERS)
    public final Collection<String> blacklistChapters;

    @JsonProperty(ATTR_SEB_PERMISSION_COMPONENTS)
    public final Collection<String> permissionComponents;

    @JsonProperty(ATTR_USER_BANNING_ENABLED)
    public final Boolean banningEnabled;

    @JsonProperty(ATTR_BROWSER_EXAM_KEYS)
    public final Collection<String> browserExamKeys;

    protected EdxCourseRestrictionAttributes(
            final Collection<String> whiteListPaths,
            final Collection<String> blacklistChapters,
            final Collection<String> permissionComponents,
            final Boolean banningEnabled,
            final Collection<String> browserExamKeys) {

        this.whiteListPaths = Utils.immutableCollectionOf(whiteListPaths);
        this.blacklistChapters = Utils.immutableCollectionOf(blacklistChapters);
        this.permissionComponents = Utils.immutableCollectionOf(permissionComponents);
        this.banningEnabled = BooleanUtils.isTrue(banningEnabled);
        this.browserExamKeys = Utils.immutableCollectionOf(browserExamKeys);
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

    public Boolean getBanningEnabled() {
        return this.banningEnabled;
    }

    public Collection<String> getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EdxCourseRestrictionAttributes [whiteListPaths=");
        builder.append(this.whiteListPaths);
        builder.append(", blacklistChapters=");
        builder.append(this.blacklistChapters);
        builder.append(", permissionComponents=");
        builder.append(this.permissionComponents);
        builder.append(", banningEnabled=");
        builder.append(this.banningEnabled);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append("]");
        return builder.toString();
    }

}
