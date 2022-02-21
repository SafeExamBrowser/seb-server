/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.ImageIcon;

public final class ColorSelection extends Composite implements Selection {

    private static final long serialVersionUID = 4775375044147842526L;
    private static final Logger log = LoggerFactory.getLogger(ColorSelection.class);

    private static final LocTextKey DEFAULT_SELECT_TOOLTIP_KEY = new LocTextKey("sebserver.overall.action.select");

    private static final int ACTION_COLUMN_WIDTH = 20;
    private static final int ACTION_COLUMN_ADJUST = 10;
    private final ColorDialog colorDialog;
    private final Composite colorField;
    private final Label colorLabel;
    private final I18nSupport i18nSupport;
    private RGB selection;

    private Listener listener = null;

    ColorSelection(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final String tooltipKeyPrefix) {

        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 1;
        gridLayout.marginLeft = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        setLayout(gridLayout);

        this.i18nSupport = widgetFactory.getI18nSupport();
        this.colorDialog = widgetFactory.getColorDialog(this);

        this.colorField = new Composite(this, SWT.NONE);
        final GridData colorCell = new GridData(SWT.FILL, SWT.TOP, true, false);
        colorCell.heightHint = 20;
        this.colorField.setLayoutData(colorCell);
        final GridLayout colorCallLayout = new GridLayout();
        colorCallLayout.horizontalSpacing = 5;
        colorCallLayout.verticalSpacing = 0;
        colorCallLayout.marginHeight = 0;
        colorCallLayout.marginTop = 2;
        this.colorField.setLayout(colorCallLayout);
        this.colorLabel = new Label(this.colorField, SWT.NONE);
        final GridData gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false);
        gridData.minimumWidth = 50;
        this.colorLabel.setLayoutData(gridData);
        this.colorLabel.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIGHT_COLOR_LABEL.key);

        final Button imageButton = widgetFactory.imageButton(
                ImageIcon.COLOR,
                this,
                (StringUtils.isNotBlank(tooltipKeyPrefix)
                        ? new LocTextKey(tooltipKeyPrefix)
                        : DEFAULT_SELECT_TOOLTIP_KEY),
                this::addColorSelection);

        final GridData actionCell = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        actionCell.widthHint = ACTION_COLUMN_WIDTH;
        imageButton.setLayoutData(actionCell);

        if (tooltipKeyPrefix != null) {
            WidgetFactory.setTestId(this, tooltipKeyPrefix);
        }

        this.addListener(SWT.Resize, this::adaptColumnWidth);
    }

    @Override
    public void setSelectionListener(final Listener listener) {
        this.listener = listener;
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
        this.selection = Utils.parseRGB(keys);
        applySelection();
    }

    @Override
    public String getSelectionValue() {
        return Utils.parseColorString(this.selection);
    }

    @Override
    public void clear() {
        this.selection = null;
    }

    @Override
    public void setAriaLabel(final String label) {
        WidgetFactory.setARIALabel(this, label);
    }

    private void addColorSelection(final Event event) {
        final Locale locale = RWT.getLocale();
        RWT.setLocale(this.i18nSupport.getUsersLanguageLocale());
        this.colorDialog.setRGB(this.selection);
        this.colorDialog.open(code -> {
            if (code == SWT.CANCEL) {
                return;
            }

            this.selection = this.colorDialog.getRGB();
            applySelection();
            if (this.listener != null) {
                this.listener.handleEvent(event);
            }
        });

        RWT.setLocale(locale);
    }

    private void applySelection() {
        if (this.selection != null) {
            this.colorField.setBackground(new Color(this.getDisplay(), this.selection));
            this.colorLabel.setText(Utils.parseColorString(this.selection));
            this.colorLabel.setData(RWT.CUSTOM_VARIANT, (Utils.darkColorContrast(this.selection))
                    ? CustomVariant.DARK_COLOR_LABEL.key
                    : CustomVariant.LIGHT_COLOR_LABEL.key);
        } else {
            this.colorField.setBackground(null);
            this.colorLabel.setText(StringUtils.EMPTY);
        }

        this.colorField.layout(true, true);
    }

    private void adaptColumnWidth(final Event event) {
        try {
            final int currentTableWidth = this.getClientArea().width;
            final GridData comboCell = (GridData) this.colorField.getLayoutData();
            comboCell.widthHint = currentTableWidth - ACTION_COLUMN_WIDTH - ACTION_COLUMN_ADJUST;
            this.layout();
        } catch (final Exception e) {
            log.warn("Failed to adaptColumnWidth: ", e);
        }
    }

}
