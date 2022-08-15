/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;

public final class ViewContext {

    private static final Logger log = LoggerFactory.getLogger(ViewContext.class);

    /** Defines a list of checkbox fields that are inverted on the display of SEB settings */
    public static final Set<String> INVERTED_CHECKBOX_SETTINGS = new HashSet<>(Arrays.asList(
            "enableSebBrowser"));

    private final Configuration configuration;
    private final View view;
    private final Function<String, ViewContext> viewContextSupplier;
    private final int rows;

    final AttributeMapping attributeMapping;
    final Map<Long, InputField> inputFieldMapping;
    final ValueChangeListener valueChangeListener;
    final I18nSupport i18nSupport;
    final boolean readonly;

    ViewContext(
            final Configuration configuration,
            final View view,
            final Function<String, ViewContext> viewContextSupplier,
            final int rows,
            final AttributeMapping attributeContext,
            final ValueChangeListener valueChangeListener,
            final I18nSupport i18nSupport,
            final boolean readonly) {

        Objects.requireNonNull(configuration);
        Objects.requireNonNull(view);
        Objects.requireNonNull(attributeContext);
        Objects.requireNonNull(valueChangeListener);

        this.configuration = configuration;
        this.view = view;
        this.viewContextSupplier = viewContextSupplier;
        this.rows = rows;

        this.attributeMapping = attributeContext;
        this.inputFieldMapping = new HashMap<>();
        this.valueChangeListener = valueChangeListener;
        this.i18nSupport = i18nSupport;
        this.readonly = readonly;
    }

