/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import org.springframework.cache.annotation.CacheEvict;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

/** Concrete EntityDAO interface of Exam entities */
public interface ExamDAO extends ActivatableEntityDAO<Exam, Exam>, BulkActionSupportDAO<Exam> {

    /** Get all active Exams for a given institution.
     *
     * @param institutionId the identifier of the institution
     * @return Result refer to a collection of all active exams of the given institution or refer to an error if
     *         happened */
    Result<Collection<Long>> allIdsOfInstituion(Long institutionId);

    /** Saves the Exam and updates the running exam cache. */
    @Override
    @CacheEvict(
            cacheNames = ExamSessionCacheService.CACHE_NAME_RUNNING_EXAM,
            key = "#exam.id")
    Result<Exam> save(Exam exam);

    /** Get an Exam by a given ClientConnection id.
     *
     * @param connectionId
     * @return a Result containing the Exam by a given ClientConnection id or refer to an error if happened */
    Result<Exam> byClientConnection(Long connectionId);

    Result<Collection<Exam>> allForRunCheck();

    Result<Collection<Exam>> allForEndCheck();

    Result<Exam> startUpdate(Long examId, String update);

    Result<Exam> endUpdate(Long examId, String update);

    Result<Boolean> isUpdating(Long examId);

    Result<Boolean> upToDate(Long examId, String lastUpdate);

}
