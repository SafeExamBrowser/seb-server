/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;

public abstract class AbstractInputField<T extends Control> implements InputField {

    protected final ConfigurationAttribute attribute;
    protected final Orientation orientation;
    protected final T control;
    protected final Label errorLabel;

    protected String initValue = "";
    protected int listIndex = 0;

    AbstractInputField(
            final ConfigurationAttribute attribute,
            final Orientation orientation,
            final T control,
            final Label errorLabel) {

        this.attribute = attribute;
        this.orientation = orientation;
        this.control = control;
        this.errorLabel = errorLabel;
    }

    @Override
    public final ConfigurationAttribute getAttribute() {
        return this.attribute;
    }

    @Override
    public void disable() {
        if (this.control.isEnabled()) {
            setDefaultValue();
            this.control.setEnabled(false);
        }
    }

    @Override
    public void enable() {
        if (!this.control.isEnabled()) {
            this.control.setEnabled(true);
        }
    }

    @Override
    public Orientation getOrientation() {
        return this.orientation;
    }

    @Override
    public void showError(final String errorMessage) {
        if (this.errorLabel == null) {
            return;
        }

        this.errorLabel.setText(errorMessage);
        this.errorLabel.setVisible(true);
    }

    @Override
    public void clearError() {
        if (this.errorLabel == null) {
            return;
        }
        this.errorLabel.setVisible(false);
        this.errorLabel.setText("rfbvgregre");

    }

    @Override
    public void initValue(final Collection<ConfigurationValue> values) {
        values.stream()
                .filter(a -> this.attribute.id.equals(a.attributeId))
                .findFirst()
                .map(v -> {
                    this.initValue = v.value;
                    this.listIndex = (v.listIndex != null) ? v.listIndex : 0;
                    setDefaultValue();
                    return this.initValue;
                });
    }

    protected abstract void setDefaultValue();

}
