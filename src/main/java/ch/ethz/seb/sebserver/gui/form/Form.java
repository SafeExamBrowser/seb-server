/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.FormBinding;
import ch.ethz.seb.sebserver.gui.widget.ImageUpload;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.ThresholdList;

public final class Form implements FormBinding {

    private final JSONMapper jsonMapper;
    private final ObjectNode objectRoot;

    private final Map<String, String> staticValues = new LinkedHashMap<>();
    private final MultiValueMap<String, FormFieldAccessor> formFields = new LinkedMultiValueMap<>();
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();

    Form(final JSONMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.objectRoot = this.jsonMapper.createObjectNode();
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
    public String getFormUrlEncoded() {
        final StringBuffer buffer = new StringBuffer();
        for (final Map.Entry<String, String> entry : this.staticValues.entrySet()) {
            appendFormUrlEncodedValue(buffer, entry.getKey(), entry.getValue());
        }

        for (final Map.Entry<String, List<FormFieldAccessor>> entry : this.formFields.entrySet()) {
            entry.getValue()
                    .stream()
                    .forEach(ffa -> appendFormUrlEncodedValue(buffer, entry.getKey(), ffa.getStringValue()));
        }

        return buffer.toString();
    }

    public void putStatic(final String name, final String value) {
        if (StringUtils.isNoneBlank(value)) {
            this.staticValues.put(name, value);
        }
    }

    public void addToGroup(final String groupName, final String fieldName) {
        if (this.formFields.containsKey(fieldName)) {
            this.groups.computeIfAbsent(groupName, k -> new HashSet<>())
                    .add(fieldName);
        }
    }

    public boolean hasFields() {
        return !this.formFields.isEmpty();
    }

    public Form putField(final String name, final Label label, final Label field) {
        this.formFields.add(name, createAccessor(label, field));
        return this;
    }

    public Form putField(final String name, final Label label, final Text field) {
        this.formFields.add(name, createAccessor(label, field));
        return this;
    }

    public void putField(final String name, final Label label, final Selection field) {
        this.formFields.add(name, createAccessor(label, field));
    }

    public void putField(final String name, final Label label, final ThresholdList field) {
        this.formFields.add(name, createAccessor(label, field));
    }

    public void putField(
            final String name,
            final Label label,
            final Selection field,
            final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter) {

        this.formFields.add(name, createAccessor(label, field, jsonValueAdapter));
    }

    public void putField(final String name, final Label label, final ImageUpload imageUpload) {
        this.formFields.add(name, createAccessor(label, imageUpload));
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

    public void setFieldVisible(final boolean visible, final String fieldName) {
        final List<FormFieldAccessor> list = this.formFields.get(fieldName);
        if (list != null) {
            list.stream().forEach(ffa -> ffa.setVisible(visible));
        }
    }

    public boolean hasAnyError() {
        return this.formFields.entrySet()
                .stream()
                .flatMap(entity -> entity.getValue().stream())
                .filter(a -> a.hasError)
                .findFirst()
                .isPresent();
    }

    public void process(
            final Predicate<String> nameFilter,
            final Consumer<FormFieldAccessor> processor) {

        this.formFields.entrySet()
                .stream()
                .filter(entity -> nameFilter.test(entity.getKey()))
                .flatMap(entity -> entity.getValue().stream())
                .forEach(processor);
    }

    private void flush() {
        for (final Map.Entry<String, String> entry : this.staticValues.entrySet()) {
            final String value = entry.getValue();
            if (StringUtils.isNoneBlank(value)) {
                this.objectRoot.put(entry.getKey(), value);
            }
        }

        for (final Map.Entry<String, List<FormFieldAccessor>> entry : this.formFields.entrySet()) {
            entry.getValue()
                    .stream()
                    .filter(ffa -> StringUtils.isNoneBlank(ffa.getStringValue()))
                    .forEach(ffa -> ffa.putJsonValue(entry.getKey(), this.objectRoot));
        }
    }

    // following are FormFieldAccessor implementations for all field types
    //@formatter:off
    private FormFieldAccessor createAccessor(final Label label, final Label field) {
        return new FormFieldAccessor(label, field) {
            @Override public String getStringValue() { return null; }
        };
    }
    private FormFieldAccessor createAccessor(final Label label, final Text text) {
        return new FormFieldAccessor(label, text) {
            @Override public String getStringValue() { return text.getText(); }
        };
    }
    private FormFieldAccessor createAccessor(final Label label, final Selection selection) {
        switch (selection.type()) {
            case MULTI:
            case MULTI_COMBO:
                return createAccessor(label, selection, Form::adaptCommaSeparatedStringToJsonArray);
            default : return createAccessor(label, selection, null);
        }
    }
    private FormFieldAccessor createAccessor(
            final Label label,
            final Selection selection,
            final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter) {

        return new FormFieldAccessor(label, selection.adaptToControl(), jsonValueAdapter) {
            @Override public String getStringValue() { return selection.getSelectionValue(); }
        };
    }
    private FormFieldAccessor createAccessor(final Label label, final ThresholdList thresholdList) {
        return new FormFieldAccessor(label, thresholdList) {
            @Override public String getStringValue() {
                return ThresholdListBuilder
                        .thresholdsToFormURLEncodedStringValue(thresholdList.getThresholds());
            }
            @Override
            public void putJsonValue(final String key, final ObjectNode objectRoot) {
                final Collection<Threshold> thresholds = thresholdList.getThresholds();
                if (thresholds == null || thresholds.isEmpty()) {
                    return;
                }

                final ArrayNode array = Form.this.jsonMapper.valueToTree(thresholds);
                objectRoot.putArray(key).addAll(array);
            }
        };
    }
    private FormFieldAccessor createAccessor(final Label label, final ImageUpload imageUpload) {
        return new FormFieldAccessor(label, imageUpload) {
            @Override public String getStringValue() { return imageUpload.getImageBase64(); }
        };
    }
    //@formatter:on

    /*
     * Adds the given name and value in from URL encoded format to the given StringBuffer.
     * Checks first if the value String is a comma separated list. If true, splits values
     * and adds every value within the same name mapping to the string buffer
     */
    private static void appendFormUrlEncodedValue(final StringBuffer buffer, final String name, final String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        final String[] split = StringUtils.split(value, Constants.LIST_SEPARATOR_CHAR);
        for (int i = 0; i < split.length; i++) {
            if (StringUtils.isBlank(split[i])) {
                continue;
            }

            if (buffer.length() > 0) {
                buffer.append(Constants.FORM_URL_ENCODED_SEPARATOR);
            }

            // check of the string value is a name-value pair. If true, use the specified name an value
            // otherwise use the general name given within this method call and
            if (split[i].contains(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)) {
                final String[] nameValue = StringUtils.split(split[i], Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                buffer.append(nameValue[0])
                        .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                        .append(Utils.encodeFormURL_UTF_8(nameValue[1]));
            } else {
                buffer.append(name)
                        .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                        .append(Utils.encodeFormURL_UTF_8(split[i]));
            }
        }
    }

    private static final void adaptCommaSeparatedStringToJsonArray(
            final Tuple<String> tuple,
            final ObjectNode jsonNode) {
        if (StringUtils.isNoneBlank(tuple._2)) {
            final ArrayNode arrayNode = jsonNode.putArray(tuple._1);
            final String[] split = StringUtils.split(tuple._2, Constants.LIST_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                arrayNode.add(split[i]);
            }
        }
    }

    public static abstract class FormFieldAccessor {

        public final Label label;
        public final Control control;
        private final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter;
        private boolean hasError;

        FormFieldAccessor(final Label label, final Control control) {
            this(label, control, null);
        }

        FormFieldAccessor(
                final Label label,
                final Control control,
                final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter) {

            this.label = label;
            this.control = control;
            if (jsonValueAdapter != null) {
                this.jsonValueAdapter = jsonValueAdapter;
            } else {
                this.jsonValueAdapter = (tuple, jsonObject) -> {
                    if (StringUtils.isNoneBlank(tuple._2)) {
                        jsonObject.put(tuple._1, tuple._2);
                    }
                };
            }
        }

        public abstract String getStringValue();

        public void setVisible(final boolean visible) {
            this.label.setVisible(visible);
            this.control.setVisible(visible);
        }

        public void putJsonValue(final String key, final ObjectNode objectRoot) {
            this.jsonValueAdapter.accept(new Tuple<>(key, getStringValue()), objectRoot);
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
