/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.graphics.RGB;
import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;

public class UtilsTest {

    @Test
    public void testImmutableCollectionOf() {
        final Collection<String> r1 = Utils.immutableCollectionOf((String[]) null);
        final Collection<String> r2 = Utils.immutableCollectionOf((String) null);
        final Collection<String> r3 = Utils.immutableCollectionOf(null, null);
        final Collection<String> r4 = Utils.immutableCollectionOf("ONE", "TWO");

        assertEquals("[]", r1.toString());
        assertEquals("[null]", r2.toString());
        assertEquals("[null, null]", r3.toString());
        assertEquals("[ONE, TWO]", r4.toString());
    }

    @Test
    public void testImmutableSetOf() {
        final Set<String> r1 = Utils.immutableSetOf((String[]) null);
        final Set<String> r2 = Utils.immutableSetOf((String) null);
        final Set<String> r3 = Utils.immutableSetOf(null, null);
        final Set<String> r4 = Utils.immutableSetOf("ONE", "TWO");
        final Set<String> r5 = Utils.immutableSetOf("ONE", "TWO", "ONE");

        assertEquals("[]", r1.toString());
        assertEquals("[null]", r2.toString());
        assertEquals("[null]", r3.toString());
        assertEquals("[ONE, TWO]", r4.toString());
        assertEquals("[ONE, TWO]", r5.toString());
    }

    @Test
    public void testEscapeHTML_XML_EcmaScript() {
        assertEquals("test", Utils.escapeHTML_XML_EcmaScript("test"));
        assertEquals("test&amp;lt;test&amp;gt;", Utils.escapeHTML_XML_EcmaScript("test<test>"));
        assertEquals("test;test", Utils.escapeHTML_XML_EcmaScript("test;test"));
        assertEquals("test-test", Utils.escapeHTML_XML_EcmaScript("test-test"));
        assertEquals("test+test", Utils.escapeHTML_XML_EcmaScript("test+test"));
        assertEquals("test&amp;amp;test", Utils.escapeHTML_XML_EcmaScript("test&test"));
        assertEquals("test[test]", Utils.escapeHTML_XML_EcmaScript("test[test]"));
    }

    @Test
    public void testPreventResponseSplittingAttack() {
        assertEquals("test", Utils.preventResponseSplittingAttack("test"));
        try {
            Utils.preventResponseSplittingAttack("test\nergerg");
        } catch (final IllegalArgumentException e) {
            assertEquals("Illegal argument: test\n"
                    + "ergerg", e.getMessage());
        }

        try {
            Utils.preventResponseSplittingAttack("test\rergerg");
        } catch (final IllegalArgumentException e) {
            assertEquals("Illegal argument: test\r"
                    + "ergerg", e.getMessage());
        }
    }

    @Test
    public void testHash_SHA_256_Base_16() {
        assertEquals(
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                Utils.hash_SHA_256_Base_16("test"));
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Utils.hash_SHA_256_Base_16(""));

