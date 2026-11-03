/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SecureSAXParserFactoryTest {

    @Test
    public void externalGeneralEntitiesAreNotResolved() throws Exception {
        final Path secret = Files.createTempFile("sebserver-xxe-test", ".txt");
        try {
            Files.write(secret, "TOP-SECRET".getBytes(StandardCharsets.UTF_8));
            final String xml = "<?xml version=\"1.0\"?>"
                    + "<!DOCTYPE plist [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                    + "<plist version=\"1.0\"><dict><key>quitURL</key><string>&xxe;</string></dict></plist>";
            try {
                assertFalse(parseText(xml).contains("TOP-SECRET"));
            } catch (final SAXException e) {
                // rejecting the document is fine as well, as long as nothing is disclosed
                assertFalse(String.valueOf(e.getMessage()).contains("TOP-SECRET"));
            }
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    public void externalDtdIsNotLoaded() throws Exception {
        // the DTD location is not reachable: loading it would make the parse fail
        final String xml = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE plist SYSTEM \"http://127.0.0.1:9/sebserver-test.dtd\">"
                + "<plist version=\"1.0\"><dict><key>a</key><string>b</string></dict></plist>";
        assertEquals("ab", parseText(xml));
    }

    @Test
    public void externalParameterEntitiesAreNotResolved() throws Exception {
        final String xml = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE plist [<!ENTITY % ext SYSTEM \"http://127.0.0.1:9/sebserver-test.dtd\"> %ext;]>"
                + "<plist version=\"1.0\"><dict><key>a</key><string>b</string></dict></plist>";
        assertEquals("ab", parseText(xml));
    }

    @Test
    public void applePlistDoctypeIsAcceptedWithoutNetworkAccess() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">"
                + "<plist version=\"1.0\"><dict><key>allowQuit</key><true/></dict></plist>";
        assertEquals("allowQuit", parseText(xml));
    }

    @Test
    public void internalEntitiesStillWork() throws Exception {
        final String xml = "<?xml version=\"1.0\"?><!DOCTYPE plist [<!ENTITY org \"ETH\">]>"
                + "<plist><string>&org; &amp; SEB</string></plist>";
        assertEquals("ETH & SEB", parseText(xml));
    }

    @Test(expected = SAXException.class)
    public void entityExpansionIsLimited() throws Exception {
        // "billion laughs": nested internal entities that would expand to 3 * 10^9 characters
        final StringBuilder dtd = new StringBuilder("<!DOCTYPE plist [<!ENTITY lol0 \"lol\">");
        for (int i = 1; i <= 9; i++) {
            dtd.append("<!ENTITY lol").append(i).append(" \"");
            for (int j = 0; j < 10; j++) {
                dtd.append("&lol").append(i - 1).append(';');
            }
            dtd.append("\">");
        }
        dtd.append("]>");
        parseText("<?xml version=\"1.0\"?>" + dtd + "<plist><string>&lol9;</string></plist>");
    }

    private static String parseText(final String xml) throws Exception {
        final StringBuilder text = new StringBuilder();
        SecureSAXParserFactory.newSAXParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                new DefaultHandler() {
                    @Override
                    public void characters(final char[] ch, final int start, final int length) {
                        text.append(ch, start, length);
                    }
                });
        return text.toString();
    }
}
