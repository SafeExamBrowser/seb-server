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
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.widgets.DropDown;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.page.PageService;

public final class MultiSelectionCombo extends Composite implements Selection {

    private static final Logger log = LoggerFactory.getLogger(MultiSelectionCombo.class);
    private static final long serialVersionUID = -7787134114963647332L;

    private final WidgetFactory widgetFactory;

    private final List<Control> selectionControls = new ArrayList<>();

    private final List<Tuple<String>> valueMapping = new ArrayList<>();
    private final List<Tuple<String>> availableValues = new ArrayList<>();
    private final List<Tuple<String>> selectedValues = new ArrayList<>();

    private final DropDown dropDown;
    private final Text textInput;
    private final GridData textCell;
    private final Composite updateAnchor;

    private Listener listener = null;

    MultiSelectionCombo(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final String locTextPrefix,
            final Composite updateAnchor) {

        super(parent, SWT.NONE);
        this.widgetFactory = widgetFactory;

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
        this.dropDown.addListener(SWT.Selection, event -> {
            final int selectionIndex = this.dropDown.getSelectionIndex();
            if (selectionIndex >= 0) {
                final String selectedItem = this.dropDown.getItems()[selectionIndex];
                addSelection(itemForName(selectedItem));
            }
        });

        this.updateAnchor = updateAnchor;
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
        this.selectionControls
                .forEach(Control::dispose);
        this.selectionControls.clear();
        this.availableValues.clear();
        this.availableValues.addAll(this.valueMapping);
    }

    private void addSelection(final Tuple<String> item) {
        if (item == null) {
            return;
        }

        this.selectedValues.add(item);
        final Label label = this.widgetFactory.label(this, item._2);
        label.setData(OPTION_VALUE, item._2);
        final GridData textCell = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        label.setLayoutData(textCell);
        label.addListener(SWT.MouseDoubleClick, this::removeComboSelection);
        this.selectionControls.add(label);

        this.availableValues.remove(item);
        PageService.updateScrolledComposite(this);
        this.updateAnchor.layout(true, true);
    }

    private void removeComboSelection(final Event event) {
        if (event.widget == null) {
            return;
        }

        final String selectionKey = (String) event.widget.getData(OPTION_VALUE);
        final Optional<Control> findFirst = this.selectionControls.stream()
                .filter(t -> selectionKey.equals(t.getData(OPTION_VALUE)))
                .findFirst();
        if (!findFirst.isPresent()) {
            return;
        }

        final Control control = findFirst.get();
        final int indexOf = this.selectionControls.indexOf(control);
        this.selectionControls.remove(control);
        control.dispose();

        final Tuple<String> value = this.selectedValues.remove(indexOf);
        this.availableValues.add(value);

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

}
