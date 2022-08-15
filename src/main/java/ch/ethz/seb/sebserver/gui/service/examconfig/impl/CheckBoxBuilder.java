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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TitleOrientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class CheckBoxBuilder implements InputFieldBuilder {

    private final WidgetFactory widgetFactory;

    protected CheckBoxBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.CHECKBOX;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        Objects.requireNonNull(parent);
        Objects.requireNonNull(attribute);
        Objects.requireNonNull(viewContext);

        final I18nSupport i18nSupport = this.widgetFactory.getI18nSupport();
        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final Button checkbox = this.widgetFactory.buttonLocalized(
                innerGrid,
                SWT.CHECK,
                (orientation.title == TitleOrientation.NONE)
                        ? ExamConfigurationService.attributeNameLocKey(attribute)
                        : null,
                ExamConfigurationService.getToolTipKey(attribute, i18nSupport),
                ExamConfigurationService.attributeNameLocKey(attribute).name,
                ExamConfigurationService.attributeNameLocKey(attribute));

        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.verticalIndent = 0;
        checkbox.setLayoutData(gridData);

        final CheckboxField checkboxField = new CheckboxField(
                attribute,
                viewContext.getOrientation(attribute.id),
                checkbox);

        if (viewContext.readonly) {
            checkbox.setEnabled(false);
        } else {
            checkbox.addListener(
                    SWT.Selection,
                    event -> viewContext.getValueChangeListener().valueChanged(
                            viewContext,
                            attribute,
                            checkboxField.getValue(),
                            checkboxField.listIndex));
        }

        return checkboxField;
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
            if (ViewContext.INVERTED_CHECKBOX_SETTINGS.contains(this.attribute.name)) {
                this.control.setSelection(!Boolean.parseBoolean(this.initValue));
            } else {
                this.control.setSelection(Boolean.parseBoolean(this.initValue));
            }
        }

        @Override
        public String getValue() {
            if (ViewContext.INVERTED_CHECKBOX_SETTINGS.contains(this.attribute.name)) {
                return this.control.getSelection()
                        ? Constants.FALSE_STRING
                        : Constants.TRUE_STRING;
            } else {
                return this.control.getSelection()
                        ? Constants.TRUE_STRING
                        : Constants.FALSE_STRING;
            }
        }

        @Override
        public String getReadableValue() {
            if (ViewContext.INVERTED_CHECKBOX_SETTINGS.contains(this.attribute.name)) {
                return this.control.getSelection()
                        ? "Inactive"
                        : "Active";
            } else {
                return this.control.getSelection()
                        ? "Active"
                        : "Inactive";
            }
        }
    }

}
