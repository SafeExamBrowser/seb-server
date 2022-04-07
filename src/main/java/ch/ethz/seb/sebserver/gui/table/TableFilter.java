/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class TableFilter<ROW extends ModelIdAware> {

    private static final Logger log = LoggerFactory.getLogger(TableFilter.class);

    private static final LocTextKey DATE_FROM_TEXT = new LocTextKey("sebserver.overall.date.from");
    private static final LocTextKey DATE_TO_TEXT = new LocTextKey("sebserver.overall.date.to");
    private static final LocTextKey ALL_TEXT = new LocTextKey("sebserver.overall.status.all");

    public enum CriteriaType {
        TEXT,
        SINGLE_SELECTION,
        DATE,
        DATE_RANGE,
        DATE_TIME_RANGE
    }

    private final Composite composite;
    private final EntityTable<ROW> entityTable;
    private final List<FilterComponent> components = new ArrayList<>();

    TableFilter(final EntityTable<ROW> entityTable) {
        this.composite = new Composite(entityTable.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.composite.setLayoutData(gridData);
        final RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginLeft = 0;
        layout.wrap = false;
        layout.center = false;
        layout.fill = true;
        this.composite.setLayout(layout);
        this.entityTable = entityTable;
        buildComponents();
    }

    public int size() {
        return this.components.size();
    }

    public MultiValueMap<String, String> getFilterParameter() {
        return this.components
                .stream()
                .reduce(new LinkedMultiValueMap<>(),
                        (map, comp) -> comp.putFilterParameter(map),
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        });
    }

    public void reset() {
        this.components
                .forEach(FilterComponent::reset);
    }

    private void buildComponents() {
        this.components.clear();
        this.components.addAll(this.entityTable.columns
                .stream()
                .map(ColumnDefinition::getFilterAttribute)
                .map(this::createFilterComponent)
                .map(comp -> comp.build(this.composite))
                .map(FilterComponent::reset)
                .collect(Collectors.toList()));

        FilterComponent lastComp = this.components.get(this.components.size() - 1);
        while (lastComp instanceof TableFilter.NullFilter) {
            this.components.remove(lastComp);
            lastComp = this.components.get(this.components.size() - 1);
        }

        addActions();
    }

    private FilterComponent createFilterComponent(final TableFilterAttribute attribute) {
        if (attribute == null) {
            return new NullFilter();
        }

        switch (attribute.type) {
            case TEXT:
                return new TextFilter(attribute);
            case SINGLE_SELECTION:
                return new SelectionFilter(attribute);
            case DATE:
                return new Date(attribute);
            case DATE_RANGE:
                return new DateRange(attribute);
            case DATE_TIME_RANGE:
                return new DateRange(attribute, true);
            default:
                throw new IllegalArgumentException("Unsupported FilterAttributeType: " + attribute.type);
        }
    }

    boolean adaptColumnWidth(final int columnIndex, final int width) {
        if (columnIndex < this.components.size()) {
            final boolean adaptWidth = this.components.get(columnIndex).adaptWidth(width + 2);
            if (adaptWidth) {
                this.composite.layout();
            }
            return adaptWidth;
        }

        return false;
    }

    String getFilterAttributes() {
        final StringBuilder builder = this.components
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, filter) -> sb
                                .append(filter.attribute.columnName)
                                .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                                .append(filter.getValue())
                                .append(Constants.LIST_SEPARATOR),
                        StringBuilder::append);
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    void setFilterAttributes(final String attribute) {
        if (StringUtils.isBlank(attribute)) {
            return;
        }

        try {
            Arrays.stream(StringUtils.split(
                    attribute,
                    Constants.LIST_SEPARATOR_CHAR))
                    .map(nameValue -> StringUtils.split(
                            nameValue,
                            Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR))
                    .forEach(nameValue -> this.components
                            .stream()
                            .filter(filter -> nameValue[0].equals(filter.attribute.columnName))
                            .findFirst()
                            .ifPresent(filter -> filter.setValue((nameValue.length > 1)
                                    ? nameValue[1]
                                    : StringUtils.EMPTY)));
        } catch (final Exception e) {
            log.error("Failed to set filter attributes: ", e);
        }
    }

    private void addActions() {
        final Composite inner = new Composite(this.composite, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(2, true);
        gridLayout.horizontalSpacing = 5;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        inner.setLayout(gridLayout);
        inner.setLayoutData(new RowData());

        final GridData gridData = new GridData(SWT.FILL, SWT.BOTTOM, true, true);
        gridData.heightHint = 20;
        final Button imageButton = this.entityTable.widgetFactory.imageButton(
                ImageIcon.SEARCH,
                inner,
                new LocTextKey("sebserver.overall.action.filter"),
                event -> this.entityTable.applyFilter());
        imageButton.setLayoutData(gridData);
        final Button imageButton2 = this.entityTable.widgetFactory.imageButton(
                ImageIcon.CANCEL,
                inner,
                new LocTextKey("sebserver.overall.action.filter.clear"),
                event -> {
                    reset();
                    this.entityTable.reset();
                });
        imageButton2.setLayoutData(gridData);
    }

    private static abstract class FilterComponent {

        static final int CELL_WIDTH_ADJUSTMENT = -5;

        protected final RowData rowData;
        final TableFilterAttribute attribute;

        FilterComponent(final TableFilterAttribute attribute) {
            this.attribute = attribute;
            this.rowData = new RowData();
        }

        LinkedMultiValueMap<String, String> putFilterParameter(
                final LinkedMultiValueMap<String, String> filterParameter) {

            final String value = getValue();
            if (StringUtils.isNotBlank(value)) {
                filterParameter.put(this.attribute.columnName, Arrays.asList(value));
            }
            return filterParameter;
        }

        abstract FilterComponent build(Composite parent);

        abstract FilterComponent reset();

        abstract String getValue();

        abstract void setValue(String value);

        boolean adaptWidth(final int width) {
            final int _width = width + CELL_WIDTH_ADJUSTMENT;
            if (_width != this.rowData.width) {
                this.rowData.width = _width;
                return true;
            }

            return false;
        }

        protected Composite createInnerComposite(final Composite parent) {
            final Composite inner = new Composite(parent, SWT.NONE);
            final GridLayout gridLayout = new GridLayout(1, true);
            gridLayout.horizontalSpacing = 0;
            gridLayout.verticalSpacing = 0;
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            inner.setLayout(gridLayout);
            inner.setLayoutData(this.rowData);
            return inner;
        }

        protected LocTextKey getAriaLabel() {
            return new LocTextKey("sebserver.form.tablefilter.label", this.attribute.columnName);
        }
    }

    private static class NullFilter extends FilterComponent {

        private Label label;

        NullFilter() {
            super(null);
        }

        @Override
        FilterComponent build(final Composite parent) {
            this.label = new Label(parent, SWT.NONE);
            this.label.setLayoutData(this.rowData);
            return this;
        }

        @Override
        FilterComponent reset() {
            return this;
        }

        @Override
        String getValue() {
            return null;
        }

        @Override
        void setValue(final String value) {
        }

    }

    private class TextFilter extends FilterComponent {

        private Text textInput;

        TextFilter(final TableFilterAttribute attribute) {
            super(attribute);
        }

        @Override
        FilterComponent reset() {
            if (this.textInput != null) {
                this.textInput.setText(super.attribute.initValue);
            }
            return this;
        }

        @Override
        FilterComponent build(final Composite parent) {
            final Composite innerComposite = createInnerComposite(parent);
            final GridData gridData = new GridData(SWT.FILL, SWT.END, true, true);

            this.textInput = TableFilter.this.entityTable.widgetFactory.textInput(
                    innerComposite,
                    TableFilter.this.entityTable.getName() + "_" + this.attribute.columnName,
                    getAriaLabel());

            this.textInput.setLayoutData(gridData);
            this.textInput.addListener(SWT.KeyUp, event -> {
                if (event.keyCode == Constants.ENTER.hashCode()) {
                    TableFilter.this.entityTable.applyFilter();
                }
            });
            return this;
        }

        @Override
        String getValue() {
            if (this.textInput != null) {
                return this.textInput.getText();
            }

            return null;
        }

        @Override
        void setValue(final String value) {
            if (this.textInput != null) {
                this.textInput.setText(value);
            }
        }
    }

    private class SelectionFilter extends FilterComponent {

        protected Selection selector;

        SelectionFilter(final TableFilterAttribute attribute) {
            super(attribute);
        }

        @Override
        FilterComponent build(final Composite parent) {
            final Composite innerComposite = createInnerComposite(parent);
            final GridData gridData = new GridData(SWT.FILL, SWT.END, true, true);

            Supplier<List<Tuple<String>>> resourceSupplier = this.attribute.resourceSupplier;
            if (this.attribute.resourceFunction != null) {
                resourceSupplier = () -> this.attribute.resourceFunction.apply(TableFilter.this.entityTable);
            }

            final Supplier<List<Tuple<String>>> _resourceSupplier = resourceSupplier;
            resourceSupplier = () -> {
                final List<Tuple<String>> list = _resourceSupplier.get();
                list.add(new Tuple<>(StringUtils.EMPTY, TableFilter.this.entityTable.i18nSupport.getText(ALL_TEXT)));
                return list;
            };

            this.selector = TableFilter.this.entityTable.widgetFactory
                    .selectionLocalized(
                            ch.ethz.seb.sebserver.gui.widget.Selection.Type.SINGLE,
                            innerComposite,
                            resourceSupplier,
                            null, null,
                            TableFilter.this.entityTable.getName() + "_" + this.attribute.columnName,
                            TableFilter.this.entityTable.widgetFactory.getI18nSupport().getText(getAriaLabel()));

            this.selector
                    .adaptToControl()
                    .setLayoutData(gridData);
            this.selector.setSelectionListener(event -> {
                TableFilter.this.entityTable.applyFilter();
            });
            return this;
        }

        @Override
        FilterComponent reset() {
            if (this.selector != null) {
                this.selector.clear();
                if (this.attribute.initValue != null) {
                    this.selector.select(this.attribute.initValue);
                }
            }
            return this;
        }

        @Override
        String getValue() {
            if (this.selector != null) {
                return this.selector.getSelectionValue();
            }

            return null;
        }

        @Override
        void setValue(final String value) {
            if (this.selector != null) {
                this.selector.select(value);
            }
        }
    }

    // NOTE: SWT DateTime month-number starting with 0 and joda DateTime with 1!
    private class Date extends FilterComponent {

        private DateTime selector;
        private final DateTimeZone timeZone;

        Date(final TableFilterAttribute attribute) {
            super(attribute);
            final UserInfo userInfo = TableFilter.this.entityTable.pageService.getCurrentUser().get();
            this.timeZone =
                    (userInfo != null && userInfo.timeZone != null && !userInfo.timeZone.equals(DateTimeZone.UTC))
                            ? userInfo.timeZone
                            : DateTimeZone.UTC;
        }

        @Override
        FilterComponent build(final Composite parent) {
            final Composite innerComposite = createInnerComposite(parent);
            this.selector = TableFilter.this.entityTable.widgetFactory.dateSelector(
                    innerComposite,
                    getAriaLabel(),
                    TableFilter.this.entityTable.getName() + "_" + this.attribute.columnName);
            this.selector.addListener(SWT.Selection, event -> {
                TableFilter.this.entityTable.applyFilter();
            });
            return this;
        }

        @Override
        FilterComponent reset() {
            if (this.selector != null) {
                try {
                    final org.joda.time.DateTime parse = org.joda.time.DateTime.parse(this.attribute.initValue);
                    this.selector.setDate(parse.getYear(), parse.getMonthOfYear() - 1, parse.getDayOfMonth());
                } catch (final RuntimeException e) {
                    final org.joda.time.DateTime now = org.joda.time.DateTime.now(DateTimeZone.UTC);
                    this.selector.setDate(now.getYear(), now.getMonthOfYear() - 1, now.getDayOfMonth());
                }
            }
            return this;
        }

        @Override
        String getValue() {
            if (this.selector != null) {
                final org.joda.time.DateTime date = org.joda.time.DateTime.now(this.timeZone)
                        .withYear(this.selector.getYear())
                        .withMonthOfYear(this.selector.getMonth() + 1)
                        .withDayOfMonth(this.selector.getDay())
                        .withTimeAtStartOfDay();

                return date.toString(Constants.STANDARD_DATE_TIME_FORMATTER);
            } else {
                return null;
            }
        }

        @Override
        void setValue(final String value) {
            if (this.selector != null) {
                try {
                    final org.joda.time.DateTime date = new org.joda.time.DateTime(
                            Utils.toDateTime(value),
                            this.timeZone);
                    this.selector.setDate(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
                } catch (final Exception e) {
                    log.error("Failed to set date filter attribute: ", e);
                }
            }
        }

        @Override
        boolean adaptWidth(final int width) {
            // NOTE: for some unknown reason RWT acts differently on width-property for date selector
            //       this is to adjust date filter criteria to the list column width
            return super.adaptWidth(width - 5);
        }
    }

    // NOTE: SWT DateTime month-number starting with 0 and joda DateTime with 1!
    private class DateRange extends FilterComponent {

        private Composite innerComposite;
        private DateTime fromDateSelector;
        private DateTime toDateSelector;
        private DateTime fromTimeSelector;
        private DateTime toTimeSelector;
        private final boolean withTime;
        private final DateTimeZone timeZone;

        DateRange(final TableFilterAttribute attribute) {
            this(attribute, false);
        }

        DateRange(final TableFilterAttribute attribute, final boolean withTime) {
            super(attribute);
            this.withTime = withTime;
            final UserInfo userInfo = TableFilter.this.entityTable.pageService.getCurrentUser().get();
            this.timeZone =
                    (userInfo != null && userInfo.timeZone != null && !userInfo.timeZone.equals(DateTimeZone.UTC))
                            ? userInfo.timeZone
                            : DateTimeZone.UTC;
        }

        @Override
        FilterComponent build(final Composite parent) {
            this.innerComposite = new Composite(parent, SWT.NONE);
            final GridLayout gridLayout = new GridLayout((this.withTime) ? 3 : 2, false);
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            gridLayout.horizontalSpacing = 5;
            gridLayout.verticalSpacing = 3;
            this.innerComposite.setLayout(gridLayout);
            this.innerComposite.setLayoutData(this.rowData);

            final String testKey = TableFilter.this.entityTable.getName() + "_" + this.attribute.columnName;
            final WidgetFactory wf = TableFilter.this.entityTable.widgetFactory;
            wf.labelLocalized(this.innerComposite, DATE_FROM_TEXT);
            this.fromDateSelector = wf.dateSelector(this.innerComposite, getAriaLabel(), testKey);
            this.fromDateSelector.addListener(SWT.Selection, event -> {
                TableFilter.this.entityTable.applyFilter();
            });
            if (this.withTime) {
                this.fromTimeSelector = wf.timeSelector(this.innerComposite, getAriaLabel(), testKey);
                this.fromTimeSelector.addListener(SWT.Selection, event -> {
                    TableFilter.this.entityTable.applyFilter();
                });
            }

            wf.labelLocalized(this.innerComposite, DATE_TO_TEXT);
            this.toDateSelector = wf.dateSelector(this.innerComposite, getAriaLabel(), testKey);
            this.toDateSelector.addListener(SWT.Selection, event -> {
                TableFilter.this.entityTable.applyFilter();
            });
            if (this.withTime) {
                this.toTimeSelector = wf.timeSelector(this.innerComposite, getAriaLabel(), testKey);
                this.toTimeSelector.addListener(SWT.Selection, event -> {
                    TableFilter.this.entityTable.applyFilter();
                });
            }

            return this;
        }

        @Override
        FilterComponent reset() {
            final org.joda.time.DateTime now = org.joda.time.DateTime.now(this.timeZone);
            if (this.fromDateSelector != null) {
                try {
                    final org.joda.time.DateTime parse = new org.joda.time.DateTime(
                            org.joda.time.DateTime.parse(this.attribute.initValue),
                            this.timeZone);

                    this.fromDateSelector.setDate(
                            parse.getYear(),
                            parse.getMonthOfYear() - 1,
                            parse.getDayOfMonth());
                    if (this.fromTimeSelector != null) {
                        this.fromTimeSelector.setTime(
                                parse.getHourOfDay(),
                                parse.getMinuteOfHour(),
                                parse.getSecondOfMinute());
                    }

                } catch (final RuntimeException e) {
                    this.fromDateSelector.setDate(
                            now.getYear(),
                            now.getMonthOfYear() - 1,
                            now.getDayOfMonth());
                    if (this.fromTimeSelector != null) {
                        this.fromTimeSelector.setTime(
                                now.getHourOfDay(),
                                now.getMinuteOfHour(),
                                now.getSecondOfMinute());
                    }
                }
            }
            if (this.toDateSelector != null) {
                this.toDateSelector.setDate(
                        now.getYear(),
                        now.getMonthOfYear() - 1,
                        now.getDayOfMonth());
                if (this.toTimeSelector != null) {
                    this.toTimeSelector.setTime(
                            now.getHourOfDay(),
                            now.getMinuteOfHour(),
                            now.getSecondOfMinute());
                }
            }
            return this;
        }

        @Override
        String getValue() {
            if (this.fromDateSelector != null && this.toDateSelector != null) {
                org.joda.time.DateTime fromDate = org.joda.time.DateTime.now(this.timeZone)
                        .withYear(this.fromDateSelector.getYear())
                        .withMonthOfYear(this.fromDateSelector.getMonth() + 1)
                        .withDayOfMonth(this.fromDateSelector.getDay())
                        .withHourOfDay((this.fromTimeSelector != null) ? this.fromTimeSelector.getHours() : 0)
                        .withMinuteOfHour((this.fromTimeSelector != null) ? this.fromTimeSelector.getMinutes() : 0)
                        .withSecondOfMinute((this.fromTimeSelector != null) ? this.fromTimeSelector.getSeconds() : 0);
                org.joda.time.DateTime toDate = org.joda.time.DateTime.now(this.timeZone)
                        .withYear(this.toDateSelector.getYear())
                        .withMonthOfYear(this.toDateSelector.getMonth() + 1)
                        .withDayOfMonth(this.toDateSelector.getDay())
                        .withHourOfDay((this.toTimeSelector != null) ? this.toTimeSelector.getHours() : 0)
                        .withMinuteOfHour((this.toTimeSelector != null) ? this.toTimeSelector.getMinutes() : 0)
                        .withSecondOfMinute((this.toTimeSelector != null) ? this.toTimeSelector.getSeconds() : 0);

                if (this.fromTimeSelector == null) {
                    fromDate = fromDate.withTimeAtStartOfDay();
                }
                if (this.toTimeSelector == null) {
                    toDate = toDate.plusDays(1).withTimeAtStartOfDay();
                }

                return fromDate.toString(Constants.STANDARD_DATE_TIME_FORMATTER) +
                        Constants.EMBEDDED_LIST_SEPARATOR +
                        toDate.toString(Constants.STANDARD_DATE_TIME_FORMATTER);
            } else {
                return null;
            }
        }

        @Override
        void setValue(final String value) {
            if (this.fromDateSelector != null && this.toDateSelector != null) {
                try {
                    final String[] split = StringUtils.split(value, Constants.EMBEDDED_LIST_SEPARATOR);
                    final org.joda.time.DateTime fromDate =
                            new org.joda.time.DateTime(Utils.toDateTime(split[0]), this.timeZone);
                    final org.joda.time.DateTime toDate =
                            new org.joda.time.DateTime(Utils.toDateTime(split[1]), this.timeZone);
                    this.fromDateSelector.setDate(
                            fromDate.getYear(),
                            fromDate.getMonthOfYear() - 1,
                            fromDate.getDayOfMonth());
                    if (this.fromTimeSelector != null) {
                        this.fromTimeSelector.setTime(
                                fromDate.getHourOfDay(),
                                fromDate.getMinuteOfHour(),
                                fromDate.getSecondOfMinute());
                    }

                    this.toDateSelector.setDate(
                            toDate.getYear(),
                            toDate.getMonthOfYear() - 1,
                            toDate.getDayOfMonth());
                    if (this.toTimeSelector != null) {
                        this.toTimeSelector.setTime(
                                toDate.getHourOfDay(),
                                toDate.getMinuteOfHour(),
                                toDate.getSecondOfMinute());
                    }

                } catch (final Exception e) {
                    log.error("Failed to set date range filter attribute: ", e);
                }
            }
        }
    }

}
