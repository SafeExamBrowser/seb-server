/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface BatchActionDAO extends EntityDAO<BatchAction, BatchAction> {

    /** This checks if there is a pending batch action to process next.
     * If so this reserves the pending batch action and mark it to be processed
     * by the given pocessId.
     * If there is no pending batch action this results with a ResourceNotFoundException.
     *
     * @param processId The process id to reserve a pending batch action before processing
     * @return Result refer to the batch action to process or to an error when happened */
    Result<BatchAction> getAndReserveNext(String processId);

    /** Use this to mark processing of a single entity of a specified batch action as successful completed.
     *
     * @param actionId The batch action identifier
     * @param processId The process identifier (must match with the processId on persistent storage)
     * @param modelId The model identifier to mark as completed for the given batch action */
    void setSuccessfull(Long actionId, String processId, String modelId);

    /** Use this to mark processing of a single entity of a specified batch action as failed.
     *
     * @param actionId The batch action identifier
     * @param processId The process identifier (must match with the processId on persistent storage)
     * @param modelId The model identifier to mark as failure for the given batch action
     * @param error The failure error if available */
    void setFailure(Long actionId, String processId, String modelId, Exception error);

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
