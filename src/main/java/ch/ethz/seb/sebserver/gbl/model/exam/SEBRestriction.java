/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SEBRestriction implements Entity {

    public static final String ATTR_BROWSER_KEYS = "browserExamKeys";
    public static final String ATTR_CONFIG_KEYS = "configKeys";
    public static final String ATTR_ADDITIONAL_PROPERTIES = "additionalProperties";

    @JsonProperty(Domain.EXAM.ATTR_ID)
    public final Long examId;
    @JsonProperty(ATTR_CONFIG_KEYS)
    public final Collection<String> configKeys;
    @JsonProperty(ATTR_BROWSER_KEYS)
    public final Collection<String> browserExamKeys;
    @JsonProperty(ATTR_ADDITIONAL_PROPERTIES)
    public final Map<String, String> additionalProperties;

    @JsonCreator
    public SEBRestriction(
            @JsonProperty(Domain.EXAM.ATTR_ID) final Long examId,
            @JsonProperty(ATTR_CONFIG_KEYS) final Collection<String> configKeys,
            @JsonProperty(ATTR_BROWSER_KEYS) final Collection<String> browserExamKeys,
            @JsonProperty(ATTR_ADDITIONAL_PROPERTIES) final Map<String, String> additionalProperties) {

        this.examId = examId;
        this.configKeys = Utils.immutableCollectionOf(configKeys);
        this.browserExamKeys = Utils.immutableCollectionOf(browserExamKeys);
        this.additionalProperties = Utils.immutableMapOf(additionalProperties);
    }

    @Override
    public String getModelId() {
        if (this.examId == null) {
            return null;
        }

        return String.valueOf(this.examId);
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_SEB_RESTRICTION;
    }

    @Override
    public String getName() {
        return null;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Collection<String> getConfigKeys() {
        return this.configKeys;
    }

    public Collection<String> getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public Map<String, String> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.additionalProperties == null) ? 0 : this.additionalProperties.hashCode());
        result = prime * result + ((this.browserExamKeys == null) ? 0 : this.browserExamKeys.hashCode());
        result = prime * result + ((this.configKeys == null) ? 0 : this.configKeys.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SEBRestriction other = (SEBRestriction) obj;
        if (this.additionalProperties == null) {
            if (other.additionalProperties != null)
                return false;
        } else if (!this.additionalProperties.equals(other.additionalProperties))
            return false;
        if (this.browserExamKeys == null) {
            if (other.browserExamKeys != null)
                return false;
        } else if (!this.browserExamKeys.equals(other.browserExamKeys))
            return false;
        if (this.configKeys == null) {
            if (other.configKeys != null)
                return false;
        } else if (!this.configKeys.equals(other.configKeys))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SEBRestriction [examId=");
        builder.append(this.examId);
        builder.append(", configKeys=");
        builder.append(this.configKeys);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append(", additionalProperties=");
        builder.append(this.additionalProperties);
        builder.append("]");
        return builder.toString();
    }

    public static SEBRestriction from(final Long examId, final OpenEdxSEBRestriction edxData) {
        final Map<String, String> attrs = new HashMap<>();

        if (!CollectionUtils.isEmpty(edxData.whiteListPaths)) {
            attrs.put(
                    OpenEdxSEBRestriction.ATTR_WHITELIST_PATHS,
                    StringUtils.join(edxData.whiteListPaths, Constants.LIST_SEPARATOR_CHAR));
        }

        if (!CollectionUtils.isEmpty(edxData.blacklistChapters)) {
            attrs.put(
                    OpenEdxSEBRestriction.ATTR_BLACKLIST_CHAPTERS,
                    StringUtils.join(edxData.blacklistChapters, Constants.LIST_SEPARATOR_CHAR));
        }

        if (!CollectionUtils.isEmpty(edxData.permissionComponents)) {
            attrs.put(
                    OpenEdxSEBRestriction.ATTR_PERMISSION_COMPONENTS,
                    StringUtils.join(edxData.permissionComponents, Constants.LIST_SEPARATOR_CHAR));
        }

        attrs.put(
                OpenEdxSEBRestriction.ATTR_USER_BANNING_ENABLED,
                (edxData.banningEnabled) ? Constants.TRUE_STRING : Constants.FALSE_STRING);

        return new SEBRestriction(
                examId,
                edxData.configKeys,
                edxData.browserExamKeys,
                attrs);
    }

}
