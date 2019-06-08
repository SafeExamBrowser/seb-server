/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

/** Defines the interface of a XML converter to be used to convert
 * ConfigurationValue for defined ConfigurationAttribute */
public interface XMLValueConverter {

    /** Gives a Set of AttributeType's a concrete converter is able to
     * handle and convert ConfigurationValue of attributes of given types.
     * 
     * @return a Set of supported AttributeType's of the converter */
    Set<AttributeType> types();

    /** The name of the Converter. This can be used if a Converter is specific to
     * an ConfigurationAttribute and not specific on a type of attribute.
     * This must give either the name if a specific ConfigurationAttribute or null/emptyString
     *
     * @return The name of a specific ConfigurationAttribute the converter works for. */
    String name();

    /** Used to convert the a given ConfigurationAttribute / ConfigurationValue
     * pair to plain XML text for block of this SEB Configuration attribute.
     * 
     * @param out The output stream to write the plain XML text block to
     * @param attribute The ConfigurationAttribute containing all attribute information
     * @param value The ConfigurationValue containing the value
     * @param xmlValueConverterService
     * @throws IOException */
    void convertToXML(
            OutputStream out,
            ConfigurationAttribute attribute,
            ConfigurationValue value,
            XMLValueConverterService xmlValueConverterService) throws IOException;

    default String extractName(final ConfigurationAttribute attribute) {
        final int lastIndexOf = attribute.name.lastIndexOf('.');
        if (lastIndexOf > 0) {
            return attribute.name.substring(lastIndexOf + 1, attribute.name.length());
        } else {
            return attribute.name;
        }
    }

}
