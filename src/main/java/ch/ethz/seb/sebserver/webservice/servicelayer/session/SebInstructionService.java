/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;

/** Service for SEB instruction handling.
 *
 * SEB instructions are sent as response of a SEB Ping on a active SEB Connection
 * If there is an instruction in the queue for a specified SEB Client. */
public interface SebInstructionService {

    /** Get a SEB instruction for the specified SEB Client connection or null of there
     * is currently no SEB instruction in the queue.
     * 
     * NOTE: If this call returns a SEB instruction instance, this instance is considered
     * as processed for the specified SEB Client afterwards and will be removed from the queue
     * 
     * @param connectionToken the SEB Client connection token
     * @return SEB instruction to sent to the SEB Client or null */
    public ClientInstruction.SebInstruction getInstruction(final String connectionToken);

}
