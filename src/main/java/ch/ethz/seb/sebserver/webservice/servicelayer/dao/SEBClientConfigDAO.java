/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;

/** Concrete EntityDAO interface of SEBClientConfig entities */
public interface SEBClientConfigDAO extends
        ActivatableEntityDAO<SEBClientConfig, SEBClientConfig>,
        BulkActionSupportDAO<SEBClientConfig> {

    /** Get a SEBClientConfig by specified client identifier
     *
     * @param clientName the client name
     * @return Result refer to the SEBClientConfig for client or refer to an error if happened */
    Result<SEBClientConfig> byClientName(String clientName);

    /** Get the configured ClientCredentials for a given SEBClientConfig.
     * The ClientCredentials are still encoded as they are on DB storage
     *
     * @param modelId the model identifier of the SEBClientConfig to get the ClientCredentials for
     * @return the configured ClientCredentials for a given SEBClientConfig */
    Result<ClientCredentials> getSEBClientCredentials(String modelId);

    /** Get the stored encrypted configuration password from a specified SEB client configuration.
     * The SEB client configuration password is used to encrypt a SEB Client Configuration
     *
     * @param modelId the model
     * @return encrypted configuration password */
    Result<CharSequence> getConfigPasswordCipher(String modelId);

    /** Get the stored encrypted configuration password from a specified SEB client configuration.
     * The SEB client configuration password is used to encrypt a SEB Client Configuration.
     *
     * The SEB client configuration must be active otherwise a error is returned
     *
     * @param clientName the client name
     * @return encrypted configuration password */
    Result<CharSequence> getConfigPasswordCipherByClientName(String clientName);

    @Override
    @CacheEvict(
            cacheNames = ClientConfigService.EXAM_CLIENT_DETAILS_CACHE,
            allEntries = true)
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

}
