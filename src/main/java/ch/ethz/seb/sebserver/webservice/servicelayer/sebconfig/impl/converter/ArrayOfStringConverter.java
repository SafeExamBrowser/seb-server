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
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;

@Lazy
@Component
@WebServiceProfile
public class ArrayOfStringConverter implements AttributeValueConverter {

    public static final Set<String> ATTRIBUTE_NAMES = Utils.immutableSetOf("ExceptionsList");

    public static final Set<AttributeType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    AttributeType.MULTI_CHECKBOX_SELECTION,
                    AttributeType.MULTI_SELECTION)));

    private static final String XML_TEMPLATE = "<key>%s</key><array>";
    private static final String XML_TEMPLATE_ENTRY = "<string>%s</string>";
    private static final String XML_TEMPLATE_EMPTY = "<key>%s</key><array></array>";
    private static final String XML_ARRAY_CLOSE = "</array>";

    private static final String JSON_TEMPLATE = "\"%s\":[";
    private static final String JSON_TEMPLATE_ENTRY = "\"%s\"";
    private static final String JSON_TEMPLATE_EMPTY = "\"%s\":[]";
    private static final String JSON_ARRAY_CLOSE = "]";

    @Override
    public Set<AttributeType> types() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Set<String> names() {
        return ATTRIBUTE_NAMES;
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, attribute, valueSupplier.apply(attribute), true);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, attribute, valueSupplier.apply(attribute), false);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value,
            final boolean xml) throws IOException {

        final String val = (value != null && value.value != null) ? value.value : attribute.getDefaultValue();
        if (StringUtils.isNotBlank(val)) {

            final String[] values = StringUtils.split(val, Constants.LIST_SEPARATOR);
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                    (xml) ? XML_TEMPLATE : JSON_TEMPLATE,
                    AttributeValueConverter.extractName(attribute)));

            for (final String singleValue : values) {
                // NOTE: Don't escape JSON characters on the value strings here,
                //       otherwise the Config-Key will be different then in SEB and SEB Config Tool
                final String v = (xml)
                        ? StringEscapeUtils.escapeXml10(singleValue)
                        : singleValue;
                sb.append(String.format(
                        (xml) ? XML_TEMPLATE_ENTRY : JSON_TEMPLATE_ENTRY,
                        v));
                if (!xml) {
                    sb.append(Constants.LIST_SEPARATOR);
                }
            }

            if (!xml) {
                // delete tailing LIST_SEPARATOR (',') from loop
                sb.deleteCharAt(sb.length() - 1);
            }

            sb.append((xml) ? XML_ARRAY_CLOSE : JSON_ARRAY_CLOSE);
            out.write(Utils.toByteArray(sb.toString()));
        } else {
            out.write(Utils.toByteArray(String.format(
                    (xml) ? XML_TEMPLATE_EMPTY : JSON_TEMPLATE_EMPTY,
                    AttributeValueConverter.extractName(attribute))));
        }
    }

}
