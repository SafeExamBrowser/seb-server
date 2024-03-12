/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.*;
import java.util.List;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.widgets.DropDown;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.page.PageService;

public final class MultiSelectionCombo extends Composite implements Selection {

    public static final LocTextKey DESELECT_TOOLTIP = new LocTextKey( "sebserver.form.multiselect.deselect.tooltip" );

    private static final Logger log = LoggerFactory.getLogger(MultiSelectionCombo.class);
    private static final long serialVersionUID = -7787134114963647332L;

    private final List<Tuple<String>> valueMapping = new ArrayList<>();
    private final List<Tuple<String>> availableValues = new ArrayList<>();
    private final List<Tuple<String>> selectedValues = new ArrayList<>();

    private final DropDown dropDown;
    private final Text textInput;
    private final GridData textCell;
    private final Composite updateAnchor;
    private final String testKey;

    private final Table selectionTable;

    private Listener listener = null;
    private WidgetFactory widgetFactory;

    MultiSelectionCombo(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final String locTextPrefix,
            final Composite updateAnchor) {

        super(parent, SWT.NONE);
        this.widgetFactory = widgetFactory;
        this.testKey = locTextPrefix;

        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        this.addListener(SWT.Resize, this::adaptColumnWidth);
        this.textInput = widgetFactory.textInput(this, locTextPrefix, "selection");
        this.textCell = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        this.textInput.setLayoutData(this.textCell);
        this.dropDown = new DropDown(this.textInput, SWT.NONE);
        this.textInput.addListener(SWT.FocusIn, event -> openDropDown());
        this.textInput.addListener(SWT.Modify, event -> openDropDown());
        this.textInput.addListener(SWT.MouseUp, event -> openDropDown());
        this.dropDown.addListener(SWT.DefaultSelection, event -> {
            final int selectionIndex = this.dropDown.getSelectionIndex();
            if (selectionIndex >= 0) {
                final String selectedItem = this.dropDown.getItems()[selectionIndex];
                addSelection(itemForName(selectedItem));
            }
        });

        selectionTable = widgetFactory.tableLocalized(this, SWT.NONE);
        final GridLayout tableLayout = new GridLayout(1, true);
        selectionTable.setLayout(tableLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        selectionTable.setLayoutData(gridData);
        //selectionTable.setToolTipText();
        selectionTable.addListener(SWT.MouseDoubleClick, this::removeComboSelection);
        selectionTable.addListener(SWT.Selection, event -> {
            selectionTable.setSelection(-1);
        });
        selectionTable.setHeaderVisible(false);
        selectionTable.setLinesVisible(true);

        this.updateAnchor = updateAnchor;
    }

    @Override
    public void setToolTipText(final String tooltipText) {
        if (tooltipText == null) {
            super.setToolTipText(widgetFactory.getI18nSupport().getText(DESELECT_TOOLTIP));
            return;
        }
        super.setToolTipText(tooltipText + "\n\n" +  widgetFactory.getI18nSupport().getText(DESELECT_TOOLTIP));
    }

    @Override
    public void setAriaLabel(final String label) {
        WidgetFactory.setARIALabel(this.dropDown, label);
    }

    @Override
    public Type type() {
        return Type.MULTI_COMBO;
    }

    @Override
    public void setSelectionListener(final Listener listener) {
        this.listener = listener;
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        this.valueMapping.clear();
        this.valueMapping.addAll(mapping);
        this.clear();
    }

    @Override
    public void select(final String keys) {
        clear();
        if (StringUtils.isBlank(keys)) {
            return;
        }

        Arrays.stream(StringUtils.split(keys, Constants.LIST_SEPARATOR))
                .map(this::itemForId)
                .forEach(this::addSelection);
    }

    @Override
    public String getSelectionValue() {
        if (this.selectedValues.isEmpty()) {
            return null;
        }
        return this.selectedValues
                .stream()
                .map(t -> t._1)
                .reduce("", (s1, s2) -> {
                    if (!StringUtils.isBlank(s1)) {
                        return s1.concat(Constants.LIST_SEPARATOR).concat(s2);
                    } else {
                        return s1.concat(s2);
                    }
                });
    }

    @Override
    public void clear() {
        this.selectedValues.clear();
        this.availableValues.clear();
        this.availableValues.addAll(this.valueMapping);
    }

    private void addSelection(final Tuple<String> item) {
        if (item == null) {
            return;
        }

        this.selectedValues.add(item);

        sortSelectedTable();

        this.availableValues.remove(item);
        PageService.updateScrolledComposite(this);
        this.updateAnchor.layout(true, true);
    }

    private void sortSelectedTable() {
        selectionTable.removeAll();
        selectedValues.sort((t1, t2) -> String.CASE_INSENSITIVE_ORDER.compare(t1._2, t2._2));
        selectedValues.stream().forEach(t -> {
            final TableItem tItem = new TableItem(selectionTable, SWT.NONE);
            tItem.setText(0, t._2);
            tItem.setData("tuple", t);
            WidgetFactory.setARIALabel(tItem, t._2);
            WidgetFactory.setTestId(tItem, (this.testKey != null) ? this.testKey + "_" + t._1 : t._1);
        });
    }

    private void removeComboSelection(final Event event) {
        if (event.widget == null) {
            return;
        }

        final TableItem item = selectionTable.getItem(new Point(event.x, event.y));
        @SuppressWarnings("unchecked")
        final Tuple<String> value = (Tuple<String>) item.getData("tuple");
        this.selectedValues.remove(value);
        this.availableValues.add(value);

        sortSelectedTable();

        PageService.updateScrolledComposite(this);
        this.updateAnchor.layout(true, true);
        if (this.listener != null) {
            this.listener.handleEvent(event);
        }
    }

    private void adaptColumnWidth(final Event event) {
        try {
            this.textCell.widthHint = this.getClientArea().width;
            this.layout();
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    private Tuple<String> itemForName(final String name) {
        final Optional<Tuple<String>> findFirst = this.availableValues
                .stream()
                .filter(it -> it._2 != null && it._2.equals(name))
                .findFirst();
        return findFirst.orElse(null);
    }

    private Tuple<String> itemForId(final String id) {
        final Optional<Tuple<String>> findFirst = this.availableValues
                .stream()
                .filter(it -> it._1 != null && it._1.equals(id))
                .findFirst();
        return findFirst.orElse(null);
    }

    private void openDropDown() {
        final String text = this.textInput.getText();
        if (text == null) {
            this.dropDown.setVisible(false);
            return;
        }
        this.dropDown.setItems(this.availableValues
                .stream()
                .filter(it -> it._2 != null && it._2.startsWith(text))
                .map(t -> t._2).toArray(String[]::new));
        this.dropDown.setSelectionIndex(0);
        this.dropDown.setVisible(true);
    }

}
