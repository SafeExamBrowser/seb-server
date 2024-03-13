/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion.ClientVersion;

public class SEBClientVersionServiceImplTest {

    @Test
    public void testClientVersion1() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion("", "");
        assertNull(clientVersion);
    }

    @Test
    public void testClientVersion2() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                "3.3.2 (x64)");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=Win, major=3, minor=3, patch=2, isAllianceVersion=false]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion3() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                "3.3.2 BETA (x64)");
        assertNull(clientVersion);
    }

    @Test
    public void testClientVersion4() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                "3.5.0 BETA (x64) Alliance Edition");
        assertNull(clientVersion);
    }

    @Test
    public void testClientVersion5() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "Windows 10, Microsoft Windows NT 10.0.19042.0 (x64)",
                "3.5.0 (x64) Alliance Edition");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=Win, major=3, minor=5, patch=0, isAllianceVersion=true]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion6() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "macOS Version 12.6.2 (Build 21G312)",
                "3.2.1AE");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=Mac, major=3, minor=2, patch=1, isAllianceVersion=true]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion7() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "macOS Version 12.6.2 (Build 21G312)",
                "3.2.1 AE");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=Mac, major=3, minor=2, patch=1, isAllianceVersion=true]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion8() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "macOS Version 12.6.4 (Build 21G521)",
                "3.2.4");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=Mac, major=3, minor=2, patch=4, isAllianceVersion=false]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion9() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "iPad (iPadOS 15.6.1)",
                "3.3");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=iOS, major=3, minor=3, patch=0, isAllianceVersion=false]",
                clientVersion.toString());
    }

    @Test
    public void testClientVersion10() {
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();
        final ClientVersion clientVersion = clientVersionServiceMockup.extractClientVersion(
                "iPhone (iOS 12.5.7)",
                "3.2.3");
        assertNotNull(clientVersion);
        assertEquals(
                "ClientVersion [osTypeString=iOS, major=3, minor=2, patch=3, isAllianceVersion=false]",
                clientVersion.toString());
    }

    @Test
    public void testExactVersionRestriction1() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.3");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3.1")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 BETA")));
    }

    @Test
    public void testExactAEVersionRestriction1() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.3.AE");
        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3.1")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3AE")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE BETA")));
    }

    @Test
    public void testMinimalVersionRestriction1() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.min");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.1.13")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));

        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1 BETA")));
    }

    @Test
    public void testMinimalVersionRestriction2() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("mac.3.2.min");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.1.13")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1")));
    }

    @Test
    public void testMinimalVersionRestriction3() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("ios.3.2.min");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3 BETA")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.1.13")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1")));
    }

    @Test
    public void testMinimalVersionRestriction4() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.1.min");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.1.13")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.3 AE")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1 BETA")));
    }

    @Test
    public void testMinimalAEVersionRestriction1() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.AE.min");

        final SEBClientVersionServiceImpl clientVersionServiceMockup = getClientVersionServiceMockup();

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "iPhone (iOS 12.5.7)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "macOS Version 12.6.4 (Build 21G521)",
                        "3.2.3")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.1.13")));

        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1")));
        assertFalse(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1")));

        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2 AE")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.0 AE")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.2.1 AE")));
        assertTrue(allowedSEBVersion.match(
                clientVersionServiceMockup.extractClientVersion(
                        "Windows 10, Microsoft Windows NT 10.0.19043.0 (x64)",
                        "3.3.1 AE")));
    }

    private SEBClientVersionServiceImpl getClientVersionServiceMockup() {
        return new SEBClientVersionServiceImpl(
                null,
                null,
                "Win,Windows",
                "macOS",
                "iOS,iPad,iPadOS",
                "1.0.0.0,BETA");
    }

}
