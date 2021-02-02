/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LabelBuilder implements InputFieldBuilder {

    private final WidgetFactory widgetFactory;

    protected LabelBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.LABEL;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final Label label = this.widgetFactory.labelLocalized(
                parent,
                ExamConfigurationService.attributeNameLocKey(attribute),
                true);

        return new LabelField(
                attribute,
                viewContext.getOrientation(attribute.id),
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
        protected void setValueToControl(final String value) {
            // Does Nothing, Label has no default value
        }

        @Override
        public String getValue() {
            return this.control.getText();
        }

    }

}
