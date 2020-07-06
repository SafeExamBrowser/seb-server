/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;

public class ExamConfigImportHandlerTest {

    private static final Function<String, ConfigurationAttribute> attributeResolver =
            name -> new ConfigurationAttribute(
                    getId(name),
                    null, name, (name.contains("array")) ? AttributeType.MULTI_SELECTION : AttributeType.TEXT_FIELD,
                    null, null, null,
                    null);

    private static final Long getId(final String name) {
        try {
            return Long.parseLong(String.valueOf(name.charAt(name.length() - 1)));
        } catch (final Exception e) {
            return -1L;
        }
    }

    @Test
    public void simpleStringValueTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attributeResolver);

        final String attribute = "param1";
        final String value = "value1";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, "string", null);
        candidate.characters(value.toCharArray(), 0, value.length());
        candidate.endElement(null, null, "string");

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        final ConfigurationValue configurationValue = valueCollector.values.get(0);
        assertNotNull(configurationValue);
        assertTrue(1L == configurationValue.attributeId);
        assertEquals("value1", configurationValue.value);
    }

    @Test
    public void simpleIntegerValueTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attributeResolver);

        final String attribute = "param2";
        final String value = "22";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, "integer", null);
        candidate.characters(value.toCharArray(), 0, value.length());
        candidate.endElement(null, null, "integer");

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        final ConfigurationValue configurationValue = valueCollector.values.get(0);
        assertNotNull(configurationValue);
        assertTrue(2L == configurationValue.attributeId);
        assertEquals("22", configurationValue.value);
    }

    @Test
    public void simpleBooleanValueTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attributeResolver);

        final String attribute = "param3";
        final String value = "true";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, value, null);
        candidate.endElement(null, null, value);

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        final ConfigurationValue configurationValue = valueCollector.values.get(0);
        assertNotNull(configurationValue);
        assertTrue(3L == configurationValue.attributeId);
        assertEquals("true", configurationValue.value);
    }

    @Test
    public void arrayOfStringValueTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attributeResolver);

        final String attribute = "array1";
        final String value1 = "val1";
        final String value2 = "val2";
        final String value3 = "val3";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");

        candidate.startElement(null, null, "array", null);

        candidate.startElement(null, null, "string", null);
        candidate.characters(value1.toCharArray(), 0, value1.length());
        candidate.endElement(null, null, "string");
        candidate.startElement(null, null, "string", null);
        candidate.characters(value2.toCharArray(), 0, value2.length());
        candidate.endElement(null, null, "string");
        candidate.startElement(null, null, "string", null);
        candidate.characters(value3.toCharArray(), 0, value3.length());
        candidate.endElement(null, null, "string");

        candidate.endElement(null, null, "array");

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        assertTrue(valueCollector.values.size() == 1);
        final ConfigurationValue configurationValue1 = valueCollector.values.get(0);
        assertEquals("val1,val2,val3", configurationValue1.value);
        assertTrue(configurationValue1.listIndex == 0);

    }

    @Test
    public void dictOfValuesTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final List<String> attrNamesCollector = new ArrayList<>();
        final Function<String, ConfigurationAttribute> attrConverter = attrName -> {
            attrNamesCollector.add(attrName);
            return attributeResolver.apply(attrName);
        };
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attrConverter);

        final String attribute = "dict1";

        final String attr1 = "attr1";
        final String attr2 = "attr2";
        final String attr3 = "attr3";
        final String value1 = "val1";
        final String value2 = "2";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");

        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attr1.toCharArray(), 0, attr1.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, "string", null);
        candidate.characters(value1.toCharArray(), 0, value1.length());
        candidate.endElement(null, null, "string");

        candidate.startElement(null, null, "key", null);
        candidate.characters(attr2.toCharArray(), 0, attr2.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, "integer", null);
        candidate.characters(value2.toCharArray(), 0, value2.length());
        candidate.endElement(null, null, "integer");

        candidate.startElement(null, null, "key", null);
        candidate.characters(attr3.toCharArray(), 0, attr3.length());
        candidate.endElement(null, null, "key");
        candidate.startElement(null, null, "true", null);
        candidate.endElement(null, null, "true");

        candidate.endElement(null, null, "dict");

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        assertTrue(valueCollector.values.size() == 3);
        assertEquals(
                "[ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=1, listIndex=0, value=val1], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=2, listIndex=0, value=2], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=3, listIndex=0, value=true]]",
                valueCollector.values.toString());

        assertEquals(
                "[attr1, attr2, attr3]",
                attrNamesCollector.toString());
    }

    @Test
    public void arrayOfDictOfValuesTest() throws Exception {
        final ValueCollector valueCollector = new ValueCollector();
        final List<String> attrNamesCollector = new ArrayList<>();
        final Function<String, ConfigurationAttribute> attrConverter = attrName -> {
            attrNamesCollector.add(attrName);
            return attributeResolver.apply(attrName);
        };
        final ExamConfigXMLParser candidate = new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueCollector,
                attrConverter);

        final String attribute = "attribute";

        final String attr1 = "attr1";
        final String attr2 = "attr2";
        final String attr3 = "attr3";
        final String value1 = "val1";
        final String value2 = "2";

        candidate.startElement(null, null, "plist", null);
        candidate.startElement(null, null, "dict", null);

        candidate.startElement(null, null, "key", null);
        candidate.characters(attribute.toCharArray(), 0, attribute.length());
        candidate.endElement(null, null, "key");

        candidate.startElement(null, null, "array", null);

        for (int i = 0; i < 3; i++) {
            candidate.startElement(null, null, "dict", null);

            candidate.startElement(null, null, "key", null);
            candidate.characters(attr1.toCharArray(), 0, attr1.length());
            candidate.endElement(null, null, "key");
            candidate.startElement(null, null, "string", null);
            candidate.characters(value1.toCharArray(), 0, value1.length());
            candidate.endElement(null, null, "string");

            candidate.startElement(null, null, "key", null);
            candidate.characters(attr2.toCharArray(), 0, attr2.length());
            candidate.endElement(null, null, "key");
            candidate.startElement(null, null, "integer", null);
            candidate.characters(value2.toCharArray(), 0, value2.length());
            candidate.endElement(null, null, "integer");

            candidate.startElement(null, null, "key", null);
            candidate.characters(attr3.toCharArray(), 0, attr3.length());
            candidate.endElement(null, null, "key");
            candidate.startElement(null, null, "true", null);
            candidate.endElement(null, null, "true");

            candidate.endElement(null, null, "dict");
        }

        candidate.endElement(null, null, "array");

        candidate.endElement(null, null, "dict");
        candidate.endElement(null, null, "plist");

        assertFalse(valueCollector.values.isEmpty());
        assertTrue(valueCollector.values.size() == 9);
        assertEquals(
                "[ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=1, listIndex=0, value=val1], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=2, listIndex=0, value=2], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=3, listIndex=0, value=true], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=1, listIndex=1, value=val1], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=2, listIndex=1, value=2], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=3, listIndex=1, value=true], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=1, listIndex=2, value=val1], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=2, listIndex=2, value=2], "
                        + "ConfigurationValue [id=null, institutionId=1, configurationId=1, attributeId=3, listIndex=2, value=true]]",
                valueCollector.values.toString());

        assertEquals(
                "[attribute.attr1, attribute.attr2, attribute.attr3, attribute.attr1, attribute.attr2, attribute.attr3, attribute.attr1, attribute.attr2, attribute.attr3, attribute]",
                attrNamesCollector.toString());
    }

    private static final class ValueCollector implements Consumer<ConfigurationValue> {
        List<ConfigurationValue> values = new ArrayList<>();

        @Override
        public void accept(final ConfigurationValue value) {
            this.values.add(value);
        }
    }
}
