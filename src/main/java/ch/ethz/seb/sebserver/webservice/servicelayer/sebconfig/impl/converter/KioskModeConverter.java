/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.XMLValueConverter;

@Lazy
@Component
@WebServiceProfile
public class KioskModeConverter implements XMLValueConverter {

    public static final String NAME = "kioskMode";

    private static final String TEMPLATE = "<key>createNewDesktop</key><%s /><key>killExplorerShell</key><%s />";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Set<AttributeType> types() {
        return Collections.emptySet();
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) throws IOException {

        final String val = value.getValue();
        out.write(Utils.toByteArray(
                String.format(
                        TEMPLATE,
                        (val != null == "0".equals(val)) ? Constants.TRUE_STRING : Constants.FALSE_STRING,
                        (val != null == "1".equals(val)) ? Constants.TRUE_STRING : Constants.FALSE_STRING)));
    }

}
