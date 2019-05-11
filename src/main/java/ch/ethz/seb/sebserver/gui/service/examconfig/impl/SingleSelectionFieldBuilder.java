/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
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
public class SingleSelectionFieldBuilder implements InputFieldBuilder {

    private final WidgetFactory widgetFactory;

    protected SingleSelectionFieldBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return attribute.type == AttributeType.SINGLE_SELECTION;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, orientation);

        final SingleSelection selection = this.widgetFactory.selectionLocalized(
                Selection.Type.SINGLE,
                innerGrid,
                () -> this.getLocalizedResources(attribute))
                .getTypeInstance();

        final SingleSelectionInputField singleSelectionInputField = new SingleSelectionInputField(
                attribute,
                orientation,
                selection,
                InputFieldBuilder.createErrorLabel(innerGrid));

        selection.setSelectionListener(event -> {
            singleSelectionInputField.clearError();
            viewContext.getValueChangeListener().valueChanged(
                    viewContext,
                    attribute,
                    String.valueOf(selection.getSelectionValue()),
                    singleSelectionInputField.listIndex);
        });

        return null;
    }

    private List<Tuple<String>> getLocalizedResources(final ConfigurationAttribute attribute) {
        if (attribute == null) {
            return Collections.emptyList();
        }

        final I18nSupport i18nSupport = this.widgetFactory.getI18nSupport();
        final String prefix = ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name + ".";
        return Arrays.asList(StringUtils.split(
                attribute.resources,
                Constants.LIST_SEPARATOR))
                .stream()
                .map(value -> new Tuple<>(
                        value,
                        i18nSupport.getText(
                                new LocTextKey(prefix + value),
                                value)))
                .collect(Collectors.toList());
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
