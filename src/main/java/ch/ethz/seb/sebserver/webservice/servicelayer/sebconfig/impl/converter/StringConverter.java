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
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.ExamConfigXMLParser;

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

    private final ClientCredentialService clientCredentialService;

    public StringConverter(final ClientCredentialService clientCredentialService) {
        this.clientCredentialService = clientCredentialService;
    }

    @Override
    public Set<AttributeType> types() {
        return SUPPORTED_TYPES;
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        final ConfigurationValue cValue = valueSupplier.apply(attribute);
        final String val = StringEscapeUtils.escapeXml10((cValue != null && cValue.value != null)
                ? cValue.value
                : attribute.getDefaultValue());

        convert(
                out,
                attribute,
                val,
                XML_TEMPLATE, XML_TEMPLATE_EMPTY);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        // NOTE: Don't escape JSON characters on the value strings here,
        //       otherwise the Config-Key will be different then in SEB and SEB Config Tool
        final ConfigurationValue cValue = valueSupplier.apply(attribute);
        final String val = (cValue != null && cValue.value != null)
                ? cValue.value
                : attribute.getDefaultValue();

        convert(
                out,
                attribute,
                val,
                JSON_TEMPLATE, JSON_TEMPLATE_EMPTY);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final String value,
            final String template,
            final String emptyTemplate) throws IOException {

        final String realName = AttributeValueConverter.extractName(attribute);
        if (StringUtils.isNotBlank(value)) {
            out.write(Utils.toByteArray(String.format(
                    template,
                    realName,
                    convertSecret(realName, value))));
        } else {
            out.write(Utils.toByteArray(String.format(
                    emptyTemplate,
                    realName)));
        }
    }

    private CharSequence convertSecret(
            final String attributeName,
            final String value) {

        if (StringUtils.isBlank(value)) {
            return value;
        }

        if (!ExamConfigXMLParser.SECRET_ATTRIBUTES.contains(attributeName)) {
            return value;
        }

        // decrypt internally encrypted password and hash it for export
        // NOTE: see special case description in ExamConfigXMLParser.createConfigurationValue
        final String plainText = this.clientCredentialService.decrypt(value).toString();
        if (plainText.endsWith(Constants.IMPORTED_PASSWORD_MARKER)) {
            return plainText.replace(Constants.IMPORTED_PASSWORD_MARKER, StringUtils.EMPTY);
        } else {
            return Utils.hash_SHA_256_Base_16(plainText);
        }
    }

}
