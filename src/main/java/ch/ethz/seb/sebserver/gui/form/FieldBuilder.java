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
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public abstract class FieldBuilder<T> {
    int spanLabel = -1;
    int spanInput = -1;
    int spanEmptyCell = -1;
    int titleValign = SWT.TOP;
    Boolean autoEmptyCellSeparation = null;
    String group = null;
    boolean readonly = false;
    boolean visible = true;
    String defaultLabel = null;
    LocTextKey infoText;
    boolean infoLeft = false;
    boolean infoRight = false;

    final String name;
    final LocTextKey label;
    final T value;

    protected FieldBuilder(final String name, final LocTextKey label, final T value) {
        this.name = name;
        this.label = label;
        this.value = value;
    }

    public FieldBuilder<T> withDefaultLabel(final String defaultLabel) {
        this.defaultLabel = defaultLabel;
        return this;
    }

    public FieldBuilder<T> withLabelSpan(final int span) {
        this.spanLabel = span;
        return this;
    }

    public FieldBuilder<T> withInfoLeft(final LocTextKey infoText) {
        this.infoText = infoText;
        this.infoLeft = true;
        return this;
    }

    public FieldBuilder<T> withInfoRight(final LocTextKey infoText) {
        this.infoText = infoText;
        this.infoRight = true;
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

    protected static Label createTitleLabel(
            final Composite parent,
            final FormBuilder builder,
            final FieldBuilder<?> fieldBuilder) {

        final Composite infoGrid = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginRight = 0;
        infoGrid.setLayout(gridLayout);

        final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        infoGrid.setLayoutData(gridData);

        if (fieldBuilder.infoText != null && fieldBuilder.infoLeft) {
            final Label info = builder.widgetFactory.imageButton(
                    WidgetFactory.ImageIcon.HELP,
                    infoGrid,
                    fieldBuilder.infoText);
            info.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }
        final Label lab = (fieldBuilder.label != null)
                ? labelLocalized(
                        builder.widgetFactory,
                        infoGrid,
                        fieldBuilder.label,
                        fieldBuilder.defaultLabel,
                        (fieldBuilder.spanLabel > 0) ? fieldBuilder.spanLabel : 1,
                        fieldBuilder.titleValign)
                : null;

        if (fieldBuilder.infoText != null && fieldBuilder.infoRight) {
            final Label info = builder.widgetFactory.imageButton(
                    WidgetFactory.ImageIcon.HELP,
                    infoGrid,
                    fieldBuilder.infoText);
            info.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }

        return lab;
    }

    public static Label labelLocalized(
            final WidgetFactory widgetFactory,
            final Composite parent,
            final LocTextKey locTextKey,
            final String defaultText,
            final int hspan) {

        return labelLocalized(widgetFactory, parent, locTextKey, defaultText, hspan, SWT.CENTER);
    }

    public static final Label labelLocalized(
            final WidgetFactory widgetFactory,
            final Composite parent,
            final LocTextKey locTextKey,
            final String defaultText,
            final int hspan,
            final int verticalAlignment) {

        final Label label = widgetFactory.labelLocalized(
                parent,
                locTextKey,
                (StringUtils.isNotBlank(defaultText) ? defaultText : locTextKey.name));
        final GridData gridData = new GridData(SWT.LEFT, verticalAlignment, true, true, hspan, 1);
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