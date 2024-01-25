package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import ch.ethz.seb.sebserver.gbl.Constants;
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
import ch.ethz.seb.sebserver.gui.widget.ColorSelection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@GuiProfile
public class ColorSelectorFieldBuilder implements InputFieldBuilder {

    private  final WidgetFactory widgetFactory;

    public ColorSelectorFieldBuilder(final WidgetFactory widgetFactory) {
        this.widgetFactory = widgetFactory;
    }


    @Override
    public void init(final InputFieldBuilderSupplier inputFieldBuilderSupplier) {
        InputFieldBuilder.super.init(inputFieldBuilderSupplier);
    }

    @Override
    public boolean builderFor(final ConfigurationAttribute attribute, final Orientation orientation) {
        if (attribute == null) {
            return false;
        }

        return attribute.type == AttributeType.COLOR_SELECTOR;
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

        final LocTextKey toolTipKey = ExamConfigurationService.getToolTipKey(
                attribute,
                i18nSupport);
        final ColorSelection colorSelector = new ColorSelection(
                innerGrid,
                widgetFactory,
                toolTipKey != null ? toolTipKey.name: null);

        colorSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        final ColorSelectionInputField singleSelectionInputField = new ColorSelectionInputField(
                attribute,
                orientation,
                colorSelector,
                FieldBuilder.createErrorLabel(innerGrid));

        if (viewContext.readonly) {
            colorSelector.setEnabled(false);
        } else {
            colorSelector.setSelectionListener(event -> {
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

    static final class ColorSelectionInputField extends AbstractInputField<ColorSelection> {

        protected ColorSelectionInputField(
                final ConfigurationAttribute attribute,
                final Orientation orientation,
                final ColorSelection control,
                final Label errorLabel) {

            super(attribute, orientation, control, errorLabel);
        }

        @Override
        public String getValue() {
            final String selection = this.control.getSelectionValue();
            if (selection == null) {
                return null;
            }
            return Constants.HASH_TAG + selection;
        }

        @Override
        protected void setValueToControl(final String value) {
            if (value != null && value.startsWith(Constants.HASH_TAG.toString())) {
                this.control.select(value.substring(1));
            } else {
                this.control.select(value);
            }
        }
    }
}
