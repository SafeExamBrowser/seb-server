/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SliderFieldBuilder implements InputFieldBuilder {

    public SliderFieldBuilder() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return attribute != null && attribute.type == AttributeType.SLIDER;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final I18nSupport i18nSupport = viewContext.getI18nSupport();
        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final Slider slider = new Slider(innerGrid, SWT.NONE);
        slider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        final String attributeNameKey = ExamConfigurationService.attributeNameKey(attribute);
        WidgetFactory.setTestId(slider, attributeNameKey);
        WidgetFactory.setARIALabel(slider, i18nSupport.getText(attributeNameKey));

        try {
            final String[] split = StringUtils.split(
                    attribute.getResources(),
                    Constants.LIST_SEPARATOR);

            slider.setMinimum(Integer.parseInt(split[0]));
            slider.setMaximum(Integer.parseInt(split[1]));
        } catch (final NumberFormatException e) {
            slider.setMinimum(0);
            slider.setMaximum(100);
        }

        final SliderInputField inputField = new SliderInputField(
                attribute,
                orientation,
                slider);

        if (viewContext.readonly) {
            slider.setEnabled(false);
        } else {
            final Listener valueChangeEventListener = event -> {
                inputField.clearError();
                viewContext.getValueChangeListener().valueChanged(
                        viewContext,
                        attribute,
                        inputField.getValue(),
                        inputField.listIndex);
            };

            slider.addListener(SWT.FocusOut, valueChangeEventListener);
            slider.addListener(SWT.Traverse, valueChangeEventListener);
        }
        return inputField;
    }

    static final class SliderInputField extends AbstractInputField<Slider> {

        SliderInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Slider control) {

            super(attribute, orientation, control, null);
        }

        @Override
        protected void setValueToControl(final String value) {
            try {
                this.control.setSelection(Integer.parseInt(value));
            } catch (final Exception e) {
                this.control.setSelection(this.control.getMinimum());
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(this.control.getSelection());
        }
    }

}
