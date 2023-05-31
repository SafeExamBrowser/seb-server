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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringDataView;
import ch.ethz.seb.sebserver.gbl.model.session.ClientStaticData;
import ch.ethz.seb.sebserver.gbl.monitoring.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringStaticClientData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetMonitoringStaticClientData;
import ch.ethz.seb.sebserver.gui.service.session.IndicatorData.ThresholdColor;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public final class ClientConnectionTable implements FullPageMonitoringGUIUpdate {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionTable.class);

    private static final int BOTTOM_PADDING = 20;

    private static final String INDICATOR_NAME_TEXT_KEY_PREFIX =
            "sebserver.exam.indicator.type.description.";
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id");
    private final static LocTextKey CONNECTION_ID_TOOLTIP_TEXT_KEY =
            new LocTextKey(CONNECTION_ID_TEXT_KEY + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONNECTION_GROUP_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.group");
    private final static LocTextKey CONNECTION_GROUP_TOOLTIP_TEXT_KEY =
            new LocTextKey(CONNECTION_GROUP_TEXT_KEY + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONNECTION_INFO_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.info");
    private final static LocTextKey CONNECTION_INFO_TOOLTIP_TEXT_KEY =
            new LocTextKey(CONNECTION_INFO_TEXT_KEY + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status");
    private final static LocTextKey CONNECTION_STATUS_TOOLTIP_TEXT_KEY =
            new LocTextKey(CONNECTION_STATUS_TEXT_KEY + Constants.TOOLTIP_TEXT_KEY_SUFFIX);

    private final PageService pageService;
    private final Exam exam;
    private final boolean checkSecurityGrant;
    private final boolean checkSEBVersion;
    private final boolean distributedSetup;

    private final Map<Long, IndicatorData> indicatorMapping;
    private final Map<Long, ClientGroup> clientGroupMapping;
    private final Table table;
    private final ColorData colorData;
    private final List<UpdatableTableItem> sortList = new ArrayList<>();
    private final Function<MonitoringEntry, String> localizedClientConnectionStatusNameFunction;
    private Consumer<ClientConnectionTable> selectionListener;

    private int tableWidth;
    private boolean needsSort = false;
    private final LinkedHashMap<Long, UpdatableTableItem> tableMapping;
    private final Set<Long> toDelete = new HashSet<>();
    private final Set<Long> toUpdateStatic = new HashSet<>();
    private final Set<Long> duplicates = new HashSet<>();

    private final Color darkFontColor;
    private final Color lightFontColor;

    private final boolean hasClientGroups;
    private final int numberOfNoneIndicatorColumns;
    private final int[] tableProportions;

    private boolean forceUpdateAll = false;

    public ClientConnectionTable(
            final PageService pageService,
            final Composite tableRoot,
            final Exam exam,
            final Collection<Indicator> indicators,
            final Collection<ClientGroup> clientGroups,
            final boolean distributedSetup) {

        this.pageService = pageService;
        this.exam = exam;
        this.checkSecurityGrant = BooleanUtils.toBoolean(
                exam.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        this.checkSEBVersion = exam.additionalAttributes.containsKey(Exam.ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS);
        this.distributedSetup = distributedSetup;

        final WidgetFactory widgetFactory = pageService.getWidgetFactory();
        final ResourceService resourceService = pageService.getResourceService();

        final Display display = tableRoot.getDisplay();
        this.colorData = new ColorData(display);

        this.darkFontColor = new Color(display, Constants.BLACK_RGB);
        this.lightFontColor = new Color(display, Constants.WHITE_RGB);

        this.hasClientGroups = clientGroups != null && !clientGroups.isEmpty();
        this.numberOfNoneIndicatorColumns = this.hasClientGroups ? 4 : 3;
        this.tableProportions = this.hasClientGroups
                ? new int[] { 3, 2, 3, 2, 1 }
                : new int[] { 3, 3, 2, 1 };

        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                this.colorData,
                this.numberOfNoneIndicatorColumns);

        this.clientGroupMapping = clientGroups == null || clientGroups.isEmpty()
                ? null
                : clientGroups
                        .stream()
                        .collect(Collectors.toMap(cg -> cg.id, Function.identity()));

        this.localizedClientConnectionStatusNameFunction =
                resourceService.localizedClientMonitoringStatusNameFunction();

        this.table = widgetFactory.tableLocalized(tableRoot, SWT.MULTI | SWT.V_SCROLL);
        final GridLayout gridLayout = new GridLayout(3 + indicators.size(), false);
        gridLayout.horizontalSpacing = 100;
        gridLayout.marginWidth = 100;
        gridLayout.marginRight = 100;
        this.table.setLayout(gridLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.table.setLayoutData(gridData);
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);
        this.table.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        this.table.addListener(SWT.Selection, event -> this.notifySelectionChange());
        this.table.addListener(SWT.MouseUp, this::notifyTableInfoClick);

        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_ID_TEXT_KEY,
                CONNECTION_ID_TOOLTIP_TEXT_KEY);
        if (this.clientGroupMapping != null && !this.clientGroupMapping.isEmpty()) {
            widgetFactory.tableColumnLocalized(
                    this.table,
                    CONNECTION_GROUP_TEXT_KEY,
                    CONNECTION_GROUP_TOOLTIP_TEXT_KEY);
        }
        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_INFO_TEXT_KEY,
                CONNECTION_INFO_TOOLTIP_TEXT_KEY);
        widgetFactory.tableColumnLocalized(
                this.table,
                CONNECTION_STATUS_TEXT_KEY,
                CONNECTION_STATUS_TOOLTIP_TEXT_KEY);
        for (final Indicator indDef : indicators) {
            final TableColumn tableColumn = widgetFactory.tableColumnLocalized(
                    this.table,
                    new LocTextKey(INDICATOR_NAME_TEXT_KEY_PREFIX + indDef.name),
                    new LocTextKey(INDICATOR_NAME_TEXT_KEY_PREFIX + indDef.type.name));
            tableColumn.setText(indDef.name);
        }

        this.tableMapping = new LinkedHashMap<>();
        this.table.layout();
    }

    public WidgetFactory getWidgetFactory() {
        return this.pageService.getWidgetFactory();
    }

    public boolean isEmpty() {
        return this.tableMapping.isEmpty();
    }

    public Exam getExam() {
        return this.exam;
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
            final Predicate<ClientMonitoringDataView> filter,
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
                if (filter.test(updatableTableItem.monitoringData)
                        && updatableTableItem.staticData != ClientStaticData.NULL_DATA) {
                    result.add(updatableTableItem.staticData.connectionToken);
                }
            }
            return result;
        } else {
            return this.tableMapping
                    .values()
                    .stream()
                    .filter(item -> filter.test(item.monitoringData) && item.staticData != ClientStaticData.NULL_DATA)
                    .map(item -> item.staticData.connectionToken)
                    .collect(Collectors.toSet());
        }
    }

    public void removeSelection() {
        if (this.table != null) {
            this.table.deselectAll();
            this.notifySelectionChange();
        }
    }

    public ClientConnectionTable withSelectionListener(final Consumer<ClientConnectionTable> selectionListener) {
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
                    new ArrayList<>(this.tableMapping.values())
                            .stream()
                            .findFirst()
                            .orElse(null);
            if (updatableTableItem != null) {
                result.add(new EntityKey(updatableTableItem.connectionId, EntityType.CLIENT_CONNECTION));
            }
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
                updatableTableItem.staticData.connectionToken);
    }

    public void forceUpdateAll() {
        this.forceUpdateAll = true;
    }

    @Override
    public void update(final MonitoringFilter monitoringStatus) {
        final Collection<ClientMonitoringData> monitoringData = monitoringStatus.getConnectionData();
        final boolean sizeChanged = monitoringData.size() != this.table.getItemCount();
        final boolean needsSync = monitoringStatus.filterChanged() ||
                this.forceUpdateAll ||
                sizeChanged ||
                (this.tableMapping != null &&
                        this.table != null &&
                        this.tableMapping.size() != this.table.getItemCount())
                ||
                this.distributedSetup;

        if (needsSync) {
            this.toDelete.clear();
            this.toDelete.addAll(this.tableMapping.keySet());
        }

        this.toUpdateStatic.clear();
        monitoringStatus.getConnectionData()
                .forEach(data -> {
                    final UpdatableTableItem tableItem = this.tableMapping.computeIfAbsent(
                            data.id,
                            UpdatableTableItem::new);
                    if (tableItem.push(data)) {
                        this.toUpdateStatic.add(data.id);
                    }
                    if (needsSync) {
                        this.toDelete.remove(data.id);
                    }
                });

        if (!this.toUpdateStatic.isEmpty() || this.forceUpdateAll) {
            fetchStaticClientConnectionData();
            this.needsSort = true;
        }

        if (!this.toDelete.isEmpty()) {
            this.toDelete.forEach(id -> this.tableMapping.remove(id));
            monitoringStatus.resetFilterChanged();
            this.toDelete.clear();
        }

        this.forceUpdateAll = false;
        this.needsSort = this.needsSort || sizeChanged;
        updateGUI();
    }

    public void updateGUI() {
        if (this.needsSort) {
            sortTable();
        }

        fillTable();
        final TableItem[] items = this.table.getItems();
        final Iterator<Entry<Long, UpdatableTableItem>> iterator = this.tableMapping.entrySet().iterator();
        for (int i = 0; i < items.length; i++) {
            final UpdatableTableItem uti = iterator.next().getValue();
            uti.update(items[i], this.needsSort);
        }

        this.needsSort = false;
        adaptTableWidth();
        this.table.getParent().layout(true, true);
    }

    private void adaptTableWidth() {
        final Rectangle area = this.table.getParent().getClientArea();
        if (this.tableWidth != area.width) {

            // proportions size
            final int pSize = this.tableProportions[0] +
                    this.tableProportions[1] +
                    this.tableProportions[2] +
                    (this.hasClientGroups ? this.tableProportions[3] : 0) +
                    (this.hasClientGroups ? this.tableProportions[4] : this.tableProportions[3])
                            * this.indicatorMapping.size();
            final int columnUnitSize = (pSize > 0)
                    ? area.width / pSize
                    : area.width / this.tableProportions.length - 1 + this.indicatorMapping.size();

            final TableColumn[] columns = this.table.getColumns();
            for (int i = 0; i < columns.length; i++) {
                final int proportionFactor = (i < this.tableProportions.length)
                        ? this.tableProportions[i]
                        : this.tableProportions[this.tableProportions.length - 1];
                columns[i].setWidth(proportionFactor * columnUnitSize);
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
        this.sortList.clear();
        this.sortList.addAll(this.tableMapping.values());
        Collections.sort(this.sortList);
        this.tableMapping.clear();
        final Iterator<UpdatableTableItem> iterator = this.sortList.iterator();
        while (iterator.hasNext()) {
            final UpdatableTableItem item = iterator.next();
            this.tableMapping.put(item.connectionId, item);
        }
    }

    private void notifySelectionChange() {
        if (this.selectionListener == null) {
            return;
        }

        this.selectionListener.accept(this);
    }

    private void notifyTableInfoClick(final Event event) {
        // TODO if right click get selected item and show additional information (notification)
    }

    private final class UpdatableTableItem implements Comparable<UpdatableTableItem>, MonitoringEntry {

        final Long connectionId;
        private boolean dataChanged = false;
        private boolean indicatorValueChanged = false;
        private ClientMonitoringData monitoringData;
        private ClientStaticData staticData = ClientStaticData.NULL_DATA;
        private int thresholdsWeight;
        private int[] indicatorWeights = null;
        private boolean marked = false;

        UpdatableTableItem(final Long connectionId) {
            this.connectionId = connectionId;
            ClientConnectionTable.this.needsSort = true;
        }

        @Override
        public ConnectionStatus getStatus() {
            if (this.monitoringData == null) {
                return ConnectionStatus.UNDEFINED;
            }
            return this.monitoringData.status;
        }

        @Override
        public boolean hasMissingPing() {
            return (this.monitoringData != null) ? this.monitoringData.missingPing : false;
        }

        @Override
        public boolean grantChecked() {
            return !ClientConnectionTable.this.checkSecurityGrant || this.monitoringData.grantChecked;
        }

        @Override
        public boolean grantDenied() {
            return ClientConnectionTable.this.checkSecurityGrant && this.monitoringData.grantDenied;
        }

        @Override
        public boolean sebVersionDenied() {
            return (this.monitoringData == null) ? false : this.monitoringData.sebVersionDenied;
        }

        @Override
        public int incidentFlag() {
            return this.monitoringData.notificationFlag;
        }

        @Override
        public boolean showNoGrantCheckApplied() {
            return ClientConnectionTable.this.checkSecurityGrant;
        }

        private void update(final TableItem tableItem, final boolean force) {
            updateDuplicateColor(tableItem);
            //if (force || this.dataChanged) {
            updateData(tableItem);
            //}
            //if (force || this.indicatorValueChanged) {
            updateIndicatorValues(tableItem);
            //}
            this.dataChanged = false;
            this.indicatorValueChanged = false;
        }

        private void updateData(final TableItem tableItem) {
            int row = 0;
            tableItem.setText(row++, getConnectionIdentifier());
            if (ClientConnectionTable.this.hasClientGroups) {
                tableItem.setText(row++, getGroupInfo());
            }
            tableItem.setText(row++, getConnectionInfo());
            if (ClientConnectionTable.this.checkSEBVersion) {
                if (sebVersionDenied()) {
                    tableItem.setBackground(row - 1, ClientConnectionTable.this.colorData.color2);
                } else {
                    tableItem.setBackground(row - 1, ClientConnectionTable.this.colorData.color1);
                }
            }
            tableItem.setText(
                    row++,
                    ClientConnectionTable.this.localizedClientConnectionStatusNameFunction.apply(this));
            if (this.monitoringData != null) {
                updateConnectionStatusColor(tableItem);
                updateNotifications(tableItem);
            }
        }

        private void updateDuplicateColor(final TableItem tableItem) {
            if (ClientConnectionTable.this.duplicates.contains(this.connectionId) &&
                    tableItem.getBackground(0) != ClientConnectionTable.this.colorData.color3) {
                tableItem.setBackground(0, ClientConnectionTable.this.colorData.color3);
                tableItem.setForeground(0, ClientConnectionTable.this.lightFontColor);
                this.marked = true;
                ClientConnectionTable.this.needsSort = true;
            } else if (!ClientConnectionTable.this.duplicates.contains(this.connectionId) &&
                    tableItem.getBackground(0) == ClientConnectionTable.this.colorData.color3) {
                tableItem.setBackground(0, null);
                tableItem.setForeground(0, ClientConnectionTable.this.darkFontColor);
                ClientConnectionTable.this.needsSort = true;
                this.marked = false;
            }
        }

        private void updateNotifications(final TableItem tableItem) {
            if (this.monitoringData != null && BooleanUtils.isTrue(this.monitoringData.pendingNotification)) {
                tableItem.setImage(0,
                        WidgetFactory.ImageIcon.NOTIFICATION.getImage(ClientConnectionTable.this.table.getDisplay()));
                tableItem.setBackground(0, ClientConnectionTable.this.colorData.color2);
            } else {
                if (tableItem.getImage(0) != null) {
                    tableItem.setImage(0, null);
                    tableItem.setBackground(0, ClientConnectionTable.this.colorData.color1);
                }
            }
        }

        private void updateConnectionStatusColor(final TableItem tableItem) {
            final Color statusColor = ClientConnectionTable.this.colorData.getStatusColor(this);
            final Color statusTextColor = ClientConnectionTable.this.colorData.getStatusTextColor(statusColor);
            final int index = ClientConnectionTable.this.hasClientGroups ? 3 : 2;
            tableItem.setBackground(index, statusColor);
            tableItem.setForeground(index, statusTextColor);
        }

        private void updateIndicatorValues(final TableItem tableItem) {
            if (this.monitoringData == null || this.indicatorWeights == null) {
                return;
            }

            this.monitoringData.indicatorVals
                    .entrySet()
                    .stream()
                    .forEach(indicatorUpdate(tableItem));
        }

        private Consumer<Map.Entry<Long, String>> indicatorUpdate(final TableItem tableItem) {
            return entry -> {
                final Long id = entry.getKey();
                final String displayValue = entry.getValue();
                final IndicatorData indicatorData = ClientConnectionTable.this.indicatorMapping.get(id);
                if (indicatorData == null || this.monitoringData == null) {
                    return;
                }

                if (!this.monitoringData.status.clientActiveStatus) {
                    final String value = (indicatorData.indicator.type.showOnlyInActiveState)
                            ? Constants.EMPTY_NOTE
                            : displayValue;
                    tableItem.setText(indicatorData.tableIndex, value);
                    tableItem.setBackground(indicatorData.tableIndex, indicatorData.defaultColor);
                    tableItem.setForeground(indicatorData.tableIndex, indicatorData.defaultTextColor);
                } else {
                    tableItem.setText(indicatorData.tableIndex, displayValue);
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
            };
        }

        @Override
        public int compareTo(final UpdatableTableItem other) {
            return Comparator.comparingInt(UpdatableTableItem::notificationWeight)
                    .thenComparingInt(UpdatableTableItem::statusWeight)
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

        int notificationWeight() {
            if (this.monitoringData == null) {
                return 0;
            }
            return BooleanUtils.isTrue(this.monitoringData.pendingNotification) ||
                    (this.monitoringData.status.establishedStatus && this.marked) ? -1 : 0;
        }

        int statusWeight() {
            return ClientConnectionTable.this.colorData.statusWeight(this);
        }

        int thresholdsWeight() {
            if (this.monitoringData != null && !this.monitoringData.status.clientActiveStatus) {
                return 0;
            }
            return -this.thresholdsWeight;
        }

        String getConnectionInfo() {
            if (this.staticData != null && this.staticData.info != null) {
                return this.staticData.info;
            }
            return Constants.EMPTY_NOTE;
        }

        private String getGroupInfo() {
            final StringBuilder sb = new StringBuilder();
            ClientConnectionTable.this.clientGroupMapping.keySet().stream().forEach(key -> {
                if (this.staticData.groups != null && this.staticData.groups.contains(key)) {
                    final ClientGroup clientGroup = ClientConnectionTable.this.clientGroupMapping.get(key);
                    sb.append(WidgetFactory.getTextWithBackgroundHTML(clientGroup.name, clientGroup.color));
                }
            });

            if (sb.length() <= 0) {
                return Constants.EMPTY_NOTE;
            } else {
                return sb.toString();
            }
        }

        String getConnectionIdentifier() {

            if (this.staticData != null && this.staticData.userSessionId != null) {
                return this.staticData.userSessionId;
            }

            return "--";
        }

        boolean push(final ClientMonitoringData monitoringData) {
            this.dataChanged = this.monitoringData == null || this.monitoringData.hasChanged(monitoringData);
            this.indicatorValueChanged = this.monitoringData == null ||
                    (this.monitoringData.status.clientActiveStatus
                            && !this.monitoringData.indicatorValuesEquals(monitoringData));
            final boolean notificationChanged = this.monitoringData == null ||
                    BooleanUtils.toBoolean(this.monitoringData.pendingNotification) != BooleanUtils
                            .toBoolean(monitoringData.pendingNotification);

            if (this.dataChanged || notificationChanged) {
                ClientConnectionTable.this.needsSort = true;
            }

            if (this.indicatorWeights == null) {
                this.indicatorWeights = new int[ClientConnectionTable.this.indicatorMapping.size()];
                for (int i = 0; i < this.indicatorWeights.length; i++) {
                    this.indicatorWeights[i] = -1;
                }
            }
            if (this.indicatorValueChanged) {
                updateIndicatorWeight();
            }
            this.monitoringData = monitoringData;

            return this.staticData == null
                    || this.staticData == ClientStaticData.NULL_DATA
                    || this.dataChanged
                    || this.monitoringData.status.connectingStatus
                    || StringUtils.isBlank(this.staticData.userSessionId);
        }

        void push(final ClientStaticData staticData) {
            this.staticData = staticData;
            this.dataChanged = true;
        }

        private void updateIndicatorWeight() {
            if (this.monitoringData == null) {
                return;
            }

            this.monitoringData.indicatorVals.entrySet().stream().forEach(entry -> {
                final Long id = entry.getKey();
                final String displayValue = entry.getValue();
                final IndicatorData indicatorData = ClientConnectionTable.this.indicatorMapping.get(id);
                if (indicatorData == null) {
                    return;
                }

                final int indicatorWeight = IndicatorData.getWeight(
                        indicatorData,
                        IndicatorValue.getFromDisplayValue(displayValue));

                if (this.indicatorWeights[indicatorData.index] != indicatorWeight) {
                    ClientConnectionTable.this.needsSort = true;
                    this.thresholdsWeight -= (indicatorData.indicator.type.inverse)
                            ? indicatorData.indicator.thresholds.size()
                                    - this.indicatorWeights[indicatorData.index]
                            : this.indicatorWeights[indicatorData.index];
                    this.indicatorWeights[indicatorData.index] = indicatorWeight;
                    this.thresholdsWeight += (indicatorData.indicator.type.inverse)
                            ? indicatorData.indicator.thresholds.size()
                                    - this.indicatorWeights[indicatorData.index]
                            : this.indicatorWeights[indicatorData.index];
                }
            });
        }

        private ClientConnectionTable getOuterType() {
            return ClientConnectionTable.this;
        }
    }

    private void fetchStaticClientConnectionData() {
        final String ids = this.toUpdateStatic
                .stream()
                .map(String::valueOf)
                .reduce("", (acc, str) -> acc + str + Constants.LIST_SEPARATOR, (acc1, acc2) -> acc1 + acc2);

        final Result<MonitoringStaticClientData> call = this.pageService
                .getRestService()
                .getBuilder(GetMonitoringStaticClientData.class)
                .withFormParam(API.PARAM_MODEL_ID_LIST, ids)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, this.exam.getModelId())
                .call();

        if (call.hasError()) {
            log.error("Failed to get client connection static data for: {}", ids, call.getError());
        } else {
            final MonitoringStaticClientData monitoringStaticClientData = call.get();
            this.duplicates.clear();
            this.duplicates.addAll(monitoringStaticClientData.duplications);
            monitoringStaticClientData.staticClientConnectionData
                    .stream()
                    .forEach(staticData -> {
                        final UpdatableTableItem updatableTableItem = this.tableMapping.get(staticData.id);
                        if (updatableTableItem != null) {
                            updatableTableItem.push(staticData);
                        } else {
                            log.error("Failed to find table entry for static data: {}", staticData);
                        }
                    });
        }
    }

}
