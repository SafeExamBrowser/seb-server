/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverterService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationFormat;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;

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
    private static final byte[] JSON_START = Utils.toByteArray("{");
    private static final byte[] JSON_END = Utils.toByteArray("}");
    private static final byte[] JSON_SEPARATOR = Utils.toByteArray(Constants.LIST_SEPARATOR);

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ConfigurationDAO configurationDAO;
    private final AttributeValueConverterService attributeValueConverterService;
    private final ZipService zipService;
    private final Cryptor cryptor;

    protected ExamConfigIO(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ConfigurationDAO configurationDAO,
            final AttributeValueConverterService attributeValueConverterService,
            final ZipService zipService,
            final Cryptor cryptor) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.configurationDAO = configurationDAO;
        this.attributeValueConverterService = attributeValueConverterService;
        this.zipService = zipService;
        this.cryptor = cryptor;
    }

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void exportPlain(
            final ConfigurationFormat exportFormat,
            final OutputStream out,
            final Long institutionId,
            final Long configurationNodeId) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Start export SEB plain XML configuration asynconously");
        }

        try {

            // get all defined root configuration attributes prepared and sorted
            final List<ConfigurationAttribute> sortedAttributes = this.configurationAttributeDAO.getAllRootAttributes()
                    .getOrThrow()
                    .stream()
                    .flatMap(this::convertAttribute)
                    .filter(exportFormatBasedAttributeFilter(exportFormat))
                    .sorted()
                    .collect(Collectors.toList());

            // get follow-up configurationId for given configurationNodeId
            final Long configurationId = this.configurationDAO
                    .getConfigurationLastStableVersion(configurationNodeId)
                    .getOrThrow().id;

            final Function<ConfigurationAttribute, ConfigurationValue> configurationValueSupplier =
                    getConfigurationValueSupplier(institutionId, configurationId);

            writeHeader(exportFormat, out);

            // write attributes
            final Iterator<ConfigurationAttribute> iterator = sortedAttributes.iterator();
            while (iterator.hasNext()) {

                final ConfigurationAttribute attribute = iterator.next();
                final AttributeValueConverter attributeValueConverter =
                        this.attributeValueConverterService.getAttributeValueConverter(attribute);

                switch (exportFormat) {
                    case XML: {
                        attributeValueConverter.convertToXML(
                                out,
                                attribute,
                                configurationValueSupplier);
                        break;
                    }
                    case JSON: {
                        attributeValueConverter.convertToJSON(
                                out,
                                attribute,
                                configurationValueSupplier);
                        if (iterator.hasNext()) {
                            out.write(JSON_SEPARATOR);
                        }
                        break;
                    }
                }
            }

            writeFooter(exportFormat, out);

            if (log.isDebugEnabled()) {
                log.debug("Finished export SEB plain XML configuration asynconously");
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to write SEB Exam Configuration XML to output stream: ", e);
            throw e;
        } finally {
            try {
                out.flush();
            } catch (final IOException e1) {
                log.error("Unable to flush output stream after error");
            }
            IOUtils.closeQuietly(out);
        }
    }

    /** This parses the XML from given InputStream with a SAX parser to avoid keeping the
     * whole XML file in memory and keep up with the streaming approach of SEB Exam Configuration
     * to avoid trouble with big SEB Exam Configuration in the future.
     *
     * @param in The InputString to constantly read the XML from
     * @param institutionId the institionId of the import
     * @param configurationId the identifier of the internal configuration to apply the imported values to */
    void importPlainXML(final InputStream in, final Long institutionId, final Long configurationId) {
        try {
            // get all attributes and map the names to ids
            final Map<String, ConfigurationAttribute> attributeMap = this.configurationAttributeDAO
                    .allMatching(new FilterMap())
                    .getOrThrow()
                    .stream()
                    .collect(Collectors.toMap(
                            attr -> attr.name,
                            Function.identity()));

            // the SAX handler with a ConfigValue sink that saves the values to DB
            // and a attribute-name/id mapping function with pre-created mapping
            final ExamConfigXMLParser examConfigImportHandler = new ExamConfigXMLParser(
                    this.cryptor,
                    institutionId,
                    configurationId,
                    value -> this.configurationValueDAO
                            .save(value)
                            .getOrThrow(),
                    attributeMap::get);

            // SAX parsing
            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            final SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(in, examConfigImportHandler);

        } catch (final ParserConfigurationException | SAXException | IOException e) {
            log.error("Unexpected error while trying to parse imported SEB Config XML: ", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    InputStream unzip(final InputStream input) throws Exception {

        final byte[] zipHeader = new byte[Constants.GZIP_HEADER_LENGTH];
        final int read = input.read(zipHeader);
        if (read < Constants.GZIP_HEADER_LENGTH) {
            throw new IllegalArgumentException("Failed to verify Zip type from input stream. Header size mismatch.");
        }

        final boolean isZipped = Byte.toUnsignedInt(zipHeader[0]) == Constants.GZIP_ID1
                && Byte.toUnsignedInt(zipHeader[1]) == Constants.GZIP_ID2
                && Byte.toUnsignedInt(zipHeader[2]) == Constants.GZIP_CM;

        final InputStream sequencedInput = new SequenceInputStream(
                new ByteArrayInputStream(zipHeader, 0, Constants.GZIP_HEADER_LENGTH),
                input);

        if (isZipped) {

            final PipedInputStream pipedIn = new PipedInputStream();
            final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
            this.zipService.read(pipedOut, sequencedInput);

            return pipedIn;
        } else {
            return sequencedInput;
        }
    }

    private Predicate<ConfigurationAttribute> exportFormatBasedAttributeFilter(final ConfigurationFormat format) {
        // Filter originatorVersion according to: https://www.safeexambrowser.org/developer/seb-config-key.html
        return attr -> !("originatorVersion".equals(attr.getName()) && format == ConfigurationFormat.JSON);
    }

    private void writeFooter(
            final ConfigurationFormat exportFormat,
            final OutputStream out) throws IOException {

        if (exportFormat == ConfigurationFormat.XML) {
            // plist close
            out.write(XML_DICT_END_UTF_8);
            out.write(XML_PLIST_END_UTF_8);
        } else {
            out.write(JSON_END);
        }
    }

    private void writeHeader(
            final ConfigurationFormat exportFormat,
            final OutputStream out) throws IOException {

        if (exportFormat == ConfigurationFormat.XML) {
            writeXMLHeaderInformation(out);
        } else {
            writeJSONHeaderInformation(out);
        }
    }

    private void writeJSONHeaderInformation(final OutputStream out) throws IOException {
        out.write(JSON_START);
    }

    private void writeXMLHeaderInformation(final OutputStream out) throws IOException {
        // write headers
        out.write(XML_VERSION_HEADER_UTF_8);
        out.write(XML_DOCTYPE_HEADER_UTF_8);

        // plist open
        out.write(XML_PLIST_START_V1_UTF_8);
        out.write(XML_DICT_START_UTF_8);
    }

    private Stream<ConfigurationAttribute> convertAttribute(final ConfigurationAttribute attr) {
        final AttributeValueConverter attributeValueConverter =
                this.attributeValueConverterService.getAttributeValueConverter(attr);
        if (attributeValueConverter != null) {
            return attributeValueConverter.convertAttribute(attr);
        } else {
            return Stream.of(attr);
        }
    }

    private Function<ConfigurationAttribute, ConfigurationValue> getConfigurationValueSupplier(
            final Long institutionId,
            final Long configurationId) {

        final Map<Long, ConfigurationValue> mapping = this.configurationValueDAO
                .allRootAttributeValues(institutionId, configurationId)
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(
                        ConfigurationValue::getAttributeId,
                        Function.identity()));

        return attr -> mapping.get(attr.id);
    }

}
