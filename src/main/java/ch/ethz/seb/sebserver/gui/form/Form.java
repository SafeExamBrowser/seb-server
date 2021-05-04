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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
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
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.FormBinding;
import ch.ethz.seb.sebserver.gui.widget.FileUploadSelection;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;
import ch.ethz.seb.sebserver.gui.widget.PasswordInput;
import ch.ethz.seb.sebserver.gui.widget.Selection;
import ch.ethz.seb.sebserver.gui.widget.Selection.Type;
import ch.ethz.seb.sebserver.gui.widget.ThresholdList;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public final class Form implements FormBinding {

    private final Cryptor cryptor;
    private final JSONMapper jsonMapper;
    private final ObjectNode objectRoot;

    private final Map<String, String> staticValues = new LinkedHashMap<>();
    private final MultiValueMap<String, FormFieldAccessor> formFields = new LinkedMultiValueMap<>();
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();

    Form(final JSONMapper jsonMapper, final Cryptor cryptor) {
        this.jsonMapper = jsonMapper;
        this.cryptor = cryptor;
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

        this.formFields.forEach((key, value) -> value
                .stream()
                .filter(Form::valueApplicationFilter)
                .forEach(ffa -> {
                    if (ffa.listValue) {
                        appendFormUrlEncodedValue(
                                buffer,
                                key,
                                ffa.getStringValue());
                    } else {
                        appendFormUrlEncodedSingleValue(
                                buffer,
                                key,
                                ffa.getStringValue(),
                                false);
                    }
                }));

        return buffer.toString();
    }

    public void putStatic(final String name, final String value) {
        if (StringUtils.isNotBlank(value)) {
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

    public boolean hasField(final String fieldName) {
        return this.formFields.containsKey(fieldName);
    }

    Form putReadonlyField(final String name, final Control label, final Text field) {
        this.formFields.add(name, createReadonlyAccessor(label, field));
        return this;
    }

    Form putReadonlyField(final String name, final Control label, final Browser field) {
        this.formFields.add(name, createReadonlyAccessor(label, field));
        return this;
    }

    Form putField(final String name, final Control label, final Text field, final Label errorLabel) {
        this.formFields.add(name, createAccessor(label, field, errorLabel));
        return this;
    }

    Form putField(final String name, final Control label, final PasswordInput field, final Label errorLabel) {
        this.formFields.add(name, createAccessor(label, field, errorLabel));
        return this;
    }

    Form putField(final String name, final Control label, final Button checkbox) {
        this.formFields.add(name, createAccessor(label, checkbox, null));
        return this;
    }

    Form putField(final String name, final Control label, final Selection field, final Label errorLabel) {
        this.formFields.add(name, createAccessor(label, field, errorLabel));
        return this;
    }

    Form putField(final String name, final Control label, final ThresholdList field, final Label errorLabel) {
        this.formFields.add(name, createAccessor(label, field, errorLabel));
        return this;
    }

    Form putField(final String name, final Control label, final ImageUploadSelection imageUpload,
            final Label errorLabel) {
        final FormFieldAccessor createAccessor = createAccessor(label, imageUpload, errorLabel);
        imageUpload.setErrorHandler(createAccessor::setError);
        this.formFields.add(name, createAccessor);
        return this;
    }

    Form putField(final String name, final Control label, final FileUploadSelection fileUpload,
            final Label errorLabel) {
        final FormFieldAccessor createAccessor = createAccessor(label, fileUpload, errorLabel);
        fileUpload.setErrorHandler(createAccessor::setError);
        this.formFields.add(name, createAccessor);
        return this;
    }

    public String getFieldValue(final String attributeName) {
        final FormFieldAccessor fieldAccessor = this.formFields.getFirst(attributeName);
        if (fieldAccessor == null) {
            return null;
        }

        return fieldAccessor.getStringValue();
    }

    public Control getFieldInput(final String attributeName) {
        final FormFieldAccessor fieldAccessor = this.formFields.getFirst(attributeName);
        if (fieldAccessor == null) {
            return null;
        }

        return fieldAccessor.input;
    }

    public void setFieldValue(final String attributeName, final String attributeValue) {
        final FormFieldAccessor fieldAccessor = this.formFields.getFirst(attributeName);
        if (fieldAccessor == null) {
            return;
        }

        fieldAccessor.setStringValue(attributeValue);
    }

    public void setFieldColor(final String attributeName, final Color color) {
        final FormFieldAccessor fieldAccessor = this.formFields.getFirst(attributeName);
        if (fieldAccessor == null) {
            return;
        }

        fieldAccessor.setBackgroundColor(color);
    }

    public void setFieldTextColor(final String attributeName, final Color color) {
        final FormFieldAccessor fieldAccessor = this.formFields.getFirst(attributeName);
        if (fieldAccessor == null) {
            return;
        }

        fieldAccessor.setTextColor(color);
    }

    public void allVisible() {
        process(
                Utils.truePredicate(),
                ffa -> ffa.setVisible(true));
    }

    public void setVisible(final boolean visible, final String group) {
        if (!this.groups.containsKey(group)) {
            return;
        }

        final Set<String> namesSet = this.groups.get(group);
        process(
                namesSet::contains,
                ffa -> ffa.setVisible(visible));
    }

    public void setFieldVisible(final boolean visible, final String fieldName) {
        final List<FormFieldAccessor> list = this.formFields.get(fieldName);
        if (list != null) {
            list.forEach(ffa -> ffa.setVisible(visible));
        }
    }

    public boolean hasAnyError() {
        return this.formFields.entrySet()
                .stream()
                .flatMap(entity -> entity.getValue().stream())
                .anyMatch(a -> a.hasError);
    }

    public void clearErrors() {
        process(
                Utils.truePredicate(),
                FormFieldAccessor::resetError);
    }

    public void setFieldError(final String fieldName, final String errorMessage) {
        final List<FormFieldAccessor> list = this.formFields.get(fieldName);
        if (list != null) {
            list.forEach(ffa -> ffa.setError(errorMessage));
        }
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
        this.objectRoot.removeAll();
        for (final Map.Entry<String, String> entry : this.staticValues.entrySet()) {
            final String value = entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                this.objectRoot.put(entry.getKey(), value);
            }
        }

        for (final Map.Entry<String, List<FormFieldAccessor>> entry : this.formFields.entrySet()) {
            entry.getValue()
                    .stream()
                    .filter(Form::valueApplicationFilter)
                    .forEach(ffa -> ffa.putJsonValue(entry.getKey(), this.objectRoot));
        }
    }

    private static boolean valueApplicationFilter(final FormFieldAccessor ffa) {
        return ffa.getStringValue() != null;
    }

    // following are FormFieldAccessor implementations for all field types
    //@formatter:off
    private FormFieldAccessor createReadonlyAccessor(final Control label, final Text field) {
        return new FormFieldAccessor(label, field, null) {
            @Override public String getStringValue() { return null; }
            @Override public void setStringValue(final String value) { field.setText( (value == null) ? StringUtils.EMPTY : value); }
        };
    }
    private FormFieldAccessor createReadonlyAccessor(final Control label, final Browser field) {
        return new FormFieldAccessor(label, field, null) {
            @Override public String getStringValue() { return null; }
            @Override public void setStringValue(final String value) { field.setText( (value == null) ? StringUtils.EMPTY : value); }
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final Text text, final Label errorLabel) {
        return new FormFieldAccessor(label, text, errorLabel) {
            @Override public String getStringValue() {return text.getText();}
            @Override public void setStringValue(final String value) {text.setText(value);}
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final PasswordInput pwdInput, final Label errorLabel) {
        return new FormFieldAccessor(label, pwdInput, errorLabel) {
            @Override public String getStringValue() {return pwdInput.getValue() != null ? pwdInput.getValue().toString() : null;}
            @Override public void setStringValue(final String value) {
                if (StringUtils.isNotBlank(value)) {
                    final CharSequence pwd = Form.this.cryptor.decrypt(value)
                        .getOrThrow();
                    pwdInput.setValue(pwd);
                } else {
                    pwdInput.setValue(value);
                }
            }
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final Button checkbox, final Label errorLabel) {
        return new FormFieldAccessor(label, checkbox, errorLabel) {
            @Override public String getStringValue() {return BooleanUtils.toStringTrueFalse(checkbox.getSelection());}
            @Override public void setStringValue(final String value) {checkbox.setSelection(BooleanUtils.toBoolean(value));}
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final Selection selection, final Label errorLabel) {
        switch (selection.type()) {
            case MULTI:
            case MULTI_COMBO:
            case MULTI_CHECKBOX:
                return createAccessor(label, selection, Form::adaptCommaSeparatedStringToJsonArray, errorLabel);
            default : return createAccessor(label, selection, null, errorLabel);
        }
    }
    private FormFieldAccessor createAccessor(
            final Control label,
            final Selection selection,
            final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter,
            final Label errorLabel) {

        return new FormFieldAccessor(
                label,
                selection.adaptToControl(),
                jsonValueAdapter,
                selection.type() != Type.SINGLE,
                errorLabel) {
            @Override public String getStringValue() { return selection.getSelectionValue(); }
            @Override public void setStringValue(final String value) { selection.select(value); }
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final ThresholdList thresholdList, final Label errorLabel) {
        return new FormFieldAccessor(label, thresholdList, null, true, errorLabel) {
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
    private FormFieldAccessor createAccessor(final Control label, final ImageUploadSelection imageUpload, final Label errorLabel) {
        return new FormFieldAccessor(label, imageUpload, errorLabel) {
            @Override public String getStringValue() { return imageUpload.getImageBase64(); }
        };
    }
    private FormFieldAccessor createAccessor(final Control label, final FileUploadSelection fileUpload, final Label errorLabel) {
        return new FormFieldAccessor(label, fileUpload, errorLabel) {
            @Override public String getStringValue() { return fileUpload.getFileName(); }
        };
    }
    //@formatter:on

    /*
     * Adds the given name and value in from URL encoded format to the given StringBuffer.
     * Checks first if the value String is a comma separated list. If true, splits values
     * and adds every value within the same name mapping to the string buffer
     */
    private static void appendFormUrlEncodedValue(
            final StringBuffer buffer,
            final String name,
            final String value) {

        if (StringUtils.isBlank(value)) {
            return;
        }

        final String[] split = StringUtils.split(value, Constants.LIST_SEPARATOR_CHAR);
        for (int i = 0; i < split.length; i++) {
            appendFormUrlEncodedSingleValue(buffer, name, split[i], true);
        }
    }

    private static void appendFormUrlEncodedSingleValue(
            final StringBuffer buffer,
            final String name,
            final String value,
            final boolean checkMultiValue) {

        if (StringUtils.isBlank(value)) {
            return;
        }

        if (buffer.length() > 0) {
            buffer.append(Constants.FORM_URL_ENCODED_SEPARATOR);
        }

        // check of the string value is a name-value pair. If true, use the specified name an value
        // otherwise use the general name given within this method call and
        if (checkMultiValue && value.contains(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)) {
            final String[] nameValue = StringUtils.split(value, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
            buffer.append(nameValue[0])
                    .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                    .append(Utils.encodeFormURL_UTF_8(nameValue[1]));
        } else {
            buffer.append(name)
                    .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                    .append(Utils.encodeFormURL_UTF_8(value));
        }
    }

    private static void adaptCommaSeparatedStringToJsonArray(
            final Tuple<String> tuple,
            final ObjectNode jsonNode) {

        if (StringUtils.isNotBlank(tuple._2)) {
            final ArrayNode arrayNode = jsonNode.putArray(tuple._1);
            final String[] split = StringUtils.split(tuple._2, Constants.LIST_SEPARATOR);
            for (int i = 0; i < split.length; i++) {
                arrayNode.add(split[i]);
            }
        }
    }

    public static abstract class FormFieldAccessor {

        public final Control label;
        public final Control input;
        private final Label errorLabel;
        private final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter;
        private boolean hasError;
        private final boolean listValue;

        FormFieldAccessor(final Control label, final Control control, final Label errorLabel) {
            this(label, control, null, false, errorLabel);
        }

        FormFieldAccessor(
                final Control label,
                final Control input,
                final BiConsumer<Tuple<String>, ObjectNode> jsonValueAdapter,
                final boolean listValue,
                final Label errorLabel) {

            this.label = label;
            this.input = input;
            this.errorLabel = errorLabel;
            if (jsonValueAdapter != null) {
                this.jsonValueAdapter = jsonValueAdapter;
            } else {
                this.jsonValueAdapter = (tuple, jsonObject) -> {
                    if (tuple._2 != null) {
                        jsonObject.put(tuple._1, tuple._2);
                    }
                };
            }
            this.listValue = listValue;
        }

        public abstract String getStringValue();

        public void setStringValue(final String value) {
            throw new UnsupportedOperationException();
        }

        public void setBackgroundColor(final Color color) {
            if (this.input != null) {
                this.input.setBackground(color);
            }
        }

        public void setTextColor(final Color color) {
            if (this.input != null) {
                this.input.setForeground(color);
            }
        }

        public void setVisible(final boolean visible) {
            if (this.label != null) {
                this.label.setVisible(visible);
            }
            this.input.setVisible(visible);
        }

        public void setEnabled(final boolean enable) {
            if (this.label != null) {
                this.label.setEnabled(enable);
            }
            this.input.setEnabled(enable);
        }

        public void putJsonValue(final String key, final ObjectNode objectRoot) {
            this.jsonValueAdapter.accept(new Tuple<>(key, getStringValue()), objectRoot);
        }

        public boolean hasError() {
            return this.hasError;
        }

        public void setError(final String errorMessage) {
            if (this.errorLabel == null) {
                return;
            }

            if (errorMessage == null) {
                resetError();
                return;
            }

            if (!this.hasError) {
                this.input.setData(RWT.CUSTOM_VARIANT, CustomVariant.ERROR.key);
                this.errorLabel.setText("- " + errorMessage);
                this.errorLabel.setVisible(true);
                this.hasError = true;
            }
        }

        public void resetError() {
            if (this.errorLabel == null) {
                return;
            }

            if (this.hasError) {
                this.input.setData(RWT.CUSTOM_VARIANT, null);
                this.errorLabel.setVisible(false);
                this.errorLabel.setText(StringUtils.EMPTY);
                this.hasError = false;
            }
        }
    }

}
