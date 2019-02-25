/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class FormBuilder {

    private static final Logger log = LoggerFactory.getLogger(FormBuilder.class);

    final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;
    public final PageContext pageContext;
    public final Composite formParent;
    public final Form form;

    boolean readonly = false;
    private int defaultSpanLabel = 1;
    private int defaultSpanInput = 2;
    private int defaultSpanEmptyCell = 1;
    private boolean emptyCellSeparation = true;

    FormBuilder(
            final EntityKey entityKey,
            final JSONMapper jsonMapper,
            final WidgetFactory widgetFactory,
            final PolyglotPageService polyglotPageService,
            final PageContext pageContext,
            final int rows) {

        this.widgetFactory = widgetFactory;
        this.polyglotPageService = polyglotPageService;
        this.pageContext = pageContext;
        this.form = new Form(jsonMapper, entityKey);

        this.formParent = new Composite(pageContext.getParent(), SWT.NONE);
        final GridLayout layout = new GridLayout(rows, true);
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 10;
        layout.marginLeft = 10;
        layout.marginTop = 10;
        this.formParent.setLayout(layout);
        this.formParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    public FormBuilder readonly(final boolean readonly) {
        this.readonly = readonly;
        return this;
    }

    public FormBuilder setVisible(final boolean visible, final String group) {
        this.form.setVisible(visible, group);
        return this;
    }

    public void setFieldVisible(final boolean visible, final String fieldName) {
        this.form.setFieldVisible(visible, fieldName);

    }

    public FormBuilder setControl(final TabItem instTab) {
        instTab.setControl(this.formParent);
        return this;
    }

    public FormBuilder withDefaultSpanLabel(final int span) {
        this.defaultSpanLabel = span;
        return this;
    }

    public FormBuilder withDefaultSpanInput(final int span) {
        this.defaultSpanInput = span;
        return this;
    }

    public FormBuilder withDefaultSpanEmptyCell(final int span) {
        this.defaultSpanEmptyCell = span;
        return this;
    }

    public FormBuilder withEmptyCellSeparation(final boolean separation) {
        this.emptyCellSeparation = separation;
        return this;
    }

    public FormBuilder addEmptyCellIf(final BooleanSupplier condition) {
        if (condition != null && condition.getAsBoolean()) {
            return addEmptyCell();
        }
        return this;
    }

    public FormBuilder addEmptyCell() {
        return addEmptyCell(1);
    }

    public FormBuilder addEmptyCell(final int span) {
        empty(this.formParent, span, 1);
        return this;
    }

    public FormBuilder putStaticValueIf(final BooleanSupplier condition, final String name, final String value) {
        if (condition != null && condition.getAsBoolean()) {
            return putStaticValue(name, value);
        }

        return this;
    }

    public FormBuilder putStaticValue(final String name, final String value) {
        try {
            this.form.putStatic(name, value);
        } catch (final Exception e) {
            log.error("Failed to put static field value to json object: ", e);
        }
        return this;
    }

    public FormBuilder addField(final FieldBuilder template) {
        if (template.condition == null || template.condition.getAsBoolean()) {
            template.spanLabel = (template.spanLabel < 0) ? this.defaultSpanLabel : template.spanLabel;
            template.spanInput = (template.spanInput < 0) ? this.defaultSpanInput : template.spanInput;
            template.spanEmptyCell = (template.spanEmptyCell < 0) ? this.defaultSpanEmptyCell : template.spanEmptyCell;
            template.autoEmptyCellSeparation = template.autoEmptyCellSeparation || this.emptyCellSeparation;

            if (template.autoEmptyCellSeparation && this.form.hasFields()) {
                addEmptyCell(template.spanEmptyCell);
            }

            template.build(this);

            if (StringUtils.isNoneBlank(template.group)) {
                this.form.addToGroup(template.group, template.name);
            }
        }
        return this;
    }

    public <T extends Entity> FormHandle<T> buildFor(
            final RestCall<T> post) {

        return new FormHandle<>(
                this.pageContext,
                this.form,
                post,
                this.polyglotPageService.getI18nSupport());
    }

    private void empty(final Composite parent, final int hspan, final int vspan) {
        final Label empty = new Label(parent, SWT.LEFT);
        empty.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan));
        empty.setText("");
    }

    public static TextFieldBuilder text(final String name, final String label) {
        return new TextFieldBuilder(name, label, null);
    }

    public static TextFieldBuilder text(final String name, final String label, final String value) {
        return new TextFieldBuilder(name, label, value);
    }

    public static SelectionFieldBuilder singleSelection(
            final String name,
            final String label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(name, label, value, itemsSupplier);
    }

    public static SelectionFieldBuilder multiSelection(
            final String name,
            final String label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(name, label, value, itemsSupplier)
                .asMultiSelection();
    }

    public static ImageUploadFieldBuilder imageUpload(final String name, final String label, final String value) {
        return new ImageUploadFieldBuilder(name, label, value);
    }

    Label labelLocalized(final Composite parent, final String locTextKey, final int hspan) {
        final Label label = this.widgetFactory.labelLocalized(parent, locTextKey);
        final GridData gridData = new GridData(SWT.RIGHT, SWT.TOP, true, false, hspan, 1);
        gridData.verticalIndent = 4;
        label.setLayoutData(gridData);
        label.setData(RWT.CUSTOM_VARIANT, "head");
        return label;
    }

    Label valueLabel(final Composite parent, final String value, final int hspan) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText((StringUtils.isNoneBlank(value)) ? value : Constants.EMPTY_NOTE);
        final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, hspan, 1);
        gridData.verticalIndent = 4;
        label.setLayoutData(gridData);
        return label;
    }

}
