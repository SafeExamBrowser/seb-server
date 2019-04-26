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

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;

public final class ViewContext {

    public final String name;
    public final String configurationId;
    public final int columns, rows;

    public final AttributeMapping attributeContext;

    private final Map<String, InputField> inputFieldMapping;
    private final ValueChangeListener valueChangeListener;

    ViewContext(
            final String name,
            final String configurationId,
            final int columns,
            final int rows,
            final AttributeMapping attributeContext,
            final ValueChangeListener valueChangeListener) {

        this.name = name;
        this.configurationId = configurationId;
        this.columns = columns;
        this.rows = rows;

        this.attributeContext = attributeContext;
        this.inputFieldMapping = new HashMap<>();
        this.valueChangeListener = valueChangeListener;
    }

    public String getName() {
        return this.name;
    }

    public String getConfigurationId() {
        return this.configurationId;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    public ValueChangeListener getValueChangeListener() {
        return this.valueChangeListener;
    }

    void registerInputField(final InputField inputField) {
        this.inputFieldMapping.put(
                inputField.getAttribute().getName(),
                inputField);
    }

    void setValuesToInputFields(final Collection<ConfigurationValue> values) {
        this.inputFieldMapping
                .values()
                .stream()
                .forEach(field -> field.initValue(values));
    }

}
