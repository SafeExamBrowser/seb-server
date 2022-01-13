/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;

public class IndicatorValueJSONTest {

    @Test
    public void testJSONForExtendedIndicatorValue() throws JsonProcessingException {
        final JSONMapper jsonMapper = new JSONMapper();
        final DistributedIndicatorValueService mock = Mockito.mock(DistributedIndicatorValueService.class);
        final ErrorLogCountClientIndicator indicator = new ErrorLogCountClientIndicator(mock, null);
        indicator.init(new Indicator(1L, 2L, "test", IndicatorType.NONE, null, null, null, null), 2L, true, true);
        final String json = jsonMapper.writeValueAsString(indicator);
        assertEquals("{\"id\":1,\"val\":\"NaN\"}", json);
    }

}
