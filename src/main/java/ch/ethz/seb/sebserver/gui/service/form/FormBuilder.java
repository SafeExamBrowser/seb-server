/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.form;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.widget.ImageUpload;
import ch.ethz.seb.sebserver.gui.service.widget.SingleSelection;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

public class FormBuilder {

    private static final Logger log = LoggerFactory.getLogger(FormBuilder.class);

    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;
    public final PageContext pageContext;
    public final Composite formParent;
    public final Form form;

    private boolean readonly = false;
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

    public FormBuilder addField(final FieldTemplate template) {
        if (template.condition == null || template.condition.getAsBoolean()) {
            template.spanLabel = (template.spanLabel < 0) ? this.defaultSpanLabel : template.spanLabel;
            template.spanInput = (template.spanInput < 0) ? this.defaultSpanInput : template.spanInput;
            template.spanEmptyCell = (template.spanEmptyCell < 0) ? this.defaultSpanEmptyCell : template.spanEmptyCell;
            template.autoEmptyCellSeparation = template.autoEmptyCellSeparation || this.emptyCellSeparation;
            template.build(this);
        }
        return this;
    }

    public <T> FormHandle<T> buildFor(
            final RestCall<T> post,
            final Function<T, T> postPostHandle) {

        return new FormHandle<>(
                this.pageContext,
                this.form,
                post,
                (postPostHandle == null) ? Function.identity() : postPostHandle,
                this.polyglotPageService.getI18nSupport());
    }

    private void empty(final Composite parent, final int hspan, final int vspan) {
        final Label empty = new Label(parent, SWT.LEFT);
        empty.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan));
        empty.setText("");
    }

    public static TextField text(final String name, final String label, final String value) {
        return new TextField(name, label, value);
    }

    public static SingleSelectionField singleSelection(
            final String name,
            final String label,
            final String value,
            final Supplier<List<Tuple<String>>> itemsSupplier) {
        return new SingleSelectionField(name, label, value, itemsSupplier);
    }

    public static ImageUploadField imageUpload(final String name, final String label, final String value) {
        return new ImageUploadField(name, label, value);
    }

    abstract static class FieldTemplate {
        int spanLabel = -1;
        int spanInput = -1;
        int spanEmptyCell = -1;
        boolean autoEmptyCellSeparation = false;
        String group = null;
        BooleanSupplier condition = null;

        final String name;
        final String label;
        final String value;

        protected FieldTemplate(final String name, final String label, final String value) {
            this.name = name;
            this.label = label;
            this.value = value;
        }

        public FieldTemplate withLabelSpan(final int span) {
            this.spanLabel = span;
            return this;
        }

        public FieldTemplate withInputSpan(final int span) {
            this.spanInput = span;
            return this;
        }

        public FieldTemplate withEmptyCellSpan(final int span) {
            this.spanEmptyCell = span;
            return this;
        }

        public FieldTemplate withEmptyCellSeparation(final boolean separation) {
            this.autoEmptyCellSeparation = separation;
            return this;
        }

        public FieldTemplate withGroup(final String group) {
            this.group = group;
            return this;
        }

        public FieldTemplate withCondition(final BooleanSupplier condition) {
            this.condition = condition;
            return this;
        }

        abstract void build(FormBuilder builder);

    }

    public static final class TextField extends FieldTemplate {

        boolean isPassword = false;

        TextField(final String name, final String label, final String value) {
            super(name, label, value);
        }

        public TextField asPasswordField() {
            this.isPassword = true;
            return this;
        }

        @Override
        void build(final FormBuilder builder) {
            if (this.isPassword && builder.readonly) {
                return;
            }

            if (this.autoEmptyCellSeparation && builder.form.hasFields()) {
                builder.addEmptyCell(this.spanEmptyCell);
            }

            final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
            if (builder.readonly) {
                builder.form.putField(this.name, lab,
                        builder.valueLabel(builder.formParent, this.value, this.spanInput));
            } else {
                final Text textInput = new Text(builder.formParent, (this.isPassword)
                        ? SWT.LEFT | SWT.BORDER | SWT.PASSWORD
                        : SWT.LEFT | SWT.BORDER);
                final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
                gridData.heightHint = 15;
                textInput.setLayoutData(gridData);
                if (this.value != null) {
                    textInput.setText(this.value);
                }
                builder.form.putField(this.name, lab, textInput);
            }
            if (StringUtils.isNoneBlank(this.group)) {
                builder.form.addToGroup(this.group, this.name);
            }
        }
    }

    public static final class SingleSelectionField extends FieldTemplate {

        final Supplier<List<Tuple<String>>> itemsSupplier;
        boolean isLocalizationSupplied = false;
        Consumer<Form> selectionListener = null;

        SingleSelectionField(
                final String name,
                final String label,
                final String value,
                final Supplier<List<Tuple<String>>> itemsSupplier) {

            super(name, label, value);
            this.itemsSupplier = itemsSupplier;
        }

        public SingleSelectionField withLocalizationSupplied() {
            this.isLocalizationSupplied = true;
            return this;
        }

        public SingleSelectionField withSelectionListener(final Consumer<Form> selectionListener) {
            this.selectionListener = selectionListener;
            return this;
        }

        @Override
        void build(final FormBuilder builder) {

            if (this.autoEmptyCellSeparation && builder.form.hasFields()) {
                builder.addEmptyCell(this.spanEmptyCell);
            }

            final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
            if (builder.readonly) {
                builder.form.putField(
                        this.name, lab,
                        builder.valueLabel(builder.formParent, this.value, this.spanInput));
            } else {
                final SingleSelection selection = (this.isLocalizationSupplied)
                        ? builder.widgetFactory.singleSelectionLocalizedSupplier(
                                builder.formParent,
                                this.itemsSupplier)
                        : builder.widgetFactory.singleSelectionLocalized(
                                builder.formParent,
                                this.itemsSupplier.get());
                final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
                gridData.heightHint = 25;
                selection.setLayoutData(gridData);
                selection.select(this.value);
                builder.form.putField(this.name, lab, selection);
                if (this.selectionListener != null) {
                    selection.addListener(SWT.Selection, e -> {
                        this.selectionListener.accept(builder.form);
                    });
                }
            }
            if (StringUtils.isNoneBlank(this.group)) {
                builder.form.addToGroup(this.group, this.name);
            }
        }
    }

    public static final class ImageUploadField extends FieldTemplate {

        ImageUploadField(final String name, final String label, final String value) {
            super(name, label, value);
        }

        @Override
        void build(final FormBuilder builder) {

            if (this.autoEmptyCellSeparation && builder.form.hasFields()) {
                builder.addEmptyCell(this.spanEmptyCell);
            }

            final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
            final ImageUpload imageUpload = builder.widgetFactory.formImageUpload(
                    builder.formParent,
                    this.value,
                    new LocTextKey("sebserver.overall.upload"),
                    this.spanInput, 1, builder.readonly);
            builder.form.putField(this.name, lab, imageUpload);
        }

    }

    private Label labelLocalized(final Composite parent, final String locTextKey, final int hspan) {
        final Label label = this.widgetFactory.labelLocalized(parent, locTextKey);
        final GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, hspan, 1);
        label.setLayoutData(gridData);
        return label;
    }

    private Label valueLabel(final Composite parent, final String value, final int hspan) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText((StringUtils.isNoneBlank(value)) ? value : Constants.EMPTY_NOTE);
        final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, hspan, 1);
        label.setLayoutData(gridData);
        return label;
    }

}
