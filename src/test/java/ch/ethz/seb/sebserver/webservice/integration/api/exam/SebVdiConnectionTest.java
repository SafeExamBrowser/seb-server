/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class SebVdiConnectionTest extends ExamAPIIntegrationTester {

    @Autowired
    private ClientConnectionRecordMapper clientConnectionRecordMapper;
    @Autowired
    private ClientEventRecordMapper clientEventRecordMapper;
    @Autowired
    private JSONMapper jsonMapper;

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCreateVDIConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("testVDI", "testVDI", "read write");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, 2L);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());
        final String contentAsString = createConnection.getContentAsString();
//        assertEquals(
//                "[{\"examId\":\"2\",\"name\":\"Demo Quiz 6 (MOCKUP)\",\"url\":\"http://lms.mockup.com/api/\",\"lmsType\":\"MOCKUP\"}]",
//                contentAsString);

        // check connection token
        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // check correct stored
        final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                .selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.examId, SqlBuilder.isEqualTo(2L))
                .build()
                .execute();

        assertTrue(records.size() == 1);
        final ClientConnectionRecord clientConnectionRecord = records.get(0);
        assertEquals("1", String.valueOf(clientConnectionRecord.getInstitutionId()));
        assertEquals("2", clientConnectionRecord.getExamId().toString());
        assertEquals("CONNECTION_REQUESTED", String.valueOf(clientConnectionRecord.getStatus()));
        assertEquals(connectionToken, clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertNull(clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 1);
        assertNull(clientConnectionRecord.getVirtualClientAddress());

        // check caching
        final Cache examCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM);
        final ValueWrapper exam = examCache.get(2L);
        assertNotNull(exam);

        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ValueWrapper connection = connectionCache.get(connectionToken);
        assertNotNull(connection);

    }

}
