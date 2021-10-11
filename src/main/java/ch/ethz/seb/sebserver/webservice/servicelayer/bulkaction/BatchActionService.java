/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Batch actions are stored in the data base and processes by a background thread. The progress
 * of a running batch action can be requested and presented by to user. Also finished batch actions
 * can be reported to the user showing successful action as well as actions that has failed. */
public interface BatchActionService {

    /** Use this to register a new batch action for further processing.
     *
     * @param institutionId The institution identifier
     * @param actionType The batch action type
     * @param ids comma separated String of model ids to process
     * @return Result refer to the stored batch action or to an error when happened */
    Result<BatchAction> registerNewBatchAction(final Long institutionId, BatchActionType actionType, String ids);

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
