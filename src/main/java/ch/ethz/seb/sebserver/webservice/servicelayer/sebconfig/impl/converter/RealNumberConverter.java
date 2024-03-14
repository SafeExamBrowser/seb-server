/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RealNumberConverter implements AttributeValueConverter {

    private static final Logger log = LoggerFactory.getLogger(IntegerConverter.class);

    public static final Set<String> SUPPORTED_ATTR_NAMES = Utils.immutableSetOf(
            "defaultPageZoomLevel",
            "defaultTextZoomLevel");

    private static final String XML_TEMPLATE = "<key>%s</key><real>%s</real>";
    private static final String JSON_TEMPLATE = "\"%s\":%s";

    private static final DecimalFormat FORMATTER = new DecimalFormat("0.##############");

    @Override
    public Set<String> names() {
        return SUPPORTED_ATTR_NAMES;
    }

    @Override
    public Set<AttributeType> types() {
        return Collections.emptySet();
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, attribute, valueSupplier.apply(attribute), XML_TEMPLATE);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, attribute, valueSupplier.apply(attribute), JSON_TEMPLATE);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value,
            final String template) throws IOException {

        final String val = (value != null && value.value != null)
                ? value.value
                : attribute.getDefaultValue();

        double realVal;
        try {
            realVal = Double.parseDouble(val);
        } catch (final NumberFormatException nfe) {
            log.error("Failed to convert SEB configuration attribute value of type real number: {}", val, nfe);
            realVal = 0;
        }

        out.write(Utils.toByteArray(String.format(
                template,
                AttributeValueConverter.extractName(attribute),
                FORMATTER.format(realVal))));
    }

}
