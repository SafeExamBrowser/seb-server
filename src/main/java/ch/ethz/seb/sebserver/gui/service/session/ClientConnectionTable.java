/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.session.IndicatorData.ThresholdColor;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public final class ClientConnectionTable {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionTable.class);

    private static final int BOTTOM_PADDING = 20;
    private static final int NUMBER_OF_NONE_INDICATOR_COLUMNS = 3;
    private static final String USER_SESSION_STATUS_FILTER_ATTRIBUTE = "USER_SESSION_STATUS_FILTER_ATTRIBUTE";

    private static final String INDICATOR_NAME_TEXT_KEY_PREFIX =
            "sebserver.exam.indicator.type.description.";
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id");
    private final static LocTextKey CONNECTION_ID_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONNECTION_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.address");
    private final static LocTextKey CONNECTION_ADDRESS_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.address" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status");
    private final static LocTextKey CONNECTION_STATUS_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);

    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final Exam exam;
    private final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder;
    private final EnumMap<IndicatorType, IndicatorData> indicatorMapping;
    private final Table table;
    private final ColorData colorData;
    private final EnumSet<ConnectionStatus> statusFilter;
    private String statusFilterParam = "";
    private boolean statusFilterChanged = false;
    private Consumer<Set<EntityKey>> selectionListener;

    private int tableWidth;
    private boolean needsSort = false;
    private LinkedHashMap<Long, UpdatableTableItem> tableMapping;
    private final Set<Long> toDelete = new HashSet<>();
    private final MultiValueMap<String, Long> sessionIds;

    private final Color darkFontColor;
    private final Color lightFontColor;

    private boolean forceUpdateAll = false;

    public ClientConnectionTable(
            final PageService pageService,
            final Composite tableRoot,
            final Exam exam,
            final Collection<Indicator> indicators,
            final RestCall<Collection<ClientConnectionData>>.RestCallBuilder restCallBuilder) {

        this.widgetFactory = pageService.getWidgetFactory();
        this.resourceService = pageService.getResourceService();
        this.exam = exam;
        this.restCallBuilder = restCallBuilder;

        final Display display = tableRoot.getDisplay();
        this.colorData = new ColorData(display);

        this.darkFontColor = new Color(display, Constants.BLACK_RGB);
        this.lightFontColor = new Color(display, Constants.WHITE_RGB);

        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                this.colorData,
                NUMBER_OF_NONE_INDICATOR_COLUMNS);

        this.statusFilter = EnumSet.noneOf(ConnectionStatus.class);
        loadStatusFilter();

        this.table = this.widgetFactory.tableLocalized(tableRoot, SWT.MULTI | SWT.V_SCROLL);
        final GridLayout gridLayout = new GridLayout(3 + indicators.size(), true);
        gridLayout.horizontalSpacing = 100;
        gridLayout.marginWidth = 100;
        gridLayout.marginRight = 100;
        this.table.setLayout(gridLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.table.setLayoutData(gridData);
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);

        this.table.addListener(SWT.Selection, event -> this.notifySelectionChange());

        this.widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_ID_TEXT_KEY,
                CONNECTION_ID_TOOLTIP_TEXT_KEY);
        this.widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_ADDRESS_TEXT_KEY,
                CONNECTION_ADDRESS_TOOLTIP_TEXT_KEY);
        this.widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_STATUS_TEXT_KEY,
                CONNECTION_STATUS_TOOLTIP_TEXT_KEY);
        for (final Indicator indDef : indicators) {
            final TableColumn tableColumn = this.widgetFactory.tableColumnLocalized(
                    this.table,
                    new LocTextKey(INDICATOR_NAME_TEXT_KEY_PREFIX + indDef.name),
                    new LocTextKey(INDICATOR_NAME_TEXT_KEY_PREFIX + indDef.type.name));
            tableColumn.setText(indDef.name);
        }

        this.tableMapping = new LinkedHashMap<>();
        this.sessionIds = new LinkedMultiValueMap<>();
        this.table.layout();
    }

    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    public boolean isEmpty() {
        return this.tableMapping.isEmpty();
    }

    public Exam getExam() {
        return this.exam;
    }

    public boolean isStatusHidden(final ConnectionStatus status) {
        return this.statusFilter.contains(status);
    }

    public void hideStatus(final ConnectionStatus status) {
        this.statusFilter.add(status);
        saveStatusFilter();
    }

    public void showStatus(final ConnectionStatus status) {
        this.statusFilter.remove(status);
        saveStatusFilter();
    }

    public ClientConnectionTable withDefaultAction(final PageAction pageAction, final PageService pageService) {
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
        return this;
    }

    public Set<String> getConnectionTokens(
            final Predicate<ClientConnection> filter,
            final boolean selected) {

        if (selected) {
            final int[] selectionIndices = this.table.getSelectionIndices();
            if (selectionIndices == null || selectionIndices.length < 1) {
                return Collections.emptySet();
            }

            final Set<String> result = new HashSet<>();
            for (int i = 0; i < selectionIndices.length; i++) {
                final UpdatableTableItem updatableTableItem =
                        new ArrayList<>(this.tableMapping.values())
                                .get(selectionIndices[i]);
                if (filter.test(updatableTableItem.connectionData.clientConnection)) {
                    result.add(updatableTableItem.connectionData.clientConnection.connectionToken);
                }
            }
            return result;
        } else {
            return this.tableMapping
                    .values()
                    .stream()
                    .map(item -> item.connectionData.clientConnection)
                    .filter(filter)
                    .map(ClientConnection::getConnectionToken)
                    .collect(Collectors.toSet());
        }
    }

    public void removeSelection() {
        if (this.table != null) {
            this.table.deselectAll();
            this.notifySelectionChange();
        }
    }

    public ClientConnectionTable withSelectionListener(final Consumer<Set<EntityKey>> selectionListener) {
        this.selectionListener = selectionListener;
        return this;
    }

    public Set<EntityKey> getSelection() {
        final int[] selectionIndices = this.table.getSelectionIndices();
        if (selectionIndices == null || selectionIndices.length < 1) {
            return Collections.emptySet();
        }

        final Set<EntityKey> result = new HashSet<>();
        for (int i = 0; i < selectionIndices.length; i++) {
            final UpdatableTableItem updatableTableItem =
                    new ArrayList<>(this.tableMapping.values()).get(selectionIndices[0]);
            result.add(new EntityKey(updatableTableItem.connectionId, EntityType.CLIENT_CONNECTION));
        }
        return result;
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

    public void forceUpdateAll() {
        this.forceUpdateAll = true;
    }

    public void updateValues() {
        if (this.statusFilterChanged || this.forceUpdateAll) {
            this.toDelete.clear();
            this.toDelete.addAll(this.tableMapping.keySet());
        }
        this.restCallBuilder
                .withHeader(API.EXAM_MONITORING_STATE_FILTER, this.statusFilterParam)
                .call()
                .getOrThrow()
                .forEach(data -> {
                    final UpdatableTableItem tableItem = this.tableMapping.computeIfAbsent(
                            data.getConnectionId(),
                            UpdatableTableItem::new);
                    tableItem.push(data);
                    if (this.statusFilterChanged || this.forceUpdateAll) {
                        this.toDelete.remove(data.getConnectionId());
                    }
                });

        if (!this.toDelete.isEmpty()) {
            this.toDelete.forEach(id -> {
                final UpdatableTableItem item = this.tableMapping.remove(id);
                if (item != null) {
                    final List<Long> list = this.sessionIds.get(item.connectionData.clientConnection.userSessionId);
                    if (list != null) {
                        list.remove(id);
                    }
                }
            });
            this.statusFilterChanged = false;
            this.toDelete.clear();
        }

        this.forceUpdateAll = false;
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
                .sorted(Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Entry::getKey,
                        Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private void saveStatusFilter() {
        try {
            this.resourceService
                    .getCurrentUser()
                    .putAttribute(
                            USER_SESSION_STATUS_FILTER_ATTRIBUTE,
                            StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR));
        } catch (final Exception e) {
            log.warn("Failed to save status filter to user session");
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            this.statusFilterChanged = true;
        }
    }

    private void loadStatusFilter() {
        try {
            final String attribute = this.resourceService
                    .getCurrentUser()
                    .getAttribute(USER_SESSION_STATUS_FILTER_ATTRIBUTE);
            this.statusFilter.clear();
            if (attribute != null) {
                Arrays.asList(StringUtils.split(attribute, Constants.LIST_SEPARATOR))
                        .forEach(name -> this.statusFilter.add(ConnectionStatus.valueOf(name)));

            } else {
                this.statusFilter.add(ConnectionStatus.DISABLED);
            }
        } catch (final Exception e) {
            log.warn("Failed to load status filter to user session");
            this.statusFilter.clear();
            this.statusFilter.add(ConnectionStatus.DISABLED);
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            this.statusFilterChanged = true;
        }
    }

    private void notifySelectionChange() {
        if (this.selectionListener == null) {
            return;
        }

        this.selectionListener.accept(this.getSelection());
    }

    private final class UpdatableTableItem implements Comparable<UpdatableTableItem> {

        final Long connectionId;
        private boolean changed = false;
        private ClientConnectionData connectionData;
        private int thresholdsWeight;
        private int[] indicatorWeights = null;
        private boolean duplicateChecked = false;

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
            if (ClientConnectionTable.this.statusFilter.contains(this.connectionData.clientConnection.status)) {
                tableItem.dispose();
                return;
            }
            updateData(tableItem);
            if (this.connectionData != null) {
                updateConnectionStatusColor(tableItem);
                updateIndicatorValues(tableItem);
                updateDuplicateColor(tableItem);
            }
        }

        void updateData(final TableItem tableItem) {
            tableItem.setText(0, getConnectionIdentifier());
            tableItem.setText(1, getConnectionAddress());
            tableItem.setText(2, getStatusName());
        }

        void updateConnectionStatusColor(final TableItem tableItem) {
            final Color statusColor = ClientConnectionTable.this.colorData.getStatusColor(this.connectionData);
            final Color statusTextColor = ClientConnectionTable.this.colorData.getStatusTextColor(statusColor);
            tableItem.setBackground(2, statusColor);
            tableItem.setForeground(2, statusTextColor);
        }

        void updateDuplicateColor(final TableItem tableItem) {

            tableItem.setBackground(0, null);
            tableItem.setForeground(0, ClientConnectionTable.this.darkFontColor);

            if (!this.duplicateChecked) {
                return;
            }

            if (this.connectionData != null
                    && StringUtils.isNotBlank(this.connectionData.clientConnection.userSessionId)) {
                final List<Long> list =
                        ClientConnectionTable.this.sessionIds.get(this.connectionData.clientConnection.userSessionId);
                if (list != null && list.size() > 1) {
                    tableItem.setBackground(0, ClientConnectionTable.this.colorData.color3);
                    tableItem.setForeground(0, ClientConnectionTable.this.lightFontColor);
                } else {
                    tableItem.setBackground(0, null);
                    tableItem.setForeground(0, ClientConnectionTable.this.darkFontColor);
                }
            }
        }

        void updateIndicatorValues(final TableItem tableItem) {
            if (this.connectionData == null || this.indicatorWeights == null) {
                return;
            }

            for (int i = 0; i < this.connectionData.indicatorValues.size(); i++) {
                final IndicatorValue indicatorValue = this.connectionData.indicatorValues.get(i);
                final IndicatorData indicatorData =
                        ClientConnectionTable.this.indicatorMapping.get(indicatorValue.getType());
                if (indicatorData == null) {
                    continue;
                }

                if (!this.connectionData.clientConnection.status.establishedStatus) {
                    final String value = (indicatorData.indicator.type.showOnlyInActiveState)
                            ? Constants.EMPTY_NOTE
                            : IndicatorValue.getDisplayValue(indicatorValue);
                    tableItem.setText(indicatorData.tableIndex, value);
                    tableItem.setBackground(indicatorData.tableIndex, indicatorData.defaultColor);
                    tableItem.setForeground(indicatorData.tableIndex, indicatorData.defaultTextColor);
                } else {
                    tableItem.setText(indicatorData.tableIndex, IndicatorValue.getDisplayValue(indicatorValue));
                    final int weight = this.indicatorWeights[indicatorData.index];
                    if (weight >= 0 && weight < indicatorData.thresholdColor.length) {
                        final ThresholdColor thresholdColor = indicatorData.thresholdColor[weight];
                        tableItem.setBackground(indicatorData.tableIndex, thresholdColor.color);
                        tableItem.setForeground(indicatorData.tableIndex, thresholdColor.textColor);
                    } else {
                        tableItem.setBackground(indicatorData.tableIndex, indicatorData.defaultColor);
                        tableItem.setForeground(indicatorData.tableIndex, indicatorData.defaultTextColor);
                    }
                }
            }
        }

        @Override
        public int compareTo(final UpdatableTableItem other) {
            return Comparator.comparingInt(UpdatableTableItem::statusWeight)
                    .thenComparingInt(UpdatableTableItem::thresholdsWeight)
                    .thenComparing(UpdatableTableItem::getConnectionIdentifier)
                    .compare(this, other);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((this.connectionId == null) ? 0 : this.connectionId.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final UpdatableTableItem other = (UpdatableTableItem) obj;
            if (getOuterType() != other.getOuterType())
                return false;
            return compareTo(other) == 0;
        }

        int statusWeight() {
            return ClientConnectionTable.this.colorData.statusWeight(this.connectionData);
        }

        int thresholdsWeight() {
            return -this.thresholdsWeight;
        }

        String getStatusName() {
            return ClientConnectionTable.this.resourceService
                    .localizedClientConnectionStatusName(this.connectionData);
        }

        String getConnectionAddress() {
            if (this.connectionData != null && this.connectionData.clientConnection.clientAddress != null) {
                return this.connectionData.clientConnection.clientAddress;
            }
            return Constants.EMPTY_NOTE;
        }

        String getConnectionIdentifier() {
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

            if (this.indicatorWeights == null) {
                this.indicatorWeights = new int[ClientConnectionTable.this.indicatorMapping.size()];
            }

            for (int i = 0; i < connectionData.indicatorValues.size(); i++) {
                final IndicatorValue indicatorValue = connectionData.indicatorValues.get(i);
                final IndicatorData indicatorData =
                        ClientConnectionTable.this.indicatorMapping.get(indicatorValue.getType());

                if (indicatorData != null) {
                    final double value = indicatorValue.getValue();
                    final int indicatorWeight = IndicatorData.getWeight(indicatorData, value);
                    if (this.indicatorWeights[indicatorData.index] != indicatorWeight) {
                        ClientConnectionTable.this.needsSort = true;
                        this.thresholdsWeight -= this.indicatorWeights[indicatorData.index];
                        this.indicatorWeights[indicatorData.index] = indicatorWeight;
                        this.thresholdsWeight += this.indicatorWeights[indicatorData.index];
                    }
                }
            }

            this.connectionData = connectionData;

            if (!this.duplicateChecked &&
                    this.connectionData.clientConnection.status != ConnectionStatus.DISABLED &&
                    StringUtils.isNotBlank(connectionData.clientConnection.userSessionId)) {

                ClientConnectionTable.this.sessionIds.add(
                        connectionData.clientConnection.userSessionId,
                        this.connectionId);
                this.duplicateChecked = true;
            }
        }

        private ClientConnectionTable getOuterType() {
            return ClientConnectionTable.this;
        }

    }

}
