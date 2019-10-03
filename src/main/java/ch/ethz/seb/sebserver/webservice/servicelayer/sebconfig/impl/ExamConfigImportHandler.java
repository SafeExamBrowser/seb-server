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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.ExamConfigImportHandler.PListNode.Type;

public class ExamConfigImportHandler extends DefaultHandler {

    private static final Set<String> VALUE_ELEMENTS = new HashSet<>(Arrays.asList(
            Constants.XML_PLIST_BOOLEAN_FALSE,
            Constants.XML_PLIST_BOOLEAN_TRUE,
            Constants.XML_PLIST_STRING,
            Constants.XML_PLIST_INTEGER));

    private final Consumer<ConfigurationValue> valueConsumer;
    private final Function<String, Long> attributeNameIdResolver;
    private final Long institutionId;
    private final Long configId;

    private final Stack<PListNode> stack = new Stack<>();

    protected ExamConfigImportHandler(
            final Long institutionId,
            final Long configId,
            final Consumer<ConfigurationValue> valueConsumer,
            final Function<String, Long> attributeNameIdResolver) {

        super();
        this.valueConsumer = valueConsumer;
        this.attributeNameIdResolver = attributeNameIdResolver;
        this.institutionId = institutionId;
        this.configId = configId;
    }

    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) throws SAXException {

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

                final String attrName = (parent.type == Type.DICT && grandParent.type == Type.ARRAY)
                        ? parent.name + "." + top.name
                        : top.name;

                this.valueConsumer.accept(new ConfigurationValue(
                        null,
                        this.institutionId,
                        this.configId,
                        this.attributeNameIdResolver.apply(attrName),
                        top.listIndex,
                        top.value));
            }
        } else if (!Constants.XML_PLIST_KEY_NAME.equals(qName)) {
            this.stack.pop();
        }
    }

    @Override
    public void characters(
            final char[] ch,
            final int start,
            final int length) throws SAXException {

        final PListNode top = this.stack.peek();
        if (top.type == Type.VALUE_STRING) {
            top.value = String.valueOf(ch);
        } else if (top.type == Type.VALUE_INTEGER) {
            top.value = String.valueOf(ch);
        } else if (top.type == Type.KEY) {
            top.name = String.valueOf(ch);
        }
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
        String name;
        int arrayCounter = 0;
        int listIndex = 0;
        String value;

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
