/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

public class JSONParserTest {

    @Test
    public void testSpecialChar1() throws JsonProcessingException {
        final String json = """
                {"meta_data": {
                  "param1": "value—dvgswg",
                  "param2": "value2-dvgswg",
                  "param3": "value3",
                  "param4": "value4-%ç/&=&&çETZGFIUZHàPIHBNHK VG$ä$à£à!èéLèPLIOU=(&&(Rç%çE"
                }}""";

        final JSONMapper jsonMapper = new JSONMapper();
        final MetaData metaData = jsonMapper.readValue(json, MetaData.class);
        assertEquals("MetaData{meta_data={param1=value—dvgswg, param2=value2-dvgswg, param3=value3, param4=value4-%ç/&=&&çETZGFIUZHàPIHBNHK VG$ä$à£à!èéLèPLIOU=(&&(Rç%çE}}", metaData.toString());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MetaData {
        @JsonProperty("meta_data")
        public final Map<String, String> meta_data;


        @JsonCreator
        private MetaData(
                @JsonProperty("meta_data") final Map<String, String> metaData) {

            meta_data = metaData;
        }

        @Override
        public String toString() {
            return "MetaData{" +
                    "meta_data=" + meta_data +
                    '}';
        }
    }
}
