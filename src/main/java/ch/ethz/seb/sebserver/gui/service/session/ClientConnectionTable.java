/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionTable.class);

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

        this.table = widgetFactory.tableLocalized(tableRoot);
        this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.table.setLayout(new GridLayout());

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

        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);

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
        for (final UpdatableTableItem uti : this.tableMapping.values()) {
            if (uti.tableItem == null) {
                createTableItem(uti);
                updateIndicatorValues(uti);
                updateConnectionStatusColor(uti);
            } else {
                if (!uti.connectionData.clientConnection.status
                        .equals(uti.previous_connectionData.clientConnection.status)) {
                    uti.tableItem.setText(0, uti.getConnectionIdentifer());
                    uti.tableItem.setText(1, uti.getStatusName());
                    updateConnectionStatusColor(uti);
                }
                if (uti.hasStatus(ConnectionStatus.ESTABLISHED)) {
                    updateIndicatorValues(uti);
                }
            }
            uti.tableItem.getDisplay();
        }

        adaptTableWidth();
    }

    private void createTableItem(final UpdatableTableItem uti) {
        uti.tableItem = new TableItem(this.table, SWT.NONE);
        uti.tableItem.setText(0, uti.getConnectionIdentifer());
        uti.tableItem.setText(1, uti.getConnectionAddress());
        uti.tableItem.setText(2, uti.getStatusName());
    }

    private void adaptTableWidth() {
        final Rectangle area = this.table.getParent().getClientArea();
        if (this.tableWidth != area.width) {
            final int columnWidth = area.width / this.table.getColumnCount();
            for (final TableColumn column : this.table.getColumns()) {
                column.setWidth(columnWidth);
            }
            this.table.layout(true, true);
            //this.table.pack();
            this.tableWidth = area.width;
        }
    }

    private void updateIndicatorValues(final UpdatableTableItem uti) {

        for (final IndicatorValue iv : uti.connectionData.indicatorValues) {
            final IndicatorData indicatorData = this.indicatorMapping.get(iv.getType());
            if (indicatorData != null) {
                uti.tableItem.setText(indicatorData.index, String.valueOf(iv.getValue()));
                uti.tableItem.setBackground(
                        indicatorData.index,
                        this.getColorForValue(indicatorData, iv.getValue()));
            }
        }
    }

    private void updateConnectionStatusColor(final UpdatableTableItem uti) {
        switch (uti.connectionData.clientConnection.status) {
            case ESTABLISHED: {
                uti.tableItem.setBackground(1, this.color1);
                break;
            }
            case ABORTED: {
                uti.tableItem.setBackground(1, this.color3);
                break;
            }
            default: {
                uti.tableItem.setBackground(1, this.color2);
            }
        }
    }

    private Color getColorForValue(final IndicatorData indicatorData, final double value) {

        for (int i = 0; i < indicatorData.thresholdColor.length; i++) {
            if (value >= indicatorData.thresholdColor[i].value) {
                return indicatorData.thresholdColor[i].color;
            }
        }

        return this.color1;
    }

    private static final class UpdatableTableItem {

        final Long connectionId;
        TableItem tableItem;
        ClientConnectionData previous_connectionData;
        ClientConnectionData connectionData;

        private UpdatableTableItem(final Table parent, final Long connectionId) {
            this.tableItem = null;
            this.connectionId = connectionId;
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

            return "- " + this.connectionId + " -";
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

}
