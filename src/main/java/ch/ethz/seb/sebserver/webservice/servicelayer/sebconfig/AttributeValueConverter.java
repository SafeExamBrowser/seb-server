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
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

/** Defines the interface of a XML converter to be used to convert
 * ConfigurationValue for defined ConfigurationAttribute */
public interface AttributeValueConverter {

    /** This can be overwritten if a XMLValueConverter needs the XMLValueConverterService.
     * The XMLValueConverterService is then injected by its self on initialization.
     *
     * @param xmlValueConverterService the AttributeValueConverterService instance */
    default void init(final AttributeValueConverterService xmlValueConverterService) {
    }

    /** Gives a Set of AttributeType's a concrete converter is able to
     * handle and convert ConfigurationValue of attributes of given types.
     *
     * @return a Set of supported AttributeType's of the converter */
    Set<AttributeType> types();

    /** The attribute names of the Converter. This can be used if a Converter is specific to
     * an ConfigurationAttribute and not specific on a type of attribute.
     * This must give either the name if a specific ConfigurationAttribute or empty set
     *
     * @return The name of a specific ConfigurationAttribute the converter works for. */
    default Set<String> names() {
        return Collections.emptySet();
    }

    /** Used to convert the a given ConfigurationAttribute to plain XML text or block of SEB Configuration attribute.
     *
     * @param out The output stream to write the plain XML text block to
     * @param attribute The ConfigurationAttribute containing all attribute information
     * @param valueSupplier The ConfigurationValue supplier
     * @throws IOException on error */
    void convertToXML(
            OutputStream out,
            ConfigurationAttribute attribute,
            Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException;

    /** Used to convert the a given ConfigurationAttribute to plain JSON text or block of SEB Configuration attribute.
     *
     * @param out The output stream to write the plain JSON text block to
     * @param attribute The ConfigurationAttribute containing all attribute information
     * @param valueSupplier The ConfigurationValue supplier
     * @throws IOException on error */
    void convertToJSON(
            OutputStream out,
            ConfigurationAttribute attribute,
            Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException;

    /** Get the real name of the SEB configuration attribute
     * by cutting of the prefixed used for nested attributes
     *
     * @param attribute ConfigurationAttribute instance
     * @return the SEB configuration attribute name */
    static String extractName(final ConfigurationAttribute attribute) {
        final int lastIndexOf = attribute.name.lastIndexOf('.');
        if (lastIndexOf > 0) {
            return attribute.name.substring(lastIndexOf + 1);
        } else {
            return attribute.name;
        }
    }

    /** Used to expand a "compressed" attribute like kioskMode -> createNewDesktop + killExplorerShell
     *
     * @param attr ConfigurationAttribute instance
     * @return  Stream of expanded attributes */
    default Stream<ConfigurationAttribute> convertAttribute(final ConfigurationAttribute attr) {
        return Stream.of(attr);
    }

}
