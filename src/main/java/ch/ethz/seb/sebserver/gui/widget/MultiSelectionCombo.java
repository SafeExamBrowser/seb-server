/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class MultiSelectionCombo extends Composite implements Selection {

    private static final Logger log = LoggerFactory.getLogger(MultiSelectionCombo.class);
    private static final long serialVersionUID = -7787134114963647332L;
    private static final int ACTION_COLUMN_WIDTH = 20;

    private final WidgetFactory widgetFactory;
    private final Table table;
    private final Combo combo;
    private final List<String> selected = new ArrayList<>();
    private final List<Tuple<String>> mapping = new ArrayList<>();

    MultiSelectionCombo(final Composite parent, final WidgetFactory widgetFactory) {
        super(parent, SWT.NONE);
        this.widgetFactory = widgetFactory;
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        setLayout(gridLayout);

        this.table = new Table(this, SWT.NONE);

        TableColumn column = new TableColumn(this.table, SWT.NONE);
        column = new TableColumn(this.table, SWT.NONE);
        column.setWidth(ACTION_COLUMN_WIDTH);
        this.table.setHeaderVisible(false);
        this.table.addListener(SWT.Resize, this::adaptColumnWidth);

        final TableItem header = new TableItem(this.table, SWT.NONE);
        final TableEditor editor = new TableEditor(this.table);
        this.combo = new Combo(this.table, SWT.NONE);
        editor.grabHorizontal = true;
        editor.setEditor(this.combo, header, 0);

        widgetFactory.imageButton(
                ImageIcon.ADD,
                this.table,
                new LocTextKey("Add"),
                this::addComboSelection);

    }

    @Override
    public Type type() {
        return Type.MULTI_COMBO;
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
        this.selected.clear();
        this.mapping.clear();
        this.mapping.addAll(mapping);

    }

    @Override
    public void select(final String keys) {
        clear();

    }

    @Override
    public String getSelectionValue() {
        if (this.selected.isEmpty()) {
            return null;
        }
        return this.mapping
                .stream()
                .filter(t -> this.selected.contains(t._2))
                .map(t -> t._1)
                .reduce("", (s1, s2) -> s1.concat(Constants.LIST_SEPARATOR).concat(s2));
    }

    @Override
    public void clear() {
        this.selected.clear();
        this.table.remove(1, this.table.getItemCount());
        final List<String> names = this.mapping
                .stream()
                .map(t -> t._2)
                .collect(Collectors.toList());
        this.combo.setItems(names.toArray(new String[names.size()]));
    }

    private void addComboSelection(final Event event) {

    }

    private void adaptColumnWidth(final Event event) {
        try {
            final int currentTableWidth = this.table.getParent().getClientArea().width;
            this.table.getColumn(0).setWidth(currentTableWidth - ACTION_COLUMN_WIDTH);
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

}
