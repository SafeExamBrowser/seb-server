/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.HtmlUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;
import io.micrometer.core.instrument.util.StringUtils;

public class EntityTable<ROW> {

    private static final Logger log = LoggerFactory.getLogger(EntityTable.class);

    private static final LocTextKey DEFAULT_SORT_COLUMN_TOOLTIP_KEY =
            new LocTextKey("sebserver.table.column.sort.default.tooltip");
    private static final String COLUMN_DEFINITION = "COLUMN_DEFINITION";
    private static final String TABLE_ROW_DATA = "TABLE_ROW_DATA";
    private static final int HEADER_HEIGHT = 40;
    private static final int ROW_HEIGHT = 25;

    private final String name;
    private final String filterAttrName;
    private final String sortAttrName;
    private final String sortOrderAttrName;
    private final String currentPageAttrName;
    private final boolean markupEnabled;

    final PageService pageService;
    final WidgetFactory widgetFactory;
    final PageSupplier<ROW> pageSupplier;
    final Function<PageSupplier.Builder<ROW>, PageSupplier.Builder<ROW>> pageSupplierAdapter;
    final I18nSupport i18nSupport;
    final PageContext pageContext;

    final List<ColumnDefinition<ROW>> columns;
    final LocTextKey emptyMessage;

    final Composite composite;
    private final TableFilter<ROW> filter;
    private final Table table;
    private final TableNavigator navigator;
    private final MultiValueMap<String, String> staticQueryParams;
    private final BiConsumer<TableItem, ROW> rowDecorator;
    private final Consumer<Set<ROW>> selectionListener;
    private final Consumer<Integer> contentChangeListener;

    private final String defaultSortColumn;
    private final PageSortOrder defaultSortOrder;

    int pageNumber;
    int pageSize;
    String sortColumn = null;
    PageSortOrder sortOrder = PageSortOrder.ASCENDING;
    boolean columnsWithSameWidth = true;
    boolean hideNavigation;

    EntityTable(
            final String name,
            final boolean markupEnabled,
            final int type,
            final PageContext pageContext,
            final PageSupplier<ROW> pageSupplier,
            final Function<PageSupplier.Builder<ROW>, PageSupplier.Builder<ROW>> pageSupplierAdapter,
            final PageService pageService,
            final List<ColumnDefinition<ROW>> columns,
            final int pageSize,
            final LocTextKey emptyMessage,
            final Function<EntityTable<ROW>, PageAction> defaultActionFunction,
            final boolean hideNavigation,
            final MultiValueMap<String, String> staticQueryParams,
            final BiConsumer<TableItem, ROW> rowDecorator,
            final Consumer<Set<ROW>> selectionListener,
            final Consumer<Integer> contentChangeListener,
            final String defaultSortColumn,
            final PageSortOrder defaultSortOrder) {

        this.name = name;
        this.filterAttrName = name + "_filter";
        this.sortAttrName = name + "_sort";
        this.sortOrderAttrName = name + "_sortOrder";
        this.currentPageAttrName = name + "_currentPage";
        this.markupEnabled = markupEnabled;

        this.defaultSortColumn = defaultSortColumn;
        this.defaultSortOrder = defaultSortOrder;

        this.composite = new Composite(pageContext.getParent(), SWT.NONE);
        this.pageService = pageService;
        this.i18nSupport = pageService.getI18nSupport();
        this.pageContext = pageContext;
        this.widgetFactory = pageService.getWidgetFactory();
        this.pageSupplier = pageSupplier;
        this.pageSupplierAdapter = (pageSupplierAdapter != null) ? pageSupplierAdapter : Function.identity();
        this.columns = Utils.immutableListOf(columns);
        this.emptyMessage = emptyMessage;
        this.hideNavigation = hideNavigation;

        final GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        this.composite.setLayout(layout);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalIndent = 2;
        this.composite.setLayoutData(gridData);
        this.staticQueryParams = staticQueryParams;
        this.rowDecorator = rowDecorator;
        this.selectionListener = selectionListener;
        this.contentChangeListener = contentChangeListener;
        this.pageSize = pageSize;
        this.filter = columns
                .stream()
                .map(ColumnDefinition::getFilterAttribute)
                .anyMatch(Objects::nonNull) ? new TableFilter<>(this) : null;

        this.table = this.widgetFactory.tableLocalized(this.composite, type);
        final GridLayout gridLayout = new GridLayout(columns.size(), true);
        this.table.setLayout(gridLayout);
        gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.table.setLayoutData(gridData);
        this.table.addListener(SWT.Resize, this::adaptColumnWidth);
        @SuppressWarnings("unchecked")
        final Consumer<Table> locFunction = (Consumer<Table>) this.table.getData(POLYGLOT_WIDGET_FUNCTION_KEY);
        final Consumer<Table> newLocFunction = t -> {
            updateValues(this);
            locFunction.accept(t);
        };
        this.table.setData(POLYGLOT_WIDGET_FUNCTION_KEY, newLocFunction);

        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);
        this.table.setData(RWT.CUSTOM_ITEM_HEIGHT, ROW_HEIGHT);
        if (this.markupEnabled) {
            this.table.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        }

