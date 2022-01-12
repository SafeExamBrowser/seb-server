/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.services;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.webservice.integration.api.admin.AdministrationAPIIntegrationTester;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ClientIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.AbstractLogIndicator;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.AbstractLogLevelCountIndicator;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class ClientEventServiceTest extends AdministrationAPIIntegrationTester {

    @Autowired
    private ClientConnectionDAO clientConnectionDAO;
    @Autowired
    private ClientEventDAO clientEventDAO;
    @Autowired
    private SEBClientConnectionService sebClientConnectionService;
    @Autowired
    @Qualifier(AsyncServiceSpringConfig.EXAM_API_EXECUTOR_BEAN_NAME)
    private Executor executor;

    @Test
    public void testCreateLogEvents() {

        final ClientConnection connection = this.clientConnectionDAO
                .createNew(
                        new ClientConnection(null, 1L, 2L, ConnectionStatus.ACTIVE, "token", "userId",
                                "1.1.1.1", "seb_os_name", "seb_machine_name", "seb_version", "", false,
                                "", 1L,
                                1L,
                                null, false))
                .getOrThrow();

        assertNotNull(connection.id);

        this.clientEventDAO
                .createNew(new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "text"))
                .getOrThrow();

        final Collection<ClientEvent> allEvents = this.clientEventDAO.allMatching(new FilterMap()).getOrThrow();
        assertNotNull(allEvents);
        assertFalse(allEvents.isEmpty());
        final ClientEvent event = allEvents.iterator().next();
        assertNotNull(event);
        assertEquals("text", event.text);
    }

    @Test
    public void testErrorLogCountIndicator() {

        final ClientConnection connection = this.clientConnectionDAO
                .createNew(
                        new ClientConnection(null, 1L, 2L, ConnectionStatus.ACTIVE, "token1", "userId",
                                "1.1.1.1", "seb_os_name", "seb_machine_name", "seb_version", "", false,
                                "", 1L,
                                1L,
                                null, false))
                .getOrThrow();

        assertNotNull(connection.id);

        final ClientConnectionData connectionData =
                this.sebClientConnectionService.getExamSessionService().getConnectionData("token1")
                        .getOrThrow();

        assertNotNull(connectionData);
        final Optional<? extends IndicatorValue> findFirst = connectionData.indicatorValues
                .stream()
                .filter(indicator -> ((ClientIndicator) indicator).getType() == IndicatorType.ERROR_COUNT)
                .findFirst();
        assertTrue(findFirst.isPresent());
        final IndicatorValue clientIndicator = findFirst.get();
        assertEquals("0", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.ERROR_COUNT));

        this.sebClientConnectionService.notifyClientEvent(
                "token1",
                new ClientEvent(null, connection.id, EventType.ERROR_LOG, 1L, 1L, 1.0, "some error"));
        waitForExecutor();
        assertEquals("1", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.ERROR_COUNT));

        this.sebClientConnectionService.notifyClientEvent(
                "token1",
                new ClientEvent(null, connection.id, EventType.ERROR_LOG, 1L, 1L, 1.0, "some error"));
        waitForExecutor();
        assertEquals("2", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.ERROR_COUNT));

        // test reset indicator value and load it from persistent storage
        ((AbstractLogLevelCountIndicator) clientIndicator).reset();
        assertEquals("2", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.ERROR_COUNT));

    }

    @Test
    public void testInfoLogWithTagCountIndicator() {

        final ClientConnection connection = this.clientConnectionDAO
                .createNew(
                        new ClientConnection(null, 1L, 2L, ConnectionStatus.ACTIVE, "token2", "userId",
                                "1.1.1.1", "seb_os_name", "seb_machine_name", "seb_version", "", false,
                                "", 1L,
                                1L,
                                null, false))
                .getOrThrow();

        assertNotNull(connection.id);

        final ClientConnectionData connectionData =
                this.sebClientConnectionService.getExamSessionService().getConnectionData("token2")
                        .getOrThrow();

        assertNotNull(connectionData);
        final Optional<? extends IndicatorValue> findFirst = connectionData.indicatorValues
                .stream()
                .filter(indicator -> ((ClientIndicator) indicator).getType() == IndicatorType.INFO_COUNT)
                .findFirst();
        assertTrue(findFirst.isPresent());
        final IndicatorValue clientIndicator = findFirst.get();
        assertEquals("0", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));

        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "some error"));
        waitForExecutor();
        assertEquals("0", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));
        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "<top> some error"));
        waitForExecutor();
        assertEquals("1", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));
        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "some error"));
        waitForExecutor();
        assertEquals("1", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));
        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "<vip> some error"));
        waitForExecutor();
        assertEquals("2", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));
        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "some error"));
        waitForExecutor();
        assertEquals("2", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));

        this.sebClientConnectionService.notifyClientEvent(
                "token2",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "<vip> some error"));
        waitForExecutor();
        // test reset indicator value and load it from persistent storage
        ((AbstractLogLevelCountIndicator) clientIndicator).reset();
        assertEquals("3", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.INFO_COUNT));

    }

    private void waitForExecutor() {
        try {
            while (((ThreadPoolTaskExecutor) this.executor).getActiveCount() > 0) {
                Thread.sleep(20);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBatteryStatusIndicator() {

        final ClientConnection connection = this.clientConnectionDAO
                .createNew(
                        new ClientConnection(null, 1L, 2L, ConnectionStatus.ACTIVE, "token3", "userId",
                                "1.1.1.1", "seb_os_name", "seb_machine_name", "seb_version", "", false,
                                "", 1L,
                                1L,
                                null, false))
                .getOrThrow();

        assertNotNull(connection.id);

        final ClientConnectionData connectionData =
                this.sebClientConnectionService.getExamSessionService().getConnectionData("token3")
                        .getOrThrow();

        assertNotNull(connectionData);
        final Optional<? extends IndicatorValue> findFirst = connectionData.indicatorValues
                .stream()
                .filter(indicator -> ((ClientIndicator) indicator).getType() == IndicatorType.BATTERY_STATUS)
                .findFirst();
        assertTrue(findFirst.isPresent());
        final IndicatorValue clientIndicator = findFirst.get();

        assertEquals("--", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.BATTERY_STATUS));

        this.sebClientConnectionService.notifyClientEvent(
                "token3",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "some info other"));
        waitForExecutor();
        this.sebClientConnectionService.notifyClientEvent(
                "token3",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 1.0, "<vip> some info other"));
        waitForExecutor();
        assertEquals("--", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.BATTERY_STATUS));

        this.sebClientConnectionService.notifyClientEvent(
                "token3",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 90.0, "<battery> some info other"));
        waitForExecutor();
        assertEquals("90", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.BATTERY_STATUS));

        this.sebClientConnectionService.notifyClientEvent(
                "token3",
                new ClientEvent(null, connection.id, EventType.INFO_LOG, 1L, 1L, 40.0, "<battery> some info other"));
        waitForExecutor();
        assertEquals("40", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.BATTERY_STATUS));

        // test reset indicator value and load it from persistent storage
        ((AbstractLogIndicator) clientIndicator).reset();
        assertEquals("40", IndicatorValue.getDisplayValue(clientIndicator, IndicatorType.BATTERY_STATUS));
    }

}
