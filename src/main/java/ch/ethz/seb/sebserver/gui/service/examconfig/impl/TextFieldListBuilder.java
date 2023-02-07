/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
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
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.TextListInput;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class TextFieldListBuilder extends AbstractTableFieldBuilder {

    protected TextFieldListBuilder(
            final RestService restService,
            final WidgetFactory widgetFactory) {

        super(restService, widgetFactory);
    }

    @Override
    public boolean builderFor(final ConfigurationAttribute attribute, final Orientation orientation) {
        return AttributeType.TEXT_FIELD_LIST == attribute.type;
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

//        final Composite scroll = PageService.createManagedVScrolledComposite(
//                innerGrid,
//                scrolledComposite -> {
//                    final Composite result = new Composite(scrolledComposite, SWT.NONE);
//                    final GridLayout gridLayout1 = new GridLayout();
//                    result.setLayout(gridLayout1);
//                    final GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, true);
//                    result.setLayoutData(gridData1);
//                    return result;
//                },
//                false,
//                false, true);

        final String attributeNameKey = ExamConfigurationService.attributeNameKey(attribute);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.minimumHeight = WidgetFactory.TEXT_AREA_INPUT_MIN_HEIGHT;
        final TextListInput textListInput = new TextListInput(
                innerGrid,
                new LocTextKey(attributeNameKey),
                3,
                this.widgetFactory,
                !viewContext.isReadonly());
        WidgetFactory.setTestId(textListInput, attributeNameKey);
        textListInput.setLayoutData(gridData);

        final TextListInputField textListInputField = new TextListInputField(
                attribute,
                orientation,
                textListInput,
                FieldBuilder.createErrorLabel(innerGrid));

        if (viewContext.readonly) {
            textListInput.setEditable(false);

        } else {
            final Listener valueChangeEventListener = event -> {
                textListInputField.clearError();
                viewContext.getValueChangeListener().valueChanged(
                        viewContext,
                        attribute,
                        textListInputField.getValue(),
                        textListInputField.listIndex);
            };

            textListInput.addListener(valueChangeEventListener);
        }

        return textListInputField;
    }

    static final class TextListInputField extends AbstractInputField<TextListInput> {

        TextListInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final TextListInput control,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
        }

        @Override
        protected void setValueToControl(final String value) {
            if (value == null) {
                this.control.setValue(StringUtils.EMPTY);
                return;
            }

            this.control.setValue(value);
        }

        @Override
        public void enable(final boolean group) {
            this.control.setData(RWT.CUSTOM_VARIANT, null);
            this.control.setEditable(true);
        }

        @Override
        public void disable(final boolean group) {
            this.control.setData(RWT.CUSTOM_VARIANT, CustomVariant.CONFIG_INPUT_READONLY.key);
            this.control.setEditable(false);
            final GridData gridData = (GridData) this.control.getLayoutData();
            gridData.heightHint = (this.attribute.type == AttributeType.TEXT_AREA)
                    ? WidgetFactory.TEXT_AREA_INPUT_MIN_HEIGHT
                    : WidgetFactory.TEXT_INPUT_MIN_HEIGHT;
        }

        @Override
        public String getValue() {
            return this.control.getValue();
        }
    }

}
