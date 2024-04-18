/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of LmsSetup entities */
public interface LmsSetupDAO extends ActivatableEntityDAO<LmsSetup, LmsSetup>, BulkActionSupportDAO<LmsSetup> {

    /** Get the configured ClientCredentials for a given LmsSetup.
     * The ClientCredentials are still encoded as they are on DB storage
     *
     * @param lmsSetupId the identifier of the LmsSetup to get the ClientCredentials for
     * @return the configured ClientCredentials for a given LmsSetup */
    Result<ClientCredentials> getLmsAPIAccessCredentials(String lmsSetupId);

    /** Use this to get additional proxy data for specified LMS Setup.
     *
     * @param lmsSetupId the LMS Setup identifier
     * @return Result refer to the proxy data or to an error if happened */
    Result<ProxyData> getLmsAPIAccessProxyData(String lmsSetupId);

    /** Checks if the given LmsSetup instance is in sync with the version on
     * database by matching the update_time field
     *
     * @param lmsSetup LmsSetup instance to check if it is up-to-date
     * @return true if the update_time has the same value on persistent */
    boolean isUpToDate(LmsSetup lmsSetup);

    /** This wil find the internal LMSSetup with the universal connectionId if it exists on this server.
     *
     * @param connectionId The connectionId UUID (unique ID)
     * @return the local LMSSetup DB PK ID */
    Result<Long> getLmsSetupIdByConnectionId(String connectionId);

    /** Set or reset LMS integration active flag.
     *
     * @param lmsSetupId LMS Setup id
     * @param active true when mark for active, false for inactive
     * @return Result refers to the specified LMS Setup or to en error when happened */
    Result<LmsSetup> setIntegrationActive(Long lmsSetupId, boolean active);

    Result<Collection<Long>> idsOfActiveWithFullIntegration(Long institutionId);

    Result<Collection<Long>> allIdsFullIntegration();
}
