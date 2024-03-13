/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class AllowedSEBVersionTest {

    @Test
    public void testInvalidVersions() {
        assertFalse(new AllowedSEBVersion("").isValidFormat);
        assertFalse(new AllowedSEBVersion("1").isValidFormat);
        assertFalse(new AllowedSEBVersion("a").isValidFormat);
        assertFalse(new AllowedSEBVersion("A").isValidFormat);
        assertFalse(new AllowedSEBVersion("WIN.1.").isValidFormat);
        assertFalse(new AllowedSEBVersion("WIN.1.1A").isValidFormat);
        assertFalse(new AllowedSEBVersion("WIN.1.1.1.1.1.1.1").isValidFormat);
        assertFalse(new AllowedSEBVersion("WIN.1111.1111.baba").isValidFormat);
        assertFalse(new AllowedSEBVersion("WIN.1.ad.1").isValidFormat);
        assertFalse(new AllowedSEBVersion("Win.1.AE").isValidFormat);
        assertFalse(new AllowedSEBVersion("Win.1.min.AE").isValidFormat);

    }

    @Test
    public void testValidVersions() {
        assertTrue(new AllowedSEBVersion("Win.1.1").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.AE.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1.AE.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.min.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1.min.AE").isValidFormat);

        assertTrue(new AllowedSEBVersion("WIN.1.1").isValidFormat);
        assertTrue(new AllowedSEBVersion("Mac.1.1.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("MAC.1.1.1").isValidFormat);
        assertTrue(new AllowedSEBVersion("iOS.1.1.1.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("IOS.1.1.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("ios.1.1.1.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("win.1.1.AE.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("mac.1.1.1.AE.min").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.min.AE").isValidFormat);
        assertTrue(new AllowedSEBVersion("Win.1.1.1.min.AE").isValidFormat);
    }

    @Test
    public void testValidVersionDetails1() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.1.2.AE.min");
        assertTrue(allowedSEBVersion.isValidFormat);
        assertTrue(AllowedSEBVersion.OS_WINDOWS_IDENTIFIER == allowedSEBVersion.osTypeString);
        assertTrue(allowedSEBVersion.major == 3);
        assertTrue(allowedSEBVersion.minor == 1);
        assertTrue(allowedSEBVersion.patch == 2);
        assertTrue(allowedSEBVersion.allianceEdition);
        assertTrue(allowedSEBVersion.minimal);
    }

    @Test
    public void testValidVersionDetails2() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.AE.min");
        assertTrue(allowedSEBVersion.isValidFormat);
        assertTrue(AllowedSEBVersion.OS_WINDOWS_IDENTIFIER == allowedSEBVersion.osTypeString);
        assertTrue(allowedSEBVersion.major == 3);
        assertTrue(allowedSEBVersion.minor == 2);
        assertTrue(allowedSEBVersion.patch == 0);
        assertTrue(allowedSEBVersion.allianceEdition);
        assertTrue(allowedSEBVersion.minimal);
    }

    @Test
    public void testValidVersionDetails3() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.min");
        assertTrue(allowedSEBVersion.isValidFormat);
        assertTrue(AllowedSEBVersion.OS_WINDOWS_IDENTIFIER == allowedSEBVersion.osTypeString);
        assertTrue(allowedSEBVersion.major == 3);
        assertTrue(allowedSEBVersion.minor == 2);
        assertTrue(allowedSEBVersion.patch == 0);
        assertFalse(allowedSEBVersion.allianceEdition);
        assertTrue(allowedSEBVersion.minimal);
    }

    @Test
    public void testValidVersionDetails4() {
        final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion("Win.3.2.5.AE");
        assertTrue(allowedSEBVersion.isValidFormat);
        assertTrue(AllowedSEBVersion.OS_WINDOWS_IDENTIFIER == allowedSEBVersion.osTypeString);
        assertTrue(allowedSEBVersion.major == 3);
        assertTrue(allowedSEBVersion.minor == 2);
        assertTrue(allowedSEBVersion.patch == 5);
        assertTrue(allowedSEBVersion.allianceEdition);
        assertFalse(allowedSEBVersion.minimal);
    }

}
