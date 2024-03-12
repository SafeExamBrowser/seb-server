/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;

@Lazy
@Component
@WebServiceProfile
public class DecimalTypeValidator implements ConfigurationValueValidator {

    public static final String NAME = "DecimalTypeValidator";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validate(
            final ConfigurationValue value,
            final ConfigurationAttribute attribute) {

        // if value is not an decimal type and validator is not specified --> skip
        if (attribute.type != AttributeType.DECIMAL && !name().equals(attribute.validator)) {
            return true;
        }

        if (StringUtils.isBlank(value.value)) {
            return true;
        }

        try {
            final double val = Double.parseDouble(value.value);

            final String resources = attribute.getResources();
            if (!StringUtils.isBlank(resources)) {
                final String[] split = StringUtils.split(resources, Constants.LIST_SEPARATOR);
                if (split.length > 0) {
                    // check lower boundary
                    if (Double.parseDouble(split[0]) < val) {
                        return false;
                    }

                    if (split.length > 1) {
                        // check upper boundary
                        if (Double.parseDouble(split[1]) > val) {
                            return false;
                        }
                    }
                }
            }

            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

}
