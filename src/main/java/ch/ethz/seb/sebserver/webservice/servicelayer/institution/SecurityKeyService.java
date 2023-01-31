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
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;

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

    /** Register SEB client connection App-Signature-Key as a new global security key registry entry
     * This is equivalent to make a global grant for specified App-Signature-Key of given SEB client connection.
     *
     * @param institutionId The institution identifier
     * @param connectionId The client connection identifier
     * @param tag A Tag for user identification of the grant within the registry
     * @return Result refer to the newly created security key entry or to an error when happened */
    Result<SecurityKey> registerGlobalAppSignatureKey(Long institutionId, Long connectionId, String tag);

    /** Grants an App-Signature-Key sent by a SEB client and register it within the granted key registry
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier for the exam based grant
     * @param connectionId The client connection identifier
     * @param tag A Tag for user identification of the grant within the registry
     * @return Result refer to the newly created security key entry or to an error when happened */
    Result<SecurityKey> grantAppSignatureKey(Long institutionId, Long examId, Long connectionId, String tag);

    /** Get the hashed App Signature Key value from a encrypted App Signature Key sent by a SEB client.
     * The App Signature Key hash is used for security checks. The plain App Signature Key will never be used nor stored
     *
     * @param appSignatureKey The encrypted App Signature Key sent by a SEB client
     * @param connectionToken The connection token of the SEB client connection
     * @return Result refer to the App Signature Key hash for given App Signature Key or to an error when happened */
    Result<String> getAppSignatureKeyHash(
            String appSignatureKey,
            String connectionToken,
            CharSequence salt);

    /** Use this to update an App Signature Key grant for a particular SEB connection. This will
     * apply the security check again and mark the connection regarding to the security check.
     *
     * This is used by the internal monitoring update task
     *
     * @param record The ClientConnectionRecord of the specific SEB client connection */
    void updateAppSignatureKeyGrant(ClientConnectionRecord record);

    /** Delete a given security key form the registry.
     *
     * @param keyId The security key registry entry identifier
     * @return Result refer to the EntityKey of the delete registry entry or to an error when happened. */
    Result<EntityKey> deleteSecurityKeyGrant(Long keyId);

}
