/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public class ColorSelection extends Composite implements Selection {

    private static final long serialVersionUID = 4775375044147842526L;
    private static final Logger log = LoggerFactory.getLogger(ColorSelection.class);

    private static final int ACTION_COLUMN_WIDTH = 20;
    private final ColorDialog colorDialog;
    private final Composite colorField;
    private RGB selection;

    ColorSelection(final Composite parent, final WidgetFactory widgetFactory) {
        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        this.colorDialog = new ColorDialog(this.getShell(), SWT.NONE);

        this.colorField = new Composite(this, SWT.NONE);
        final GridData colorCell = new GridData(SWT.FILL, SWT.TOP, true, false);
        colorCell.heightHint = 20;
        this.colorField.setLayoutData(colorCell);

        final Label imageButton = widgetFactory.imageButton(
                ImageIcon.COLOR,
                this,
                new LocTextKey("Set Color"),
                this::addColorSelection);

        final GridData actionCell = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        actionCell.widthHint = ACTION_COLUMN_WIDTH;
        imageButton.setLayoutData(actionCell);

        this.addListener(SWT.Resize, this::adaptColumnWidth);
    }

    @Override
    public Type type() {
        return Type.COLOR;
    }

    @Override
    public void applyNewMapping(final List<Tuple<String>> mapping) {
    }

    @Override
    public void select(final String keys) {
        this.selection = parseRGB(keys);
        applySelection();
    }

    @Override
    public String getSelectionValue() {
        return parseColorString(this.selection);
    }

    @Override
    public void clear() {
        this.selection = null;
    }

    private void addColorSelection(final Event event) {
        this.colorDialog.open(code -> {
            if (code == SWT.CANCEL) {
                return;
            }

            this.selection = this.colorDialog.getRGB();
            applySelection();
        });
    }

    private void applySelection() {
        if (this.selection != null) {
            this.colorField.setBackground(new Color(this.getDisplay(), this.selection));
        } else {
            this.colorField.setBackground(null);
        }
    }

    private void adaptColumnWidth(final Event event) {
        try {
            final int currentTableWidth = this.getClientArea().width;
            final GridData comboCell = (GridData) this.colorField.getLayoutData();
            comboCell.widthHint = currentTableWidth - ACTION_COLUMN_WIDTH;
            this.layout();
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

    static String parseColorString(final RGB color) {
        if (color == null) {
            return null;
        }

        return Integer.toHexString(color.red)
                + Integer.toHexString(color.green)
                + Integer.toHexString(color.blue);
    }

    static RGB parseRGB(final String colorString) {
        if (StringUtils.isBlank(colorString)) {
            return null;
        }

        final int r = Integer.parseInt(colorString.substring(0, 2), 16);
        final int g = Integer.parseInt(colorString.substring(2, 4), 16);
        final int b = Integer.parseInt(colorString.substring(4, 6), 16);

        return new RGB(r, g, b);
    }

}
