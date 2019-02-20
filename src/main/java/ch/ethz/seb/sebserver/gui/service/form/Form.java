/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.form;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.FormBinding;
import ch.ethz.seb.sebserver.gui.service.widget.ImageUpload;
import ch.ethz.seb.sebserver.gui.service.widget.SingleSelection;

public final class Form implements FormBinding {

    private final JSONMapper jsonMapper;
    private final ObjectNode objectRoot;

    private final Map<String, FormFieldAccessor> formFields = new LinkedHashMap<>();
    private final Map<String, Form> subForms = new LinkedHashMap<>();
    private final Map<String, List<Form>> subLists = new LinkedHashMap<>();
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();

    private final EntityKey entityKey;

    Form(final JSONMapper jsonMapper, final EntityKey entityKey) {
        this.jsonMapper = jsonMapper;
        this.objectRoot = this.jsonMapper.createObjectNode();
        this.entityKey = entityKey;
    }

    @Override
    public EntityKey entityKey() {
        return this.entityKey;
    }

    @Override
    public String getFormAsJson() {
        try {
            flush();
            return this.jsonMapper.writeValueAsString(this.objectRoot);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error while trying to create json form Form post: ", e);
        }
    }

    @Override
    public MultiValueMap<String, String> getFormAsQueryAttributes() {
        final LinkedMultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        for (final Map.Entry<String, FormFieldAccessor> entry : this.formFields.entrySet()) {
            final String value = entry.getValue().getValue();
            if (StringUtils.isNoneBlank(value)) {
                result.add(entry.getKey(), value);
            }
        }

        return result;
    }

    public String getValue(final String name) {
        final FormFieldAccessor formFieldAccessor = this.formFields.get(name);
        if (formFieldAccessor != null) {
            return formFieldAccessor.getValue();
        }

        return null;
    }

    public void putStatic(final String name, final String value) {
        this.objectRoot.put(name, value);
    }

    public void addToGroup(final String groupName, final String fieldName) {
        if (this.formFields.containsKey(fieldName)) {
            this.groups.computeIfAbsent(groupName, k -> new HashSet<>())
                    .add(fieldName);
        }
    }

    public Form putField(final String name, final Label label, final Label field) {
        this.formFields.put(name, createAccessor(label, field));
        return this;
    }

    public Form putField(final String name, final Label label, final Text field) {
        this.formFields.put(name, createAccessor(label, field));
        return this;
    }

    public void putField(final String name, final Label label, final Combo field) {
        if (field instanceof SingleSelection) {
            this.formFields.put(name, createAccessor(label, (SingleSelection) field));
        }
    }

    public void putField(final String name, final Label label, final ImageUpload imageUpload) {
        this.formFields.put(name, createAccessor(label, imageUpload));
    }

    public void putSubForm(final String name, final Form form) {
        this.subForms.put(name, form);
    }

    public Form getSubForm(final String name) {
        return this.subForms.get(name);
    }

    public void addSubForm(final String arrayName, final Form form) {
        final List<Form> array = this.subLists.computeIfAbsent(arrayName, k -> new ArrayList<>());
        array.add(form);
    }

    public Form getSubForm(final String arrayName, final int index) {
        final List<Form> array = this.subLists.get(arrayName);
        if (array == null) {
            return null;
        }

        return array.get(index);
    }

    public void allVisible() {
        process(
                name -> true,
                ffa -> ffa.setVisible(true));
    }

    public void setVisible(final boolean visible, final String group) {
        if (!this.groups.containsKey(group)) {
            return;
        }

        final Set<String> namesSet = this.groups.get(group);
        process(
                name -> namesSet.contains(name),
                ffa -> ffa.setVisible(visible));
    }

    public void process(
            final Predicate<String> nameFilter,
            final Consumer<FormFieldAccessor> processor) {

        this.formFields.entrySet()
                .stream()
                .filter(entity -> nameFilter.test(entity.getKey()))
                .map(entity -> entity.getValue())
                .forEach(processor);
    }

    private void flush() {
        for (final Map.Entry<String, FormFieldAccessor> entry : this.formFields.entrySet()) {
            final FormFieldAccessor accessor = entry.getValue();
            if (accessor.control.isVisible()) {
                this.objectRoot.put(entry.getKey(), accessor.getValue());
            }
        }

        for (final Map.Entry<String, Form> entry : this.subForms.entrySet()) {
            final Form subForm = entry.getValue();
            subForm.flush();
            final ObjectNode objectNode = this.jsonMapper.createObjectNode();
            this.objectRoot.set(entry.getKey(), objectNode);
        }

        for (final Map.Entry<String, List<Form>> entry : this.subLists.entrySet()) {
            final List<Form> value = entry.getValue();
            final ArrayNode arrayNode = this.jsonMapper.createArrayNode();
            final int index = 0;
            for (final Form arrayForm : value) {
                arrayForm.flush();
                arrayNode.insert(index, arrayForm.objectRoot);
            }
            this.objectRoot.set(entry.getKey(), arrayNode);
        }
    }

    //@formatter:off
    private FormFieldAccessor createAccessor(final Label label, final Label field) {
        return  new FormFieldAccessor(label, field) {
            @Override public String getValue() { return field.getText(); }
            @Override public void setValue(final String value) { field.setText(value); }
        };
    }
    private FormFieldAccessor createAccessor(final Label label, final Text text) {
        return new FormFieldAccessor(label, text) {
            @Override public String getValue() { return text.getText(); }
            @Override public void setValue(final String value) { text.setText(value); }
        };
    }
    private FormFieldAccessor createAccessor(
            final Label label,
            final SingleSelection singleSelection) {

        return new FormFieldAccessor(label, singleSelection) {
            @Override public String getValue() { return singleSelection.getSelectionValue(); }
            @Override public void setValue(final String value) { singleSelection.select(value); }
        };
    }

    private FormFieldAccessor createAccessor(final Label label, final ImageUpload imageUpload) {
        return new FormFieldAccessor(label, imageUpload) {
            @Override public String getValue() { return imageUpload.getImageBase64(); }
            @Override public void setValue(final String value) { imageUpload.setImageBase64(value); }
        };
    }
    //@formatter:on

    public static abstract class FormFieldAccessor {

        public final Label label;
        public final Control control;
        private boolean hasError;

        public FormFieldAccessor(final Label label, final Control control) {
            this.label = label;
            this.control = control;
        }

        public abstract String getValue();

        public abstract void setValue(String value);

        public void setVisible(final boolean visible) {
            this.label.setVisible(visible);
            this.control.setVisible(visible);
        }

        public void setError(final String errorTooltip) {
            if (!this.hasError) {
                this.control.setData(RWT.CUSTOM_VARIANT, "error");
                this.control.setToolTipText(errorTooltip);
                this.hasError = true;
            }
        }

        public void resetError() {
            if (this.hasError) {
                this.control.setData(RWT.CUSTOM_VARIANT, null);
                this.control.setToolTipText(null);
                this.hasError = false;
            }
        }
    }

}
