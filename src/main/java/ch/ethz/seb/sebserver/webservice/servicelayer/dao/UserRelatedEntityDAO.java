/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface UserRelatedEntityDAO<T extends Entity> extends EntityDAO<T> {

    Result<Collection<T>> getAllForUser(String userId);

    Result<Integer> deleteUserReferences(final String userId);

    Result<Integer> deleteUserEnities(final String userId);

}
