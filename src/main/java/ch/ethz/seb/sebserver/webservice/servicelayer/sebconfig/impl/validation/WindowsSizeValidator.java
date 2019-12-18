/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;

@Lazy
@Component
@WebServiceProfile
public class WindowsSizeValidator implements ConfigurationValueValidator {

    public static final String NAME = "WindowsSizeValidator";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validate(final ConfigurationValue value, final ConfigurationAttribute attribute) {
        if (!NAME.equals(attribute.validator)) {
            return true;
        }

        if (StringUtils.isBlank(value.value)) {
            return true;
        }

        final String number = (value.value.endsWith(Constants.PERCENTAGE_STRING))
                ? value.value.substring(0, value.value.length() - 1)
                : value.value;

        try {
            Integer.parseInt(number);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

}
