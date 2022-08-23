/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;

/** Defines a client connection to client group matcher for a specific client group type.
 * Use this to check whether a specific client connection is in a defined client group of specified type. */
public interface ClientGroupConnectionMatcher {

    ClientGroupType matcherType();

    boolean isInGroup(ClientConnection clientConnection, ClientGroup group);

}
