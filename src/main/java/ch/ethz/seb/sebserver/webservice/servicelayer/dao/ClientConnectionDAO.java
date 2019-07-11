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
import org.springframework.cache.annotation.Cacheable;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ClientConnectionDAO extends EntityDAO<ClientConnection, ClientConnection> {

    public static final String CONNECTION_TOKENS_CACHE = "CONNECTION_TOKENS_CACHE";

    /** Get a list of all connection tokens of all connections (no matter what state)
     * of an exam.
     *
     * @param examId The exam identifier
     * @return list of all connection tokens of all connections (no matter what state)
     *         of an exam */
    @Cacheable(
            cacheNames = CONNECTION_TOKENS_CACHE,
            key = "#examId",
            unless = "#result.hasError()")
    Result<Collection<String>> getConnectionTokens(Long examId);

    @Override
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<ClientConnection> createNew(ClientConnection data);

    @Override
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

    Result<ClientConnection> byConnectionToken(String connectionToken);

}
