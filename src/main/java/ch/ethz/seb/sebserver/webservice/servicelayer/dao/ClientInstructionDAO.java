/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;

public interface ClientInstructionDAO {

    /** Inserts a new client instruction with the given attributes in the database
     * Note, if a client instruction needs confirmation an "instruction-confirm" attribute
     * is added to the given attributes with a unique confirmation number
     *
     * @param examId The exam identifier
     * @param type the type of instruction
     * @param attributes attributes in a JSON array
     * @param connectionToken the connection token of the SEB Client connection
     * @param needsConfirmation indicates whether this instruction needs confirmation or not
     * @return */
    Result<ClientInstructionRecord> insert(
            Long examId,
            InstructionType type,
            String attributes,
            String connectionToken,
            boolean needsConfirmation);

    /** Gets all instructions that are younger then one minute
     *
     * @return Result refer to all instructions that are younger then one minute or to an error when happened */
    Result<Collection<ClientInstructionRecord>> getAllActive();

    /** Get all active instructions for a specified connection token
     *
     * @param connectionToken the connection token
     * @return Collection of all active instructions for specified connection token */
    Result<Collection<ClientInstructionRecord>> getAllActive(String connectionToken);

    /** Deletes all old instructions from the persistent storage to clean-up.
     * Old in this case means the timestamp is older then one minute or a configured time interval
     *
     * @param timestamp the time-stamp (milliseconds) of the time in the past from that earlier instructions are
     *            considered inactive
     * @return Result collection of keys of deleted entities or refer to an error when happened */
    Result<Collection<EntityKey>> deleteAllInactive(long timestamp);

    /** Deletes the specified instruction from the data base
     *
     * @param id the identifier (PK) if the ClientInstruction to delete
     * @return Void Result refer to an error if happened */
    Result<Void> delete(Long id);

}
