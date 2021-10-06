/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface BatchActionService {

    Result<BatchAction> registerNewBatchAction(BatchAction action);

    Result<Collection<BatchAction>> getRunningActions(Long institutionId);

    Result<Collection<BatchAction>> getFinishedActions(Long institutionId);

}
