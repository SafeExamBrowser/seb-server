/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;

@Lazy
@Component
@WebServiceProfile
public class SEBVersionValidator implements ConfigurationValueValidator {

    public static final String NAME = "SEBVersionValidator";

    private final ConfigurationValueDAO configurationValueDAO;

    public SEBVersionValidator(final ConfigurationValueDAO configurationValueDAO) {
        this.configurationValueDAO = configurationValueDAO;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validate(final ConfigurationValue value, final ConfigurationAttribute attribute) {
        // if validator is not specified --> skip
        if (!name().equals(attribute.validator)) {
            return true;
        }

        if (StringUtils.isBlank(value.value)) {
            return true;
        }

        final String[] split = StringUtils.split(value.value, Constants.LIST_SEPARATOR);
        for (int i = 0; i < split.length; i++) {
            if (!isValidSEBVersionMarker(split[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidSEBVersionMarker(final String versionMarker) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String createErrorMessage(final ConfigurationValue value, final ConfigurationAttribute attribute) {
        if (StringUtils.isBlank(value.value)) {
            return ConfigurationValueValidator.super.createErrorMessage(value, attribute);
        }

        final String[] split = StringUtils.split(value.value, Constants.LIST_SEPARATOR);
        final List<String> newValues = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            if (isValidSEBVersionMarker(split[i])) {
                newValues.add(split[i]);
            }
        }

        // save with the removed invalid values
        if (!newValues.isEmpty()) {
            this.configurationValueDAO.save(new ConfigurationValue(
                    value.id,
                    value.institutionId,
                    value.configurationId,
                    value.attributeId,
                    value.listIndex,
                    StringUtils.join(newValues, Constants.LIST_SEPARATOR)));
        }

        return ConfigurationValueValidator.super.createErrorMessage(value, attribute);
    }

}
