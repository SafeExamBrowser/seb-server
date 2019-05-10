/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;

public final class ViewContext {

    private final Configuration configuration;
    private final View view;
    private final int columns, rows;

    private final AttributeMapping attributeMapping;
    private final Map<Long, InputField> inputFieldMapping;
    private final ValueChangeListener valueChangeListener;
    private final I18nSupport i18nSupport;

    ViewContext(
            final Configuration configuration,
            final View view,
            final int columns,
            final int rows,
            final AttributeMapping attributeContext,
            final ValueChangeListener valueChangeListener,
            final I18nSupport i18nSupport) {

        Objects.requireNonNull(configuration);
        Objects.requireNonNull(view);
        Objects.requireNonNull(attributeContext);
        Objects.requireNonNull(valueChangeListener);

        this.configuration = configuration;
        this.view = view;
        this.columns = columns;
        this.rows = rows;

        this.attributeMapping = attributeContext;
        this.inputFieldMapping = new HashMap<>();
        this.valueChangeListener = valueChangeListener;
        this.i18nSupport = i18nSupport;
    }

    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    public Long getId() {
        return this.view.id;
    }

    public String getName() {
        return this.view.name;
    }

    public Long getConfigurationId() {
        return this.configuration.id;
    }

    public Long getInstitutionId() {
        return this.configuration.institutionId;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public View getView() {
        return this.view;
    }
//
//    public AttributeMapping getAttributeMapping() {
//        return this.attributeMapping;
//    }

    public Collection<ConfigurationAttribute> getChildAttributes(final Long id) {
        return this.attributeMapping.childAttributeMapping.get(id);
    }

    public Collection<ConfigurationAttribute> getAttributes() {
        return this.attributeMapping.getAttributes();
    }

    public ConfigurationAttribute getAttribute(final Long attributeId) {
        return this.attributeMapping.getAttribute(attributeId);
    }

    public Collection<Orientation> getOrientationsOfGroup(final ConfigurationAttribute attribute) {
        return this.attributeMapping.getOrientationsOfGroup(attribute);
    }

    public Orientation getOrientation(final Long attributeId) {
        return this.attributeMapping.getOrientation(attributeId);
    }

    public ValueChangeListener getValueChangeListener() {
        return this.valueChangeListener;
    }

    public void showError(final Long attributeId, final String errorMessage) {
        final InputField inputField = this.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.showError(errorMessage);
    }

    public void clearError(final Long attributeId) {
        final InputField inputField = this.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.clearError();
    }

    void registerInputField(final InputField inputField) {
        this.inputFieldMapping.put(
                inputField.getAttribute().id,
                inputField);
    }

    void setValuesToInputFields(final Collection<ConfigurationValue> values) {
        this.inputFieldMapping
                .values()
                .stream()
                .forEach(field -> field.initValue(values));
    }

    /** Removes all registered InputFields with the given attribute ids
     *
     * @param values Collection of attribute ids */
    void flushInputFields(final Collection<Long> values) {
        if (values == null) {
            return;
        }

        values.stream()
                .forEach(attrId -> this.inputFieldMapping.remove(attrId));
    }

}
