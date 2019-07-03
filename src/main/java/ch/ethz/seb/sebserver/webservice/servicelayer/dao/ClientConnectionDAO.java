/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ClientConnectionDAO extends EntityDAO<ClientConnection, ClientConnection> {

    /** Get a list of all connection tokens of all connections (no matter what state)
     * of an exam.
     *
     * @param examId The exam identifier
     * @return list of all connection tokens of all connections (no matter what state)
     *         of an exam */
    Result<Collection<String>> getConnectionTokens(Long examId);

    /** Get a ClientConnection for a specified token.
     *
     * @param connectionToken the connection token
     * @return Result refer to ClientConnection or refer to a error if happened */
    Result<ClientConnection> byConnectionToken(String connectionToken);

}
