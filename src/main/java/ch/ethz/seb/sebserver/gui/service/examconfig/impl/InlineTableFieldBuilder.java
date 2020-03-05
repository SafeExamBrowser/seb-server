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
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
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
import ch.ethz.seb.sebserver.gui.widget.GridTable;
import ch.ethz.seb.sebserver.gui.widget.GridTable.ColumnDef;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class InlineTableFieldBuilder implements InputFieldBuilder {

    private final WidgetFactory widgetFactory;

    protected InlineTableFieldBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public boolean builderFor(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return attribute != null && attribute.type == AttributeType.INLINE_TABLE;
    }

    @Override
    public InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        final Orientation orientation = viewContext
                .getOrientation(attribute.id);
        final Composite innerGrid = InputFieldBuilder
                .createInnerGrid(parent, attribute, orientation);

        final Map<String, String> defaultValues = StringUtils.isBlank(attribute.defaultValue)
                ? Collections.emptyMap()
                : Arrays.stream(StringUtils.split(
                        attribute.defaultValue,
                        Constants.EMBEDDED_LIST_SEPARATOR))
                        .map(valueString -> StringUtils.split(
                                valueString,
                                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR))
                        .collect(Collectors.toMap(
                                valueMap -> valueMap[0],
                                valueMap -> (valueMap.length > 1) ? valueMap[1] : StringUtils.EMPTY));

        final List<ColumnDef> columns = Arrays.stream(StringUtils.split(
                attribute.getResources(),
                Constants.EMBEDDED_LIST_SEPARATOR))
                .map(columnString -> ColumnDef.fromString(columnString, defaultValues))
                .collect(Collectors.toList());

        final GridTable gridTable = new GridTable(
                innerGrid,
                columns,
                ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name + ".",
                this.widgetFactory);

        final InlineTableInputField inlineTableInputField = new InlineTableInputField(
                attribute,
                viewContext.getOrientation(attribute.id),
                gridTable);

        if (viewContext.readonly) {
            gridTable.setEnabled(false);
            gridTable.setListener(event -> {
            });
        } else {
            gridTable.setListener(event -> viewContext.getValueChangeListener().valueChanged(
                    viewContext,
                    attribute,
                    inlineTableInputField.getValue(),
                    inlineTableInputField.listIndex));
        }

        return inlineTableInputField;

    }

    static final class InlineTableInputField extends AbstractInputField<GridTable> {

        protected InlineTableInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final GridTable control) {

            super(attribute, orientation, control, null);
        }

        @Override
        public String getValue() {
            return this.control.getValue();
        }

        @Override
        protected void setValueToControl(final String value) {
            this.control.setValue(value);
        }
    }

}
