/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.util.Collection;
import java.util.Objects;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigServiceImpl implements SebExamConfigService {

    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final Collection<ConfigurationValueValidator> validators;

    protected SebExamConfigServiceImpl(
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final Collection<ConfigurationValueValidator> validators) {

        this.configurationAttributeDAO = configurationAttributeDAO;
        this.validators = validators;
    }

    @Override
    public void validate(final ConfigurationValue value) {
        Objects.requireNonNull(value);

        final ConfigurationAttribute attribute = this.configurationAttributeDAO.byPK(value.attributeId)
                .getOrThrow();

        this.validators
                .stream()
                .filter(validator -> !validator.validate(value, attribute))
                .findFirst()
                .ifPresent(validator -> validator.throwValidationError(value, attribute));
    }

    @Override
    public void validate(final ConfigurationTableValue tableValue) {
        // TODO Auto-generated method stub

    }

}
