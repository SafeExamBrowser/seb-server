/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;

public class ConfigAttributeSortOrderTest {

    @Test
    public void testSortOrder() throws Exception {
        // test example from: https://www.safeexambrowser.org/developer/seb-config-key.html
        // For example the key <key>allowWlan</key> comes before <key>allowWLAN</key>

        final ConfigurationAttribute aa = new ConfigurationAttribute(
                1L,
                null,
                "aa",
                AttributeType.CHECKBOX,
                null, null, null, "false");

        final ConfigurationAttribute allowWlan = new ConfigurationAttribute(
                1L,
                null,
                "allowWlan",
                AttributeType.CHECKBOX,
                null, null, null, "false");
        final ConfigurationAttribute allowWLAN = new ConfigurationAttribute(
                1L,
                null,
                "allowWLAN",
                AttributeType.CHECKBOX,
                null, null, null, "false");

        final ConfigurationAttribute zz = new ConfigurationAttribute(
                1L,
                null,
                "zz",
                AttributeType.CHECKBOX,
                null, null, null, "false");

        final List<ConfigurationAttribute> list = Arrays.asList(zz, allowWLAN, aa, allowWlan);
        Collections.sort(list);

        assertEquals(
                "[aa, allowWlan, allowWLAN, zz]",
                list.stream()
                        .map(ConfigurationAttribute::getName)
                        .collect(Collectors.toList())
                        .toString());
    }

}
