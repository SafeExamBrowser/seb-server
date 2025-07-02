/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class AttributeMapping {

    private static final Logger log = LoggerFactory.getLogger(AttributeMapping.class);

    public final Long templateId;

    public final Map<Long, ConfigurationAttribute> attributeIdMapping;
    public final Map<String, Long> attributeNameIdMapping;

    public final Map<Long, Orientation> orientationAttributeMapping;
    public final Map<String, Orientation> orientationAttributeNameMapping;

    public final Map<Long, List<ConfigurationAttribute>> childAttributeMapping;
    public final Map<String, List<ConfigurationAttribute>> attributeGroupMapping;

    AttributeMapping(
            final Long templateId,
            final Collection<ConfigurationAttribute> attributes,
            final Collection<Orientation> orientations) {

        Objects.requireNonNull(templateId);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(orientations);

        this.templateId = templateId;
        this.orientationAttributeMapping = Utils.immutableMapOf(orientations
                .stream()
                .collect(Collectors.toMap(
                        o -> o.attributeId,
                        Function.identity(),
                        (first, second) -> {
                            log.warn("*** Found duplicate orientation, use {} instead of {}", second, first);
                            return second;
                        })));
        this.attributeIdMapping = Utils.immutableMapOf(attributes
                .stream()
                .filter(attr -> this.orientationAttributeMapping.containsKey(attr.id))
                .collect(Collectors.toMap(
                        attr -> attr.id,
                        Function.identity(),
                        (first, second) -> second)));

        this.attributeNameIdMapping = Utils.immutableMapOf(attributes
                .stream()
                .filter(attr -> this.orientationAttributeMapping.containsKey(attr.id))
                .collect(Collectors.toMap(
                        attr -> attr.name,
                        attr -> attr.id,
                        (first, second) -> second)));

        this.orientationAttributeNameMapping = Utils.immutableMapOf(orientations
                .stream()
                .filter(o -> this.attributeIdMapping.containsKey(o.attributeId))
                .collect(Collectors.toMap(
                        o -> this.attributeIdMapping.get(o.attributeId).name,
                        Function.identity(),
                        (first, second) -> second)));

        this.childAttributeMapping = Utils.immutableMapOf(attributes
                .stream()
                .filter(attr -> this.orientationAttributeMapping.containsKey(attr.id))
                .collect(Collectors.toMap(
                        attr -> attr.id,
                        this::getChildAttributes,
                        (first, second) -> second)));

        this.attributeGroupMapping = Utils.immutableMapOf(orientations
                .stream()
                .filter(o -> o.groupId != null)
                .map(o -> o.groupId)
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getAttributesOfGroup,
                        (first, second) -> second)));
    }

    public Collection<ConfigurationAttribute> getAttributes() {
        return Collections.unmodifiableCollection(this.attributeIdMapping.values());
    }

    public ConfigurationAttribute getAttribute(final Long attributeId) {
        return this.attributeIdMapping.get(attributeId);
    }

    public Orientation getOrientation(final Long attributeId) {
        return this.orientationAttributeMapping.get(attributeId);
    }

    public Orientation getOrientation(final String attributeName) {
        return this.orientationAttributeNameMapping.get(attributeName);
    }

    public ConfigurationAttribute getAttribute(final String attributeName) {
        return this.attributeIdMapping.get(this.attributeNameIdMapping.get(attributeName));
    }

    public List<ConfigurationAttribute> getAttributes(final Long viewId) {
        if (viewId == null) {
            return Utils.immutableListOf(this.attributeIdMapping.values());
        } else {
            return Utils.immutableListOf(this.attributeIdMapping
                    .values()
                    .stream()
                    .filter(attr -> this.orientationAttributeMapping.containsKey(attr.id)
                            && viewId.equals(this.orientationAttributeMapping.get(attr.id).viewId))
                    .collect(Collectors.toList()));
        }
    }

    public List<String> getAttributeNames(final Long viewId) {
        if (viewId == null) {
            return Utils.immutableListOf(this.attributeIdMapping
                    .values()
                    .stream()
                    .map(attr -> attr.name)
                    .collect(Collectors.toList()));
        } else {
            return Utils.immutableListOf(this.attributeIdMapping
                    .values()
                    .stream()
                    .filter(attr -> this.orientationAttributeMapping.containsKey(attr.id)
                            && viewId.equals(this.orientationAttributeMapping.get(attr.id).viewId))
                    .map(attr -> attr.name)
                    .collect(Collectors.toList()));
        }
    }

    public Collection<Long> getViewIds() {
        return this.orientationAttributeMapping.values()
                .stream()
                .map(o -> o.viewId)
                .collect(Collectors.toSet());
    }

    public Collection<Orientation> getOrientationsOfExpandable(final ConfigurationAttribute attribute) {
        final Orientation orientation = this.orientationAttributeMapping.get(attribute.id);
        if (orientation == null) {
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(orientation.groupId)) {
            return Collections.emptyList();
        }

        final String expandGroupKey = ViewGridBuilder.getExpandGroupKey(orientation.groupId);
        if (expandGroupKey == null) {
            return Collections.emptyList();
        }

        try {
            return Collections.unmodifiableCollection(this.orientationAttributeMapping
                    .values()
                    .stream()
                    .filter(o -> o.groupId != null && o.groupId.contains(expandGroupKey))
                    .collect(Collectors.toList()));
        } catch (final Exception e) {
            log.error("Failed to verify expandable identifier from group identifier", e);
            return Collections.emptyList();
        }
    }

    public Collection<Orientation> getOrientationsOfGroup(final ConfigurationAttribute attribute) {
        final Orientation orientation = this.orientationAttributeMapping.get(attribute.id);
        if (orientation == null) {
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(orientation.groupId)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(this.orientationAttributeMapping
                .values()
                .stream()
                .filter(o -> orientation.groupId.equals(o.groupId))
                .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "AttributeMapping [templateId=" + this.templateId +
                ", attributeIdMapping=" + this.attributeIdMapping +
                ", attributeNameIdMapping=" + this.attributeNameIdMapping +
                ", orientationAttributeMapping=" + this.orientationAttributeMapping +
                ", orientationAttributeNameMapping=" + this.orientationAttributeNameMapping +
                ", childAttributeMapping=" + this.childAttributeMapping +
                ", attributeGroupMapping=" + this.attributeGroupMapping +
                "]";
    }

    private List<ConfigurationAttribute> getChildAttributes(final ConfigurationAttribute attribute) {
        return this.attributeIdMapping
                .values()
                .stream()
                .filter(a -> attribute.id.equals(a.parentId))
                .sorted((a1, a2) -> {
                    final Orientation o1 = this.orientationAttributeMapping.get(a1.id);
                    final Orientation o2 = this.orientationAttributeMapping.get(a2.id);
                    final Integer i1 = o1 != null ? o1.xPosition : 0;
                    final Integer i2 = o2 != null ? o2.xPosition : 0;
                    return i1.compareTo(i2);
                })
                .collect(Collectors.toList());
    }

    private List<ConfigurationAttribute> getAttributesOfGroup(final String groupName) {
        if (groupName == null) {
            return Collections.emptyList();
        }
        return this.orientationAttributeMapping
                .values()
                .stream()
                .filter(o -> groupName.equals(o.groupId))
                .sorted((o1, o2) -> (o1.yPosition != null && o1.yPosition.equals(o2.yPosition))
                        ? o1.xPosition.compareTo(o2.xPosition)
                        : o1.yPosition.compareTo(o2.yPosition))
                .map(o -> this.attributeIdMapping.get(o.attributeId))
                .collect(Collectors.toList());
    }

}
