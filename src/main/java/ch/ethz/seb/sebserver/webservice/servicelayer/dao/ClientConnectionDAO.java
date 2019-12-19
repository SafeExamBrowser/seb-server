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
     * Caches the resulting collection of tokens with the given examId as key
     * unless the result has no error.
     *
     * @param examId The exam identifier
     * @return list of all connection tokens of all connections (no matter what state)
     *         of an exam */
    @Cacheable(
            cacheNames = CONNECTION_TOKENS_CACHE,
            key = "#examId",
            unless = "#result.hasError()")
    Result<Collection<String>> getConnectionTokens(Long examId);

    /** Creates new ClientConnection from the given ClientConnection data.
     *
     * This evicts all entries from the CONNECTION_TOKENS_CACHE.
     *
     * TODO improvement: Use the examId to evict only the relevant cache entry
     *
     * @param data ClientConnection instance
     * @return Result refer to the newly created ClientConnection data or to an error if happened */
    @Override
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<ClientConnection> createNew(ClientConnection data);

    @Override
    // TODO check if this is needed
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<ClientConnection> save(ClientConnection data);

    /** Deletes the given ClientConnection data.
     *
     * This evicts all entries from the CONNECTION_TOKENS_CACHE.
     *
     * TODO improvement: Use the examId to evict only the relevant cache entry
     *
     * @param data ClientConnection instance
     * @return Result refer to a collection of deleted entities or to an error if happened */
    @Override
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

    /** Get a ClientConnection by connection token.
     *
     * @param connectionToken the connection token
     * @return Result refer to the ClientConnection for the specified connection token or to an error if happened */
    Result<ClientConnection> byConnectionToken(String connectionToken);

}
