/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of ClientGroup entities */
public interface ClientGroupDAO extends EntityDAO<ClientGroup, ClientGroup>, BulkActionSupportDAO<ClientGroup> {

    String CACHE_NAME_RUNNING_EXAM_CLIENT_GROUP_CACHE = "RUNNING_EXAM_CLIENT_GROUP_CACHE";

    /** Get a collection of all ClientGroup entities for a specified exam.
     *
     * @param examId the Exam identifier to get the ClientGroups for
     * @return Result referring to the collection of ClientGroups of an Exam or to an error if happened */
    @Cacheable(
            cacheNames = CACHE_NAME_RUNNING_EXAM_CLIENT_GROUP_CACHE,
            key = "#examId",
            condition = "#examId!=null",
            unless = "#result.hasError()")
    Result<Collection<ClientGroup>> allForExam(Long examId);

    @CacheEvict(
            cacheNames = CACHE_NAME_RUNNING_EXAM_CLIENT_GROUP_CACHE,
            key = "#examId")
    default void evictCacheForExam(final Long examId) {
        // just evict the cache
    }

    /** Delete all client groups for a particular exam.
     *
     * @param examId the exam identifier
     * @return Result refer to the list of deleted client groups or to an error when happened */
    Result<Collection<EntityKey>> deleteAllForExam(Long examId);

}