        if (defaultActionFunction != null) {
            final PageAction defaultAction = defaultActionFunction.apply(this);
            if (defaultAction != null) {
                this.table.addListener(SWT.MouseDoubleClick, event -> {
                    // if the action has its own selection function, apply this
                    EntityKey selection = defaultAction.getSingleSelection();
                    if (selection == null) {
                        // otherwise use current selection of this table
                        selection = getSingleSelection();
                    }
                    if (selection != null) {
                        this.pageService.executePageAction(
                                defaultAction.withEntityKey(selection));
                    }
                });
            }
        }
        this.table.addListener(SWT.MouseDown, event -> {
            if (event.button == Constants.RWT_MOUSE_BUTTON_1) {
                return;
            }
            final Rectangle bounds = event.getBounds();
            final Point point = new Point(bounds.x, bounds.y);
            final TableItem item = this.table.getItem(point);
            if (item == null) {
                return;
            }

            for (int i = 0; i < columns.size(); i++) {
                final Rectangle itemBounds = item.getBounds(i);
                if (itemBounds.contains(point)) {
                    handleCellSelection(item, i);
                    return;
                }
            }
        });

        this.table.addListener(SWT.Selection, event -> this.notifySelectionChange());

        this.navigator = (pageSize > 0) ? new TableNavigator(this) : new TableNavigator();

        createTableColumns();
        this.pageNumber = initCurrentPageFromUserAttr();
        initFilterFromUserAttrs();
        initSortFromUserAttr();
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public String getName() {
        return this.name;
    }

    public String getSortColumn() {
        return this.sortColumn;
    }

    public PageSortOrder getSortOrder() {
        return this.sortOrder;
    }

    public EntityType getEntityType() {
        if (this.pageSupplier != null) {
            return this.pageSupplier.getEntityType();
        }

        return null;
    }

    public PageContext getPageContext() {
        if (this.pageContext == null) {
            return null;
        }

        return this.pageContext.copy();
    }

    public boolean hasAnyContent() {
        return this.table.getItemCount() > 0;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
    }

    public void selectPage(final int pageSelection) {
        // verify input
        this.pageNumber = pageSelection;
        if (this.pageNumber < 1) {
            this.pageNumber = 1;
        }

        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);

