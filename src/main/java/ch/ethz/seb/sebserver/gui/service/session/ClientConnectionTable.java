/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public final class ClientConnectionTable {

    private static final int BOTTOM_PADDING = 20;

    private final static String STATUS_LOC_TEXT_KEY_PREFIX = "sebserver.monitoring.connection.status.";

    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id");
    private final static LocTextKey CONNECTION_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.address");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status");

    private final WidgetFactory widgetFactory;
    private final Exam exam;
    private final EnumMap<IndicatorType, IndicatorData> indicatorMapping;
    private final Table table;

    private final Color color1;
    private final Color color2;
    private final Color color3;

    private int tableWidth;
    private boolean needsSort = false;
    private LinkedHashMap<Long, UpdatableTableItem> tableMapping;

    public ClientConnectionTable(
            final WidgetFactory widgetFactory,
            final Composite tableRoot,
            final Exam exam,
            final Collection<Indicator> indicators) {

        this.widgetFactory = widgetFactory;
        this.exam = exam;

        final Display display = tableRoot.getDisplay();

        this.indicatorMapping = new EnumMap<>(IndicatorType.class);
        int i = 3;
        for (final Indicator indicator : indicators) {
            this.indicatorMapping.put(indicator.type, new IndicatorData(indicator, i, display));
            i++;
        }

        this.table = widgetFactory.tableLocalized(tableRoot, SWT.SINGLE | SWT.V_SCROLL);
        final GridLayout gridLayout = new GridLayout(3 + indicators.size(), true);
        this.table.setLayout(gridLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        //gridData.heightHint = 200;
        this.table.setLayoutData(gridData);
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);

        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_ID_TEXT_KEY);
        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_ADDRESS_TEXT_KEY);
        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_STATUS_TEXT_KEY);
        for (final Indicator indDef : indicators) {
            final TableColumn tc = new TableColumn(this.table, SWT.NONE);
            tc.setText(indDef.name);
        }

        this.color1 = new Color(display, new RGB(0, 255, 0), 100);
        this.color2 = new Color(display, new RGB(249, 166, 2), 100);
        this.color3 = new Color(display, new RGB(255, 0, 0), 100);

        this.tableMapping = new LinkedHashMap<>();
        this.table.layout();
    }

    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    public Exam getExam() {
        return this.exam;
    }

    public void updateValues(final Collection<ClientConnectionData> connectionInfo) {
        for (final ClientConnectionData data : connectionInfo) {
            final UpdatableTableItem tableItem = this.tableMapping.computeIfAbsent(
                    data.getConnectionId(),
                    userIdentifier -> new UpdatableTableItem(data.getConnectionId()));
            tableItem.push(data);
        }
    }

    public void updateGUI() {
        fillTable();
        if (this.needsSort) {
            sortTable();
        }

        final TableItem[] items = this.table.getItems();
        final Iterator<Entry<Long, UpdatableTableItem>> iterator = this.tableMapping.entrySet().iterator();
        for (int i = 0; i < items.length; i++) {
            final UpdatableTableItem uti = iterator.next().getValue();
            uti.update(items[i], this.needsSort);
        }

        this.needsSort = false;
        adaptTableWidth();
        this.table.layout(true, true);
    }

    private void adaptTableWidth() {
        final Rectangle area = this.table.getParent().getClientArea();
        if (this.tableWidth != area.width) {
            final int columnWidth = area.width / this.table.getColumnCount() - 5;
            for (final TableColumn column : this.table.getColumns()) {
                column.setWidth(columnWidth);
            }
            this.tableWidth = area.width;
        }

        // update table height
        final GridData gridData = (GridData) this.table.getLayoutData();
        gridData.heightHint = area.height - BOTTOM_PADDING;
    }

    private void fillTable() {
        if (this.tableMapping.size() > this.table.getItemCount()) {
            while (this.tableMapping.size() > this.table.getItemCount()) {
                new TableItem(this.table, SWT.NONE);
            }
        } else if (this.tableMapping.size() < this.table.getItemCount()) {
            while (this.tableMapping.size() < this.table.getItemCount()) {
                this.table.getItem(0).dispose();
            }
        }
    }

    private void sortTable() {
        this.tableMapping = this.tableMapping.entrySet()
                .stream()
                .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .collect(Collectors.toMap(
                        Entry::getKey,
                        Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private final class UpdatableTableItem implements Comparable<UpdatableTableItem> {

        @SuppressWarnings("unused")
        final Long connectionId;
        private boolean changed = false;
        private ClientConnectionData connectionData;
        private int[] thresholdColorIndices;

        UpdatableTableItem(final Long connectionId) {
            this.connectionId = connectionId;
        }

        void update(final TableItem tableItem, final boolean force) {
            if (force || this.changed) {
                update(tableItem);
            }
            this.changed = false;
        }

        void update(final TableItem tableItem) {
            updateData(tableItem);
            if (this.connectionData != null) {
                updateConnectionStatusColor(tableItem);
                updateIndicatorValues(tableItem);
            }
        }

        void updateData(final TableItem tableItem) {
            tableItem.setText(0, getConnectionIdentifer());
            tableItem.setText(1, getConnectionAddress());
            tableItem.setText(2, getStatusName());
        }

        void updateConnectionStatusColor(final TableItem tableItem) {
            switch (this.connectionData.clientConnection.status) {
                case ESTABLISHED: {
                    tableItem.setBackground(2, ClientConnectionTable.this.color1);
                    break;
                }
                case ABORTED: {
                    tableItem.setBackground(2, ClientConnectionTable.this.color3);
                    break;
                }
                default: {
                    tableItem.setBackground(2, ClientConnectionTable.this.color2);
                }
            }
        }

        void updateIndicatorValues(final TableItem tableItem) {
            if (this.connectionData == null) {
                return;
            }

            final boolean fillEmpty = this.connectionData.clientConnection.status != ConnectionStatus.ESTABLISHED;

            for (int i = 0; i < this.connectionData.indicatorValues.size(); i++) {
                final IndicatorValue indicatorValue = this.connectionData.indicatorValues.get(i);
                final IndicatorData indicatorData =
                        ClientConnectionTable.this.indicatorMapping.get(indicatorValue.getType());

                if (fillEmpty) {
                    tableItem.setText(indicatorData.index, Constants.EMPTY_NOTE);
                    tableItem.setBackground(
                            indicatorData.index,
                            indicatorData.defaultColor);
                } else {
                    tableItem.setText(indicatorData.index, String.valueOf(indicatorValue.getValue()));
                    tableItem.setBackground(
                            indicatorData.index,
                            indicatorData.thresholdColor[this.thresholdColorIndices[i]].color);
                }
            }
        }

        @Override
        public int compareTo(final UpdatableTableItem other) {
            return Comparator.comparingInt(UpdatableTableItem::statusWeight)
                    .thenComparingInt(UpdatableTableItem::thresholdsWeight)
                    .thenComparing(UpdatableTableItem::getConnectionIdentifer)
                    .compare(this, other);
        }

        int statusWeight() {
            if (this.connectionData == null) {
                return 100;
            }

            switch (this.connectionData.clientConnection.status) {
                case ABORTED:
                    return 0;
                case CONNECTION_REQUESTED:
                case AUTHENTICATED:
                    return 1;
                case ESTABLISHED:
                    return 2;
                case CLOSED:
                    return 3;
                default:
                    return 10;
            }
        }

        int thresholdsWeight() {
            if (this.thresholdColorIndices == null) {
                return 100;
            }

            int weight = 0;
            for (int i = 0; i < this.thresholdColorIndices.length; i++) {
                weight += this.thresholdColorIndices[i];
            }
            return 100 - weight;
        }

        String getStatusName() {

            String name;
            if (this.connectionData != null && this.connectionData.clientConnection.status != null) {
                name = this.connectionData.clientConnection.status.name();
            } else {
                name = ConnectionStatus.UNDEFINED.name();
            }
            return ClientConnectionTable.this.widgetFactory.getI18nSupport()
                    .getText(STATUS_LOC_TEXT_KEY_PREFIX + name, name);
        }

        String getConnectionAddress() {
            if (this.connectionData != null && this.connectionData.clientConnection.clientAddress != null) {
                return this.connectionData.clientConnection.clientAddress;
            }
            return Constants.EMPTY_NOTE;
        }

        String getConnectionIdentifer() {
            if (this.connectionData != null && this.connectionData.clientConnection.userSessionId != null) {
                return this.connectionData.clientConnection.userSessionId;
            }

            return "--";
        }

        void push(final ClientConnectionData connectionData) {
            this.changed = this.connectionData == null ||
                    !this.connectionData.dataEquals(connectionData);

            final boolean statusChanged = this.connectionData == null ||
                    this.connectionData.clientConnection.status != connectionData.clientConnection.status;

            if (statusChanged) {
                ClientConnectionTable.this.needsSort = true;
            }

            if (this.thresholdColorIndices == null) {
                this.thresholdColorIndices = new int[connectionData.indicatorValues.size()];
            }

            for (int i = 0; i < connectionData.indicatorValues.size(); i++) {
                final IndicatorValue indicatorValue = connectionData.indicatorValues.get(i);
                final IndicatorData indicatorData =
                        ClientConnectionTable.this.indicatorMapping.get(indicatorValue.getType());

                final double value = indicatorValue.getValue();
                final int colorIndex = getColorIndex(indicatorData, value);
                if (this.thresholdColorIndices[i] != colorIndex) {
                    ClientConnectionTable.this.needsSort = true;
                }
                this.thresholdColorIndices[i] = colorIndex;
            }

            this.connectionData = connectionData;
        }

    }

    private static final int getColorIndex(final IndicatorData indicatorData, final double value) {
        for (int j = 0; j < indicatorData.thresholdColor.length; j++) {
            if (value < indicatorData.thresholdColor[j].value) {
                return j;
            }
        }

        return indicatorData.thresholdColor.length - 1;
    }

    private static final class IndicatorData {
        final int index;
        @SuppressWarnings("unused")
        final Indicator indicator;
        final Color defaultColor;
        final ThresholdColor[] thresholdColor;

        protected IndicatorData(final Indicator indicator, final int index, final Display display) {
            this.indicator = indicator;
            this.index = index;

            if (StringUtils.isNotBlank(indicator.defaultColor)) {
                final RGB rgb = new RGB(
                        Integer.parseInt(indicator.defaultColor.substring(0, 2), 16),
                        Integer.parseInt(indicator.defaultColor.substring(2, 4), 16),
                        Integer.parseInt(indicator.defaultColor.substring(4, 6), 16));
                this.defaultColor = new Color(display, rgb, 100);
            } else {
                this.defaultColor = new Color(display, new RGB(255, 255, 255), 100);
            }

            this.thresholdColor = new ThresholdColor[indicator.thresholds.size()];
            for (int i = 0; i < indicator.thresholds.size(); i++) {
                this.thresholdColor[i] = new ThresholdColor(indicator.thresholds.get(i), display);
            }
        }
    }

    private static final class ThresholdColor {
        final double value;
        final Color color;

        protected ThresholdColor(final Threshold threshold, final Display display) {
            this.value = threshold.value;
            if (StringUtils.isNotBlank(threshold.color)) {
                final RGB rgb = new RGB(
                        Integer.parseInt(threshold.color.substring(0, 2), 16),
                        Integer.parseInt(threshold.color.substring(2, 4), 16),
                        Integer.parseInt(threshold.color.substring(4, 6), 16));
                this.color = new Color(display, rgb, 100);
            } else {
                this.color = new Color(display, new RGB(255, 255, 255), 100);
            }
        }
    }
}
