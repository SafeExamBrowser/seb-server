/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
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
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public final class ClientConnectionTable {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionTable.class);

    private static final int BOTTOM_PADDING = 20;

    private final static String STATUS_LOC_TEXT_KEY_PREFIX = "sebserver.monitoring.connection.status.";

    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id");
    private final static LocTextKey CONNECTION_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.address");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status");

    private static final int NUMBER_OF_NONE_INDICATOR_COLUMNS = 3;

    private final WidgetFactory widgetFactory;
    private final Exam exam;
    private final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder;
    private final EnumMap<IndicatorType, IndicatorData> indicatorMapping;
    private final Table table;
    private final StatusData statusData;

    private int tableWidth;
    private boolean needsSort = false;
    private LinkedHashMap<Long, UpdatableTableItem> tableMapping;
    private final Set<String> sessionIds;

    public ClientConnectionTable(
            final WidgetFactory widgetFactory,
            final Composite tableRoot,
            final Exam exam,
            final Collection<Indicator> indicators,
            final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder) {

        this.widgetFactory = widgetFactory;
        this.exam = exam;
        this.restCallBuilder = restCallBuilder;

        final Display display = tableRoot.getDisplay();
        this.statusData = new StatusData(display);

        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                NUMBER_OF_NONE_INDICATOR_COLUMNS);

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
            final String indicatorName = widgetFactory.getI18nSupport().getText(
                    "sebserver.monitoring.connection.list.column.indicator." + indDef.name,
                    indDef.name);
            tc.setText(indicatorName);
        }

        this.tableMapping = new LinkedHashMap<>();
        this.sessionIds = new HashSet<>();
        this.table.layout();
    }

    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    public Exam getExam() {
        return this.exam;
    }

    public void withDefaultAction(final PageAction pageAction, final PageService pageService) {
        this.table.addListener(SWT.MouseDoubleClick, event -> {
            final Tuple<String> selection = getSingleSelection();
            if (selection == null) {
                return;
            }

            final PageAction copyOfPageAction = PageAction.copyOf(pageAction);
            copyOfPageAction.withEntityKey(new EntityKey(
                    selection._1,
                    EntityType.CLIENT_CONNECTION));
            copyOfPageAction.withAttribute(
                    Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                    selection._2);
            pageService.executePageAction(copyOfPageAction);
        });

    }

    public Tuple<String> getSingleSelection() {
        final int[] selectionIndices = this.table.getSelectionIndices();
        if (selectionIndices == null || selectionIndices.length < 1) {
            return null;
        }

        final UpdatableTableItem updatableTableItem =
                new ArrayList<>(this.tableMapping.values()).get(selectionIndices[0]);
        return new Tuple<>(
                (updatableTableItem.connectionId != null)
                        ? String.valueOf(updatableTableItem.connectionId)
                        : null,
                updatableTableItem.connectionData.clientConnection.connectionToken);
    }

    public void updateValues() {
        this.restCallBuilder
                .call()
                .get(error -> {
                    log.error("Error poll connection data: ", error);
                    return Collections.emptyList();
                })
                .stream()
                .forEach(data -> {
                    final UpdatableTableItem tableItem = this.tableMapping.computeIfAbsent(
                            data.getConnectionId(),
                            connectionId -> new UpdatableTableItem(connectionId));
                    tableItem.push(data);
                });
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

        final Long connectionId;
        private boolean changed = false;
        private ClientConnectionData connectionData;
        private int[] thresholdColorIndices;
        private boolean duplicateChecked = false;
        private final boolean duplicateMarked = false;
        private boolean isDuplicate = false;

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
                updateDuplicateColor(tableItem);
            }
        }

        void updateData(final TableItem tableItem) {
            tableItem.setText(0, getConnectionIdentifer());
            tableItem.setText(1, getConnectionAddress());
            tableItem.setText(2, getStatusName());
        }

        void updateConnectionStatusColor(final TableItem tableItem) {
            tableItem.setBackground(
                    2,
                    ClientConnectionTable.this.statusData.getStatusColor(this.connectionData));
        }

        void updateDuplicateColor(final TableItem tableItem) {
            if (this.isDuplicate && this.duplicateChecked && !this.duplicateMarked) {
                tableItem.setBackground(0, ClientConnectionTable.this.statusData.color3);
            } else {
                tableItem.setBackground(0, null);
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
            return ClientConnectionTable.this.statusData.statusWeight(this.connectionData);
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
                final int colorIndex = IndicatorData.getColorIndex(indicatorData, value);
                if (this.thresholdColorIndices[i] != colorIndex) {
                    ClientConnectionTable.this.needsSort = true;
                }
                this.thresholdColorIndices[i] = colorIndex;
            }

            this.connectionData = connectionData;

            if (!this.duplicateChecked && StringUtils.isNotBlank(connectionData.clientConnection.userSessionId)) {
                if (ClientConnectionTable.this.sessionIds.contains(connectionData.clientConnection.userSessionId)) {
                    this.isDuplicate = true;
                } else {
                    ClientConnectionTable.this.sessionIds.add(connectionData.clientConnection.userSessionId);
                }
                this.duplicateChecked = true;
            }
        }

    }

}
