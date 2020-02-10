/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;

public class IndicatorTest {

    @Test
    public void testDefault() throws IOException {
        final Indicator indicator = new Indicator(
                null,
                1L,
                "Ping",
                IndicatorType.LAST_PING,
                "b4b4b4",
                Arrays.asList(
                        new Indicator.Threshold(2000d, "22b14c"),
                        new Indicator.Threshold(5000d, "ff7e00"),
                        new Indicator.Threshold(10000d, "ed1c24")));

        final JSONMapper mapper = new JSONMapper();
        final String jsonString = mapper.writeValueAsString(indicator);
        assertEquals(
                "{\"examId\":1,\"name\":\"Ping\",\"type\":\"LAST_PING\",\"color\":\"b4b4b4\",\"thresholds\":[{\"value\":2000.0,\"color\":\"22b14c\"},{\"value\":5000.0,\"color\":\"ff7e00\"}",
                jsonString);

        final String threholds =
                "[{\"value\":2000.0,\"color\":\"22b14c\"},{\"value\":5000.0,\"color\":\"ff7e00\"},{\"value\":10000.0,\"color\":\"ed1c24\"}]";
        final Collection<Threshold> values = mapper.readValue(threholds, new TypeReference<Collection<Threshold>>() {
        });

        assertEquals(
                "[Threshold [value=2000.0, color=22b14c], Threshold [value=5000.0, color=ff7e00], Threshold [value=10000.0, color=ed1c24]]",
                values.toString());
    }

}
