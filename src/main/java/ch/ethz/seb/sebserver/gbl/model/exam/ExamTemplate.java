/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM_TEMPLATE;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamTemplate implements GrantEntity {

    public static final String FILTER_ATTR_EXAM_TYPE = EXAM_TEMPLATE.ATTR_EXAM_TYPE;
    public static final String ATTR_CLIENT_GROUP_TEMPLATES = "CLIENT_GROUP_TEMPLATES";
    public static final String ATTR_EXAM_ATTRIBUTES = "EXAM_ATTRIBUTES";
    public static final String ATTR_QUIT_PASSWORD = "quitPassword";

    @JsonProperty(EXAM_TEMPLATE.ATTR_ID)
    public final Long id;

    @JsonProperty(EXAM_TEMPLATE.ATTR_INSTITUTION_ID)
    @NotNull
    public final Long institutionId;

    @NotNull(message = "configurationNode:name:notNull")
    @Size(min = 3, max = 255, message = "examTemplate:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(EXAM_TEMPLATE.ATTR_NAME)
    public final String name;

    @Size(max = 4000, message = "examTemplate:description:size:{min}:{max}:${validatedValue}")
    @JsonProperty(EXAM_TEMPLATE.ATTR_DESCRIPTION)
    public final String description;

    @JsonProperty(EXAM_TEMPLATE.ATTR_EXAM_TYPE)
    public final ExamType examType;

    @JsonProperty(EXAM_TEMPLATE.ATTR_SUPPORTER)
    public final Collection<String> supporter;

    @JsonProperty(EXAM_TEMPLATE.ATTR_CONFIGURATION_TEMPLATE_ID)
    public final Long configTemplateId;

    @JsonProperty(EXAM_TEMPLATE.ATTR_INDICATOR_TEMPLATES)
    public final Collection<IndicatorTemplate> indicatorTemplates;

    @JsonProperty(ATTR_CLIENT_GROUP_TEMPLATES)
    public final Collection<ClientGroupTemplate> clientGroupTemplates;

    @JsonProperty(ATTR_EXAM_ATTRIBUTES)
    public final Map<String, String> examAttributes;

    @JsonProperty(EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT)
    public final Boolean institutionalDefault;

    @JsonProperty(EXAM_TEMPLATE.ATTR_LMS_INTEGRATION)
    public final Boolean lmsIntegration;

    @JsonProperty(EXAM_TEMPLATE.ATTR_CLIENT_CONFIGURATION_ID)
    public final Long clientConfigurationId;

    @JsonCreator
    public ExamTemplate(
            @JsonProperty(EXAM_TEMPLATE.ATTR_ID) final Long id,
            @JsonProperty(EXAM_TEMPLATE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM_TEMPLATE.ATTR_NAME) final String name,
            @JsonProperty(EXAM_TEMPLATE.ATTR_DESCRIPTION) final String description,
            @JsonProperty(EXAM_TEMPLATE.ATTR_EXAM_TYPE) final ExamType examType,
            @JsonProperty(EXAM_TEMPLATE.ATTR_SUPPORTER) final Collection<String> supporter,
            @JsonProperty(EXAM_TEMPLATE.ATTR_CONFIGURATION_TEMPLATE_ID) final Long configTemplateId,
            @JsonProperty(EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT) final Boolean institutionalDefault,
            @JsonProperty(EXAM_TEMPLATE.ATTR_LMS_INTEGRATION) final Boolean lmsIntegration,
            @JsonProperty(EXAM_TEMPLATE.ATTR_CLIENT_CONFIGURATION_ID) final Long clientConfigurationId,
            @JsonProperty(EXAM_TEMPLATE.ATTR_INDICATOR_TEMPLATES) final Collection<IndicatorTemplate> indicatorTemplates,
            @JsonProperty(ATTR_CLIENT_GROUP_TEMPLATES) final Collection<ClientGroupTemplate> clientGroupTemplates,
            @JsonProperty(ATTR_EXAM_ATTRIBUTES) final Map<String, String> examAttributes) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.description = description;
        this.examType = examType;
        this.supporter = supporter;
        this.configTemplateId = configTemplateId;
        this.indicatorTemplates = Utils.immutableCollectionOf(indicatorTemplates);
        this.clientGroupTemplates = Utils.immutableCollectionOf(clientGroupTemplates);
        this.institutionalDefault = BooleanUtils.toBoolean(institutionalDefault);
        this.lmsIntegration = BooleanUtils.toBoolean(lmsIntegration);
        this.clientConfigurationId = clientConfigurationId;
        if (examAttributes != null && examAttributes.containsKey(ATTR_CLIENT_GROUP_TEMPLATES)) {
            final HashMap<String, String> attrs = new HashMap<>(examAttributes);
            attrs.remove(ATTR_CLIENT_GROUP_TEMPLATES);
            this.examAttributes = Utils.immutableMapOf(attrs);
        } else {
            this.examAttributes = Utils.immutableMapOf(examAttributes);
        }
    }

    public ExamTemplate(
            final Long institutionId,
            final POSTMapper mapper) {

        this.id = null;
        this.institutionId = institutionId;
        this.name = mapper.getString(Domain.EXAM_TEMPLATE.ATTR_NAME);
        this.description = mapper.getString(Domain.EXAM_TEMPLATE.ATTR_DESCRIPTION);
        this.examType = mapper.getEnum(EXAM_TEMPLATE.ATTR_EXAM_TYPE, ExamType.class, ExamType.UNDEFINED);
        this.supporter = mapper.getStringSet(EXAM_TEMPLATE.ATTR_SUPPORTER);
        this.configTemplateId = mapper.getLong(Domain.EXAM_TEMPLATE.ATTR_CONFIGURATION_TEMPLATE_ID);
        this.institutionalDefault = mapper.getBoolean(Domain.EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT);
        this.lmsIntegration = mapper.getBoolean(EXAM_TEMPLATE.ATTR_LMS_INTEGRATION);
        this.clientConfigurationId = mapper.getLong(EXAM_TEMPLATE.ATTR_CLIENT_CONFIGURATION_ID);
        this.indicatorTemplates = Collections.emptyList();
        this.clientGroupTemplates = Collections.emptyList();
        this.examAttributes = Utils.immutableMapOf(null);
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM_TEMPLATE;
    }

    @Override
    public String getName() {
        return this.name;
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

    public String getDescription() {
        return this.description;
    }

    public Long getConfigTemplateId() {
        return this.configTemplateId;
    }

    public Boolean getInstitutionalDefault() {
        return this.institutionalDefault;
    }

    public Boolean getLmsIntegration() {
        return lmsIntegration;
    }

    public Long getClientConfigurationId() {
        return clientConfigurationId;
    }

    public Collection<IndicatorTemplate> getIndicatorTemplates() {
        return this.indicatorTemplates;
    }

    public Collection<ClientGroupTemplate> getClientGroupTemplates() {
        return this.clientGroupTemplates;
    }

    public Map<String, String> getExamAttributes() {
        return this.examAttributes;
    }

    public ExamType getExamType() {
        return this.examType;
    }

    public Collection<String> getSupporter() {
        return this.supporter;
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ExamTemplate that = (ExamTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ExamTemplate [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append(", examType=");
        builder.append(this.examType);
        builder.append(", supporter=");
        builder.append(this.supporter);
        builder.append(", configTemplateId=");
        builder.append(this.configTemplateId);
        builder.append(", indicatorTemplates=");
        builder.append(this.indicatorTemplates);
        builder.append(", clientGroupTemplates=");
        builder.append(this.clientGroupTemplates);
        builder.append(", examAttributes=");
        builder.append(this.examAttributes);
        builder.append(", institutionalDefault=");
        builder.append(this.institutionalDefault);
        builder.append(", lmsIntegration=");
        builder.append(this.lmsIntegration);
        builder.append("]");
        return builder.toString();
    }

    public static ExamTemplate createNew(final Long institutionId) {
        return new ExamTemplate(null, institutionId, null, null, ExamType.UNDEFINED, null, null, false, false, null, null, null, null);
    }

}
