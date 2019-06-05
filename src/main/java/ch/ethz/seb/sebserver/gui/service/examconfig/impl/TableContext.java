/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class TableContext {

    private static final Logger log = LoggerFactory.getLogger(TableContext.class);

    private final InputFieldBuilderSupplier inputFieldBuilderSupplier;
    private final WidgetFactory widgetFactory;

    public final ConfigurationAttribute attribute;
    public final Orientation orientation;

    private final List<ConfigurationAttribute> rowAttributes;
    private final List<ConfigurationAttribute> columnAttributes;
    private final ViewContext viewContext;

    public TableContext(
            final InputFieldBuilderSupplier inputFieldBuilderSupplier,
            final WidgetFactory widgetFactory,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        this.inputFieldBuilderSupplier = Objects.requireNonNull(inputFieldBuilderSupplier);
        this.widgetFactory = Objects.requireNonNull(widgetFactory);
        this.attribute = Objects.requireNonNull(attribute);
        this.viewContext = Objects.requireNonNull(viewContext);

        this.orientation = Objects.requireNonNull(viewContext
                .getOrientation(attribute.id));

        this.rowAttributes = viewContext.getChildAttributes(attribute.id)
                .stream()
                .sorted(rowAttributeComparator(viewContext))
                .collect(Collectors.toList());

        this.columnAttributes = this.rowAttributes
                .stream()
                .filter(attr -> viewContext.getOrientation(attr.id).xPosition > 0)
                .sorted(columnAttributeComparator(viewContext))
                .collect(Collectors.toList());
    }

    public I18nSupport i18nSupport() {
        return this.viewContext.i18nSupport;
    }

    public InputFieldBuilderSupplier getInputFieldBuilderSupplier() {
        return this.inputFieldBuilderSupplier;
    }

    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    public ConfigurationAttribute getAttribute() {
        return this.attribute;
    }

    public Orientation getOrientation() {
        return this.orientation;
    }

    public Orientation getOrientation(final Long attributeId) {
        return this.viewContext.getOrientation(attributeId);
    }

    public List<ConfigurationAttribute> getRowAttributes() {
        return this.rowAttributes;
    }

    public List<ConfigurationAttribute> getRowAttributes(final String rowGroupId) {
        if (StringUtils.isBlank(rowGroupId)) {
            return getRowAttributes();
        } else {
            return this.rowAttributes
                    .stream()
                    .filter(attr -> rowGroupId.equals(ConfigurationAttribute.getDependencyValue(
                            ConfigurationAttribute.DEPENDENCY_GROUP_ID,
                            attr)))
                    .sorted(rowAttributeComparator(this.viewContext))
                    .collect(Collectors.toList());
        }
    }

    public List<ConfigurationAttribute> getColumnAttributes() {
        return this.columnAttributes;
    }

    public ViewContext getViewContext() {
        return this.viewContext;
    }

    public ValueChangeListener getValueChangeListener() {
        return this.viewContext.getValueChangeListener();
    }

    public Long getInstitutionId() {
        return this.viewContext.getInstitutionId();
    }

    public Long getConfigurationId() {
        return this.viewContext.getConfigurationId();
    }

    public ConfigurationAttribute getAttribute(final Long attributeId) {
        return this.viewContext.getAttribute(attributeId);
    }

    public void flushInputFields(final Set<Long> attributeIds) {
        this.viewContext.flushInputFields(attributeIds);
    }

    public InputFieldBuilder getInputFieldBuilder(
            final ConfigurationAttribute attribute2,
            final Orientation orientation) {

        return this.inputFieldBuilderSupplier.getInputFieldBuilder(attribute2, orientation);
    }

    public void registerInputField(final InputField inputField) {
        this.viewContext.registerInputField(inputField);
    }

    private Comparator<ConfigurationAttribute> rowAttributeComparator(final ViewContext viewContext) {
        return (a1, a2) -> {
            try {
                final Orientation o1 = viewContext.getOrientation(a1.id);
                final Orientation o2 = viewContext.getOrientation(a2.id);
                return o1.yPosition.compareTo(o2.yPosition);
            } catch (final Exception e) {
                log.warn("Failed to get Orientations of ConfigurationAttribute to compare: ", e);
                return -1;
            }
        };
    }

    private Comparator<ConfigurationAttribute> columnAttributeComparator(final ViewContext viewContext) {
        return (a1, a2) -> {
            try {
                final Orientation o1 = viewContext.getOrientation(a1.id);
                final Orientation o2 = viewContext.getOrientation(a2.id);
                return o1.xPosition.compareTo(o2.xPosition);
            } catch (final Exception e) {
                log.warn("Failed to get Orientations of ConfigurationAttribute to compare: ", e);
                return -1;
            }
        };
    }

}
