package ch.ethz.seb.sebserver.gui.form;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.DateTimeSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;

public class DateTimeSelectorFieldBuilder extends FieldBuilder<DateTime> {

    public DateTimeSelectorFieldBuilder(final String name, final LocTextKey label, final DateTime value) {
        super(name, label, value);
    }

    @Override
    void build(final FormBuilder builder) {
        final boolean readonly = builder.readonly || this.readonly;
        final Control titleLabel = createTitleLabel(builder.formParent, builder, this);
        final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);

        if (readonly) {
            final Text readonlyLabel = builder.widgetFactory.textInput(fieldGrid, this.label);
            readonlyLabel.setEditable(false);
            readonlyLabel.setText(builder.i18nSupport.formatDisplayDateWithTimeZone(value));
            readonlyLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
            builder.form.putReadonlyField(this.name, titleLabel, readonlyLabel);
            return;
        }

        final DateTimeSelector dateTimeSelector = new DateTimeSelector(
                fieldGrid,
                builder.widgetFactory,
                builder.pageService.getCurrentUser().get().timeZone,
                this.label.name,
                label.name);

        dateTimeSelector.setValue(value);
        final Label errorLabel = createErrorLabel(fieldGrid);
        builder.form.putField(this.name, titleLabel, dateTimeSelector, errorLabel);
    }
}
