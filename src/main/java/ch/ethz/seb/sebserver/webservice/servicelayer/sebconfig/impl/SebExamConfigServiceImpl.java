/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.OutputStream;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverter;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigServiceImpl implements SebExamConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigServiceImpl.class);

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final Collection<ConfigurationValueValidator> validators;
    private final Collection<XMLValueConverter> converters;

    protected SebExamConfigServiceImpl(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final Collection<ConfigurationValueValidator> validators,
            final Collection<XMLValueConverter> converters) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.validators = validators;
        this.converters = converters;
    }

    @Override
    public void validate(final ConfigurationValue value) {
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
    public void validate(final ConfigurationTableValues tableValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exportXML(final OutputStream out, final Long configurationNodeId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exportForExam(final OutputStream out, final Long configExamMappingId) {
        // TODO Auto-generated method stub

    }

}