    public boolean isReadonly() {
        return this.readonly;
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
        return this.view.columns;
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

    public Collection<ConfigurationAttribute> getChildAttributes(final Long id) {
        return this.attributeMapping.childAttributeMapping.get(id);
    }

    public Collection<ConfigurationAttribute> getAttributes() {
        return this.attributeMapping.getAttributes();
    }

    public ConfigurationAttribute getAttribute(final Long attributeId) {
        return this.attributeMapping.getAttribute(attributeId);
    }

    public Long getAttributeIdByName(final String name) {
        return this.attributeMapping.attributeNameIdMapping.get(name);
    }

    public ConfigurationAttribute getAttributeByName(final String name) {
        final Long attributeId = this.attributeMapping.attributeNameIdMapping.get(name);
        if (attributeId != null) {
            return getAttribute(attributeId);
        }
        return null;
    }

    public Collection<Orientation> getOrientationsOfGroup(final ConfigurationAttribute attribute) {
        return this.attributeMapping.getOrientationsOfGroup(attribute);
    }

    public Collection<Orientation> getOrientationsOfExpandable(final ConfigurationAttribute attribute) {
        return this.attributeMapping.getOrientationsOfExpandable(attribute);
    }

    public Orientation getOrientation(final Long attributeId) {
        return this.attributeMapping.getOrientation(attributeId);
    }

    public ValueChangeListener getValueChangeListener() {
        return this.valueChangeListener;
    }

    public void disable(final String attributeName) {
        disable(this, this.getAttributeIdByName(attributeName));
    }

    public void disable(final String viewName, final String attributeName) {
        final ViewContext viewContext = this.viewContextSupplier.apply(viewName);
        if (viewContext != null) {
            disable(viewContext, viewContext.getAttributeIdByName(attributeName));
        }
    }

    public void disable(final ViewContext context, final Long attributeId) {
        final InputField inputField = context.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.disable(false);
    }

    public void enable(final String attributeName) {
        enable(this, this.getAttributeIdByName(attributeName));
    }

    public void enable(final String viewName, final String attributeName) {
        final ViewContext viewContext = this.viewContextSupplier.apply(viewName);
        if (viewContext != null) {
            enable(viewContext, viewContext.getAttributeIdByName(attributeName));
        }
    }

    public void enable(final ViewContext context, final Long attributeId) {
        final InputField inputField = context.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.enable(false);
    }

    public void disableGroup(final String attributeName) {
        disableGroup(this, this.getAttributeIdByName(attributeName));
    }

    public void disableGroup(final String viewName, final String attributeName) {
        final ViewContext viewContext = this.viewContextSupplier.apply(viewName);
        if (viewContext != null) {
            disableGroup(viewContext, viewContext.getAttributeIdByName(attributeName));
        }
    }

    public void disableGroup(final ViewContext context, final Long attributeId) {
        final InputField inputField = context.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.disable(true);

        try {
            context.attributeMapping.attributeGroupMapping
                    .get(inputField.getOrientation().groupId)
                    .stream()
                    .map(ConfigurationAttribute::getId)
                    .map(context.inputFieldMapping::get)
                    .forEach(InputField::setDefaultValue);
        } catch (final Exception e) {
            log.warn("Failed to send attribute value update to server: ", e);
        }
    }

    public void enableGroup(final String attributeName) {
        enableGroup(this, this.getAttributeIdByName(attributeName));
    }

    public void enableGroup(final String viewName, final String attributeName) {
        final ViewContext viewContext = this.viewContextSupplier.apply(viewName);
        if (viewContext != null) {
            enableGroup(viewContext, viewContext.getAttributeIdByName(attributeName));
        }
    }

    public void enableGroup(final ViewContext context, final Long attributeId) {
        final InputField inputField = context.inputFieldMapping.get(attributeId);
        if (inputField == null) {
            return;
        }

        inputField.enable(true);
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

    public void registerInputField(final InputField inputField) {
        this.inputFieldMapping.put(
                inputField.getAttribute().id,
                inputField);
    }

    public String getValue(final String name) {
        try {
            final ConfigurationAttribute attributeByName = getAttributeByName(name);
            final InputField inputField = this.inputFieldMapping.get(attributeByName.id);
            return inputField.getValue();
        } catch (final Exception e) {
            log.error("Failed to get attribute value: {}, cause {}", name, e.getMessage());
            return null;
        }
    }

    public void putValue(final String name, final String value) {
        try {
            final ConfigurationAttribute attributeByName = getAttributeByName(name);
            final InputField inputField = this.inputFieldMapping.get(attributeByName.id);
            inputField.initValue(value, 0);
        } catch (final Exception e) {
            log.error("Failed to put attribute value: {} : {}, cause {}", name, value, e.getMessage());
        }
    }

    public void setValue(final String name, final String value) {
        try {
            final ConfigurationAttribute attributeByName = getAttributeByName(name);
            final InputField inputField = this.inputFieldMapping.get(attributeByName.id);
            inputField.initValue(value, 0);
            if (this.valueChangeListener != null) {
                this.valueChangeListener.valueChanged(
                        this,
                        attributeByName,
                        value,
                        0);
            }
        } catch (final Exception e) {
            log.error("Failed to set attribute value: {} : {}, cause {}", name, value, e.getMessage());
        }
    }

    void setValuesToInputFields(final Collection<ConfigurationValue> values) {
        try {
            this.inputFieldMapping
                    .values()
                    .forEach(field -> {
                        try {
                            final ConfigurationValue initValue = field.initValue(values);
                            if (initValue != null) {
                                this.valueChangeListener.notifyGUI(this, field.getAttribute(), initValue);
                            }
                        } catch (final Exception e) {
                            log.error("Failed to initialize SEB setting: {}", field.getAttribute(), e);
                        }
                    });
        } catch (final Exception e) {
            log.error("Unexpected error while initialize SEB settings: ", e);
        }
    }

    /** Removes all registered InputFields with the given attribute ids
     *
     * @param values Collection of attribute ids */
    void flushInputFields(final Collection<Long> values) {
        if (values == null) {
            return;
        }

        values.forEach(this.inputFieldMapping::remove);
    }

}
