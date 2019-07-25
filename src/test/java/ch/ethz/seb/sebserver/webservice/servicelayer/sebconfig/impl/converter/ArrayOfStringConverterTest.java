/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

public class ArrayOfStringConverterTest {

    @Test
    public void testXML() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToXML(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, "one,two,tree"));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><array><string>one</string><string>two</string><string>tree</string></array>",
                xmlString);
    }

    @Test
    public void testEmptyXML() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToXML(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, ""));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><array></array>",
                xmlString);
    }

    @Test
    public void testNullXML() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToXML(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, null));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><array><string>defaultValue</string></array>",
                xmlString);
    }

    @Test
    public void testJSON() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToJSON(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, "one,two,tree"));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "\"attribute\":[\"one\",\"two\",\"tree\"]",
                xmlString);
    }

    @Test
    public void testEmptyJSON() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToJSON(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, ""));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "\"attribute\":[]",
                xmlString);
    }

    @Test
    public void testNullJSON() throws Exception {
        final ArrayOfStringConverter arrayOfStringConverter = new ArrayOfStringConverter();
        final ConfigurationAttribute attribute = new ConfigurationAttribute(
                1L,
                null,
                "attribute",
                AttributeType.CHECKBOX,
                null, null, null, "defaultValue");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        arrayOfStringConverter.convertToJSON(
                out,
                attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 1, null));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "\"attribute\":[\"defaultValue\"]",
                xmlString);
    }

}
