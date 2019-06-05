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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverterService;

@Lazy
@Component
@WebServiceProfile
public class TableConverter implements XMLValueConverter {

    public static final Set<AttributeType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    AttributeType.TABLE,
                    AttributeType.INLINE_TABLE,
                    AttributeType.COMPOSITE_TABLE)));

    private static final String KEY_TEMPLATE = "<key>%s</key>";
    private static final byte[] ARRAY_START = Utils.toByteArray("<array>");
    private static final byte[] ARRAY_END = Utils.toByteArray("</array>");
    private static final byte[] DICT_START = Utils.toByteArray("<dict>");
    private static final byte[] DICT_END = Utils.toByteArray("</dict>");
    private static final byte[] EMPTY_ARRAY = Utils.toByteArray("<array />");

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;

    public TableConverter(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
    }

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
            final ConfigurationValue value,
            final XMLValueConverterService xmlValueConverterService) throws IOException {

        out.write(Utils.toByteArray(String.format(KEY_TEMPLATE, extractName(attribute))));

        final List<List<ConfigurationValue>> values = this.configurationValueDAO.getOrderedTableValues(
                value.institutionId,
                value.configurationId,
                attribute.id).getOrThrow();

        if (values == null || values.isEmpty()) {
            out.write(EMPTY_ARRAY);
            out.flush();
            return;
        }

        if (attribute.type != AttributeType.COMPOSITE_TABLE) {
            out.write(ARRAY_START);
        }

        writeRows(
                out,
                getAttributes(attribute),
                values,
                xmlValueConverterService);

        if (attribute.type != AttributeType.COMPOSITE_TABLE) {
            out.write(ARRAY_END);
        }

        out.flush();
    }

    private Map<Long, ConfigurationAttribute> getAttributes(final ConfigurationAttribute attribute) {
        return this.configurationAttributeDAO
                .allMatching(new FilterMap().putIfAbsent(
                        ConfigurationAttribute.FILTER_ATTR_PARENT_ID,
                        attribute.getModelId()))
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(
                        attr -> attr.id,
                        Function.identity()));

    }

    private void writeRows(
            final OutputStream out,
            final Map<Long, ConfigurationAttribute> attributeMap,
            final List<List<ConfigurationValue>> values,
            final XMLValueConverterService xmlValueConverterService) throws IOException {

        for (final List<ConfigurationValue> rowValues : values) {
            out.write(DICT_START);
            for (final ConfigurationValue value : rowValues) {
                final ConfigurationAttribute attr = attributeMap.get(value.attributeId);
                final XMLValueConverter converter = xmlValueConverterService.getXMLConverter(attr);
                converter.convertToXML(out, attr, value, xmlValueConverterService);
            }
            out.write(DICT_END);
            out.flush();
        }
    }

}
