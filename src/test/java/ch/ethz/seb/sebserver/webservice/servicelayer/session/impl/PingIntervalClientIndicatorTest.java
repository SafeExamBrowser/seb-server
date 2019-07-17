/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.ClientEventExtentionMapper;

public class PingIntervalClientIndicatorTest {

    @Test
    public void testCreation() {

        DateTimeUtils.setCurrentMillisFixed(1);

        final ClientEventExtentionMapper clientEventExtentionMapper = Mockito.mock(ClientEventExtentionMapper.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(clientEventExtentionMapper);
        assertEquals("0.0", String.valueOf(pingIntervalClientIndicator.getValue()));
    }

    @Test
    public void testInterval() {
        DateTimeUtils.setCurrentMillisFixed(1);

        final ClientEventExtentionMapper clientEventExtentionMapper = Mockito.mock(ClientEventExtentionMapper.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(clientEventExtentionMapper);
        assertEquals("0.0", String.valueOf(pingIntervalClientIndicator.getValue()));

        DateTimeUtils.setCurrentMillisFixed(10);

        assertEquals("9.0", String.valueOf(pingIntervalClientIndicator.getValue()));
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        DateTimeUtils.setCurrentMillisFixed(1);

        final ClientEventExtentionMapper clientEventExtentionMapper = Mockito.mock(ClientEventExtentionMapper.class);

        final PingIntervalClientIndicator pingIntervalClientIndicator =
                new PingIntervalClientIndicator(clientEventExtentionMapper);
        final JSONMapper jsonMapper = new JSONMapper();
        final String json = jsonMapper.writeValueAsString(pingIntervalClientIndicator);
        assertEquals("{\"indicatorValue\":0.0,\"indicatorType\":\"LAST_PING\"}", json);
    }

}
