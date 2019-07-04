/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverterService;

@Lazy
@Component
@WebServiceProfile
public class ExamConfigIO {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigIO.class);

    private static final byte[] XML_VERSION_HEADER_UTF_8 = Utils.toByteArray(Constants.XML_VERSION_HEADER);
    private static final byte[] XML_DOCTYPE_HEADER_UTF_8 = Utils.toByteArray(Constants.XML_DOCTYPE_HEADER);
    private static final byte[] XML_PLIST_START_V1_UTF_8 = Utils.toByteArray(Constants.XML_PLIST_START_V1);
    private static final byte[] XML_PLIST_END_UTF_8 = Utils.toByteArray(Constants.XML_PLIST_END);
    private static final byte[] XML_DICT_START_UTF_8 = Utils.toByteArray(Constants.XML_DICT_START);
    private static final byte[] XML_DICT_END_UTF_8 = Utils.toByteArray(Constants.XML_DICT_END);

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ConfigurationDAO configurationDAO;
    private final XMLValueConverterService xmlValueConverterService;

    protected ExamConfigIO(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ConfigurationDAO configurationDAO,
            final XMLValueConverterService xmlValueConverterService) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.configurationDAO = configurationDAO;
        this.xmlValueConverterService = xmlValueConverterService;
    }

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void exportPlainXML(
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) {

        // get all defined root configuration attributes
        final Map<Long, ConfigurationAttribute> attributes = this.configurationAttributeDAO.getAllRootAttributes()
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(
                        ConfigurationAttribute::getId,
                        Function.identity()));

        final List<ConfigurationAttribute> sortedAttributes = attributes
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        // get follow-up configurationId for given configurationNodeId
        final Long configurationId = this.configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow().id;

        // get all values for that attributes for given configurationId
        final Map<Long, ConfigurationValue> values = this.configurationValueDAO
                .allRootAttributeValues(institutionId, configurationId)
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(
                        ConfigurationValue::getAttributeId,
                        Function.identity()));

        try {
            // write headers
            out.write(XML_VERSION_HEADER_UTF_8);
            out.write(XML_DOCTYPE_HEADER_UTF_8);

            // plist open
            out.write(XML_PLIST_START_V1_UTF_8);
            out.write(XML_DICT_START_UTF_8);

            // write attributes
            for (final ConfigurationAttribute attribute : sortedAttributes) {
                final ConfigurationValue configurationValue = values.get(attribute.id);
                if (configurationValue != null) {
                    this.xmlValueConverterService.getXMLConverter(attribute).convertToXML(
                            out,
                            attribute,
                            configurationValue);
                }
            }

            // plist close
            out.write(XML_DICT_END_UTF_8);
            out.write(XML_PLIST_END_UTF_8);
            out.flush();

        } catch (final Exception e) {
            log.error("Unexpected error while trying to write SEB Exam Configuration XML to output stream: ", e);
            try {
                out.flush();
            } catch (final IOException e1) {
                log.error("Unable to flush output stream after error");
            }
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void importPlainXML(final InputStream in, final Long institutionId, final Long configurationNodeId) {
        // TODO version 1
    }

}
