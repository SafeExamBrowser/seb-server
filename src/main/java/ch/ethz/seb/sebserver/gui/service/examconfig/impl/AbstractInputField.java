/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;

public abstract class AbstractInputField<T extends Control> implements InputField {

    private static final Logger log = LoggerFactory.getLogger(AbstractInputField.class);

    protected final ConfigurationAttribute attribute;
    protected final Orientation orientation;
    protected final T control;
    protected final Label errorLabel;

    protected String initValue = "";
    protected int listIndex = 0;

    protected AbstractInputField(
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
    public void disable(final boolean group) {
        if (this.control.isEnabled()) {
            if (group) {
                this.control.getParent().getParent().setEnabled(false);
            } else {
                setDefaultValue();
                this.control.setEnabled(false);
            }
        }
    }

    @Override
    public void enable(final boolean group) {
        if (!this.control.isEnabled()) {
            if (group) {
                this.control.getParent().getParent().setEnabled(true);
            } else {
                this.control.setEnabled(true);
            }
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
    public boolean hasError() {
        if (this.errorLabel == null) {
            return false;
        }

        return this.errorLabel.isVisible();
    }

    @Override
    public void clearError() {
        if (this.errorLabel == null) {
            return;
        }
        this.errorLabel.setVisible(false);
        this.errorLabel.setText(StringUtils.EMPTY);

    }

    @Override
    public ConfigurationValue initValue(final Collection<ConfigurationValue> values) {
        return values.stream()
                .filter(a -> this.attribute.id.equals(a.attributeId))
                .findFirst()
                .map(v -> {
                    initValue(v.value, v.listIndex);
                    return v;
                })
                .orElseGet(() -> {
                    initValue(this.attribute.defaultValue, 0);
                    return null;
                });
    }

    @Override
    public void initValue(final String value, final Integer listIndex) {
        this.initValue = value;
        this.listIndex = (listIndex != null) ? listIndex : 0;
        setValueToControl(this.initValue);
    }

    @Override
    public void setDefaultValue() {
        setValueToControl(this.attribute.defaultValue);
        final Event event = new Event();
        try {
            this.control.notifyListeners(SWT.FocusOut, event);
        } catch (final Exception e) {
            log.warn("Failed to send value update to server: {}", this.attribute, e);
        }
    }

    @Override
    public String getReadableValue() {
        return getValue();

    }

    protected abstract void setValueToControl(String value);

}
