/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.client;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class ProxyDataTest {

    @Test
    public void testCreation() {
        final ProxyData proxyData = new ProxyData("proxyName", 8080, new ClientCredentials("user1", "password"));
        assertEquals(
                "ProxyData [proxyName=proxyName, proxyPort=8080, clientCredentials=ClientCredentials [clientId=user1]]",
                proxyData.toString());
    }

}
