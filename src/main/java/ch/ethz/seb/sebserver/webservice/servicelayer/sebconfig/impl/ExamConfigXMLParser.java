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
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.ExamConfigXMLParser.PListNode.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter.KioskModeConverter;

public class ExamConfigXMLParser extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigXMLParser.class);

    // comma separated list of SEB exam config keys that can be ignored on imports
    // See: https://jira.let.ethz.ch/browse/SEBSERV-100
    private static final Set<String> SEB_EXAM_CONFIG_KEYS_TO_IGNORE = new HashSet<>(Arrays.asList(
            // SEB Server specific
            "sebMode",
            "sebServerFallback",
            "sebServerURL",

            // Obsolete on SEB Server
            "startURL",
            "startURLAllowDeepLink",
            "startURLAppendQueryParameter",

            // These keys don't exist anymore:
            "examConfigKeyContainedKeys",
            "allowWLAN",
            "insideSebEnableEnableNetworkConnectionSelector",
            "ignoreQuitPassword",
            "oskBehavior",
            "outsideSebEnableChangeAPassword",
            "outsideSebEnableEaseOfAccess",
            "outsideSebEnableLockThisComputer",
            "outsideSebEnableLogOff",
            "outsideSebEnableShutDownurlFilterRegex",
            "outsideSebEnableStartTaskManager",
            "outsideSebEnableSwitchUser",
            "outsideSebEnableVmWareClientShade",
            "outsideSebEnableShutDown",
            "enableURLContentFilter",
            "enableURLFilter",
            "prohibitedProcesses.windowHandlingProcess",
            "permittedProcesses.windowHandlingProcess",
            "backgroundOpenSEBConfig",

            // These keys are only used internally
            "urlFilterRegex",
            "urlFilterTrustedContent",
            "blacklistURLFilter",
            "whitelistURLFilter",
            "URLFilterIgnoreList"));

    private static final Set<String> VALUE_ELEMENTS = new HashSet<>(Arrays.asList(
            Constants.XML_PLIST_BOOLEAN_FALSE,
            Constants.XML_PLIST_BOOLEAN_TRUE,
            Constants.XML_PLIST_STRING,
            Constants.XML_PLIST_DATA,
            Constants.XML_PLIST_INTEGER));

    private static final Set<String> KNOWN_INLINE_TABLES = new HashSet<>(Arrays.asList(
            "arguments"));

    public static final Set<String> SECRET_ATTRIBUTES = new HashSet<>(Arrays.asList(
            "hashedQuitPassword",
            "hashedAdminPassword"));

    private final Cryptor cryptor;
    private final Consumer<ConfigurationValue> valueConsumer;
    private final Function<String, ConfigurationAttribute> attributeResolver;
    private final Long institutionId;
    private final Long configId;

    private final Stack<PListNode> stack = new Stack<>();

    private Boolean killExplorerShell = null;
    private Boolean createNewDesktop = null;

    public ExamConfigXMLParser(
            final Cryptor cryptor,
            final Long institutionId,
            final Long configId,
            final Consumer<ConfigurationValue> valueConsumer,
            final Function<String, ConfigurationAttribute> attributeResolver) {

        super();
        this.cryptor = cryptor;
        this.valueConsumer = valueConsumer;
        this.attributeResolver = attributeResolver;
        this.institutionId = institutionId;
        this.configId = configId;
    }

    @Override
    public void startDocument() {
        if (log.isDebugEnabled()) {
            log.debug("Start parsing document");
        }
    }

    @Override
    public void endDocument() {
        if (log.isDebugEnabled()) {
            log.debug("End parsing document");
        }
    }

    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) {

        if (log.isDebugEnabled()) {
            log.debug("start element: {}", qName);
        }

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
            final String qName) {

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

            if (top.inlineTable) {
                createInlineTableValue(top, attrName, attribute);
                return;
            }

            // check if we have a simple values array
            if (attribute != null && (attribute.type == AttributeType.MULTI_CHECKBOX_SELECTION
                    || attribute.type == AttributeType.MULTI_SELECTION
                    || attribute.type == AttributeType.TEXT_AREA)) {

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

        // no or blank value
        if (StringUtils.isBlank(top.value)) {
            saveValue(attrName, attribute, top.listIndex, null);
            return;
        }

        final String[] names = StringUtils.split(top.valueName, Constants.LIST_SEPARATOR);
        final String[] values = StringUtils.split(top.value, Constants.LIST_SEPARATOR);
        final String[] columns = StringUtils.split(attribute.getResources(), Constants.EMBEDDED_LIST_SEPARATOR);
        final int numColumns = columns.length;
        if (names.length != values.length) {
            throw new IllegalArgumentException(
                    "Failed to get InlineTable values. value/name array length mismatch");
        }

        final StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i != 0) {
                if (i % numColumns == 0) {
                    valueBuilder.append(Constants.LIST_SEPARATOR);
                } else {
                    valueBuilder.append(Constants.EMBEDDED_LIST_SEPARATOR);
                }
            }
            valueBuilder
                    .append(names[i])
                    .append(Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR)
                    .append(values[i]);
        }

        saveValue(attrName, attribute, top.listIndex, valueBuilder.toString());
    }

    @Override
    public void characters(
            final char[] ch,
            final int start,
            final int length) {

        final char[] valueChar = new char[length];
        System.arraycopy(ch, start, valueChar, 0, length);
        final String value = String.valueOf(valueChar);
        final PListNode top = this.stack.peek();
        if (top.type == Type.VALUE_STRING) {
            if (top.value == null) {
                top.value = StringEscapeUtils.unescapeXml(value);
            } else {
                top.value += StringEscapeUtils.unescapeXml(value);
            }
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
                checkValueType(value, attribute));

        if (configurationValue != null) {
            if (log.isDebugEnabled()) {
                log.debug("Put value: {} : {}", name, configurationValue);
            }

            this.valueConsumer.accept(configurationValue);
        }
    }

    private String checkValueType(final String value, final ConfigurationAttribute attribute) {
        if (attribute == null) {
            return value;
        }

        if (attribute.type == null) {
            log.warn(
                    "Invalid attribute type detected. Name: {} type: {} value: {} : import with default value for this attribute",
                    attribute.name,
                    attribute.type,
                    value);
            return attribute.defaultValue;
        }

        switch (attribute.type) {
            case CHECKBOX: {
                try {
                    Boolean.parseBoolean(value);
                    return value;
                } catch (final Exception e) {
                    log.warn(
                            "Invalid attribute value detected. Name: {} type: {} value: {} : import with default value for this attribute",
                            attribute.name,
                            attribute.type,
                            value);
                    return attribute.defaultValue;
                }
            }
            case INTEGER:
            case RADIO_SELECTION:
            case SINGLE_SELECTION: {
                try {
                    Integer.parseInt(value);
                    return value;
                } catch (final Exception e) {
                    log.warn(
                            "Invalid attribute value detected. Name: {} type: {} value: {} : import with default value for this attribute",
                            attribute.name,
                            attribute.type,
                            value);
                    return attribute.defaultValue;
                }
            }
            case DECIMAL: {
                try {
                    Double.parseDouble(value);
                    return value;
                } catch (final Exception e) {
                    log.warn(
                            "Invalid attribute value detected. Name: {} type: {} value: {} : import with default value for this attribute",
                            attribute.name,
                            attribute.type,
                            value);
                    return attribute.defaultValue;
                }
            }
            default:
                return value;
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

            if (SEB_EXAM_CONFIG_KEYS_TO_IGNORE.contains(name)) {
                log.debug("Black-listed attribute. name={} value={}", name, value);
            } else {
                log.warn("Unknown attribute. name={} value={}", name, value);
            }
            return null;
        }

        if (SECRET_ATTRIBUTES.contains(name)) {
            // NOTE this is a special case, if a hashed password is imported it is not possible to view this password
            //      later in plain text to the administrator. Therefore this password hash is marked here as imported
            //      and internally encrypted as usual. So the password will be decrypted while viewing and is recognizable
            //      for the export so that the password can be decrypted with internal encryption and then, if import
            //      marked, just send to the export by removing the marker and do not rehash the already hashed password.
            return new ConfigurationValue(
                    null,
                    this.institutionId,
                    this.configId,
                    attribute.id,
                    listIndex,
                    StringUtils.isNotBlank(value)
                            ? this.cryptor.encrypt(value + Constants.IMPORTED_PASSWORD_MARKER).toString()
                            : value);
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

            Type(final boolean isValueType, final String typeName) {
                this.isValueType = isValueType;
                this.typeName = typeName;
            }

            public static boolean isBooleanValue(final Type type) {
                return type == VALUE_BOOLEAN_TRUE || type == VALUE_BOOLEAN_FALSE;
            }

            public static Type getType(final String qName) {
                return Arrays.stream(Type.values())
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
