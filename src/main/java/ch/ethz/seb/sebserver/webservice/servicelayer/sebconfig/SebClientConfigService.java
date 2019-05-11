/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.OutputStream;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SebClientConfigService {

    static String SEB_CLIENT_CONFIG_EXAMPLE_XML =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
                    "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">"
                    +
                    "<plist version=\"1.0\">" +
                    "<dict>" +
                    "<key>sebMode</key>" +
                    "<integer>1</integer>" +
                    "<key>sebServerFallback</key>" +
                    "<true />" +
                    "<key>sebServerURL</key>" +
                    "<string>%s</string>" +
                    "<key>sebServerConfiguration</key>" +
                    "<array>" +
                    "<dict>" +
                    "<key>institution</key>" +
                    "<string>%s</string>" +
                    "<key>clientName</key>" +
                    "<string>%s</string>" +
                    "<key>clientSecret</key>" +
                    "<string>%s</string>" +
                    "<key>accessTokenEndpoint</key>" +
                    "<string>%s</string>" +
                    "<key>handshakeEndpoint</key>" +
                    "<string>%s</string>" +
                    "<key>examConfigEndpoint</key>" +
                    "<string>%s</string>" +
                    "<key>pingEndpoint</key>" +
                    "<string>%s</string>" +
                    "<key>eventEndpoint</key>" +
                    "<string>%s</string>" +
                    "</dict>" +
                    "</array>" +
                    "</dict>" +
                    "</plist>";

    boolean hasSebClientConfigurationForInstitution(Long institutionId);

    Result<SebClientConfig> autoCreateSebClientConfigurationForInstitution(Long institutionId);

    void exportSebClientConfiguration(
            OutputStream out,
            final String modelId);

}
