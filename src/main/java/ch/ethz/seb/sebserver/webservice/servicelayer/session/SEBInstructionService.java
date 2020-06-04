/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;

/** Service for SEB instruction handling.
 *
 * SEB instructions are sent as response of a SEB Ping on a active SEB Connection
 * If there is an instruction in the queue for a specified SEB Client. */
public interface SEBInstructionService {

    /** Get the underling WebserviceInfo
     *
     * @return the underling WebserviceInfo */
    WebserviceInfo getWebserviceInfo();

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param clientInstruction the ClientInstruction instance to register */
    default Result<Void> registerInstruction(final ClientInstruction clientInstruction) {
        return Result.tryCatch(() -> registerInstruction(
                clientInstruction.examId,
                clientInstruction.type,
                clientInstruction.attributes,
                new HashSet<>(Arrays.asList(StringUtils.split(
                        clientInstruction.connectionToken,
                        Constants.LIST_SEPARATOR))))
                                .getOrThrow());
    }

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param examId The exam identifier
     * @param type The InstructionType
     * @param attributes The instruction's attributes
     * @param connectionTokens A Set of connectionTokens to register the instruction for.
     * @return A Result refer to a void marker or to an error if happened */
    Result<Void> registerInstruction(
            final Long examId,
            InstructionType type,
            Map<String, String> attributes,
            Set<String> connectionTokens);

    /** Get a SEB instruction for the specified SEB Client connection or null of there
     * is currently no SEB instruction in the queue.
     *
     * NOTE: If this call returns a SEB instruction instance, this instance is considered
     * as processed for the specified SEB Client afterwards and will be removed from the queue
     *
     * @param connectionToken the SEB Client connection token
     * @return SEB instruction to sent to the SEB Client or null */
    String getInstructionJSON(final String connectionToken);

}
