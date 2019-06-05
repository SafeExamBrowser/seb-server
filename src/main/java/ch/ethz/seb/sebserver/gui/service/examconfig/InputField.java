/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;

/** Adapter interface for SEB Exam Configuration based input fields. */
public interface InputField {

    /** Get the underling ConfigurationAttribute of the InputField.
     *
     * @return the underling ConfigurationAttribute of the InputField. */
    ConfigurationAttribute getAttribute();

    /** Get the underling Orientation of the InputField.
     *
     * @return the underling Orientation of the InputField. */
    Orientation getOrientation();

    /** Initialize the field value from a collection of ConfigurationValue.
     * The input field searches the given values for a matching value of the input field regarding to its
     * ConfigurationAttribute and takes the first found.
     *
     * @param values collection of available ConfigurationValue
     * @return the ConfigurationValue that was used to initialize the field value */
    ConfigurationValue initValue(Collection<ConfigurationValue> values);

    /** Initialize the field value directly by given value and list index.
     *
     * @param value the value to set as field value
     * @param listIndex the list index of the field */
    void initValue(final String value, final Integer listIndex);

    /** Get the current field value.
     * 
     * @return the current field value. */
    String getValue();

    /** get the current human-readable field value.
     *
     * @return the current human-readable field value. */
    String getReadableValue();

    /** Use this to show an error message below the input field.
     * This is only possible if the concrete input field has an error label, otherwise ignored
     * 
     * @param errorMessage the error message to display below the input field */
    void showError(String errorMessage);

    /** Indicated if the input field has an error on the currently set value.
     *
     * @return true if the input field has an error on the currently set value. */
    boolean hasError();

    /** Use this to clear any error on the input field. */
    void clearError();

    /** Use this to disable the input field.
     *
     * @param group indicates if instead of the field, the entire group of the field shall be disabled. */
    void disable(boolean group);

    /** Use this to enable the input field.
     *
     * @param group indicates if instead of the field, the entire group of the field shall be enabled. */
    void enable(boolean group);

    /** Use this to set/reset the default value of the underling ConfigurationAttribute to the field value. */
    void setDefaultValue();

}
