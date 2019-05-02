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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
public class LabelBuilder implements InputFieldBuilder {

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.LABEL;
    };

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        Objects.requireNonNull(parent);
        Objects.requireNonNull(attribute);
        Objects.requireNonNull(viewContext);

        final Label label = new Label(parent, SWT.NONE);
        label.setText(attribute.name);

        return new LabelField(
                attribute,
                viewContext.attributeMapping.getOrientation(attribute.id),
                label);
    }

    static final class LabelField extends AbstractInputField<Label> {

        LabelField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Label control) {

            super(attribute, orientation, control, null);
        }

        @Override
        protected void setDefaultValue() {
            // Does Nothing, Label has no default value
        }

    }

}
