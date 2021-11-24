/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

public class WebserviceTest extends AdministrationAPIIntegrationTester {

    private static final String WEBSERVICE_1 = "WEBSERVICE_1";
    private static final String WEBSERVICE_2 = "WEBSERVICE_2";

    @Autowired
    private WebserviceInfoDAO webserviceInfoDAO;
    @Autowired
    private WebserviceInfo webserviceInfo;

    @Before
    public void init() {
        this.webserviceInfoDAO.unregister(this.webserviceInfo.getWebserviceUUID());
        this.webserviceInfoDAO.register(WEBSERVICE_1, "0.0.0.1");
        this.webserviceInfoDAO.register(WEBSERVICE_2, "0.0.0.1");
    }

    @After
    public void cleanup() {
        this.webserviceInfoDAO.unregister(WEBSERVICE_1);
        this.webserviceInfoDAO.unregister(WEBSERVICE_2);
        this.webserviceInfoDAO.register(
                this.webserviceInfo.getWebserviceUUID(),
                this.webserviceInfo.getLocalHostAddress());
    }

    @Test
    public void testFistBecomeMaster() {
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
        assertFalse(this.webserviceInfoDAO.isMaster(WEBSERVICE_2));
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
    }

    @Test
    public void testUnregister_OtherBecomeMaster() {
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
        assertTrue(this.webserviceInfoDAO.unregister(WEBSERVICE_1));
        assertFalse(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_2));
    }

    @Test
    public void testOtherBecomeMasterAfterTimout() {
        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_1));
        assertFalse(this.webserviceInfoDAO.isMaster(WEBSERVICE_2));

        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
        }

        // Still not master
        assertFalse(this.webserviceInfoDAO.isMaster(WEBSERVICE_2));

        try {
            Thread.sleep(600);
        } catch (final InterruptedException e) {
        }

        assertTrue(this.webserviceInfoDAO.isMaster(WEBSERVICE_2));
    }

}
