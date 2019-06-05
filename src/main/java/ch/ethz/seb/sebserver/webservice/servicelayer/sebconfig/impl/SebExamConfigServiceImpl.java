/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverterService;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigServiceImpl implements SebExamConfigService, XMLValueConverterService {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigServiceImpl.class);

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ConfigurationDAO configurationDAO;
    private final Collection<ConfigurationValueValidator> validators;
    private final Map<String, XMLValueConverter> convertersByAttributeName;
    private final Map<AttributeType, XMLValueConverter> convertersByAttributeType;

    protected SebExamConfigServiceImpl(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ConfigurationDAO configurationDAO,
            final Collection<ConfigurationValueValidator> validators,
            final Collection<XMLValueConverter> converters) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.configurationDAO = configurationDAO;
        this.validators = validators;
        this.convertersByAttributeName = new HashMap<>();
        this.convertersByAttributeType = new HashMap<>();
        for (final XMLValueConverter converter : converters) {
            if (StringUtils.isNoneBlank(converter.name())) {
                this.convertersByAttributeName.put(converter.name(), converter);
            }

            for (final AttributeType aType : converter.types()) {
                if (this.convertersByAttributeType.containsKey(aType)) {
                    log.warn(
                            "Unexpected state in inititalization: A XMLValueConverter for AttributeType {} exists already: {}",
                            aType,
                            converter);
                }
                this.convertersByAttributeType.put(aType, converter);
            }
        }
    }

    @Override
    public XMLValueConverter getXMLConverter(final ConfigurationAttribute attribute) {
        if (this.convertersByAttributeName.containsKey(attribute.name)) {
            return this.convertersByAttributeName.get(attribute.name);
        }

        if (this.convertersByAttributeType.containsKey(attribute.type)) {
            return this.convertersByAttributeType.get(attribute.type);
        }

        throw new IllegalStateException("No XMLValueConverter found for attribute: " + attribute);
    }

    @Override
    public void validate(final ConfigurationValue value) throws FieldValidationException {
        if (value == null) {
            log.warn("Validate called with null reference. Ignore this and skip validation");
            return;
        }

        final ConfigurationAttribute attribute = this.configurationAttributeDAO
                .byPK(value.attributeId)
                .getOrThrow();

        this.validators
                .stream()
                .filter(validator -> !validator.validate(value, attribute))
                .findFirst()
                .ifPresent(validator -> validator.throwValidationError(value, attribute));
    }

    @Override
    public void validate(final ConfigurationTableValues tableValue) throws FieldValidationException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void exportPlainXML(
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
            out.write(Constants.XML_VERSION_HEADER_UTF_8);
            out.write(Constants.XML_DOCTYPE_HEADER_UTF_8);

            // plist open
            out.write(Constants.XML_PLIST_START_V1_UTF_8);
            out.write(Constants.XML_DICT_START_UTF_8);

            // write attributes
            for (final ConfigurationAttribute attribute : sortedAttributes) {
                final ConfigurationValue configurationValue = values.get(attribute.id);
                if (configurationValue != null) {
                    this.getXMLConverter(attribute).convertToXML(
                            out,
                            attribute,
                            configurationValue,
                            this);
                }
            }

            // plist close
            out.write(Constants.XML_DICT_END_UTF_8);
            out.write(Constants.XML_PLIST_END_UTF_8);
            out.flush();

        } catch (final IOException e) {
            log.error("Unexpected error while trying to write SEB Exam Configuration XML to output stream: ", e);
            try {
                out.flush();
            } catch (final IOException e1) {
                log.error("Unable to flush output stream after error");
            }
        }
    }

    @Override
    public void exportForExam(final OutputStream out, final Long configExamMappingId) {
        // TODO Auto-generated method stub

    }

    @Override
    public String generateConfigKey(final Long configurationNodeId) {
        // TODO https://www.safeexambrowser.org/developer/seb-config-key.html
        throw new UnsupportedOperationException("TODO");
    }

}
