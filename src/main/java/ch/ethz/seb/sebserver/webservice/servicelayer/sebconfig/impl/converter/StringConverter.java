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
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;

@Lazy
@Component
@WebServiceProfile
public class StringConverter implements AttributeValueConverter {

    public static final Set<AttributeType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    AttributeType.TEXT_FIELD,
                    AttributeType.TEXT_AREA,
                    AttributeType.PASSWORD_FIELD,
                    AttributeType.DECIMAL,
                    AttributeType.COMBO_SELECTION)));

    private static final String XML_TEMPLATE = "<key>%s</key><string>%s</string>";
    private static final String XML_TEMPLATE_EMPTY = "<key>%s</key><string />";

    private static final String JSON_TEMPLATE = "\"%s\":\"%s\"";
    private static final String JSON_TEMPLATE_EMPTY = "\"%s\":\"\"";

    @Override
    public Set<AttributeType> types() {
        return SUPPORTED_TYPES;
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(
                out,
                attribute,
                valueSupplier.apply(attribute),
                XML_TEMPLATE, XML_TEMPLATE_EMPTY);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(
                out,
                attribute,
                valueSupplier.apply(attribute),
                JSON_TEMPLATE, JSON_TEMPLATE_EMPTY);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value,
            final String template,
            final String emptyTemplate) throws IOException {

        final String val = (value != null && value.value != null) ? value.value : attribute.getDefaultValue();
        if (StringUtils.isNotBlank(val)) {
            out.write(Utils.toByteArray(String.format(
                    template,
                    AttributeValueConverter.extractName(attribute),
                    val)));
        } else {
            out.write(Utils.toByteArray(String.format(
                    emptyTemplate,
                    AttributeValueConverter.extractName(attribute))));
        }
    }

}
