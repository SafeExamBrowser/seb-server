/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.SecureSAXParserFactory;

public class ExamConfigXMLParserSecurityTest {

    private static final Function<String, ConfigurationAttribute> ATTRIBUTE_RESOLVER =
            name -> new ConfigurationAttribute(
                    1L,
                    null, name, (name.contains("array")) ? AttributeType.MULTI_SELECTION : AttributeType.TEXT_FIELD,
                    null, null, null,
                    null);

    @Test
    public void externalEntityContentIsNotImported() throws Exception {
        final Path secret = Files.createTempFile("sebserver-xxe-test", ".txt");
        try {
            Files.write(secret, "TOP-SECRET".getBytes(StandardCharsets.UTF_8));
            final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<!DOCTYPE plist [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                    + "<plist version=\"1.0\"><dict><key>quitURL</key><string>&xxe;</string></dict></plist>";

            final ValueCollector collector = new ValueCollector();
            SecureSAXParserFactory.newSAXParser().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                    newParser(collector));

            for (final ConfigurationValue value : collector.values) {
                assertFalse(String.valueOf(value.value).contains("TOP-SECRET"));
            }
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    public void textReportedInManyChunksIsCollectedCompletely() throws Exception {
        final ValueCollector collector = new ValueCollector();
        final ExamConfigXMLParser parser = newParser(collector);
        startStringValue(parser, "param1");
        final StringBuilder expected = new StringBuilder();
        final char[] chunk = "abc".toCharArray();
        for (int i = 0; i < 20_000; i++) {
            parser.characters(chunk, 0, chunk.length);
            expected.append(chunk);
        }
        endStringValue(parser);

        assertEquals(1, collector.values.size());
        assertEquals(expected.toString(), collector.values.get(0).value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooLongValueIsRejected() throws Exception {
        final ExamConfigXMLParser parser = newParser(new ValueCollector());
        startStringValue(parser, "param1");
        final char[] chunk = new char[1024];
        Arrays.fill(chunk, 'x');
        for (int i = 0; i <= ExamConfigXMLParser.MAX_VALUE_LENGTH / chunk.length; i++) {
            parser.characters(chunk, 0, chunk.length);
        }
    }

    @Test
    public void largeArrayIsJoinedCompletely() throws Exception {
        final ValueCollector collector = new ValueCollector();
        final ExamConfigXMLParser parser = newParser(collector);
        final StringJoiner expected = new StringJoiner(",");
        parser.startElement(null, null, "plist", null);
        parser.startElement(null, null, "dict", null);
        key(parser, "array1");
        parser.startElement(null, null, "array", null);
        for (int i = 0; i < 5_000; i++) {
            final String item = "v" + i;
            parser.startElement(null, null, "string", null);
            parser.characters(item.toCharArray(), 0, item.length());
            parser.endElement(null, null, "string");
            expected.add(item);
        }
        parser.endElement(null, null, "array");
        parser.endElement(null, null, "dict");
        parser.endElement(null, null, "plist");

        assertEquals(1, collector.values.size());
        assertEquals(expected.toString(), collector.values.get(0).value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooLongArrayIsRejected() throws Exception {
        final ExamConfigXMLParser parser = newParser(new ValueCollector());
        final char[] item = new char[1000];
        Arrays.fill(item, 'x');
        parser.startElement(null, null, "plist", null);
        parser.startElement(null, null, "dict", null);
        key(parser, "array1");
        parser.startElement(null, null, "array", null);
        for (int i = 0; i <= ExamConfigXMLParser.MAX_VALUE_LENGTH / item.length; i++) {
            parser.startElement(null, null, "string", null);
            parser.characters(item, 0, item.length);
            parser.endElement(null, null, "string");
        }
    }

    private static ExamConfigXMLParser newParser(final Consumer<ConfigurationValue> valueConsumer) {
        return new ExamConfigXMLParser(
                Mockito.mock(Cryptor.class),
                1L,
                1L,
                valueConsumer,
                ATTRIBUTE_RESOLVER);
    }

    private static void key(final ExamConfigXMLParser parser, final String name) {
        parser.startElement(null, null, "key", null);
        parser.characters(name.toCharArray(), 0, name.length());
        parser.endElement(null, null, "key");
    }

    private static void startStringValue(final ExamConfigXMLParser parser, final String name) {
        parser.startElement(null, null, "plist", null);
        parser.startElement(null, null, "dict", null);
        key(parser, name);
        parser.startElement(null, null, "string", null);
    }

    private static void endStringValue(final ExamConfigXMLParser parser) {
        parser.endElement(null, null, "string");
        parser.endElement(null, null, "dict");
        parser.endElement(null, null, "plist");
    }

    private static final class ValueCollector implements Consumer<ConfigurationValue> {
        final List<ConfigurationValue> values = new ArrayList<>();

        @Override
        public void accept(final ConfigurationValue value) {
            this.values.add(value);
        }
    }
}
