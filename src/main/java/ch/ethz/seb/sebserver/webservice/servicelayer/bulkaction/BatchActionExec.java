/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Interface for a batch action single entity execution implementation
 * for a batch action of specific type. */
public interface BatchActionExec {

    /** The action type of the batch action
     *
     * @return action type of the batch action */
    BatchActionType actionType();

    /** Implements a consistency check for the given batch action attributes.
     * This shall check whether the needed attributes are available for a proper processing of the actions.
     * This is called just before a new batch action is created and processing is started.
     *
     * @param batchAction
     * @return APIMessage if there is an consistency failure */
    APIMessage checkConsistency(Map<String, String> actionAttributes);

    /** Executes the action on a single entity.
     *
     * @param modelId The model identifier of the entity to process
     * @param batchAction The batch action metadata
     * @return Result refer to the entity key or to an error when happened */
    Result<EntityKey> doSingleAction(String modelId, BatchAction batchAction);

}
