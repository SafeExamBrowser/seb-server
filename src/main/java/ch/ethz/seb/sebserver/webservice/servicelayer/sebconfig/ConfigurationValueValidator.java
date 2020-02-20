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

/** Defines a validator for validating ConfigurationValue model instances */
public interface ConfigurationValueValidator {

    String MESSAGE_VALUE_OBJECT_NAME = "examConfigValue";

    /** The name of the validator.
     * Can be used within the validator field of an ConfigurationAttribute (SQL: configuration_attribute table)
     * to force a Validator to validate attribute values of a certain ConfigurationAttribute.
     * 
     * @return name of the validator */
    String name();

    /** Indicates if a ConfigurationValue is validated by this concrete validator.
     *
     * @param value ConfigurationValue instance
     * @param attribute ConfigurationAttribute instance
     * @return true if a ConfigurationValue is validated by this concrete validator. */
    boolean validate(
            ConfigurationValue value,
            ConfigurationAttribute attribute);

    /** Default convenient method to handle validation exception if validation failed.
     *
     * @param value ConfigurationValue instance
     * @param attribute ConfigurationAttribute instance
     * @throws FieldValidationException the FieldValidationException that is created and thrown */
    default void throwValidationError(
            final ConfigurationValue value,
            final ConfigurationAttribute attribute) throws FieldValidationException {

        throw new FieldValidationException(
                attribute.name,
                this.createErrorMessage(value, attribute));
    }

    /** Default convenient method to to create an error message in case of validation failure.
     * 
     * @param value ConfigurationValue instance
     * @param attribute ConfigurationAttribute instance
     * @return error message */
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
