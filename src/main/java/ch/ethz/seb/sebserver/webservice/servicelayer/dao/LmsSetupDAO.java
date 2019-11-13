/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ProxyData;

/** Concrete EntityDAO interface of LmsSetup entities */
public interface LmsSetupDAO extends ActivatableEntityDAO<LmsSetup, LmsSetup>, BulkActionSupportDAO<LmsSetup> {

    /** Get the configured ClientCredentials for a given LmsSetup.
     * The ClientCredentials are still encoded as they are on DB storage
     *
     * @param lmsSetupId the identifier of the LmsSetup to get the ClientCredentials for
     * @return the configured ClientCredentials for a given LmsSetup */
    Result<ClientCredentials> getLmsAPIAccessCredentials(String lmsSetupId);

    Result<ProxyData> getLmsAPIAccessProxyData(String lmsSetupId);
}
