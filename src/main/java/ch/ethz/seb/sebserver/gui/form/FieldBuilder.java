/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public abstract class FieldBuilder<T> {

    public static final LocTextKey MANDATORY_TEXT_KEY = new LocTextKey("sebserver.form.mandatory");
    public static final String TOOLTIP_KEY_SUFFIX_LABEL = ".tooltip";
    public static final String TOOLTIP_KEY_SUFFIX_LEFT = ".tooltip.left";
    public static final String TOOLTIP_KEY_SUFFIX_RIGHT = ".tooltip.right";

    int spanLabel = -1;
    int spanInput = -1;
    int spanEmptyCell = -1;
    int titleValign = SWT.TOP;
    Boolean autoEmptyCellSeparation = null;
    String group = null;
    boolean readonly = false;
    boolean visible = true;
    String defaultLabel = null;
    boolean isMandatory = false;

    final String name;
    final LocTextKey label;
    final LocTextKey tooltipLabel;
    final LocTextKey tooltipKeyLeft;
    final LocTextKey tooltipKeyRight;
    final T value;

    protected FieldBuilder(final String name, final LocTextKey label, final T value) {
        this.name = name;
        this.label = label;
        this.value = value;
        this.tooltipLabel = (label != null) ? new LocTextKey(label.name + TOOLTIP_KEY_SUFFIX_LABEL) : null;
        this.tooltipKeyLeft = (label != null) ? new LocTextKey(label.name + TOOLTIP_KEY_SUFFIX_LEFT) : null;
        this.tooltipKeyRight = (label != null) ? new LocTextKey(label.name + TOOLTIP_KEY_SUFFIX_RIGHT) : null;
    }

    public FieldBuilder<T> withDefaultLabel(final String defaultLabel) {
        this.defaultLabel = defaultLabel;
        return this;
    }

    public FieldBuilder<T> withLabelSpan(final int span) {
        this.spanLabel = span;
        return this;
    }

    public FieldBuilder<T> mandatory() {
        this.isMandatory = true;
        return this;
    }

    public FieldBuilder<T> mandatory(final boolean mandatory) {
        this.isMandatory = mandatory;
        return this;
    }

    public FieldBuilder<T> withInputSpan(final int span) {
        this.spanInput = span;
        return this;
    }

    public FieldBuilder<T> withEmptyCellSpan(final int span) {
        this.spanEmptyCell = span;
        return this;
    }

    public FieldBuilder<T> withEmptyCellSeparation(final boolean separation) {
        this.autoEmptyCellSeparation = separation;
        return this;
    }

    public FieldBuilder<T> withGroup(final String group) {
        this.group = group;
        return this;
    }

    public FieldBuilder<T> readonly(final boolean readonly) {
        this.readonly = readonly;
        return this;
    }

    public FieldBuilder<T> visibleIf(final boolean visible) {
        this.visible = visible;
        return this;
    }

    public FieldBuilder<T> readonlyIf(final BooleanSupplier readonly) {
        this.readonly = readonly != null && readonly.getAsBoolean();
        return this;
    }

    abstract void build(FormBuilder builder);

    protected static Control createTitleLabel(
            final Composite parent,
            final FormBuilder builder,
            final FieldBuilder<?> fieldBuilder) {

        if (fieldBuilder.label == null) {
            return null;
        }

        final Composite infoGrid = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(4, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginRight = 0;
        infoGrid.setLayout(gridLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = (fieldBuilder.spanLabel > 0) ? fieldBuilder.spanLabel : 1;
        infoGrid.setLayoutData(gridData);

        if (fieldBuilder.tooltipKeyLeft != null &&
                StringUtils.isNotBlank(builder.i18nSupport.getText(fieldBuilder.tooltipKeyLeft, ""))) {

            final Label info = builder.widgetFactory.imageButton(
                    WidgetFactory.ImageIcon.HELP,
                    infoGrid,
                    fieldBuilder.tooltipKeyLeft);
            info.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }

        final boolean hasLabelTooltip = (fieldBuilder.tooltipLabel != null &&
                StringUtils.isNotBlank(builder.i18nSupport.getText(fieldBuilder.tooltipLabel, "")));

        final Label label = labelLocalized(
                builder.widgetFactory,
                infoGrid,
                fieldBuilder.label,
                fieldBuilder.defaultLabel,
                (hasLabelTooltip) ? fieldBuilder.tooltipLabel : null,
                1,
                fieldBuilder.titleValign);

        if (fieldBuilder.isMandatory) {
            final Label mandatory = builder.widgetFactory.imageButton(
                    WidgetFactory.ImageIcon.MANDATORY,
                    infoGrid,
                    MANDATORY_TEXT_KEY);
            mandatory.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }

        if (fieldBuilder.tooltipKeyRight != null &&
                StringUtils.isNotBlank(builder.i18nSupport.getText(fieldBuilder.tooltipKeyRight, ""))) {

            final Label info = builder.widgetFactory.imageButton(
                    WidgetFactory.ImageIcon.HELP,
                    infoGrid,
                    fieldBuilder.tooltipKeyRight);
            info.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }

        return infoGrid;
    }

    public static Label labelLocalized(
            final WidgetFactory widgetFactory,
            final Composite parent,
            final LocTextKey locTextKey,
            final String defaultText,
            final int hspan) {

        return labelLocalized(widgetFactory, parent, locTextKey, defaultText, null, hspan, SWT.CENTER);
    }

    public static Label labelLocalized(
            final WidgetFactory widgetFactory,
            final Composite parent,
            final LocTextKey locTextKey,
            final String defaultText,
            final LocTextKey tooltipTextKey,
            final int hspan,
            final int verticalAlignment) {

        final LocTextKey labelKey = StringUtils.isNotBlank(defaultText)
                ? new LocTextKey(defaultText)
                : locTextKey;

        final Label label = widgetFactory.labelLocalized(
                parent,
                labelKey,
                tooltipTextKey);
        final GridData gridData = new GridData(SWT.LEFT, verticalAlignment, false, false, hspan, 1);
        gridData.heightHint = FormBuilder.FORM_ROW_HEIGHT;
        label.setLayoutData(gridData);
        label.setData(RWT.CUSTOM_VARIANT, CustomVariant.TITLE_LABEL.key);
        return label;
    }

    public static Composite createFieldGrid(final Composite parent, final int hspan) {
        final Composite fieldGrid = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginRight = 0;
        fieldGrid.setLayout(gridLayout);

        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = hspan;
        fieldGrid.setLayoutData(gridData);

        return fieldGrid;
    }

    public static Label createErrorLabel(final Composite innerGrid) {
        final Label errorLabel = new Label(innerGrid, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        errorLabel.setLayoutData(gridData);
        errorLabel.setVisible(false);
        errorLabel.setData(RWT.CUSTOM_VARIANT, CustomVariant.ERROR.key);
        return errorLabel;
    }

}