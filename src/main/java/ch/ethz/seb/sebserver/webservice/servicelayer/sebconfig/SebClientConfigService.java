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
            "  <dict>\r\n" +
                    "    <key>sebMode</key>\r\n" +
                    "    <integer>1</integer>\r\n" +
                    "    <key>sebConfigPurpose</key>\r\n" +
                    "    <integer>1</integer>" +
                    "    <key>sebServerFallback</key>\r\n" +
                    "    <true />\r\n" +
                    "    <key>sebServerURL</key>\r\n" +
                    "    <string>%s</string>\r\n" +
                    "    <key>sebServerConfiguration</key>\r\n" +
                    "    <dict>\r\n" +
                    "        <key>institution</key>\r\n" +
                    "        <string>%s</string>\r\n" +
                    "        <key>clientName</key>\r\n" +
                    "        <string>%s</string>\r\n" +
                    "        <key>clientSecret</key>\r\n" +
                    "        <string>%s</string>\r\n" +
                    "        <key>apiDiscovery</key>\r\n" +
                    "        <string>%s</string>\r\n" +
                    "    </dict>\r\n" +
                    "  </dict>\r\n";

    String getServerURL();

    boolean hasSebClientConfigurationForInstitution(Long institutionId);

    Result<SebClientConfig> autoCreateSebClientConfigurationForInstitution(Long institutionId);

    void exportSebClientConfiguration(
            OutputStream out,
            final String modelId);

    Result<String> getEncodedClientSecret(String clientId);

}
