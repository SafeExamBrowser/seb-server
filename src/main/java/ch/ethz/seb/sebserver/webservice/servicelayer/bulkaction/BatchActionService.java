/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Batch actions are stored in the data base and processes by a background thread. The progress
 * of a running batch action can be requested and presented by to user. Also finished batch actions
 * can be reported to the user showing successful action as well as actions that has failed. */
public interface BatchActionService {

    /** Validates a given batch action.
     *
     * @param batchAction the batch action data
     * @return Result refer to the BatchAction or to an error when happened */
    Result<BatchAction> validate(BatchAction batchAction);

    /** Use this to notify a new batch action for further processing.
     *
     * @param batchAction BatchAction */
    Result<BatchAction> notifyNewBatchAction(BatchAction batchAction);

    /** Use this to get a specific BatchAction.
     *
     * @param actionId The batch action identifier
     * @return Result refer to the batch actions or to an error when happened */
    Result<BatchAction> getRunningAction(String actionId);

    /** Use this to get all currently running batch actions for a given institution.
     *
     * @param institutionId The institution identifier
     * @return Result refer to a collection of found batch actions or to an error when happened */
    Result<Collection<BatchAction>> getRunningActions(Long institutionId);

    /** Use this to get all currently running batch actions for a given institution and entity type.
     *
     * @param institutionId The institution identifier
     * @param entityType The entity type
     * @return Result refer to a collection of found batch actions or to an error when happened */
    Result<Collection<BatchAction>> getRunningActions(Long institutionId, EntityType entityType);

    /** Use this to get all finished batch action for the specified institution.
     *
     * @param institutionId he institution identifier
     * @return Result refer to a collection with all batch actions or to an error when happened */
    Result<Collection<BatchAction>> getFinishedActions(Long institutionId);

}
