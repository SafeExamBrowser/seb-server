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

public interface XMLValueConverter {

    Set<AttributeType> types();

    String name();

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
