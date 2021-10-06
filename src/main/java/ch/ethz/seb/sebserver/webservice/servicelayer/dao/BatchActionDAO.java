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

    Result<BatchAction> updateFail(Long actionId, String processId, String modelIds);

    Result<BatchAction> finishUp(Long actionId, String processId);

}
