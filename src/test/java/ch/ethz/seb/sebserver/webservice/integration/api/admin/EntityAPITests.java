/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class EntityAPITests extends AdministrationAPIIntegrationTester {

    // TODO test to create two Institutions with the same name (should not be possible)
    @Test
    public void createInstitutionsWithSameName() {

    }

    // TOOD test get for ids with several equal ids --> should get only one instance per id
    @Test
    public void getInstitutionForIdsWithEqualIds() {

    }
}
