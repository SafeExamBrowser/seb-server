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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTimeZone;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class TableFilter<ROW extends Entity> {

    public static enum CriteriaType {
        TEXT,
        SINGLE_SELECTION,
        DATE
    }

    private final Composite composite;
    private final EntityTable<ROW> entityTable;
    private final List<FilterComponent> components = new ArrayList<>();

    TableFilter(final EntityTable<ROW> entityTable) {
        this.composite = new Composite(entityTable.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.composite.setLayoutData(gridData);
        final RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.spacing = 5;
        layout.wrap = false;
        this.composite.setLayout(layout);

// TODO just for debugging, remove when tested
//        this.composite.setBackground(new Color(entityTable.composite.getDisplay(), new RGB(0, 0, 200)));

        this.entityTable = entityTable;
        buildComponents();
    }

    public MultiValueMap<String, String> getFilterParameter() {
        return this.components
                .stream()
                .reduce(new LinkedMultiValueMap<String, String>(),
                        (map, comp) -> comp.putFilterParameter(map),
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        });
    }

    public void reset() {
        this.components
                .stream()
                .forEach(comp -> comp.reset());
    }

    private void buildComponents() {
        this.components.clear();
        this.components.addAll(this.entityTable.columns
                .stream()
                .map(column -> column.filterAttribute)
                .map(this::createFilterComponent)
                .map(comp -> comp.build(this.composite))
                .map(comp -> comp.reset())
                .collect(Collectors.toList()));

        final FilterComponent lastComp = this.components.get(this.components.size() - 1);
        if (lastComp instanceof TableFilter.NullFilter) {
            this.components.remove(lastComp);
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
            default:
                throw new IllegalArgumentException("Unsupported FilterAttributeType: " + attribute.type);
        }
    }

    boolean adaptColumnWidth(final int columnIndex, final int width) {
        if (columnIndex < this.components.size()) {
            final boolean adaptWidth = this.components.get(columnIndex).adaptWidth(width);
            if (adaptWidth) {
                this.composite.layout();
            }
            return adaptWidth;
        }

        return false;
    }

    private void addActions() {
        this.entityTable.widgetFactory.imageButton(
                ImageIcon.SEARCH,
                this.composite,
                new LocTextKey("sebserver.overall.action.filter"),
                event -> {
                    this.entityTable.applyFilter();
                });
        this.entityTable.widgetFactory.imageButton(
                ImageIcon.CANCEL,
                this.composite,
                new LocTextKey("sebserver.overall.action.filter.clear"),
                event -> {
                    reset();
                });
    }

    private static abstract class FilterComponent {

        static final int CELL_WIDTH_ADJUSTMENT = -30;

        protected final RowData rowData = new RowData();
        final TableFilterAttribute attribute;

        FilterComponent(final TableFilterAttribute attribute) {
            this.attribute = attribute;
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

        boolean adaptWidth(final int width) {
            final int _width = width + CELL_WIDTH_ADJUSTMENT;
            if (_width != this.rowData.width) {
                this.rowData.width = _width;
                return true;
            }

            return false;
        }
    }

    private class NullFilter extends FilterComponent {

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
        boolean adaptWidth(final int width) {
            return super.adaptWidth(width - CELL_WIDTH_ADJUSTMENT);
        }

        @Override
        FilterComponent reset() {
            return this;
        }

        @Override
        String getValue() {
            return null;
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
            this.textInput = TableFilter.this.entityTable.widgetFactory.textInput(parent, false);
            this.textInput.setLayoutData(this.rowData);
            return this;
        }

        @Override
        String getValue() {
            if (this.textInput != null) {
                return this.textInput.getText();
            }

            return null;
        }

    }

    private class SelectionFilter extends FilterComponent {

        protected Selection selector;

        SelectionFilter(final TableFilterAttribute attribute) {
            super(attribute);
        }

        @Override
        FilterComponent build(final Composite parent) {
            this.selector = TableFilter.this.entityTable.widgetFactory
                    .selectionLocalized(
                            ch.ethz.seb.sebserver.gui.widget.Selection.Type.SINGLE,
                            parent,
                            this.attribute.resourceSupplier);
            this.selector
                    .adaptToControl()
                    .setLayoutData(this.rowData);
            return this;
        }

        @Override
        FilterComponent reset() {
            if (this.selector != null) {
                this.selector.clear();
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
        boolean adaptWidth(final int width) {
            // NOTE: for some unknown reason RWT acts differently on width-property for text inputs and selectors
            //       this is to adjust selection filter criteria to the list column width
            return super.adaptWidth(width + 25);
        }
    }

    private class Date extends FilterComponent {

        private DateTime selector;

        Date(final TableFilterAttribute attribute) {
            super(attribute);
        }

        @Override
        FilterComponent build(final Composite parent) {
            this.selector = new DateTime(parent, SWT.DATE | SWT.BORDER);
            this.selector.setLayoutData(this.rowData);
            return this;
        }

        @Override
        FilterComponent reset() {
            final org.joda.time.DateTime now = org.joda.time.DateTime.now(DateTimeZone.UTC);
            this.selector.setDate(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth());
            return this;
        }

        @Override
        String getValue() {
            if (this.selector != null) {
                final org.joda.time.DateTime date = org.joda.time.DateTime.now(DateTimeZone.UTC)
                        .withYear(this.selector.getYear())
                        .withMonthOfYear(this.selector.getMonth())
                        .withDayOfMonth(this.selector.getDay());

                return date.toString(Constants.STANDARD_DATE_TIME_FORMATTER);
            } else {
                return null;
            }
        }

    }

}
