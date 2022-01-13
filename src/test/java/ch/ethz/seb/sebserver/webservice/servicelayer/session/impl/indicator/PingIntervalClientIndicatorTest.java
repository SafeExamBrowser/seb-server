/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;

public class PingIntervalClientIndicatorTest {

    @After
    public void cleanup() {
        DateTimeUtils.setCurrentMillisProvider(DateTimeUtils.SYSTEM_MILLIS_PROVIDER);
    }

    @Test
    public void testCreation() {

        DateTimeUtils.setCurrentMillisProvider(() -> 1L);

        final DistributedIndicatorValueService distributedPingCache =
                Mockito.mock(DistributedIndicatorValueService.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(distributedPingCache);
        pingIntervalClientIndicator.init(null, null, true, true);
        assertEquals("0.0", String.valueOf(pingIntervalClientIndicator.getValue()));
    }

    @Test
    public void testInterval() {

        DateTimeUtils.setCurrentMillisProvider(() -> 1L);

        final DistributedIndicatorValueService distributedPingCache =
                Mockito.mock(DistributedIndicatorValueService.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(distributedPingCache);
        pingIntervalClientIndicator.init(null, null, true, true);
        assertEquals("0.0", String.valueOf(pingIntervalClientIndicator.getValue()));

        DateTimeUtils.setCurrentMillisProvider(() -> 10L);

        assertEquals("9.0", String.valueOf(pingIntervalClientIndicator.getValue()));
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        DateTimeUtils.setCurrentMillisProvider(() -> 1L);

        final DistributedIndicatorValueService distributedPingCache =
                Mockito.mock(DistributedIndicatorValueService.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(distributedPingCache);
        pingIntervalClientIndicator.init(new Indicator(2L, 3L, "test", IndicatorType.NONE, null, null, null, null), 1L,
                true, true);
        final JSONMapper jsonMapper = new JSONMapper();
        final String json = jsonMapper.writeValueAsString(pingIntervalClientIndicator);
        assertEquals("{\"id\":2,\"val\":0.0}", json);
    }

}
