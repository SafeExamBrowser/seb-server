/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.institution.EdxCourseRestrictionAttributes;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SebRestrictionData;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenEdxCourseRestrictionData {

    public static final String ATTR_USER_BANNING_ENABLED = "USER_BANNING_ENABLED";
    public static final String ATTR_PERMISSION_COMPONENTS = "PERMISSION_COMPONENTS";
    public static final String ATTR_BLACKLIST_CHAPTERS = "BLACKLIST_CHAPTERS";
    public static final String ATTR_WHITELIST_PATHS = "WHITELIST_PATHS";
    public static final String ATTR_BROWSER_KEYS = "BROWSER_KEYS";
    public static final String ATTR_CONFIG_KEYS = "CONFIG_KEYS";

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
    public final boolean banningEnabled;

    @JsonCreator
    public OpenEdxCourseRestrictionData(
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

    public OpenEdxCourseRestrictionData(final SebRestrictionData data) {
        this.configKeys = Utils.immutableCollectionOf(data.configKeys);
        this.browserExamKeys = Utils.immutableCollectionOf(data.browserExamKeys);

        final String whiteListPaths = data.additionalAttributes.get(ATTR_WHITELIST_PATHS);
        if (StringUtils.isNotBlank(whiteListPaths)) {
            this.whiteListPaths = Utils.immutableCollectionOf(Arrays.asList(
                    StringUtils.split(whiteListPaths, Constants.LIST_SEPARATOR)));
        } else {
            this.whiteListPaths = Collections.emptyList();
        }

        final String blacklistChapters = data.additionalAttributes.get(ATTR_BLACKLIST_CHAPTERS);
        if (StringUtils.isNotBlank(blacklistChapters)) {
            this.blacklistChapters = Utils.immutableCollectionOf(Arrays.asList(
                    StringUtils.split(blacklistChapters, Constants.LIST_SEPARATOR)));
        } else {
            this.blacklistChapters = Collections.emptyList();
        }

        final String permissionComponents = data.additionalAttributes.get(ATTR_PERMISSION_COMPONENTS);
        if (StringUtils.isNotBlank(permissionComponents)) {
            this.permissionComponents = Utils.immutableCollectionOf(Arrays.asList(
                    StringUtils.split(permissionComponents, Constants.LIST_SEPARATOR)));
        } else {
            final Collection<String> defaultPermissions = new ArrayList<>();
            defaultPermissions.add(EdxCourseRestrictionAttributes.PermissionComponents.ALWAYS_ALLOW_STUFF.name);
            if (!this.configKeys.isEmpty()) {
                defaultPermissions.add(EdxCourseRestrictionAttributes.PermissionComponents.CHECK_CONFIG_KEY.name);
            }
            if (!this.browserExamKeys.isEmpty()) {
                defaultPermissions.add(EdxCourseRestrictionAttributes.PermissionComponents.CHECK_BROWSER_EXAM_KEY.name);
            }

            this.permissionComponents = Utils.immutableCollectionOf(defaultPermissions);
        }

        this.banningEnabled = BooleanUtils.toBoolean(data.additionalAttributes.get(ATTR_USER_BANNING_ENABLED));
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

}
