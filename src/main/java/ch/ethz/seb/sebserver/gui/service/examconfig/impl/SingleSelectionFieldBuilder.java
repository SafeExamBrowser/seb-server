/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.FieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.SingleSelection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SingleSelectionFieldBuilder extends SelectionFieldBuilder implements InputFieldBuilder {

    private final WidgetFactory widgetFactory;

    protected SingleSelectionFieldBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return attribute.type == AttributeType.SINGLE_SELECTION ||
                attribute.type == AttributeType.COMBO_SELECTION;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final I18nSupport i18nSupport = this.widgetFactory.getI18nSupport();
        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final LocTextKey toolTipKey = ExamConfigurationService.getToolTipKey(attribute, i18nSupport);
        final String attributeNameKey = ExamConfigurationService.attributeNameKey(attribute);
        final SingleSelection selection = this.widgetFactory.selectionLocalized(
                (attribute.type == AttributeType.COMBO_SELECTION)
                        ? Selection.Type.SINGLE_COMBO
                        : Selection.Type.SINGLE,
                innerGrid,
                () -> this.getLocalizedResources(attribute, viewContext),
                toolTipKey,
                null,
                attributeNameKey,
                i18nSupport.getText(attributeNameKey))
                .getTypeInstance();

        selection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        final SingleSelectionInputField singleSelectionInputField = new SingleSelectionInputField(
                attribute,
                orientation,
                selection,
                FieldBuilder.createErrorLabel(innerGrid));

        if (viewContext.readonly) {
            selection.setEnabled(false);
        } else {
            selection.setSelectionListener(event -> {
                singleSelectionInputField.clearError();
                viewContext.getValueChangeListener().valueChanged(
                        viewContext,
                        attribute,
                        singleSelectionInputField.getValue(),
                        singleSelectionInputField.listIndex);
            });
        }

        return singleSelectionInputField;
    }

    static final class SingleSelectionInputField extends AbstractInputField<SingleSelection> {

        protected SingleSelectionInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final SingleSelection control,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
        }

        @Override
        public String getValue() {
            return this.control.getSelectionValue();
        }

        @Override
        protected void setValueToControl(final String value) {
            this.control.select(value);
        }
    }

}
