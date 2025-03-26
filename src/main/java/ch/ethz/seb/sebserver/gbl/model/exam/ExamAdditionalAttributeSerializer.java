/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ExamAdditionalAttributeSerializer extends StdSerializer<Map<String, String>>  {

    public ExamAdditionalAttributeSerializer() {
        this(null);
    }

    public ExamAdditionalAttributeSerializer(Class<Map<String, String>> t) {
        super(t);
    }

    @Override
    public void serialize(
            final Map<String, String> stringStringMap,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        if (stringStringMap != null) {
            // remove some additional attributes only used in backend
            final Map<String, String> attrs = new HashMap<>(stringStringMap);
            attrs.remove("spsServiceURL");
            attrs.remove("SCREEN_PROCTORING_SETTINGS");
            attrs.remove("spsAPISecret");
            attrs.remove("spsAccessData");
            attrs.remove("spsCollectingStrategy");
            attrs.remove("spsAccountId");
            attrs.remove("spsAPIKey");
            attrs.remove("spsAccountPassword");
            attrs.remove("spsCollectingGroupSize");
            attrs.remove("SIGNATURE_KEY_SALT");
            jsonGenerator.writePOJO(attrs);
        } else {
            jsonGenerator.writePOJO(stringStringMap);
        }
    }
}
