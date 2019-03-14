/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.institution.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;

/** Concrete EntityDAO interface of SebClientConfig entities */
public interface SebClientConfigDAO extends
        ActivatableEntityDAO<SebClientConfig, SebClientConfig>,
        BulkActionSupportDAO<SebClientConfig> {

    /** Get the configured ClientCredentials for a given SebClientConfig.
     * The ClientCredentials are still encoded as they are on DB storage
     *
     * @param modelId the model identifier of the SebClientConfig to get the ClientCredentials for
     * @return the configured ClientCredentials for a given SebClientConfig */
    Result<ClientCredentials> getSebClientCredentials(String modelId);

}
