/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.CONFIGURATION_ATTRIBUTE;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationAttribute implements Entity, Comparable<ConfigurationAttribute> {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationAttribute.class);

    /** This is used to compare the attribute names for sorting used to generate the Config-Key
     * See: https://www.safeexambrowser.org/developer/seb-config-key.html */
    public static final Collator CULTURE_INVARIANT_COLLATOR = Collator.getInstance(Locale.ROOT);

    /** This configuration attribute dependency key can be used to set a specific localized text key prefix for
     * resources. This is usually convenient if two different attributes use the same resources and to avoid
     * to multiply the resources for each attribute with the attribute name prefix, we can set a specific
     * resourceLocTextKey prefix to use. */
    public static final String DEPENDENCY_RESOURCE_LOC_TEXT_KEY = "resourceLocTextKey";

    /** This configuration attribute dependency key indicates the group identifier for grouped COMPOSITE_TYPE types */
    public static final String DEPENDENCY_GROUP_ID = "groupId";

    /** This configuration attribute dependency key indicates if a default value should be created even if the
     * attribute is a child attribute and normally no default value is generated on creation. */
    public static final String DEPENDENCY_CREATE_DEFAULT_VALUE = "createDefaultValue";

    /** his configuration attribute dependency key indicates if the input field should be shown in the directly
     * in the View even if the attribute is a child attribute and is usually shown in a table or composite. */
    public static final String DEPENDENCY_SHOW_IN_VIEW = "showInView";

    public static final String FILTER_ATTR_PARENT_ID = "parentId";
    public static final String FILTER_ATTR_TYPE = "type";

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_ID)
    public final Long id;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID)
    public final Long parentId;

    @NotNull(message = "configurationAttribute:name:notNull")
    @Size(min = 3, max = 255, message = "configurationAttribute:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_NAME)
    public final String name;

    @NotNull(message = "configurationAttribute:type:notNull")
    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_TYPE)
    public final AttributeType type;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES)
    public final String resources;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_VALIDATOR)
    public final String validator;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES)
    public final String dependencies;

    @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE)
    public final String defaultValue;

    @JsonCreator
    public ConfigurationAttribute(
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_ID) final Long id,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID) final Long parentId,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String name,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_TYPE) final AttributeType type,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES) final String resources,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_VALIDATOR) final String validator,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES) final String dependencies,
            @JsonProperty(CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE) final String defaultValue) {

        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.type = type;
        this.resources = resources;
        this.validator = validator;
        this.dependencies = dependencies;
        this.defaultValue = defaultValue;
    }

    public ConfigurationAttribute(final POSTMapper postParams) {
        this.id = null;
        this.parentId = postParams.getLong(Domain.CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID);
        this.name = postParams.getString(Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME);
        this.type = postParams.getEnum(Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE, AttributeType.class);
        this.resources = postParams.getString(Domain.CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES);
        this.validator = postParams.getString(Domain.CONFIGURATION_ATTRIBUTE.ATTR_VALIDATOR);
        this.dependencies = postParams.getString(Domain.CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES);
        this.defaultValue = postParams.getString(Domain.CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE);
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CONFIGURATION_ATTRIBUTE;
    }

    public Long getId() {
        return this.id;
    }

    public Long getParentId() {
        return this.parentId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public AttributeType getType() {
        return this.type;
    }

    public String getResources() {
        return this.resources;
    }

    public String getValidator() {
        return this.validator;
    }

    public String getDependencies() {
        return this.dependencies;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public int compareTo(final ConfigurationAttribute attribute) {
        return CULTURE_INVARIANT_COLLATOR.compare(
                this.name,
                attribute.name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        final ConfigurationAttribute other = (ConfigurationAttribute) obj;
        return this.compareTo(other) == 0;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ConfigurationAttribute [id=");
        builder.append(this.id);
        builder.append(", parentId=");
        builder.append(this.parentId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", resources=");
        builder.append(this.resources);
        builder.append(", validator=");
        builder.append(this.validator);
        builder.append(", dependencies=");
        builder.append(this.dependencies);
        builder.append(", defaultValue=");
        builder.append(this.defaultValue);
        builder.append("]");
        return builder.toString();
    }

    public static boolean hasDependency(
            final String dependencyName,
            final ConfigurationAttribute attribute) {

        if (StringUtils.isBlank(attribute.dependencies)) {
            return false;
        }

        return attribute.dependencies.contains(dependencyName);
    }

    public static String getDependencyValue(
            final String dependencyName,
            final ConfigurationAttribute attribute) {

        return getDependencyValue(dependencyName, attribute.dependencies);
    }

    public static String getDependencyValue(
            final String dependencyName,
            final String dependenciesString) {

        if (StringUtils.isBlank(dependenciesString)) {
            return null;
        }

        return getAttributeDependencyMap(dependenciesString).get(dependencyName);
    }

    public static Map<String, String> getAttributeDependencyMap(final ConfigurationAttribute attribute) {
        if (StringUtils.isBlank(attribute.dependencies)) {
            return Collections.emptyMap();
        }

        return getAttributeDependencyMap(attribute.dependencies);
    }

    public static Map<String, String> getAttributeDependencyMap(final String dependenciesString) {
        try {
            return Arrays.stream(StringUtils.split(dependenciesString, Constants.LIST_SEPARATOR))
                    .map(s -> StringUtils.split(s, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR))
                    .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
        } catch (final Exception e) {
            log.error("Unexpected error while trying to parse dependency map of: {}",
                    dependenciesString,
                    e);
            return Collections.emptyMap();
        }
    }

}
