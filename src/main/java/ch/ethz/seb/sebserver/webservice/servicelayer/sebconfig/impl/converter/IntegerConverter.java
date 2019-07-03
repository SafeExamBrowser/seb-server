/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverter;

@Lazy
@Component
@WebServiceProfile
public class IntegerConverter implements XMLValueConverter {

    public static final Set<AttributeType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    AttributeType.INTEGER,
                    AttributeType.SLIDER,
                    AttributeType.SINGLE_SELECTION,
                    AttributeType.RADIO_SELECTION)));

    private static final String TEMPLATE = "<key>%s</key><integer>%s</integer>";
    private static final String TEMPLATE_EMPTY = "<key>%s</key><integer />";

    @Override
    public Set<AttributeType> types() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String name() {
        return StringUtils.EMPTY;
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) throws IOException {

        final String val = (value.value != null) ? value.value : attribute.getDefaultValue();
        if (StringUtils.isNotBlank(val)) {
            out.write(Utils.toByteArray(String.format(TEMPLATE, extractName(attribute), val)));
        } else {
            out.write(Utils.toByteArray(String.format(TEMPLATE_EMPTY, extractName(attribute))));
        }

    }
}
