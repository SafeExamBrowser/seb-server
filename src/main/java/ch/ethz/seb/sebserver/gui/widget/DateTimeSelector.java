package ch.ethz.seb.sebserver.gui.widget;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.joda.time.DateTimeZone;

public class DateTimeSelector extends Composite  {

    private final DateTime date;
    private final DateTime time;
    private final DateTimeZone timeZone;

    public DateTimeSelector(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final DateTimeZone timeZone,
            final String label,
            final String testKey) {

        super(parent, SWT.NONE);
        this.timeZone = timeZone;

        final GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.verticalSpacing = 5;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        this.date = widgetFactory.dateSelector(this, new LocTextKey(label), testKey);
        this.time = widgetFactory.timeSelector(this, new LocTextKey(label), testKey);
        final Label timeZoneLabel = widgetFactory.label(this, timeZone.getID());

        setDateTime(org.joda.time.DateTime.now(this.timeZone));
    }

    public void setValue(final String dateTimeString) {
        if (dateTimeString == null) {
            setDateTime(org.joda.time.DateTime.now(this.timeZone));
            return;
        }
        setDateTime(Utils.toDateTime(dateTimeString).withZone(this.timeZone));
    }

    public void setValue(final org.joda.time.DateTime time) {
        if (time == null) {
            setDateTime(org.joda.time.DateTime.now(this.timeZone));
            return;
        }
        setDateTime(time.withZone(this.timeZone));
    }

    private void setDateTime(final org.joda.time.DateTime time) {
        this.date.setDate(time.getYear(), time.getMonthOfYear() - 1, time.getDayOfMonth());
        this.time.setTime(time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute());
    }

    public String getValue() {
        return Utils.formatDate(org.joda.time.DateTime.now(this.timeZone)
                .withYear(this.date.getYear())
                .withMonthOfYear(this.date.getMonth() + 1)
                .withDayOfMonth(this.date.getDay())
                .withHourOfDay((this.time != null) ? this.time.getHours() : 0)
                .withMinuteOfHour((this.time != null) ? this.time.getMinutes() : 0)
                .withSecondOfMinute((this.time != null) ? this.time.getSeconds() : 0));
    }

}
