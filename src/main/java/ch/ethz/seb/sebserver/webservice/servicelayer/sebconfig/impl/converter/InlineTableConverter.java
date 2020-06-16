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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverterService;

@Lazy
@Component
@WebServiceProfile
public class InlineTableConverter implements AttributeValueConverter {

    private static final String XML_KEY_TEMPLATE = "<key>%s</key>";
    private static final byte[] XML_ARRAY_START = Utils.toByteArray("<array>");
    private static final byte[] XML_ARRAY_END = Utils.toByteArray("</array>");
    private static final byte[] XML_DICT_START = Utils.toByteArray("<dict>");
    private static final byte[] XML_DICT_END = Utils.toByteArray("</dict>");
    private static final byte[] XML_EMPTY_ARRAY = Utils.toByteArray("<array />");

    private static final String JSON_KEY_TEMPLATE = "\"%s\":";
    private static final byte[] JSON_ARRAY_START = Utils.toByteArray("[");
    private static final byte[] JSON_ARRAY_END = Utils.toByteArray("]");
    private static final byte[] JSON_DICT_START = Utils.toByteArray("{");
    private static final byte[] JSON_DICT_END = Utils.toByteArray("}");
    private static final byte[] JSON_EMPTY_ARRAY = Utils.toByteArray("[]");

    public static final Set<AttributeType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(AttributeType.INLINE_TABLE)));

    private AttributeValueConverterService attributeValueConverterService;

    @Override
    public void init(final AttributeValueConverterService attributeValueConverterService) {
        this.attributeValueConverterService = attributeValueConverterService;
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

        out.write(Utils.toByteArray(String.format(
                (xml) ? XML_KEY_TEMPLATE : JSON_KEY_TEMPLATE,
                AttributeValueConverter.extractName(attribute))));

        if (value == null || StringUtils.isBlank(value.value)) {
            out.write((xml) ? XML_EMPTY_ARRAY : JSON_EMPTY_ARRAY);
            out.flush();
            return;
        }

        out.write((xml) ? XML_ARRAY_START : JSON_ARRAY_START);

        final String[] rows = StringUtils.split(value.value, Constants.LIST_SEPARATOR);
        final String[] columns = getSortedColumns(attribute.getResources());

        StringUtils.split(attribute.resources, Constants.LIST_SEPARATOR);
        for (int i = 0; i < rows.length; i++) {
            final String[] values = StringUtils.split(rows[i], Constants.EMBEDDED_LIST_SEPARATOR);

            out.write((xml) ? XML_DICT_START : JSON_DICT_START);

            for (int j = 0; j < columns.length; j++) {
                final String[] val = StringUtils.split(values[j], Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                final String[] column = StringUtils.split(columns[j], Constants.COMPLEX_VALUE_SEPARATOR);
                final AttributeType attributeType = AttributeType.valueOf(column[2]);
                final AttributeValueConverter attributeValueConverter = this.attributeValueConverterService
                        .getAttributeValueConverter(attributeType);

                final ConfigurationAttribute configurationAttribute = new ConfigurationAttribute(
                        -1L,
                        attribute.id,
                        val[0],
                        attributeType,
                        StringUtils.EMPTY,
                        StringUtils.EMPTY,
                        StringUtils.EMPTY,
                        StringUtils.EMPTY);

                final ConfigurationValue configurationValue = new ConfigurationValue(
                        -1L,
                        value.institutionId,
                        value.configurationId,
                        configurationAttribute.id,
                        0,
                        val[1]);

                if (xml) {
                    attributeValueConverter.convertToXML(
                            out,
                            configurationAttribute,
                            a -> configurationValue);
                } else {
                    attributeValueConverter.convertToJSON(
                            out,
                            configurationAttribute,
                            a -> configurationValue);
                    if (j < columns.length - 1) {
                        out.write(Utils.toByteArray(Constants.LIST_SEPARATOR));
                    }
                }

            }

            out.write((xml) ? XML_DICT_END : JSON_DICT_END);
            if (!xml && i < rows.length - 1) {
                out.write(Utils.toByteArray(Constants.LIST_SEPARATOR));
            }
        }

        out.write((xml) ? XML_ARRAY_END : JSON_ARRAY_END);

    }

    private String[] getSortedColumns(final String resources) {
        final String[] columns = StringUtils.split(resources, Constants.EMBEDDED_LIST_SEPARATOR);
        final List<String> list = Arrays.asList(columns);
        Collections.sort(list, (s1, s2) -> {
            final String name1 = StringUtils.split(s1, Constants.COMPLEX_VALUE_SEPARATOR)[1];
            final String name2 = StringUtils.split(s2, Constants.COMPLEX_VALUE_SEPARATOR)[1];
            return ConfigurationAttribute.CULTURE_INVARIANT_COLLATOR.compare(
                    name1,
                    name2);
        });

        return list.toArray(new String[columns.length]);
    }

}
