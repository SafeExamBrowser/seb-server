/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.List;

import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;

public interface SEBClientVersionService {

    /** Use this to check if a given SEB Version from SEB client connections are valid
     *
     * @param clientOSName The client OS name sent by SEB client
     * @param clientVersion The SEB version sent by SEB client
     * @param allowedSEBVersions List of allowed SEB version conditions given from the Exam Configuration.
     * @return True if the given SEB Version matches one of the defined AllowedSEBVersion conditions */
    boolean isAllowedVersion(
            String clientOSName,
            String clientVersion,
            List<AllowedSEBVersion> allowedSEBVersions);

    void checkVersionAndUpdateClientConnection(
            ClientConnectionRecord record,
            List<AllowedSEBVersion> allowedSEBVersions);

}
