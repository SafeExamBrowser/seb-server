/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class OpenEdxSEBRestriction {

    public static final String ATTR_USER_BANNING_ENABLED = "USER_BANNING_ENABLED";
    public static final String ATTR_PERMISSION_COMPONENTS = "PERMISSION_COMPONENTS";
    public static final String ATTR_BLACKLIST_CHAPTERS = "BLACKLIST_CHAPTERS";
    public static final String ATTR_WHITELIST_PATHS = "WHITELIST_PATHS";
    public static final String ATTR_BROWSER_KEYS = "BROWSER_KEYS";
    public static final String ATTR_CONFIG_KEYS = "CONFIG_KEYS";

    public enum WhiteListPath {
        ABOUT("about"),
        COURSE_OUTLINE("course-outline"),
        COURSE_WARE("courseware"),
        DISCUSSION("discussion"),
        PROGRESS("progress"),
        WIKI("wiki");

        public final String key;

        WhiteListPath(final String key) {
            this.key = key;
        }
    }

    public enum PermissionComponent {
        ALWAYS_ALLOW_STAFF("AlwaysAllowStaff"),
        CHECK_BROWSER_EXAM_KEY("CheckSEBHashBrowserExamKey"),
        CHECK_CONFIG_KEY("CheckSEBHashConfigKey"),
        CHECK_BROWSER_EXAM_OR_CONFIG_KEY("CheckSEBHashBrowserExamKeyOrConfigKey");
        public final String key;

        PermissionComponent(final String key) {
            this.key = key;
        }
    }

    public static final Set<String> ADDITIONAL_PROPERTY_NAMES = Utils.immutableSetOf(
            ATTR_USER_BANNING_ENABLED,
            ATTR_PERMISSION_COMPONENTS,
            ATTR_BLACKLIST_CHAPTERS,
            ATTR_WHITELIST_PATHS);

    public static final Set<String> WHITE_LIST_PATHS = EnumSet.allOf(WhiteListPath.class)
            .stream()
            .map(e -> e.key)
            .collect(Collectors.toSet());

    public static final Set<String> PERMISSION_COMPONENTS = EnumSet.allOf(PermissionComponent.class)
            .stream()
            .map(e -> e.key)
            .collect(Collectors.toSet());

    @JsonProperty(ATTR_CONFIG_KEYS)
    public final Collection<String> configKeys;

    @JsonProperty(ATTR_BROWSER_KEYS)
    public final Collection<String> browserExamKeys;

    @JsonProperty(ATTR_WHITELIST_PATHS)
    public final Collection<String> whiteListPaths;

    @JsonProperty(ATTR_BLACKLIST_CHAPTERS)
    public final Collection<String> blacklistChapters;

    @JsonProperty(ATTR_PERMISSION_COMPONENTS)
    public final Collection<String> permissionComponents;

    @JsonProperty(ATTR_USER_BANNING_ENABLED)
    public final Boolean banningEnabled;

    @JsonCreator
    OpenEdxSEBRestriction(
            @JsonProperty(ATTR_CONFIG_KEYS) final Collection<String> configKeys,
            @JsonProperty(ATTR_BROWSER_KEYS) final Collection<String> browserExamKeys,
            @JsonProperty(ATTR_WHITELIST_PATHS) final Collection<String> whiteListPaths,
            @JsonProperty(ATTR_BLACKLIST_CHAPTERS) final Collection<String> blacklistChapters,
            @JsonProperty(ATTR_PERMISSION_COMPONENTS) final Collection<String> permissionComponents,
            @JsonProperty(ATTR_USER_BANNING_ENABLED) final boolean banningEnabled) {

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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("OpenEdxCourseRestrictionData [configKeys=");
        builder.append(this.configKeys);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append(", whiteListPaths=");
        builder.append(this.whiteListPaths);
        builder.append(", blacklistChapters=");
        builder.append(this.blacklistChapters);
        builder.append(", permissionComponents=");
        builder.append(this.permissionComponents);
        builder.append(", banningEnabled=");
        builder.append(this.banningEnabled);
        builder.append("]");
        return builder.toString();
    }

    public static OpenEdxSEBRestriction from(final SEBRestriction sebRestrictionData) {

        final String whiteListPathsString = sebRestrictionData.additionalProperties
                .get(ATTR_WHITELIST_PATHS);
        Collection<String> whiteListPaths;
        if (StringUtils.isNotBlank(whiteListPathsString)) {
            whiteListPaths = Utils.immutableCollectionOf(Arrays.stream(
                    StringUtils.split(whiteListPathsString, Constants.LIST_SEPARATOR))
                    .filter(WHITE_LIST_PATHS::contains)
                    .collect(Collectors.toList()));
        } else {
            whiteListPaths = Collections.emptyList();
        }

        final String blacklistChaptersString = sebRestrictionData.additionalProperties
                .get(ATTR_BLACKLIST_CHAPTERS);
        Collection<String> blacklistChapters;
        if (StringUtils.isNotBlank(blacklistChaptersString)) {
            blacklistChapters = Utils.immutableCollectionOf(Arrays.asList(
                    StringUtils.split(blacklistChaptersString, Constants.LIST_SEPARATOR)));
        } else {
            blacklistChapters = Collections.emptyList();
        }

        final String permissionComponentsString = sebRestrictionData.additionalProperties
                .get(ATTR_PERMISSION_COMPONENTS);
        Collection<String> permissionComponents;
        if (StringUtils.isNotBlank(permissionComponentsString)) {
            permissionComponents = Utils.immutableCollectionOf(Arrays.stream(
                    StringUtils.split(permissionComponentsString, Constants.LIST_SEPARATOR))
                    .filter(PERMISSION_COMPONENTS::contains)
                    .collect(Collectors.toList()));
        } else {
            final Collection<String> defaultPermissions = new ArrayList<>();
            defaultPermissions.add(PermissionComponent.ALWAYS_ALLOW_STAFF.key);
            if (!sebRestrictionData.configKeys.isEmpty()) {
                defaultPermissions.add(PermissionComponent.CHECK_CONFIG_KEY.key);
            }
            if (!sebRestrictionData.browserExamKeys.isEmpty()) {
                defaultPermissions.add(PermissionComponent.CHECK_BROWSER_EXAM_KEY.key);
            }

            permissionComponents = Utils.immutableCollectionOf(defaultPermissions);
        }

        return new OpenEdxSEBRestriction(
                sebRestrictionData.configKeys,
                sebRestrictionData.browserExamKeys,
                whiteListPaths,
                blacklistChapters,
                permissionComponents,
                BooleanUtils.toBoolean(sebRestrictionData.additionalProperties
                        .get(ATTR_USER_BANNING_ENABLED)));
    }

}
