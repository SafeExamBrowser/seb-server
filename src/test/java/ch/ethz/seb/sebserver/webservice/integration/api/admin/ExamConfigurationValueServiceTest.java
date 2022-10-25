/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class ExamConfigurationValueServiceTest extends AdministrationAPIIntegrationTester {

    @Autowired
    private ExamConfigurationValueService examConfigurationValueService;
    @Autowired
    private Cryptor cryptor;

    @Test
    public void testGetConfigValues() {
        final String allowQuit = this.examConfigurationValueService
                .getMappedDefaultConfigAttributeValue(2L, "allowQuit");

        assertNotNull(allowQuit);
        assertEquals("true", allowQuit);

        final String hashedQuitPassword = this.examConfigurationValueService
                .getMappedDefaultConfigAttributeValue(2L, "hashedQuitPassword");

        assertNotNull(hashedQuitPassword);
        assertEquals(
                "e6494b842987b4e039a101f14d4a76acc338d33205336f2562c7d8071e3ed65886edbbe3b71a4a33cc09c6",
                hashedQuitPassword);

        final CharSequence plainQuitPassword = this.cryptor.decrypt(hashedQuitPassword).getOrThrow();
        assertEquals("123", plainQuitPassword);
    }

}
