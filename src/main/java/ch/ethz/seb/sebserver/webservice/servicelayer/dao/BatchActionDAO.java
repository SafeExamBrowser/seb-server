/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface BatchActionDAO extends EntityDAO<BatchAction, BatchAction> {

    Result<BatchAction> getAndReserveNext(String processId);

    Result<BatchAction> updateProgress(Long actionId, String processId, Collection<String> modelIds);

    void setSuccessfull(Long actionId, String processId, String modelId);

    /** This is used by a processing background task that is processing a batch action to finish up
     * its work and register the batch action as done within the persistent storage.
     * </p>
     * If force is set to true, no integrity check will be done before marking the specified batch
     * action as done.
     *
     * @param actionId the batch action identifier
     * @param processId the identifier of the batch action processor
     * @param force skip integrity check if set to true and just mark the action as done
     * @return Result refer to the involved batch action or to an error when happened */
    Result<BatchAction> finishUp(Long actionId, String processId, boolean force);

}
