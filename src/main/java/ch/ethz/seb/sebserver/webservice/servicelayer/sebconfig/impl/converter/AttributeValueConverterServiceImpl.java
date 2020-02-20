/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverterService;

@Lazy
@Service
@WebServiceProfile
public class AttributeValueConverterServiceImpl implements AttributeValueConverterService {

    private static final Logger log = LoggerFactory.getLogger(AttributeValueConverterServiceImpl.class);

    private final Map<String, AttributeValueConverter> convertersByAttributeName;
    private final Map<AttributeType, AttributeValueConverter> convertersByAttributeType;

    public AttributeValueConverterServiceImpl(final Collection<AttributeValueConverter> converters) {
        this.convertersByAttributeName = new HashMap<>();
        this.convertersByAttributeType = new HashMap<>();
        for (final AttributeValueConverter converter : converters) {
            converter.init(this);
            for (final String attributeName : converter.names()) {
                this.convertersByAttributeName.put(attributeName, converter);
            }

            for (final AttributeType aType : converter.types()) {
                if (this.convertersByAttributeType.containsKey(aType)) {
                    log.warn(
                            "Unexpected state in initialization: A XMLValueConverter for AttributeType {} exists already: {}",
                            aType,
                            converter);
                }
                this.convertersByAttributeType.put(aType, converter);
            }
        }
    }

    @Override
    public AttributeValueConverter getAttributeValueConverter(final ConfigurationAttribute attribute) {
        if (this.convertersByAttributeName.containsKey(attribute.name)) {
            return this.convertersByAttributeName.get(attribute.name);
        }

        if (this.convertersByAttributeType.containsKey(attribute.type)) {
            return this.convertersByAttributeType.get(attribute.type);
        }

        throw new IllegalStateException("No XMLValueConverter found for attribute: " + attribute);
    }

    @Override
    public AttributeValueConverter getAttributeValueConverter(final AttributeType attributeType) {
        return this.convertersByAttributeType.get(attributeType);
    }

    @Override
    public AttributeValueConverter getAttributeValueConverter(final String attributeName) {
        return this.convertersByAttributeName.get(attributeName);
    }

}
