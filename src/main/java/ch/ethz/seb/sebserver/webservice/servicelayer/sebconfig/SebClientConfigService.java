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

    /** Get the server URL prefix in form of;
     * [scheme{http|https}]://[server-address{DNS-name|IP}]:[port]
     *
     * E.g.: https://seb.server.ch:8080
     *
     * @return the server URL prefix */
    String getServerURL();

    /** Indicates if there is any SebClientConfiguration for a specified institution.
     *
     * @param institutionId the institution identifier
     * @return true if there is any SebClientConfiguration for a specified institution. False otherwise */
    boolean hasSebClientConfigurationForInstitution(Long institutionId);

    /** Use this to auto-generate a SebClientConfiguration for a specified institution.
     * clientName and clientSecret are randomly generated.
     *
     * @param institutionId the institution identifier
     * @return the created SebClientConfig */
    Result<SebClientConfig> autoCreateSebClientConfigurationForInstitution(Long institutionId);

    /** Use this to export a specified SebClientConfiguration within a given OutputStream.
     * The SEB Client Configuration is exported in the defined SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out OutputStream to write the export to
     * @param modelId the model identifier of the SebClientConfiguration to export */
    void exportSebClientConfiguration(
            OutputStream out,
            final String modelId);

    /** Use this to get a encoded clientSecret for the SebClientConfiguration with specified clientId/clientName.
     *
     * @param clientId the clientId/clientName
     * @return encoded clientSecret for that SebClientConfiguration with clientId or null of not existing */
    Result<CharSequence> getEncodedClientSecret(String clientId);

}
