/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.init;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.ExamConfigXMLParser;

@Lazy
@Component
@WebServiceProfile
public class XMLAttributeLoader {

    private static final Logger log = LoggerFactory.getLogger(XMLAttributeLoader.class);

    private final Cryptor cryptor;

    public XMLAttributeLoader(final Cryptor cryptor) {
        this.cryptor = cryptor;
    }

    public Collection<ConfigurationValue> loadFromXML(
            final Long institutionId,
            final Long configurationId,
            final Function<String, ConfigurationAttribute> attributeResolver,
            final String xmlFileName) {

        InputStream inputStream;
        try {
            final ClassPathResource configFileResource = new ClassPathResource(xmlFileName);
            inputStream = configFileResource.getInputStream();
        } catch (final Exception e) {
            log.error("Failed to get config resources from: {}", xmlFileName, e);
            return Collections.emptyList();
        }

        try {

            final Collection<ConfigurationValue> values = new ArrayList<>();

            final ExamConfigXMLParser examConfigImportHandler = new ExamConfigXMLParser(
                    this.cryptor,
                    institutionId,
                    configurationId,
                    values::add,
                    attributeResolver);

            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            final SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(inputStream, examConfigImportHandler);

            return Utils.immutableCollectionOf(values);

        } catch (final Exception e) {
            log.error("Unexpected error while trying to get initial permitted processes", e);
            return Collections.emptyList();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}
