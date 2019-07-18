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
import java.util.HashMap;
import java.util.Map;

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

    private final Map<Long, UpdatableTableItem> tableMapping;

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

        this.tableMapping = new HashMap<>();
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
                    userIdentifier -> new UpdatableTableItem(this.table, data.getConnectionId()));
            tableItem.push(data);
        }
    }

    public void updateGUI() {
        boolean sort = false;
        for (final UpdatableTableItem uti : this.tableMapping.values()) {
            if (uti.tableItem == null) {
                createTableItem(uti);
                updateIndicatorValues(uti, false);
                updateConnectionStatusColor(uti);
                sort = true;
            } else {
                if (uti.previous_connectionData == null || !uti.connectionData.clientConnection.status
                        .equals(uti.previous_connectionData.clientConnection.status)) {
                    uti.tableItem.setText(0, uti.getConnectionIdentifer());
                    uti.tableItem.setText(2, uti.getStatusName());
                    updateConnectionStatusColor(uti);
                    sort = true;
                }
                if (uti.hasStatus(ConnectionStatus.ESTABLISHED)) {
                    sort = updateIndicatorValues(uti, true);
                }
            }
            uti.tableItem.getDisplay();
        }

        adaptTableWidth();
        if (sort) {
            sortTable();
        }
        this.table.getParent().layout(true, true);
    }

    private void createTableItem(final UpdatableTableItem uti) {
        uti.tableItem = new TableItem(this.table, SWT.NONE);
        uti.tableItem.setText(0, uti.getConnectionIdentifer());
        uti.tableItem.setText(1, uti.getConnectionAddress());
        uti.tableItem.setText(2, uti.getStatusName());
        uti.tableItem.setData("TABLE_ITEM_DATA", uti);
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

    private void sortTable() {
        System.out.println("************** sortTable");
//        final TableItem[] items = this.table.getItems();
//        Arrays.sort(items, TABLE_COMPARATOR);
//        for (final TableItem item : items) {
//            final UpdatableTableItem uti = (UpdatableTableItem) item.getData("TABLE_ITEM_DATA");
//            item.dispose();
//            createTableItem(uti);
//            updateIndicatorValues(uti, false);
//            updateConnectionStatusColor(uti);
//        }
    }

    private boolean updateIndicatorValues(final UpdatableTableItem uti, final boolean established) {
        boolean sort = false;
        for (final IndicatorValue iv : uti.connectionData.indicatorValues) {
            final IndicatorData indicatorData = this.indicatorMapping.get(iv.getType());
            if (indicatorData != null) {
                if (!established && iv.getType() == IndicatorType.LAST_PING) {
                    uti.tableItem.setText(indicatorData.index, "--");
                } else {
                    uti.tableItem.setText(indicatorData.index, String.valueOf(iv.getValue()));
                    final Color newColor = this.getColorForValue(indicatorData, iv.getValue());
                    final Color background = uti.tableItem.getBackground(indicatorData.index);
                    if (newColor != background) {
                        uti.tableItem.setBackground(indicatorData.index, newColor);
                        sort = true;
                    }
                }
            }
        }
        return sort;
    }

    private void updateConnectionStatusColor(final UpdatableTableItem uti) {
        switch (uti.connectionData.clientConnection.status) {
            case ESTABLISHED: {
                uti.tableItem.setBackground(2, this.color1);
                break;
            }
            case ABORTED: {
                uti.tableItem.setBackground(2, this.color3);
                break;
            }
            default: {
                uti.tableItem.setBackground(2, this.color2);
            }
        }
    }

    private Color getColorForValue(final IndicatorData indicatorData, final double value) {

        for (int i = 0; i < indicatorData.thresholdColor.length; i++) {
            if (value < indicatorData.thresholdColor[i].value) {
                return indicatorData.thresholdColor[i].color;
            }
        }

        return indicatorData.thresholdColor[indicatorData.thresholdColor.length - 1].color;
    }

    private static final class UpdatableTableItem {

        @SuppressWarnings("unused")
        final Long connectionId;
        TableItem tableItem;
        ClientConnectionData previous_connectionData;
        ClientConnectionData connectionData;

        private UpdatableTableItem(final Table parent, final Long connectionId) {
            this.tableItem = null;
            this.connectionId = connectionId;
        }

        public void update(final TableItem tableItem) {

        }

        public String getStatusName() {
            if (this.connectionData != null && this.connectionData.clientConnection.status != null) {
                return this.connectionData.clientConnection.status.name();
            }
            return ConnectionStatus.UNDEFINED.name();
        }

        public String getConnectionAddress() {
            if (this.connectionData != null && this.connectionData.clientConnection.clientAddress != null) {
                return this.connectionData.clientConnection.clientAddress;
            }
            return Constants.EMPTY_NOTE;
        }

        public String getConnectionIdentifer() {
            if (this.connectionData != null && this.connectionData.clientConnection.userSessionId != null) {
                return this.connectionData.clientConnection.userSessionId;
            }

            return "--";
        }

        public boolean hasStatus(final ConnectionStatus status) {
            if (this.connectionData != null && this.connectionData.clientConnection != null) {
                return status == this.connectionData.clientConnection.status;
            }

            return false;
        }

        public void push(final ClientConnectionData connectionData) {
            this.previous_connectionData = this.connectionData;
            this.connectionData = connectionData;
        }
    }

    private static final class IndicatorData {
        final int index;
        @SuppressWarnings("unused")
        final Indicator indicator;
        final ThresholdColor[] thresholdColor;

        protected IndicatorData(final Indicator indicator, final int index, final Display display) {
            this.indicator = indicator;
            this.index = index;
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
            final RGB rgb = new RGB(
                    Integer.parseInt(threshold.color.substring(0, 2), 16),
                    Integer.parseInt(threshold.color.substring(2, 4), 16),
                    Integer.parseInt(threshold.color.substring(4, 6), 16));
            this.color = new Color(display, rgb, 100);
        }
    }

    private static final Comparator<TableItem> TABLE_COMPARATOR = new Comparator<>() {

        @Override
        public int compare(final TableItem o1, final TableItem o2) {
            final String name1 = o1.getText(0);
            final String name2 = o2.getText(0);
            if (name1 != null) {
                return name1.compareTo(name2);
            } else if (name2 != null) {
                return name2.compareTo(name1);
            } else {
                return -1;
            }
        }

    };

}
