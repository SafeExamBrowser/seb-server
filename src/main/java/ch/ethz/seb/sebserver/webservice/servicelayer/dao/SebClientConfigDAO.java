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

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;

/** Concrete EntityDAO interface of SebClientConfig entities */
public interface SebClientConfigDAO extends
        ActivatableEntityDAO<SebClientConfig, SebClientConfig>,
        BulkActionSupportDAO<SebClientConfig> {

    /** Get a SebClientConfig by specified client identifier
     *
     * @param clientName the client name
     * @return Result refer to the SebClientConfig for client or refer to an error if happened */
    Result<SebClientConfig> byClientName(String clientName);

    /** Get the configured ClientCredentials for a given SebClientConfig.
     * The ClientCredentials are still encoded as they are on DB storage
     *
     * @param modelId the model identifier of the SebClientConfig to get the ClientCredentials for
     * @return the configured ClientCredentials for a given SebClientConfig */
    Result<ClientCredentials> getSebClientCredentials(String modelId);

    /** Get the stored encrypted configuration password from a specified SEB client configuration.
     * The SEB client configuration password is used to encrypt a SEB Client Configuration
     *
     * @param modelId the model
     * @return encrypted configuration password */
    Result<CharSequence> getConfigPasswortCipher(String modelId);

    /** Get the stored encrypted configuration password from a specified SEB client configuration.
     * The SEB client configuration password is used to encrypt a SEB Client Configuration.
     *
     * The SEB client configuration must be active otherwise a error is returned
     *
     * @param clientName the client name
     * @return encrypted configuration password */
    Result<CharSequence> getConfigPasswortCipherByClientName(String clientName);

    @Override
    @CacheEvict(
            cacheNames = ClientConfigService.EXAM_CLIENT_DETAILS_CACHE,
            allEntries = true)
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

}
