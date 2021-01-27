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
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

public interface ClientConnectionDAO extends
        EntityDAO<ClientConnection, ClientConnection>,
        BulkActionSupportDAO<ClientConnection> {

    String CONNECTION_TOKENS_CACHE = "CONNECTION_TOKENS_CACHE";

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

    /** Get a list of all connection tokens of all connections (no matter what state)
     * of an exam.
     *
     * @param examId The exam identifier
     * @return list of all connection tokens of all connections (no matter what state)
     *         of an exam */
    default Result<Collection<String>> getConnectionTokensNoCache(final Long examId) {
        return getConnectionTokens(examId);
    }

    /** Get a collection of all client connections records that needs a room update
     * and that are in the status ACTIVE.
     * This also flags the involved connections for no update needed within the
     * same transaction. So if something will go wrong in the update process
     * the affected client connection(s) must be marked for need update again.
     *
     * @return Result refer to a collection of all ClientConnection records for update or to an error when happened */
    Result<Collection<ClientConnectionRecord>> getAllConnectionIdsForRoomUpdateActive();

    /** Get a collection of all client connections records that needs a room update
     * and that are NOT in the status ACTIVE.
     * This also flags the involved connections for no update needed within the
     * same transaction. So if something will go wrong in the update process
     * the affected client connection(s) must be marked for need update again.
     *
     * @return Result refer to a collection of all ClientConnection records for update or to an error when happened */
    Result<Collection<ClientConnectionRecord>> getAllConnectionIdsForRoomUpdateInactive();

    /** Used to re-mark a client connection record for room update in error case. */
    void setNeedsRoomUpdate(Long connectionId);

    /** Get all ClientConnection that are assigned to a defined proctoring collecting room.
     *
     * @param roomId The proctoring room identifier
     * @return Result refer to a collection of all ClientConnection of the room or to an error if happened */
    Result<Collection<ClientConnection>> getRoomConnections(final Long roomId);

    /** Get all ClientConnections that are assigned to a proctoring room by a given room name and exam
     *
     * @param examId The exam identifier
     * @param roomName the room name
     * @return Result refer to a collection of all ClientConnection of the room or to an error if happened */
    Result<Collection<ClientConnection>> getRoomConnections(final Long examId, final String roomName);

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
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<ClientConnection> save(ClientConnection data);

    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken")
    Result<Void> assignToProctoringRoom(Long connectionId, String connectionToken, Long roomId);

    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION,
            key = "#connectionToken")
    Result<Void> removeFromProctoringRoom(Long connectionId, String connectionToken);

    /** Deletes the given ClientConnection data.
     *
     * This evicts all entries from the CONNECTION_TOKENS_CACHE.
     *
     * TODO improvement: Use the examId to evict only the relevant cache entry
     *
     * @param all Set of EntityKey for entities to delete
     * @return Result refer to a collection of deleted entities or to an error if happened */
    @Override
    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, allEntries = true)
    Result<Collection<EntityKey>> delete(Set<EntityKey> all);

    /** Get a ClientConnection by connection token.
     *
     * @param connectionToken the connection token
     * @return Result refer to the ClientConnection for the specified connection token or to an error if happened */
    Result<ClientConnection> byConnectionToken(String connectionToken);

    /** Indicates if the client connection for exam and connection token is an active connection.
     *
     * @param examId the exam identifier
     * @param connectionToken the connection token
     * @return Result refer to the active connection flag or to an error when happened */
    Result<Boolean> isActiveConnection(Long examId, String connectionToken);

    /** Filters a set of client connection tokens to a set containing only
     * connection tokens of active client connections.
     *
     * Use this if you have a bunch of client connections to filter only the active connections
     *
     * @param examId
     * @param connectionToken
     * @return */
    Result<Set<String>> filterActive(Long examId, Set<String> connectionToken);

}
