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
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
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

    @CacheEvict(cacheNames = CONNECTION_TOKENS_CACHE, key = "#examId")
    default void evictConnectionTokenCache(final Long examId) {

    }

    /** Get a list of all connection tokens of all connections (no matter what state)
     * of an exam.
     *
     * @param examId The exam identifier
     * @return list of all connection tokens of all connections (no matter what state)
     *         of an exam */
    default Result<Collection<String>> getConnectionTokensNoCache(final Long examId) {
        return getConnectionTokens(examId);
    }

    /** Get a list of all connection tokens of all connections of an exam
     * that are in state active
     *
     * @param examId The exam identifier
     * @return Result refer to the collection of connection tokens or to an error when happened */
    Result<Collection<String>> getActiveConnctionTokens(Long examId);

    /** Get all inactive connection tokens from the set of given tokens.
     * This is usually used for cleanup purposes to filter a bunch of connection tokens
     * by activity. Inactive connections are in state CLOSED or DISABLED
     *
     * @param connectionTokens The set of connection tokens to filter
     * @return Result refer to all inactive connection tokens from the given set */
    Result<Collection<String>> getInactiveConnctionTokens(Set<String> connectionTokens);

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
    Result<Collection<ClientConnection>> getCollectingRoomConnections(final Long examId, final String roomName);

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

    /** Used to re-mark a client connection record for room update in error case. */
    Result<Void> markForProctoringUpdate(Long id);

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

    /** Use this to check whether a single ClientConnection is up to date or needs a refresh.
     *
     * @param clientConnection the actual ClientConnection (from the internal cache)
     * @return Result refer to true if the given ClientConnection is up to date */
    Result<Boolean> isUpToDate(ClientConnection clientConnection);

    Result<Set<String>> getClientConnectionsOutOfSyc(Long examId, Set<Long> timestamps);

    /** Indicates if the client connection for given exam and connection token is
     * in a ready state to send instructions.
     *
     * @param examId the exam identifier
     * @param connectionToken the connection token
     * @return Result refer to the active connection flag or to an error when happened */
    Result<Boolean> isInInstructionStatus(Long examId, String connectionToken);

    /** Filters a set of client connection tokens to a set containing only
     * connection tokens of client connections that are in a ready state to send instructions.
     *
     * @param examId the exam identifier
     * @param connectionToken a Set of connection tokens to filter
     * @return Result refer to filtered Set of connection tokens or to an error when happened */
    Result<Set<String>> filterForInstructionStatus(Long examId, Set<String> connectionToken);

    /** Used to get the VDI paired connection if it already exsits.
     *
     * @param clientName the VDI connection identifier sent by the SEB client on connect
     * @return Result refer to the relevant VDI pair connection if exists or to an error if not */
    Result<ClientConnectionRecord> getVDIPairCompanion(Long examId, String clientName);

    /** Deletes all client indicator value entries within the client_indicator table for a given exam.
     *
     * @param exam the Exam to delete all currently registered indicator value entries
     * @return Result refer to the given Exam or to an error when happened. */
    Result<Exam> deleteClientIndicatorValues(Exam exam);

}