        assertNull(Utils.hash_SHA_256_Base_16(null));
    }

    @Test
    public void testTruePredicate() {
        assertTrue(Utils.truePredicate().test("some"));
    }

    @Test
    public void testFalsePredicate() {
        assertFalse(Utils.falsePredicate().test("some"));
    }

    @Test
    public void testToSeconds() {
        assertEquals("60", String.valueOf(Utils.toSeconds(Constants.MINUTE_IN_MILLIS)));
    }

    @Test
    public void testToRGB() {
        final String rgbString1 = null;
        final String rgbString2 = "";
        final String rgbString3 = "wrfgwr";
        final String rgbString4 = "#aabbcc";
        final String rgbString5 = "aabbcc";

        assertEquals("RGB {255, 255, 255}", Utils.toRGB(rgbString1).toString());
        assertEquals("RGB {255, 255, 255}", Utils.toRGB(rgbString2).toString());
        try {
            assertEquals("RGB {255, 255, 255}", Utils.toRGB(rgbString3).toString());
            fail("NumberFormatException expected here");
        } catch (final NumberFormatException e) {
            assertEquals("For input string: \"wr\"", e.getMessage());
        }
        assertEquals("RGB {170, 187, 204}", Utils.toRGB(rgbString4).toString());
        assertEquals("RGB {170, 187, 204}", Utils.toRGB(rgbString5).toString());

    }

    @Test
    public void testParseRGB() {
        String colorString = "FFFFFF";
        assertEquals(
                "RGB {255, 255, 255}",
                Utils.parseRGB(colorString).toString());

        colorString = "FFaa34";
        assertEquals(
                "RGB {255, 170, 52}",
                Utils.parseRGB(colorString).toString());
    }

    @Test
    public void testGetErrorCauseMessage() {
        assertEquals("--", Utils.getErrorCauseMessage(null));
        assertEquals("--", Utils.getErrorCauseMessage(new RuntimeException("origMessage")));
        assertEquals("java.lang.RuntimeException : null",
                Utils.getErrorCauseMessage(new RuntimeException("origMessage", new RuntimeException())));
        assertEquals("java.lang.RuntimeException : causeMessage",
                Utils.getErrorCauseMessage(new RuntimeException("origMessage", new RuntimeException("causeMessage"))));
    }

    @Test
    public void testDarkColor() {
        final RGB color = new RGB(255, 255, 255);
        final RGB color1 = new RGB(101, 100, 200);
        final RGB color2 = new RGB(100, 100, 200);
        final RGB color3 = new RGB(0, 0, 0);

        assertTrue(Utils.darkColorContrast(color));
        assertTrue(Utils.darkColorContrast(color1));
        assertFalse(Utils.darkColorContrast(color2));
        assertFalse(Utils.darkColorContrast(color3));

    }

    @Test
    public void testParseColorString() {
        final RGB color = new RGB(255, 255, 255);
        assertEquals("ffffff", Utils.parseColorString(color));
        assertNull(Utils.parseColorString(null));
    }

    @Test
    public void testTimestamp() {
        final long currentTimeMillis = DateTimeUtils.currentTimeMillis();
        final long millisecondsNow = Utils.getMillisecondsNow();
        System.out.println("************* currentTimeMillis: " + currentTimeMillis);
        System.out.println("************* millisecondsNow: " + millisecondsNow);
        System.out.println("************* div: " + (millisecondsNow - currentTimeMillis));
    }

    @Test
    public void testToAppFormUrlEncodedBody() {
        String appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody("attr1", Arrays.asList("1", "2", "3"));
        assertEquals("attr1[]=1&attr1[]=2&attr1[]=3", appFormUrlEncodedBody);

        appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody("attr1", Collections.emptyList());
        assertEquals("", appFormUrlEncodedBody);

        appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody("attr1", null);
        assertEquals("", appFormUrlEncodedBody);

        final LinkedMultiValueMap<String, String> linkedMultiValueMap = new LinkedMultiValueMap<>();
        linkedMultiValueMap.add("attr1", "value1");
        linkedMultiValueMap.add("attr2", "value2");
        linkedMultiValueMap.add("attr3", "value3");
        appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody(linkedMultiValueMap);
        assertEquals("attr1=value1&attr2=value2&attr3=value3", appFormUrlEncodedBody);

        appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody(new LinkedMultiValueMap<>());
        assertEquals("", appFormUrlEncodedBody);

        appFormUrlEncodedBody = Utils.toAppFormUrlEncodedBody(null);
        assertEquals("", appFormUrlEncodedBody);

    }

    @Test
    public void testPingHost() {
        assertTrue(Utils.pingHost("https://www.google.com"));
        assertFalse(Utils.pingHost("www.google.com"));
        assertFalse(Utils.pingHost("some"));
    }

    @Test
    public void testToCSVString() {
        final String nullString = Utils.toCSVString(null);
        final String emptyString = Utils.toCSVString("");
        assertEquals(StringUtils.EMPTY, nullString);
        assertEquals(StringUtils.EMPTY, emptyString);
    }

    @Test
    public void testGetOrEmptyDisplayValue() {
        final String nullValue = Utils.getOrEmptyDisplayValue(null);
        final String emptyString = Utils.getOrEmptyDisplayValue("");
        final String someString = Utils.getOrEmptyDisplayValue("some");

        assertEquals(Constants.EMPTY_NOTE, nullValue);
        assertEquals(Constants.EMPTY_NOTE, emptyString);
        assertEquals("some", someString);
    }

    @Test
    public void testFormatStackTracePrint() {
        final StringBuilder formatStackTracePrint =
                Utils.formatStackTracePrint(10, Thread.currentThread().getStackTrace());
        assertTrue(formatStackTracePrint.toString()
                .contains("ch.ethz.seb.sebserver.gbl.util.UtilsTest.testFormatStackTracePrint"));
    }

}
