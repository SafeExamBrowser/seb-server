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
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class EntityTable<ROW extends Entity> {

    private static final Logger log = LoggerFactory.getLogger(EntityTable.class);

    private static final String COLUMN_DEFINITION = "COLUMN_DEFINITION";
    private static final String TABLE_ROW_DATA = "TABLE_ROW_DATA";
    private static final int HEADER_HEIGHT = 40;
    private static final int ROW_HEIGHT = 25;

    final PageService pageService;
    final WidgetFactory widgetFactory;
    final RestCall<Page<ROW>> restCall;
    final Function<RestCall<Page<ROW>>.RestCallBuilder, RestCall<Page<ROW>>.RestCallBuilder> restCallAdapter;
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

    int pageNumber = 1;
    int pageSize;
    String sortColumn = null;
    PageSortOrder sortOrder = PageSortOrder.ASCENDING;
    boolean columnsWithSameWidth = true;
    boolean hideNavigation = false;

    EntityTable(
            final int type,
            final PageContext pageContext,
            final RestCall<Page<ROW>> restCall,
            final Function<RestCall<Page<ROW>>.RestCallBuilder, RestCall<Page<ROW>>.RestCallBuilder> restCallAdapter,
            final PageService pageService,
            final List<ColumnDefinition<ROW>> columns,
            final int pageSize,
            final LocTextKey emptyMessage,
            final Function<EntityTable<ROW>, PageAction> defaultActionFunction,
            final boolean hideNavigation,
            final MultiValueMap<String, String> staticQueryParams,
            final BiConsumer<TableItem, ROW> rowDecorator) {

        this.composite = new Composite(pageContext.getParent(), type);
        this.pageService = pageService;
        this.i18nSupport = pageService.getI18nSupport();
        this.pageContext = pageContext;
        this.widgetFactory = pageService.getWidgetFactory();
        this.restCall = restCall;
        this.restCallAdapter = (restCallAdapter != null) ? restCallAdapter : Function.identity();
        this.columns = Utils.immutableListOf(columns);
        this.emptyMessage = emptyMessage;
        this.hideNavigation = hideNavigation;

        final GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        this.composite.setLayout(layout);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.composite.setLayoutData(gridData);
        this.staticQueryParams = staticQueryParams;
        this.rowDecorator = rowDecorator;

// TODO just for debugging, remove when tested
//        this.composite.setBackground(new Color(parent.getDisplay(), new RGB(0, 200, 0)));

        this.pageSize = pageSize;
        this.filter =
                columns
                        .stream()
                        .map(column -> column.getFilterAttribute())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .isPresent() ? new TableFilter<>(this) : null;

        this.table = this.widgetFactory.tableLocalized(this.composite);
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
        this.table.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);

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
                final Rectangle itemBoundes = item.getBounds(i);
                if (itemBoundes.contains(point)) {
                    handleCellSelection(item, i);
                    return;
                }
            }
        });

        this.navigator = new TableNavigator(this);

        createTableColumns();
        updateTableRows(
                this.pageNumber,
                this.pageSize,
                this.sortColumn,
                this.sortOrder);
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
    }

    public void applyFilter() {
        try {
            updateTableRows(
                    this.pageNumber,
                    this.pageSize,
                    this.sortColumn,
                    this.sortOrder);
        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply filter: ", e);
        }
    }

    public void applySort(final String columnName) {
        try {
            this.sortColumn = columnName;
            this.sortOrder = PageSortOrder.ASCENDING;

            updateTableRows(
                    this.pageNumber,
                    this.pageSize,
                    this.sortColumn,
                    this.sortOrder);
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

    public ROW getSelectedROWData() {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null || selection.length == 0) {
            return null;
        }

        return getRowData(selection[0]);
    }

    public Set<EntityKey> getSelection() {
        return getSelection(null);
    }

    public Set<EntityKey> getSelection(final Predicate<ROW> grantCheck) {
        final TableItem[] selection = this.table.getSelection();
        if (selection == null) {
            return Collections.emptySet();
        }

        return Arrays.asList(selection)
                .stream()
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

    private void createTableColumns() {
        for (final ColumnDefinition<ROW> column : this.columns) {
            final TableColumn tableColumn = this.widgetFactory.tableColumnLocalized(
                    this.table,
                    column.displayName,
                    column.getTooltip());

            tableColumn.addListener(SWT.Resize, this::adaptColumnWidthChange);
            tableColumn.setData(COLUMN_DEFINITION, column);

            if (column.isSortable()) {
                tableColumn.addListener(SWT.Selection, event -> {
                    if (!column.columnName.equals(this.sortColumn)) {
                        applySort(column.columnName);
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
        this.restCall.newBuilder()
                .withPaging(pageNumber, pageSize)
                .withSorting(sortColumn, sortOrder)
                .withQueryParams((this.filter != null) ? this.filter.getFilterParameter() : null)
                .withQueryParams(this.staticQueryParams)
                .apply(this.restCallAdapter)
                .call()
                .map(this::createTableRowsFromPage)
                .map(this.navigator::update)
                .onError(this.pageContext::notifyUnexpectedError);

        this.composite.getParent().layout(true, true);
        PageService.updateScrolledComposite(this.composite);
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
            item.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
            item.setData(TABLE_ROW_DATA, row);
            if (this.rowDecorator != null) {
                this.rowDecorator.accept(item, row);
            }

            int index = 0;
            for (final ColumnDefinition<ROW> column : this.columns) {
                setValueToCell(item, index, column.valueSupplier.apply(row));
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
                            (acc1, acc2) -> acc1 + acc2);

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
        return getRowData(item).getEntityKey();
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
                    setValueToCell(items[j], i, columnDefinition.valueSupplier.apply(rowData));
                }
            }
        }
    }

    private void setValueToCell(final TableItem item, final int index, final Object value) {
        if (value instanceof Boolean) {
            addBooleanCell(item, index, value);
        } else if (value instanceof DateTime) {
            item.setText(index, this.i18nSupport.formatDisplayDate((DateTime) value));
        } else {
            if (value != null) {
                final String val = String.valueOf(value).replace('\n', ' ');
                item.setText(index, val);
            } else {
                item.setText(index, Constants.EMPTY_NOTE);
            }
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

}
