/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

public interface ConfigurationValueValidator {

    public static final String MESSAGE_VALUE_OBJECT_NAME = "examConfigValue";

    String name();

    boolean validate(
            ConfigurationValue value,
            ConfigurationAttribute attribute);

    default void throwValidationError(
            final ConfigurationValue value,
            final ConfigurationAttribute attribute) {

        throw new FieldValidationException(
                attribute.name,
                this.createErrorMessage(value, attribute));
    }

    default String createErrorMessage(
            final ConfigurationValue value,
            final ConfigurationAttribute attribute) {

        return new StringBuffer("examConfigValue:")
                .append(attribute.name)
                .append(":")
                .append(name())
                .append(":")
                .append(value.listIndex)
                .toString();
    }

}
