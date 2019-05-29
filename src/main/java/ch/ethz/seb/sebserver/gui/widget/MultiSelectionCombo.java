/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public final class MultiSelectionCombo extends Composite implements Selection {

    private static final Logger log = LoggerFactory.getLogger(MultiSelectionCombo.class);
    private static final long serialVersionUID = -7787134114963647332L;
    private static final int ACTION_COLUMN_WIDTH = 20;

    private static final LocTextKey DEFAULT_ADD_TOOLTIP_KEY = new LocTextKey("sebserver.overall.add");
    private static final LocTextKey DEFAULT_REMOVE_TOOLTIP_KEY = new LocTextKey("sebserver.overall.remove");

    private final WidgetFactory widgetFactory;
    private final Combo combo;
    private final LocTextKey addTextKey;
    private final LocTextKey removeTextKey;

    private final List<Tuple<Control>> selectionControls = new ArrayList<>();
    private final List<Tuple<String>> selectedValues = new ArrayList<>();
    private final Map<String, String> mapping = new HashMap<>();

    private final GridData comboCell;
    private final GridData actionCell;

    private Listener listener = null;

    MultiSelectionCombo(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final String locTextPrefix) {

        super(parent, SWT.NONE);
        this.widgetFactory = widgetFactory;
        this.addTextKey = (locTextPrefix != null)
                ? new LocTextKey(locTextPrefix + ".add")
                : DEFAULT_ADD_TOOLTIP_KEY;
        this.removeTextKey = (locTextPrefix != null)
                ? new LocTextKey(locTextPrefix + ".remove")
                : DEFAULT_REMOVE_TOOLTIP_KEY;

        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        this.addListener(SWT.Resize, this::adaptColumnWidth);

        this.combo = new Combo(this, SWT.NONE);
        this.comboCell = new GridData(SWT.FILL, SWT.CENTER, true, false);
        this.combo.setLayoutData(this.comboCell);

        final Label imageButton = widgetFactory.imageButton(
                ImageIcon.ADD_BOX,
                this,
                this.addTextKey,
                this::addComboSelection);
        this.actionCell = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        this.actionCell.widthHint = ACTION_COLUMN_WIDTH;
        imageButton.setLayoutData(this.actionCell);
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
        this.mapping.putAll(mapping.stream()
                .collect(Collectors.toMap(t -> t._1, t -> t._2)));
        this.clear();
    }

    @Override
    public void select(final String keys) {
        clear();
        if (StringUtils.isBlank(keys)) {
            return;
        }

        Arrays.asList(StringUtils.split(keys, Constants.LIST_SEPARATOR))
                .stream()
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
        this.selectionControls
                .stream()
                .forEach(t -> {
                    t._1.dispose();
                    t._2.dispose();
                });
        this.selectionControls.clear();
        this.combo.setItems(this.mapping.values().toArray(new String[this.mapping.size()]));
    }

    private void addComboSelection(final Event event) {
        final int selectionIndex = this.combo.getSelectionIndex();
        if (selectionIndex < 0) {
            return;
        }

        final String itemName = this.combo.getItem(selectionIndex);
        if (itemName == null) {
            return;
        }

        final Optional<Entry<String, String>> findFirst = this.mapping.entrySet()
                .stream()
                .filter(entity -> entity.getValue().equals(itemName))
                .findFirst();

        if (!findFirst.isPresent()) {
            return;
        }

        addSelection(findFirst.get().getKey());
        if (this.listener != null) {
            this.listener.handleEvent(event);
        }
    }

    private void addSelection(final String itemKey) {
        final String itemName = this.mapping.get(itemKey);
        if (itemName == null) {
            return;
        }

        this.selectedValues.add(new Tuple<>(itemKey, itemName));
        final Label label = this.widgetFactory.label(this, itemName);
        final Label imageButton = this.widgetFactory.imageButton(
                ImageIcon.REMOVE_BOX,
                this,
                this.removeTextKey,
                this::removeComboSelection);
        imageButton.setData(OPTION_VALUE, itemName);
        final GridData actionCell = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        actionCell.widthHint = ACTION_COLUMN_WIDTH;
        imageButton.setLayoutData(actionCell);

        this.selectionControls.add(new Tuple<>(label, imageButton));

        this.combo.remove(itemName);
        this.getParent().layout();
    }

    private void removeComboSelection(final Event event) {
        if (event.widget == null) {
            return;
        }

        final String selectionKey = (String) event.widget.getData(OPTION_VALUE);
        final Optional<Tuple<Control>> findFirst = this.selectionControls.stream()
                .filter(t -> selectionKey.equals(t._2.getData(OPTION_VALUE)))
                .findFirst();
        if (!findFirst.isPresent()) {
            return;
        }

        final Tuple<Control> tuple = findFirst.get();
        final int indexOf = this.selectionControls.indexOf(tuple);
        this.selectionControls.remove(tuple);

        tuple._1.dispose();
        tuple._2.dispose();

        final Tuple<String> value = this.selectedValues.remove(indexOf);
        this.combo.add(value._2, this.combo.getItemCount());

        this.getParent().layout();
        if (this.listener != null) {
            this.listener.handleEvent(event);
        }
    }

    private void adaptColumnWidth(final Event event) {
        try {
            final int currentTableWidth = this.getClientArea().width;
            this.comboCell.widthHint = currentTableWidth - ACTION_COLUMN_WIDTH;
            this.layout();
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

}
