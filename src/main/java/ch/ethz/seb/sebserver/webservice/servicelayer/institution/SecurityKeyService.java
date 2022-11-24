/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.AppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SecurityKeyService {

    /** Get the stored App-Signature-Key of a SEB connection within a SecurityKey container.
     *
     * @param institutionId The institution identifier
     * @param connectionId The SEB connection identifier
     * @return Result refer to the App-Signature-Key of the SEB client connection or to an error when happened */
    Result<SecurityKey> getAppSignatureKey(Long institutionId, Long connectionId);

    /** Get a list of all different SEB App-Signature-Key for a given exam with also the number of SEB
     * clients that has propagated the respective App-Signature-Key
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier
     * @return Result refer to the list of AppSignatureKeyInfo for the given exam or to an error when happened */
    Result<Collection<AppSignatureKeyInfo>> getAppSignatureKeyInfo(Long institutionId, Long examId);

    /** Get a list of all security key registry entries of for given institution and exam.
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier
     * @param type The key type filter criteria
     * @return Result refer to the list of security key registry entries or to an error when happened */
    Result<Collection<SecurityKey>> getSecurityKeyEntries(Long institutionId, Long examId, KeyType type);

    /** Register a new security key entry in the registry.
     *
     * @param key The security key data
     * @return Result refer to the newly created and stored security key entry or to an error when happened */
    Result<SecurityKey> registerSecurityKey(SecurityKey key);

    /** Register SEB client connection App-Signature-Key as a new global security key registry entry
     * This is equivalent to make a global grant for specified App-Signature-Key of given SEB client connection.
     *
     * @param institutionId The institution identifier
     * @param connectionId The client connection identifier
     * @param tag A Tag for user identification of the grant within the registry
     * @return Result refer to the newly created security key entry or to an error when happened */
    Result<SecurityKey> registerGlobalAppSignatureKey(Long institutionId, Long connectionId, String tag);

    /** Register SEB client connection App-Signature-Key as a new exam based security key registry entry
     * This is equivalent to make a exam specific grant for specified App-Signature-Key of given SEB client connection.
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier for the exam based grant
     * @param connectionId The client connection identifier
     * @param tag A Tag for user identification of the grant within the registry
     * @return Result refer to the newly created security key entry or to an error when happened */
    Result<SecurityKey> registerExamAppSignatureKey(Long institutionId, Long examId, Long connectionId, String tag);

    /** Used to apply a SEB client App-signature-Key check for a given App-Signature-Key sent by the SEB.
     * Note: This also stores the given App-Signature-Key sent by SEB if not already stored for the SEB connection.
     *
     * @param clientConnection The SEB client connection token
     * @param appSignatureKey The App-Signature-Key sent by the SEB client
     * @return true if the check was successful and the SEB has a grant, false otherwise */
    boolean checkAppSignatureKey(ClientConnection clientConnection, String appSignatureKey);

    /** Used to process an update of the App-Signature-Key grant for all SEB connection within given
     * exam that has not been already granted.
     *
     * @param examId The exam identifier */
    void updateAppSignatureKeyGrants(Long examId);

    /** Delete a given security key form the registry.
     *
     * @param keyId The security key registry entry identifier
     * @return Result refer to the EntityKey of the delete registry entry or to an error when happened. */
    Result<EntityKey> deleteSecurityKeyGrant(Long keyId);

}
