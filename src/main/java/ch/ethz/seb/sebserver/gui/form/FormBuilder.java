/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class FormBuilder {

    public static final int FORM_ROW_HEIGHT = 25;

    private static final Logger log = LoggerFactory.getLogger(FormBuilder.class);

    final Cryptor cryptor;
    final I18nSupport i18nSupport;
    final PageService pageService;
    final WidgetFactory widgetFactory;
    public final PageContext pageContext;
    public final Composite formParent;
    public final Form form;

    boolean readonly = false;
    private int defaultSpanLabel = 2;
    private int defaultSpanInput = 5;
    private int defaultSpanEmptyCell = 1;
    private boolean emptyCellSeparation = true;

    public FormBuilder(
            final PageService pageService,
            final PageContext pageContext,
            final Cryptor cryptor,
            final int rows) {

        this.cryptor = cryptor;
        this.i18nSupport = pageService.getI18nSupport();
        this.pageService = pageService;
        this.widgetFactory = pageService.getWidgetFactory();
        this.pageContext = pageContext;
        this.form = new Form(pageService.getJSONMapper(), cryptor);

        this.formParent = this.widgetFactory.formGrid(
                pageContext.getParent(),
                rows);
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

    public FormBuilder addFieldIf(
            final BooleanSupplier condition,
            final Supplier<FieldBuilder<?>> templateSupplier) {

        if (condition.getAsBoolean()) {
            return addField(templateSupplier.get());
        }

        return this;
    }

    public FormBuilder addField(final FieldBuilder<?> template) {
        template.spanLabel = (template.spanLabel < 0) ? this.defaultSpanLabel : template.spanLabel;
        template.spanInput = (template.spanInput < 0) ? this.defaultSpanInput : template.spanInput;
        template.spanEmptyCell = (template.spanEmptyCell < 0) ? this.defaultSpanEmptyCell : template.spanEmptyCell;
        template.autoEmptyCellSeparation = (template.autoEmptyCellSeparation != null)
                ? template.autoEmptyCellSeparation
                : this.emptyCellSeparation;

        if (template.autoEmptyCellSeparation && this.form.hasFields()) {
            addEmptyCell(template.spanEmptyCell);
        }

        template.build(this);

        if (StringUtils.isNotBlank(template.group)) {
            this.form.addToGroup(template.group, template.name);
        }

        return this;
    }

    public <T extends Entity> FormHandle<T> build() {
        return buildFor(null);
    }

    public <T extends Entity> FormHandle<T> buildFor(final RestCall<T> post) {
        return new FormHandle<>(
                this.pageService,
                this.pageContext,
                this.form,
                post);
    }

    private void empty(final Composite parent, final int hspan, final int vspan) {
        final Label empty = new Label(parent, SWT.LEFT);
        final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false, hspan, vspan);
        gridData.minimumWidth = 0;
        gridData.widthHint = 0;
        gridData.heightHint = 0;
        gridData.minimumHeight = 0;
        empty.setLayoutData(gridData);
    }

    public static CheckboxFieldBuilder checkbox(final String name, final LocTextKey label) {
        return new CheckboxFieldBuilder(name, label, null);
    }

    public static CheckboxFieldBuilder checkbox(final String name, final LocTextKey label, final String value) {
        return new CheckboxFieldBuilder(name, label, value);
    }

    public static TextFieldBuilder text(final String name) {
        return new TextFieldBuilder(name, null, null);
    }

    public static TextFieldBuilder text(final String name, final LocTextKey label) {
        return new TextFieldBuilder(name, label, null);
    }

    public static TextFieldBuilder text(final String name, final LocTextKey label, final String value) {
        return new TextFieldBuilder(name, label, value);
    }

    public static TextFieldBuilder text(
            final String name,
            final LocTextKey label,
            final Supplier<String> valueSupplier) {

        return new TextFieldBuilder(name, label, valueSupplier.get());
    }

    public static PasswordFieldBuilder password(final String name, final LocTextKey label, final CharSequence value) {
        return new PasswordFieldBuilder(name, label, value);
    }

    public static SelectionFieldBuilder singleSelection(
            final String name,
            final LocTextKey label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(Selection.Type.SINGLE, name, label, value, itemsSupplier);
    }

    public static SelectionFieldBuilder multiSelection(
            final String name,
            final LocTextKey label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(Selection.Type.MULTI, name, label, value, itemsSupplier);
    }

    public static SelectionFieldBuilder multiCheckboxSelection(
            final String name,
            final LocTextKey label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(Selection.Type.MULTI_CHECKBOX, name, label, value, itemsSupplier);
    }

    public static SelectionFieldBuilder multiComboSelection(
            final String name,
            final LocTextKey label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return new SelectionFieldBuilder(Selection.Type.MULTI_COMBO, name, label, value, itemsSupplier);
    }

    public static SelectionFieldBuilder colorSelection(
            final String name,
            final LocTextKey label,
            final String value) {

        return new SelectionFieldBuilder(Selection.Type.COLOR, name, label, value, null);
    }

    public static ThresholdListBuilder thresholdList(
            final String name,
            final LocTextKey label,
            final List<Threshold> thresholds) {

        return new ThresholdListBuilder(
                name,
                label,
                thresholds);
    }

    public static ImageUploadFieldBuilder imageUpload(final String name, final LocTextKey label, final String value) {
        return new ImageUploadFieldBuilder(name, label, value);
    }

    public static FileUploadFieldBuilder fileUpload(
            final String name,
            final LocTextKey label,
            final String value,
            final String... supportedFiles) {

        return new FileUploadFieldBuilder(
                name,
                label,
                value,
                (supportedFiles != null) ? Arrays.asList(supportedFiles) : Collections.emptyList());
    }

}
