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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;

/** Service for SEB instruction handling.
 *
 * SEB instructions are sent as response of a SEB Ping on a active SEB Connection
 * If there is an instruction in the queue for a specified SEB Client. */
public interface SEBClientInstructionService {

    static final Logger log = LoggerFactory.getLogger(SEBClientInstructionService.class);

    /** Get the underling WebserviceInfo
     *
     * @return the underling WebserviceInfo */
    WebserviceInfo getWebserviceInfo();

    /** This is called from the SEB Server initializer to initialize this service.
     * Do not use this directly. */
    @EventListener(SEBServerInitEvent.class)
    void init();

    /** Used to register a SEB client instruction for one or more active client connections
     * within an other background thread. This is none-blocking.
     *
     * @param clientInstruction the ClientInstruction instance to register
     * @return A Result refer to a void marker or to an error if happened */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    default void registerInstructionAsync(final ClientInstruction clientInstruction) {
        registerInstruction(clientInstruction, false)
                .onError(error -> log.error("Failed to register client instruction asynchronously: {}",
                        clientInstruction,
                        error));
    }

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param clientInstruction the ClientInstruction instance to register
     * @return A Result refer to a void marker or to an error if happened */
    default Result<Void> registerInstruction(final ClientInstruction clientInstruction) {
        return registerInstruction(clientInstruction, false);
    }

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param clientInstruction the ClientInstruction instance to register
     * @param needsConfirm indicates whether the SEB instruction needs a confirmation or not
     * @return A Result refer to a void marker or to an error if happened */
    default Result<Void> registerInstruction(
            final ClientInstruction clientInstruction,
            final boolean needsConfirmation) {

        return registerInstruction(
                clientInstruction.examId,
                clientInstruction.type,
                clientInstruction.attributes,
                new HashSet<>(Arrays.asList(StringUtils.split(
                        clientInstruction.connectionToken,
                        Constants.LIST_SEPARATOR))),
                needsConfirmation);
    }

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param examId The exam identifier
     * @param type The InstructionType
     * @param attributes The instruction's attributes
     * @param connectionToken a connectionToken to register the instruction for.
     * @param needsConfirm indicates whether the SEB instruction needs a confirmation or not
     * @return A Result refer to a void marker or to an error if happened */
    Result<Void> registerInstruction(
            final Long examId,
            InstructionType type,
            Map<String, String> attributes,
            String connectionToken,
            boolean needsConfirm);

    /** Used to register a SEB client instruction for one or more active client connections
     *
     * @param examId The exam identifier
     * @param type The InstructionType
     * @param attributes The instruction's attributes
     * @param connectionTokens A Set of connectionTokens to register the instruction for.
     * @param needsConfirm indicates whether the SEB instruction needs a confirmation or not
     * @return A Result refer to a void marker or to an error if happened */
    Result<Void> registerInstruction(
            final Long examId,
            InstructionType type,
            Map<String, String> attributes,
            Set<String> connectionTokens,
            boolean needsConfirm);

    /** Get a SEB instruction for the specified SEB Client connection or null of there
     * is currently no SEB instruction in the queue.
     *
     * NOTE: If this call returns a SEB instruction instance, this instance is considered
     * as processed for the specified SEB Client afterwards and will be removed from the queue
     *
     * @param connectionToken the SEB Client connection token
     * @return SEB instruction to sent to the SEB Client or null */
    String getInstructionJSON(final String connectionToken);

    /** This is used to confirm SEB instructions that must be confirmed by the SEB client.
     *
     * @param connectionToken The SEB client connection token
     * @param instructionConfirm the instruction confirm identifier */
    void confirmInstructionDone(String connectionToken, String instructionConfirm);

    /** Used to cleanup out-dated instructions on the persistent storage */
    void cleanupInstructions();

}
