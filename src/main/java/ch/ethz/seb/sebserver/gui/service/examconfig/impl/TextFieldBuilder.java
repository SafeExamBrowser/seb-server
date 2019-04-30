/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;

@Lazy
@Component
@GuiProfile
public class TextFieldBuilder implements InputFieldBuilder {

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.TEXT_FIELD ||
                attribute.type == AttributeType.TEXT_AREA ||
                attribute.type == AttributeType.INTEGER ||
                attribute.type == AttributeType.DECIMAL;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final Orientation orientation = viewContext.attributeMapping
                .getOrientation(attribute.id);

        final Composite comp = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        comp.setLayout(gridLayout);
        final GridData gridData = new GridData(
                SWT.FILL, SWT.FILL,
                true, false,
                (orientation != null) ? orientation.width() : 1,
                (orientation != null) ? orientation.height() : 1);
        comp.setLayoutData(gridData);

        final Text text;
        if (attribute.type == AttributeType.INTEGER ||
                attribute.type == AttributeType.DECIMAL) {

            text = new Text(comp, SWT.RIGHT | SWT.BORDER);
        } else {
            text = new Text(comp, SWT.LEFT | SWT.BORDER);
        }
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        final Label errorLabel = new Label(comp, SWT.NONE);
        errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        errorLabel.setVisible(false);
        errorLabel.setData(RWT.CUSTOM_VARIANT, "error");

        final TextInputField textInputField = new TextInputField(attribute, orientation, text, errorLabel);
        final ValueChangeListener valueListener = viewContext.getValueChangeListener();
        text.addListener(
                SWT.FocusOut,
                event -> {
                    textInputField.clearError();
                    valueListener.valueChanged(
                            viewContext,
                            attribute,
                            String.valueOf(text.getText()),
                            textInputField.listIndex);
                });

        return textInputField;
    }

    static final class TextInputField extends ControlFieldAdapter<Text> {

        private String initValue = "";
        private int listIndex = 0;

        TextInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final Text control,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
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

        @Override
        protected void setDefaultValue() {
            this.control.setText(this.initValue);
        }

    }

}
