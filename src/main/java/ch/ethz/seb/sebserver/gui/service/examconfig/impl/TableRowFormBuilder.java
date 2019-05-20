/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues.TableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.TableFieldBuilder.TableInputField;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class TableRowFormBuilder implements ModalInputDialogComposer<Map<Long, TableValue>> {

    private final TableContext tableContext;
    private final Map<Long, TableValue> rowValues;
    private final int listIndex;

    public TableRowFormBuilder(
            final TableContext tableContext,
            final Map<Long, TableValue> rowValues,
            final int listIndex) {

        this.tableContext = tableContext;
        this.rowValues = rowValues;
        this.listIndex = listIndex;
    }

    @Override
    public Supplier<Map<Long, TableValue>> compose(final Composite parent) {

        final List<InputField> inputFields = new ArrayList<>();
        final Composite grid = this.tableContext
                .getWidgetFactory()
                .formGrid(parent, 2);

        final GridLayout layout = (GridLayout) grid.getLayout();
        layout.verticalSpacing = 0;

        for (final ConfigurationAttribute attribute : this.tableContext.getRowAttributes()) {
            createLabel(grid, attribute);
            inputFields.add(createInputField(grid, attribute));
        }

        // when the pop-up gets closed we have to remove the input fields from the view context
        grid.addDisposeListener(event -> {
            this.tableContext.flushInputFields(this.rowValues.keySet());
        });

        return () -> inputFields.stream()
                .map(field -> new TableValue(
                        field.getAttribute().id,
                        this.listIndex,
                        field.getValue()))
                .collect(Collectors.toMap(tv -> tv.attributeId, Function.identity()));
    }

    private InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute) {

//        if (attribute.type == AttributeType.TABLE) {
//            throw new UnsupportedOperationException(
//                    "Table type is currently not supported within a table row form view!");
//        }

        final Orientation orientation = this.tableContext
                .getOrientation(attribute.id);
        final InputFieldBuilder inputFieldBuilder = this.tableContext
                .getInputFieldBuilder(attribute, orientation);
        final InputField inputField = inputFieldBuilder.createInputField(
                parent,
                attribute,
                this.tableContext.getViewContext());

        if (attribute.type == AttributeType.TABLE) {
            ((TableInputField) inputField).initValue(new ArrayList<>(this.rowValues.values()));
        } else {
            inputField.initValue(
                    this.rowValues.get(attribute.id).value,
                    this.listIndex);
        }

        // we have to register the input field within the ViewContext to receive error messages
        this.tableContext.registerInputField(inputField);

        return inputField;
    }

    private void createLabel(
            final Composite parent,
            final ConfigurationAttribute attribute) {

        final LocTextKey locTextKey = new LocTextKey(
                ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX +
                        attribute.name,
                attribute.name);
        final Label label = this.tableContext
                .getWidgetFactory()
                .labelLocalized(parent, locTextKey);
        final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, true, false);
        gridData.verticalIndent = 4;
        label.setLayoutData(gridData);
        label.setData(RWT.CUSTOM_VARIANT, CustomVariant.TITLE_LABEL.key);
    }

}