        updateCurrentPageAttr();
    }

    public void reset() {
        this.sortColumn = this.defaultSortColumn;
        this.sortOrder = this.defaultSortOrder;
        updateSortUserAttr();
        setTableSort();
        applyFilter();
    }

    public void applyFilter() {
        try {

            updateFilterUserAttrs();
            this.selectPage(1);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply filter: ", e);
        }
    }

    public MultiValueMap<String, String> getFilterCriteria() {
        if (this.filter == null) {
            return new LinkedMultiValueMap<>();
        }

        return this.filter.getFilterParameter();
    }

    public void applySort(final String columnName, final PageSortOrder order) {
        try {
            this.sortColumn = columnName;
            this.sortOrder = order;

            if (columnName != null) {
                updateTableRows(
                        this.pageNumber,
                        this.pageSize,
                        this.sortColumn,
                        this.sortOrder);
            }

            updateSortUserAttr();

        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply sort: ", e);
        }
    }

    public void changeSortOrder() {
        try {
            this.sortOrder = (this.sortOrder == PageSortOrder.ASCENDING)
                    ? PageSortOrder.DESCENDING
                    : PageSortOrder.ASCENDING;

            updateTableRows(
                    this.pageNumber,
                    this.pageSize,
                    this.sortColumn,
                    this.sortOrder);

            updateSortUserAttr();

        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply sort: ", e);
        }
    }

    public EntityKey getSingleSelection() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null || selection.length == 0) {
            return null;
        }

        return getRowDataId(selection[0]);
    }

    public ROW getFirstRowData() {
        if (!this.hasAnyContent()) {
            return null;
        }

        final TableItem item = this.table.getItem(0);
        if (item == null) {
            return null;
        }

        return getRowData(item);
    }

    public ROW getSingleSelectedROWData() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null || selection.length == 0) {
            return null;
        }

        return getRowData(selection[0]);
    }

    public Set<ROW> getSelectedROWData() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null || selection.length == 0) {
            return Collections.emptySet();
        }

        return Arrays.stream(selection)
                .map(this::getRowData)
                .collect(Collectors.toSet());
    }

    public Set<EntityKey> getSelection() {
        return getSelection(null);
    }

    public Set<EntityKey> getSelection(final Predicate<ROW> grantCheck) {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null) {
            return Collections.emptySet();
        }

        return Arrays.stream(selection)
                .filter(item -> grantCheck == null || grantCheck.test(getRowData(item)))
                .map(this::getRowDataId)
                .collect(Collectors.toSet());
    }

    public Supplier<Set<EntityKey>> getGrantedSelection(
            final CurrentUser currentUser,
            final LocTextKey denyMessage) {

        return () -> getSelection(e -> {
            if (!(e instanceof GrantEntity)) {
                return true;
            }
            if (currentUser.entityGrantCheck((GrantEntity) e).m()) {
                return true;
            } else {
                throw new PageMessageException(denyMessage);
            }
        });
    }

    private TableColumn getTableColumn(final String name) {
        return Arrays.stream(this.table.getColumns())
                .filter(col -> {
                    @SuppressWarnings("unchecked")
                    final ColumnDefinition<ROW> def = (ColumnDefinition<ROW>) col.getData(COLUMN_DEFINITION);
                    return name.equals(def.columnName);
                })
                .findFirst()
                .orElse(null);
    }

    private void createTableColumns() {
        final String sortText = this.i18nSupport.getText(DEFAULT_SORT_COLUMN_TOOLTIP_KEY, "");

        for (final ColumnDefinition<ROW> column : this.columns) {

            final LocTextKey _tooltip = column.getTooltip();
            final LocTextKey tooltip = (_tooltip != null && this.i18nSupport.hasText(_tooltip))
                    ? (column.isSortable())
                            ? new LocTextKey(_tooltip.name, sortText)
                            : new LocTextKey(_tooltip.name, "")
                    : (column.isSortable())
                            ? DEFAULT_SORT_COLUMN_TOOLTIP_KEY
                            : null;

            final TableColumn tableColumn = this.widgetFactory.tableColumnLocalized(
                    this.table,
                    column.displayName,
                    tooltip);

            tableColumn.addListener(SWT.Resize, this::adaptColumnWidthChange);
            tableColumn.setData(COLUMN_DEFINITION, column);

            if (column.isSortable()) {
                tableColumn.addListener(SWT.Selection, event -> {
                    if (!column.columnName.equals(this.sortColumn)) {
                        applySort(column.columnName, PageSortOrder.ASCENDING);
                        this.table.setSortColumn(tableColumn);
                        this.table.setSortDirection(SWT.UP);
                    } else {
                        changeSortOrder();
                        this.table.setSortDirection(
                                (this.sortOrder == PageSortOrder.ASCENDING) ? SWT.UP : SWT.DOWN);
                    }
                });
            }

            if (column.getWidthProportion() > 0) {
                this.columnsWithSameWidth = false;
            }
        }
    }

    private void updateTableRows(
            final int pageNumber,
            final int pageSize,
            final String sortColumn,
            final PageSortOrder sortOrder) {

        // first remove all rows if there are some
        this.table.removeAll();

        // get page data and create rows
        this.pageSupplier.newBuilder()
                .withPaging(pageNumber, pageSize)
                .withSorting(sortColumn, sortOrder)
                .withQueryParams((this.filter != null) ? this.filter.getFilterParameter() : null)
                .withQueryParams(this.staticQueryParams)
                .apply(this.pageSupplierAdapter)
                .getPage()
                .map(this::createTableRowsFromPage)
                .map(this.navigator::update)
                .onError(this.pageContext::notifyUnexpectedError);

        this.composite.getParent().layout(true, true);
        PageService.updateScrolledComposite(this.composite);
        this.notifyContentChange();
        this.notifySelectionChange();
    }

    private Page<ROW> createTableRowsFromPage(final Page<ROW> page) {
        if (page.isEmpty()) {
            final GridData gridData = (GridData) this.table.getLayoutData();
            gridData.heightHint = ROW_HEIGHT;
            return page;
        }

        final GridData gridData = (GridData) this.table.getLayoutData();
        gridData.heightHint = (this.pageNumber > 1)
                ? (this.pageSize * ROW_HEIGHT) + HEADER_HEIGHT
                : (page.content.size() * ROW_HEIGHT) + HEADER_HEIGHT;

        for (final ROW row : page.content) {
            final TableItem item = new TableItem(this.table, SWT.NONE);
            if (this.markupEnabled) {
                item.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
            }
            item.setData(TABLE_ROW_DATA, row);
            if (this.rowDecorator != null) {
                this.rowDecorator.accept(item, row);
            }

            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {
                setValueToCell(item, index, column, column.valueSupplier.apply(row));
                index++;
            }
        }

        return page;
    }

    private void adaptColumnWidth(final Event event) {
        try {
            int currentTableWidth = this.table.getParent().getClientArea().width;
            // If we have all columns with filter we need some more space for the
            // filter actions in the right hand side. This tweak gives enough space for that
            if (this.filter != null && this.columns.size() == this.filter.size()) {
                currentTableWidth -= 60;
            }

            // The proportion size, the sum of all given proportion values
            final int pSize = this.columns
                    .stream()
                    .filter(c -> c.getWidthProportion() > 0)
                    .reduce(0,
                            (acc, c) -> acc + c.getWidthProportion(),
                            Integer::sum);

            // The unit size either with proportion or for a entire column if all columns are equal in size
            final int columnUnitSize = (pSize > 0)
                    ? currentTableWidth / pSize
                    : currentTableWidth / this.columns.size();

            // Apply the column width for each column
            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {
                final TableColumn tableColumn = this.table.getColumn(index);
                final int newWidth = (pSize > 0)
                        ? columnUnitSize * column.getWidthProportion()
                        : columnUnitSize;
                tableColumn.setWidth(newWidth);
                if (this.filter != null) {
                    this.filter.adaptColumnWidth(this.table.indexOf(tableColumn), newWidth);
                }
                if (index == this.columns.size() - 1) {
                    tableColumn.setWidth(newWidth - 10);
                }

                index++;
            }
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    private void adaptColumnWidthChange(final Event event) {
        final Widget widget = event.widget;
        if (widget instanceof TableColumn) {
            final TableColumn tableColumn = ((TableColumn) widget);
            if (this.filter != null) {
                this.filter.adaptColumnWidth(
                        this.table.indexOf(tableColumn),
                        tableColumn.getWidth());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ROW getRowData(final TableItem item) {
        return (ROW) item.getData(TABLE_ROW_DATA);
    }

    private EntityKey getRowDataId(final TableItem item) {
        final ROW rowData = getRowData(item);
        if (rowData instanceof Entity) {
            return ((Entity) rowData).getEntityKey();
        }
        return null;
    }

    private void updateValues(final EntityTable<ROW> table) {
        final TableItem[] items = table.table.getItems();
        final TableColumn[] columns = table.table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            final ColumnDefinition<ROW> columnDefinition = table.columns.get(i);
            if (columnDefinition.isLocalized()) {
                for (int j = 0; j < items.length; j++) {
                    @SuppressWarnings("unchecked")
                    final ROW rowData = (ROW) items[j].getData(TABLE_ROW_DATA);
                    setValueToCell(items[j], i, columnDefinition, columnDefinition.valueSupplier.apply(rowData));
                }
            }
        }
    }

    private void setValueToCell(
            final TableItem item,
            final int index,
            final ColumnDefinition<ROW> columnDefinition,
            final Object value) {

        if (value instanceof Boolean) {
            addBooleanCell(item, index, value);
        } else if (value instanceof DateTime) {
            item.setText(index, this.i18nSupport.formatDisplayDateTime((DateTime) value));
        } else {
            item.setText(index, renderTextValue(
                    (value != null) ? String.valueOf(value) : null,
                    columnDefinition));
        }
    }

    private String renderTextValue(final String raw, final ColumnDefinition<ROW> columnDefinition) {
        if (StringUtils.isBlank(raw)) {
            return Constants.EMPTY_NOTE;
        }
        if (!this.markupEnabled) {
            return raw;
        }

        if (columnDefinition.markupEnabled()) {
            return raw;
        } else {
            return HtmlUtils.htmlEscapeHex(raw);
        }
    }

    private static void addBooleanCell(final TableItem item, final int index, final Object value) {
        if ((Boolean) value) {
            item.setImage(index, ImageIcon.YES.getImage(item.getDisplay()));
        } else {
            item.setImage(index, ImageIcon.NO.getImage(item.getDisplay()));
        }
    }

    private void handleCellSelection(final TableItem item, final int index) {
        // TODO handle selection tool-tips on cell level
    }

    private void notifySelectionChange() {
        if (this.selectionListener == null) {
            return;
        }

        this.selectionListener.accept(this.getSelectedROWData());
    }

    private void updateCurrentPageAttr() {
        try {
            this.pageService
                    .getCurrentUser()
                    .putAttribute(this.currentPageAttrName, String.valueOf(this.pageNumber));
        } catch (final Exception e) {
            log.error("Failed to put current page attribute to current user attributes", e);
        }
    }

    private int initCurrentPageFromUserAttr() {
        try {
            final String currentPage = this.pageService
                    .getCurrentUser()
                    .getAttribute(this.currentPageAttrName);
            if (StringUtils.isNotBlank(currentPage)) {
                return Integer.parseInt(currentPage);
            } else {
                return 1;
            }
        } catch (final Exception e) {
            log.error("Failed to get sort attribute form current user attributes", e);
            return 1;
        }
    }

    private void updateSortUserAttr() {
        try {
            this.pageService
                    .getCurrentUser()
                    .putAttribute(this.sortAttrName, this.sortColumn);
            this.pageService
                    .getCurrentUser()
                    .putAttribute(this.sortOrderAttrName, this.sortOrder.name());
        } catch (final Exception e) {
            log.error("Failed to put sort attribute to current user attributes", e);
        }
    }

    private void initSortFromUserAttr() {
        try {
            final String sort = this.pageService
                    .getCurrentUser()
                    .getAttribute(this.sortAttrName);
            if (StringUtils.isNotBlank(sort)) {
                this.sortColumn = sort;
            } else {
                this.sortColumn = this.defaultSortColumn;
            }

            final String sortOrder = this.pageService
                    .getCurrentUser()
                    .getAttribute(this.sortOrderAttrName);
            if (StringUtils.isNotBlank(sortOrder)) {
                this.sortOrder = PageSortOrder.valueOf(sortOrder);
            } else {
                this.sortOrder = this.defaultSortOrder;
            }

            setTableSort();

        } catch (final Exception e) {
            log.error("Failed to get sort attribute form current user attributes", e);
        }
    }

    private void setTableSort() {
        if (this.sortColumn != null) {
            final TableColumn tableColumn = getTableColumn(this.sortColumn);
            if (tableColumn != null) {
                this.table.setSortColumn(tableColumn);
            }
            this.table.setSortDirection(this.sortOrder == PageSortOrder.ASCENDING ? SWT.UP : SWT.DOWN);
        } else {
            this.table.setSortColumn(null);
            this.table.setSortDirection(SWT.NONE);
        }
    }

    private void updateFilterUserAttrs() {
        if (this.filter != null) {
            try {
                this.pageService
                        .getCurrentUser()
                        .putAttribute(this.filterAttrName, this.filter.getFilterAttributes());
            } catch (final Exception e) {
                log.error("Failed to put filter attributes to current user attributes", e);
            }
        }
    }

    private void initFilterFromUserAttrs() {
        if (this.filter != null) {
            try {
                this.filter.setFilterAttributes(
                        this.pageService
                                .getCurrentUser()
                                .getAttribute(this.filterAttrName));
            } catch (final Exception e) {
                log.error("Failed to get filter attributes form current user attributes", e);
            }
        }
    }

    private void notifyContentChange() {
        if (this.contentChangeListener != null) {
            this.contentChangeListener.accept(this.table.getItemCount());
        }
    }

    public void refreshPageSize() {
        if (this.pageSupplier.newBuilder()
                .withPaging(this.pageNumber, this.pageSize)
                .withSorting(this.sortColumn, this.sortOrder)
                .withQueryParams((this.filter != null) ? this.filter.getFilterParameter() : null)
                .withQueryParams(this.staticQueryParams)
                .apply(this.pageSupplierAdapter)
                .getPage()
                .map(page -> page.content.size())
                .map(size -> size != this.table.getItems().length)
                .getOr(false)) {
            reset();
        }
    }

}
