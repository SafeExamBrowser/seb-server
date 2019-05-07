/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;

@Lazy
@Component
@GuiProfile
public class CheckBoxBuilder implements InputFieldBuilder {

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.CHECK_FIELD ||
                attribute.type == AttributeType.CHECKBOX;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        Objects.requireNonNull(parent);
        Objects.requireNonNull(attribute);
        Objects.requireNonNull(viewContext);

        final Button checkbox = new Button(parent, SWT.CHECK);
        if (attribute.type == AttributeType.CHECKBOX) {
            checkbox.setText(attribute.name);
        }

        checkbox.addListener(
                SWT.Selection,
                event -> viewContext.getValueChangeListener().valueChanged(
                        viewContext,
                        attribute,
                        String.valueOf(checkbox.getSelection()),
                        0));

        return new CheckboxField(
                attribute,
                viewContext.attributeMapping.getOrientation(attribute.id),
                checkbox);
    }

    static final class CheckboxField extends AbstractInputField<Button> {

        CheckboxField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Button control) {

            super(attribute, orientation, control, null);
        }

        @Override
        protected void setValueToControl(final String value) {
            this.control.setSelection(Boolean.valueOf(this.initValue));
        }
    }

}
