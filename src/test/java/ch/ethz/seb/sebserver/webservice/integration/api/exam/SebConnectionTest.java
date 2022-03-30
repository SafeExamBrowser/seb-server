/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ClientConnectionDataInternal;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class SebConnectionTest extends ExamAPIIntegrationTester {

    @Autowired
    private ClientConnectionRecordMapper clientConnectionRecordMapper;
    @Autowired
    private ClientEventRecordMapper clientEventRecordMapper;
    @Autowired
    private JSONMapper jsonMapper;

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCreateConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, 2L);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());
        final String contentAsString = createConnection.getContentAsString();
        assertEquals(
                "[{\"examId\":\"2\",\"name\":\"Demo Quiz 6 (MOCKUP)\",\"url\":\"http://lms.mockup.com/api/\",\"lmsType\":\"MOCKUP\"}]",
                contentAsString);

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
        assertTrue(clientConnectionRecord.getVdi() == 0);
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

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCreateConnectionWithExamId() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, 2L);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());
        final String contentAsString = createConnection.getContentAsString();
        assertEquals(
                "[{\"examId\":\"2\",\"name\":\"Demo Quiz 6 (MOCKUP)\",\"url\":\"http://lms.mockup.com/api/\",\"lmsType\":\"MOCKUP\"}]",
                contentAsString);

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
        assertEquals("2", String.valueOf(clientConnectionRecord.getExamId()));
        assertEquals("CONNECTION_REQUESTED", String.valueOf(clientConnectionRecord.getStatus()));
        assertEquals(connectionToken, clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertNull(clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 0);
        assertNull(clientConnectionRecord.getVirtualClientAddress());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCreateConnectionWithWrongExamId() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, 1L);
        assertNotNull(createConnection);

        // expecting error status
        assertTrue(createConnection.getStatus() != HttpStatus.OK.value());
        final String contentAsString = createConnection.getContentAsString();
        final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                contentAsString,
                new TypeReference<Collection<APIMessage>>() {
                });
        final APIMessage error = errorMessage.iterator().next();
        assertEquals(ErrorMessage.UNEXPECTED.messageCode, error.messageCode);
        assertEquals("The exam 1 is not running", error.details);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCreateConnectionNoInstitutionId() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, null, null);
        assertNotNull(createConnection);

        // expecting error status
        assertTrue(createConnection.getStatus() != HttpStatus.OK.value());
        final String contentAsString = createConnection.getContentAsString();
        final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                contentAsString,
                new TypeReference<Collection<APIMessage>>() {
                });
        final APIMessage error = errorMessage.iterator().next();
        assertEquals(ErrorMessage.ILLEGAL_API_ARGUMENT.messageCode, error.messageCode);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testUpdateConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // check cache after creation
        Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNull(ccdi.clientConnection.examId);
        assertTrue(ccdi.indicatorValues.isEmpty());

        final MockHttpServletResponse updatedConnection = super.updateConnection(
                accessToken,
                connectionToken,
                2L,
                "userSessionId");

        // check correct response
        assertTrue(HttpStatus.OK.value() == updatedConnection.getStatus());

        // check correct stored
        final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                .selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.examId, SqlBuilder.isEqualTo(2L))
                .build()
                .execute();

        assertTrue(records.size() == 1);
        final ClientConnectionRecord clientConnectionRecord = records.get(0);
        assertEquals("1", String.valueOf(clientConnectionRecord.getInstitutionId()));
        assertEquals("2", String.valueOf(clientConnectionRecord.getExamId()));
        assertEquals("AUTHENTICATED", String.valueOf(clientConnectionRecord.getStatus()));
        assertNotNull(clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertEquals("-- (userSessionId)", clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 0);
        assertNull(clientConnectionRecord.getVirtualClientAddress());

        // check cache after update
        connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testUpdateConnectionWithWrongConnectionToken() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse updatedConnection = super.updateConnection(
                accessToken,
                "",
                2L,
                "userSessionId");

        // expecting error status
        assertTrue(HttpStatus.OK.value() != updatedConnection.getStatus());
        final String contentAsString = updatedConnection.getContentAsString();
        final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                contentAsString,
                new TypeReference<Collection<APIMessage>>() {
                });
        final APIMessage error = errorMessage.iterator().next();
        assertEquals(ErrorMessage.RESOURCE_NOT_FOUND.messageCode, error.messageCode);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testEstablishConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // check cache after creation
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNull(ccdi.clientConnection.examId);
        assertTrue(ccdi.indicatorValues.isEmpty());

        final MockHttpServletResponse updatedConnection = super.establishConnection(
                accessToken,
                connectionToken,
                2L,
                "userSessionId");

        // check correct response
        assertTrue(HttpStatus.OK.value() == updatedConnection.getStatus());

        // check correct stored
        final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                .selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.examId, SqlBuilder.isEqualTo(2L))
                .build()
                .execute();

        assertTrue(records.size() == 1);
        final ClientConnectionRecord clientConnectionRecord = records.get(0);
        assertEquals("1", String.valueOf(clientConnectionRecord.getInstitutionId()));
        assertEquals("2", String.valueOf(clientConnectionRecord.getExamId()));
        assertEquals("ACTIVE", String.valueOf(clientConnectionRecord.getStatus()));
        assertNotNull(clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertEquals("-- (userSessionId)", clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 0);
        assertNull(clientConnectionRecord.getVirtualClientAddress());

        // check cache after update
        ccdi = (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testEstablishConnectionNoExamLeadsToError() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // check cache after creation
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNull(ccdi.clientConnection.examId);
        assertTrue(ccdi.indicatorValues.isEmpty());

        final MockHttpServletResponse updatedConnection = super.establishConnection(
                accessToken,
                connectionToken,
                null,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() != updatedConnection.getStatus());
        final String contentAsString = updatedConnection.getContentAsString();
        final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                contentAsString,
                new TypeReference<Collection<APIMessage>>() {
                });
        final APIMessage error = errorMessage.iterator().next();
        assertEquals(ErrorMessage.UNEXPECTED.messageCode, error.messageCode);
        assertEquals("ClientConnection integrity violation", error.details);

        // check correct stored (no changes)
        final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                .selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.examId, SqlBuilder.isNull())
                .build()
                .execute();

        assertTrue(records.size() == 1);
        final ClientConnectionRecord clientConnectionRecord = records.get(0);
        assertEquals("1", String.valueOf(clientConnectionRecord.getInstitutionId()));
        assertNull(clientConnectionRecord.getExamId());
        assertEquals("CONNECTION_REQUESTED", String.valueOf(clientConnectionRecord.getStatus()));
        assertNotNull(clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertNull(clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 0);
        assertNull(clientConnectionRecord.getVirtualClientAddress());

        // check cache fail remains the same
        ccdi = (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNull(ccdi.clientConnection.examId);
        assertTrue(ccdi.indicatorValues.isEmpty());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testCloseConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse establishConnection = super.establishConnection(
                accessToken,
                connectionToken,
                2L,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() == establishConnection.getStatus());

        // check cache after creation
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());

        // close connection
        final MockHttpServletResponse closedConnection = super.closeConnection(
                accessToken,
                connectionToken);

        // check correct response
        assertTrue(HttpStatus.OK.value() == closedConnection.getStatus());

        // check correct stored (no changes)
        final List<ClientConnectionRecord> records = this.clientConnectionRecordMapper
                .selectByExample()
                .where(ClientConnectionRecordDynamicSqlSupport.examId, SqlBuilder.isEqualTo(2L))
                .build()
                .execute();

        assertTrue(records.size() == 1);
        final ClientConnectionRecord clientConnectionRecord = records.get(0);
        assertEquals("1", String.valueOf(clientConnectionRecord.getInstitutionId()));
        assertEquals("2", String.valueOf(clientConnectionRecord.getExamId()));
        assertEquals("CLOSED", String.valueOf(clientConnectionRecord.getStatus()));
        assertNotNull(clientConnectionRecord.getConnectionToken());
        assertNotNull(clientConnectionRecord.getClientAddress());
        assertNull(clientConnectionRecord.getExamUserSessionId());
        assertTrue(clientConnectionRecord.getVdi() == 0);
        assertNull(clientConnectionRecord.getVirtualClientAddress());

        // check cache after update
        ccdi = (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());
        assertEquals("CLOSED", ccdi.clientConnection.status.toString());

    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testPing() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse establishConnection = super.establishConnection(
                accessToken,
                connectionToken,
                2L,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() == establishConnection.getStatus());

        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());
        final IndicatorValue pingIndicator = ccdi.indicatorValues.iterator().next();
        assertTrue(pingIndicator.getIndicatorId() == 1L);

        super.sendPing(accessToken, connectionToken, 1);
        Thread.sleep(200);
        super.sendPing(accessToken, connectionToken, 2);
        Thread.sleep(200);
        super.sendPing(accessToken, connectionToken, 3);
        Thread.sleep(200);
        super.sendPing(accessToken, connectionToken, 5);
        Thread.sleep(200);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testSendPingToNoneEstablishedConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse sendPing = super.sendPing(accessToken, connectionToken, 1);
        // check correct response
        assertTrue(HttpStatus.NO_CONTENT.value() == sendPing.getStatus());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testEvent() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse establishConnection = super.establishConnection(
                accessToken,
                connectionToken,
                2L,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() == establishConnection.getStatus());

        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);
        assertFalse(ccdi.indicatorValues.isEmpty());
        final IndicatorValue pingIndicator = ccdi.indicatorValues.iterator().next();
        assertTrue(pingIndicator.getIndicatorId() == 1L);

        MockHttpServletResponse sendEvent = super.sendEvent(
                accessToken,
                connectionToken,
                "INFO_LOG",
                1l,
                100.0,
                "testEvent1");

        // check correct response
        assertTrue(HttpStatus.NO_CONTENT.value() == sendEvent.getStatus());

        // check event stored on db
        List<ClientEventRecord> events = this.clientEventRecordMapper
                .selectByExample()
                .where(
                        ClientEventRecordDynamicSqlSupport.type,
                        SqlBuilder.isNotEqualTo(EventType.REMOVED_EVENT_TYPE_LAST_PING))
                .build()
                .execute();

        assertFalse(events.isEmpty());
        final ClientEventRecord clientEventRecord = events.get(0);
        assertNotNull(clientEventRecord);
        assertEquals(Long.valueOf(1), clientEventRecord.getClientTime());
        assertEquals("testEvent1", clientEventRecord.getText());

        // send another event
        sendEvent = super.sendEvent(
                accessToken,
                connectionToken,
                "ERROR_LOG",
                2l,
                10000.0,
                "testEvent2");

        // check correct response
        assertTrue(HttpStatus.NO_CONTENT.value() == sendEvent.getStatus());

        // check event stored on db
        events = this.clientEventRecordMapper
                .selectByExample()
                .build()
                .execute();

        assertFalse(events.isEmpty());
        assertTrue(events.stream().filter(ev -> ev.getClientTime().equals(2l)).findFirst().isPresent());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testSendEventToNoneEstablishedConnectionShouldBePossible() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse sendEvent = super.sendEvent(
                accessToken,
                connectionToken,
                "INFO_LOG",
                1l,
                100.0,
                "testEvent1");
        // check correct response
        assertTrue(HttpStatus.NO_CONTENT.value() == sendEvent.getStatus());

        final List<ClientEventRecord> events = this.clientEventRecordMapper
                .selectByExample()
                .build()
                .execute();
        assertFalse(events.isEmpty());
        final ClientEventRecord clientEventRecord = events.get(0);
        assertEquals("testEvent1", clientEventRecord.getText());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testSendEventToNoneExistingConnectionIsIgnored() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse sendEvent = super.sendEvent(
                accessToken,
                "someInvalidConnectionToken",
                "INFO_LOG",
                1l,
                100.0,
                "testEvent1");
        // check correct response
        assertTrue(HttpStatus.NO_CONTENT.value() == sendEvent.getStatus());

        final List<ClientEventRecord> events = this.clientEventRecordMapper
                .selectByExample()
                .build()
                .execute();
        assertTrue(events.isEmpty());
    }
}
