/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Lazy
@Component
public class JSONMapper extends ObjectMapper {

    private static final Logger log = LoggerFactory.getLogger(JSONMapper.class);

    private static final long serialVersionUID = 2883304481547670626L;

    public JSONMapper() {
        super();
        super.registerModule(new JodaModule());
        super.configure(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                false);
        super.configure(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_ZONE_ID,
                false);
        super.setSerializationInclusion(Include.NON_NULL);
    }

    public String writeValueAsStringOr(final Object entity, final String or) {
        if (entity == null) {
            return or;
        }
        try {
            return super.writeValueAsString(entity);
        } catch (final Exception e) {
            log.error("Failed to serialize value: {}", entity, e);
            return or;
        }
    }
}
