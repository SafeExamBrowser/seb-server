/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class ExamAPIAccessTokenRequestTest extends ExamAPIIntegrationTester {

    @Test
    public void testRequestAccessToken() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);
    }

}
