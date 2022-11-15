/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_SECURITY_KEY_REGISTRY;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityKey implements GrantEntity {

    public static final String FILTER_ATTR_KEY_TYPE = Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_TYPE;
    public static final String FILTER_ATTR_EXAM_ID = Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID;
    public static final String FILTER_ATTR_EXAM_TEMPLATE_ID = Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_TEMPLATE_ID;
    public static final String FILTER_ATTR_TAG = Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG;

    public static enum KeyType {
        UNDEFINED,
        CONFIG_KEY,
        BROWSER_EXAM_KEY,
        APP_SIGNATURE_KEY;

        public static KeyType byString(final String type) {
            try {
                return KeyType.valueOf(type);
            } catch (final Exception e) {
                return UNDEFINED;
            }
        }
    }

    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_TYPE)
    public final KeyType keyType;

    @NotNull
    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE)
    public final CharSequence key;

    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_TAG)
    public final String tag;

    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_TEMPLATE_ID)
    public final Long examTemplateId;

    @JsonCreator
    public SecurityKey(
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_ID) final Long id,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_TYPE) final KeyType keyType,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE) final CharSequence key,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_TAG) final String tag,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_TEMPLATE_ID) final Long examTemplateId) {

        this.id = id;
        this.institutionId = institutionId;
        this.keyType = keyType;
        this.key = key;
        this.tag = tag;
        this.examId = examId;
        this.examTemplateId = examTemplateId;
    }

    public SecurityKey(final POSTMapper postMap) {
        this.id = null;
        this.institutionId = postMap.getLong(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_INSTITUTION_ID);
        this.keyType = postMap.getEnum(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_TYPE, KeyType.class);
        this.key = postMap.getString(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE);
        this.tag = postMap.getString(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG);
        this.examId = postMap.getLong(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_ID);
        this.examTemplateId = postMap.getLong(Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_EXAM_TEMPLATE_ID);
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_SECURITY_KEY_REGISTRY;
    }

    @Override
    public String getName() {
        return this.tag;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getId() {
        return this.id;
    }

    public KeyType getKeyType() {
        return this.keyType;
    }

    public CharSequence getKey() {
        return this.key;
    }

    public String getTag() {
        return this.tag;
    }

    public Long getExamId() {
        return this.examId;
    }

    public Long getExamTemplateId() {
        return this.examTemplateId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SecurityKey other = (SecurityKey) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SecurityKeyRegistry [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", keyType=");
        builder.append(this.keyType);
        builder.append(", key=");
        builder.append(this.key);
        builder.append(", tag=");
        builder.append(this.tag);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", examTemplateId=");
        builder.append(this.examTemplateId);
        builder.append("]");
        return builder.toString();
    }

}
