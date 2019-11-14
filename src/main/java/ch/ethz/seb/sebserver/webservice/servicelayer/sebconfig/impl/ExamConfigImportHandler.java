/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.ExamConfigImportHandler.PListNode.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter.KioskModeConverter;

public class ExamConfigImportHandler extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigImportHandler.class);

    private static final Set<String> VALUE_ELEMENTS = new HashSet<>(Arrays.asList(
            Constants.XML_PLIST_BOOLEAN_FALSE,
            Constants.XML_PLIST_BOOLEAN_TRUE,
            Constants.XML_PLIST_STRING,
            Constants.XML_PLIST_DATA,
            Constants.XML_PLIST_INTEGER));

    private static final Set<String> KNOWN_INLINE_TABLES = new HashSet<>(Arrays.asList(
            "arguments"));

    private final Consumer<ConfigurationValue> valueConsumer;
    private final Function<String, ConfigurationAttribute> attributeResolver;
    private final Long institutionId;
    private final Long configId;

    private final Stack<PListNode> stack = new Stack<>();

    private Boolean killExplorerShell = null;
    private Boolean createNewDesktop = null;

    protected ExamConfigImportHandler(
            final Long institutionId,
            final Long configId,
            final Consumer<ConfigurationValue> valueConsumer,
            final Function<String, ConfigurationAttribute> attributeResolver) {

        super();
        this.valueConsumer = valueConsumer;
        this.attributeResolver = attributeResolver;
        this.institutionId = institutionId;
        this.configId = configId;
    }

    @Override
    public void startDocument() throws SAXException {
        log.debug("Start parsing document");
    }

    @Override
    public void endDocument() throws SAXException {
        log.debug("End parsing document");
    }

    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) throws SAXException {

        log.debug("start element: {}", qName);

        final Type type = Type.getType(qName);
        final PListNode top = (this.stack.isEmpty()) ? null : this.stack.peek();

        switch (type) {
            case PLIST:
                startPList(type);
                break;
            case DICT:
                startDict(type, top);
                break;
            case ARRAY:
                startArray(type, top);
                break;
            case KEY:
                startKey(type, top);
                break;
            case VALUE_BOOLEAN_FALSE:
            case VALUE_BOOLEAN_TRUE:
            case VALUE_STRING:
            case VALUE_DATA:
            case VALUE_INTEGER:
                startValueElement(type, top);
                break;
        }
    }

    private void startKey(final Type type, final PListNode top) {
        final PListNode key = new PListNode(type);
        switch (top.type) {
            case DICT: {
                key.listIndex = top.listIndex;
                this.stack.push(key);
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private void startArray(final Type type, final PListNode top) {
        final PListNode array = new PListNode(type);
        switch (top.type) {
            case KEY: {
                array.inlineTable = isInlineTable(top.name);
                array.name = top.name;
                array.listIndex = top.listIndex;
                this.stack.pop();
                this.stack.push(array);
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private boolean isInlineTable(final String name) {
        return KNOWN_INLINE_TABLES.contains(name);
    }

    private void startDict(final Type type, final PListNode top) {
        final PListNode dict = new PListNode(type);
        switch (top.type) {
            case PLIST: {
                this.stack.push(dict);
                break;
            }
            case ARRAY: {
                dict.name = top.name;
                dict.listIndex = top.arrayCounter++;
                this.stack.push(dict);
                break;
            }
            case KEY: {
                dict.name = top.name;
                dict.listIndex = top.listIndex;
                this.stack.pop();
                this.stack.push(dict);
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private void startPList(final Type type) {
        if (this.stack.isEmpty()) {
            this.stack.push(new PListNode(type));
        } else {
            throw new IllegalStateException();
        }
    }

    private void startValueElement(final Type type, final PListNode top) {
        final PListNode value = new PListNode(type);
        if (top.type == Type.KEY) {

            if (Type.isBooleanValue(type)) {
                this.stack.pop();
                value.name = top.name;
                value.listIndex = top.listIndex;
                value.value = type == Type.VALUE_BOOLEAN_TRUE
                        ? Constants.XML_PLIST_BOOLEAN_TRUE
                        : Constants.XML_PLIST_BOOLEAN_FALSE;
                this.stack.push(value);
            } else {
                this.stack.pop();
                value.name = top.name;
                value.listIndex = top.listIndex;
                this.stack.push(value);
            }
        } else if (top.type == Type.ARRAY) {
            if (Type.isBooleanValue(type)) {
                value.name = top.name;
                value.listIndex = top.arrayCounter++;
                value.value = type == Type.VALUE_BOOLEAN_TRUE
                        ? Constants.XML_PLIST_BOOLEAN_TRUE
                        : Constants.XML_PLIST_BOOLEAN_FALSE;
                this.stack.push(value);
            } else {
                value.name = top.name;
                value.listIndex = top.arrayCounter++;
                this.stack.push(value);
            }
        }
    }

    @Override
    public void endElement(
            final String uri,
            final String localName,
            final String qName) throws SAXException {

        final PListNode top = this.stack.peek();
        if (VALUE_ELEMENTS.contains(qName)) {
            if (top.type.isValueType) {
                this.stack.pop();
                final PListNode parent = this.stack.pop();
                final PListNode grandParent = this.stack.peek();
                this.stack.push(parent);

                // if we are in a values-array
                if (parent.type == Type.ARRAY) {
                    if (StringUtils.isBlank(parent.value)) {
                        parent.value = top.value;
                    } else {
                        parent.value += "," + top.value;
                    }
                    return;
                }

                // if we are in an inline table array
                if (grandParent.type == Type.ARRAY && grandParent.inlineTable) {
                    if (StringUtils.isBlank(grandParent.value)) {
                        grandParent.value = top.value;
                    } else {
                        grandParent.value += "," + top.value;
                    }
                    if (StringUtils.isBlank(grandParent.valueName)) {
                        grandParent.valueName = top.name;
                    } else {
                        grandParent.valueName += "," + top.name;
                    }
                    return;
                }

                final String attrName = (parent.type == Type.DICT && grandParent.type == Type.ARRAY)
                        ? parent.name + "." + top.name
                        : top.name;

                final ConfigurationAttribute attribute = this.attributeResolver.apply(attrName);
                saveValue(attrName, attribute, top.listIndex, top.value);
            }
        } else if (top.type == Type.ARRAY) {
            this.stack.pop();

            final PListNode parent = this.stack.pop();
            final PListNode grandParent = this.stack.peek();
            this.stack.push(parent);
            final String attrName = (parent.type == Type.DICT && grandParent.type == Type.ARRAY)
                    ? parent.name + "." + top.name
                    : top.name;
            final ConfigurationAttribute attribute = this.attributeResolver.apply(attrName);

            if (attribute == null) {
                log.warn("Import of unknown attribute. name={} value={}", attrName, top.value);
                return;
            }

            if (top.inlineTable) {
                createInlineTableValue(top, attrName, attribute);
                return;
            }

            // check if we have a simple values array
            if (attribute.type == AttributeType.MULTI_CHECKBOX_SELECTION
                    || attribute.type == AttributeType.MULTI_SELECTION) {

                saveValue(attrName, attribute, top.listIndex, (top.value == null) ? "" : top.value);
            }

        } else if (!Constants.XML_PLIST_KEY_NAME.equals(qName)) {
            this.stack.pop();
        }
    }

    private void createInlineTableValue(
            final PListNode top,
            final String attrName,
            final ConfigurationAttribute attribute) {

        final String[] names = StringUtils.split(top.valueName, Constants.LIST_SEPARATOR);
        final String[] values = StringUtils.split(top.value, Constants.LIST_SEPARATOR);
        final String[] columns = StringUtils.split(attribute.getResources(), Constants.EMBEDDED_LIST_SEPARATOR);
        final int numColumns = columns.length;
        if (names.length != values.length) {
            throw new IllegalArgumentException(
                    "Failed to import InlineTable values. value/name array length mismatch");
        }

        String val = "";
        for (int i = 0; i < names.length; i++) {
            if (i != 0) {
                if (i % numColumns == 0) {
                    val = val + Constants.LIST_SEPARATOR;
                } else {
                    val = val + Constants.EMBEDDED_LIST_SEPARATOR;
                }
            }
            val = val + names[i] + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR + values[i];
        }

        saveValue(attrName, attribute, top.listIndex, val);
    }

    @Override
    public void characters(
            final char[] ch,
            final int start,
            final int length) throws SAXException {

        final char[] valueChar = new char[length];
        System.arraycopy(ch, start, valueChar, 0, length);
        final String value = String.valueOf(valueChar);
        final PListNode top = this.stack.peek();
        if (top.type == Type.VALUE_STRING) {
            top.value = value;
        } else if (top.type == Type.VALUE_INTEGER) {
            top.value = value;
        } else if (top.type == Type.KEY) {
            top.name = value;
        }
    }

    private void saveValue(
            final String name,
            final ConfigurationAttribute attribute,
            final int listIndex,
            final String value) {

        final ConfigurationValue configurationValue = createConfigurationValue(
                name,
                attribute,
                listIndex,
                value);

        if (configurationValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("Save imported value: {} : {}", name, configurationValue);
            }

            this.valueConsumer.accept(configurationValue);
        }
    }

    private ConfigurationValue createConfigurationValue(
            final String name,
            final ConfigurationAttribute attribute,
            final int listIndex,
            final String value) {

        if (attribute == null) {
            if (KioskModeConverter.NAMES.contains(name)) {
                return handleKioskMode(name, listIndex, value);
            }

            log.warn("Import of unknown attribute. name={} value={}", name, value);
            return null;
        }

        return new ConfigurationValue(
                null,
                this.institutionId,
                this.configId,
                attribute.id,
                listIndex,
                value);
    }

    private ConfigurationValue handleKioskMode(final String name, final int listIndex, final String value) {
        if (KioskModeConverter.ATTR_NAME_KILL_SHELL.equals(name)) {
            this.killExplorerShell = BooleanUtils.toBoolean(value);
        } else if (KioskModeConverter.ATTR_NAME_CREATE_NEW_DESKTOP.equals(name)) {
            this.createNewDesktop = BooleanUtils.toBoolean(value);
        }

        if (this.killExplorerShell != null && this.createNewDesktop != null) {
            final ConfigurationAttribute kioskMode = this.attributeResolver.apply(
                    KioskModeConverter.ATTR_NAME_KIOSK_MODE);

            final String val = (this.createNewDesktop)
                    ? "0"
                    : (this.killExplorerShell)
                            ? "1"
                            : "2";

            return new ConfigurationValue(
                    null,
                    this.institutionId,
                    this.configId,
                    kioskMode.id,
                    listIndex,
                    val);
        }

        return null;
    }

    final static class PListNode {

        enum Type {
            PLIST(false, Constants.XML_PLIST_NAME),
            DICT(false, Constants.XML_PLIST_DICT_NAME),
            ARRAY(false, Constants.XML_PLIST_ARRAY_NAME),
            KEY(false, Constants.XML_PLIST_KEY_NAME),
            VALUE_BOOLEAN_TRUE(true, Constants.XML_PLIST_BOOLEAN_TRUE),
            VALUE_BOOLEAN_FALSE(true, Constants.XML_PLIST_BOOLEAN_FALSE),
            VALUE_STRING(true, Constants.XML_PLIST_STRING),
            VALUE_DATA(true, Constants.XML_PLIST_DATA),
            VALUE_INTEGER(true, Constants.XML_PLIST_INTEGER);

            private final boolean isValueType;
            private final String typeName;

            private Type(final boolean isValueType, final String typeName) {
                this.isValueType = isValueType;
                this.typeName = typeName;
            }

            public static boolean isBooleanValue(final Type type) {
                return type == VALUE_BOOLEAN_TRUE || type == VALUE_BOOLEAN_FALSE;
            }

            public static Type getType(final String qName) {
                return Arrays.asList(Type.values()).stream()
                        .filter(type -> type.typeName.equals(qName))
                        .findFirst()
                        .orElse(null);
            }
        }

        final Type type;
        boolean inlineTable = false;
        String name;
        int arrayCounter = 0;
        int listIndex = 0;
        String valueName;
        String value;
        boolean saveNullValueAsBlank = false;

        protected PListNode(final Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("PListNode [type=");
            builder.append(this.type);
            builder.append(", name=");
            builder.append(this.name);
            builder.append(", listIndex=");
            builder.append(this.listIndex);
            builder.append(", value=");
            builder.append(this.value);
            builder.append("]");
            return builder.toString();
        }
    }

}
