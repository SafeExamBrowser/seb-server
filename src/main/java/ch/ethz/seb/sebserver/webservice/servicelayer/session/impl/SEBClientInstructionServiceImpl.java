/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.SizedArrayNonBlockingQueue;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientInstructionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@Lazy
@Service
@WebServiceProfile
public class SEBClientInstructionServiceImpl implements SEBClientInstructionService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientInstructionServiceImpl.class);

    private static final long PERSISTENT_UPDATE_INTERVAL = 2 * Constants.SECOND_IN_MILLIS;
    private static final int INSTRUCTION_QUEUE_MAX_SIZE = 10;
    private static final String JSON_INST = "instruction";
    private static final String JSON_ATTR = "attributes";

    private final WebserviceInfo webserviceInfo;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientInstructionDAO clientInstructionDAO;
    private final JSONMapper jsonMapper;

    private final Map<String, SizedArrayNonBlockingQueue<ClientInstructionRecord>> instructions;

    private long lastRefresh = 0;
    private long lastClean = 0;

    public SEBClientInstructionServiceImpl(
            final WebserviceInfo webserviceInfo,
            final ClientConnectionDAO clientConnectionDAO,
            final ClientInstructionDAO clientInstructionDAO,
            final JSONMapper jsonMapper) {

        this.webserviceInfo = webserviceInfo;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientInstructionDAO = clientInstructionDAO;
        this.jsonMapper = jsonMapper;
        this.instructions = new ConcurrentHashMap<>();
    }

    @Override
    public WebserviceInfo getWebserviceInfo() {
        return this.webserviceInfo;
    }

    @Override
    public void init() {
        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Run SEBInstructionService...");

        loadInstructions()
                .onError(
                        error -> log.error(
                                "Failed  to initialize and load persistent storage SEB client instructions: ",
                                error));

        if (this.instructions.size() > 0) {
            SEBServerInit.INIT_LOGGER.info("------> Loaded {} SEB client instructions from persistent storage",
                    this.instructions.size());
        } else {
            SEBServerInit.INIT_LOGGER.info("------> No pending SEB client instructions found on persistent storage");
        }
    }

    @Override
    public Result<Void> registerInstruction(
            final Long examId,
            final InstructionType type,
            final Map<String, String> attributes,
            final String connectionToken,
            final boolean needsConfirm) {

        return Result.tryCatch(() -> {

            final boolean isActive = this.clientConnectionDAO
                    .isInInstructionStatus(examId, connectionToken)
                    .getOr(false);

            if (isActive) {
                try {

                    final String attributesString = (attributes != null && !attributes.isEmpty())
                            ? this.jsonMapper.writeValueAsString(attributes)
                            : null;

                    this.clientInstructionDAO
                            .insert(examId, type, attributesString, connectionToken, needsConfirm)
                            .map(this::putToCache)
                            .onError(error -> log.error("Failed to register instruction: {}", error.getMessage()))
                            .getOrThrow();

                } catch (final Exception e) {
                    throw new RuntimeException("Unexpected: ", e);
                }
            } else {
                log.warn(
                        "The SEB client connection : {} is not in a ready state to process instructions. Instruction registration has been skipped",
                        connectionToken);
            }
        });
    }

    @Override
    public Result<Void> registerInstruction(
            final Long examId,
            final InstructionType type,
            final Map<String, String> attributes,
            final Set<String> connectionTokens,
            final boolean needsConfirm) {

        return Result.tryCatch(() -> {

            final String attributesString = this.jsonMapper.writeValueAsString(attributes);
            final Set<String> activeConnections = this.clientConnectionDAO
                    .filterForInstructionStatus(examId, connectionTokens)
                    .getOrElse(Collections::emptySet);

            connectionTokens
                    .stream()
                    .filter(activeConnections::contains)
                    .map(token -> this.clientInstructionDAO
                            .insert(examId, type, attributesString, token, needsConfirm))
                    .map(result -> result.get(
                            error -> log.error("Failed to register instruction: {}", error.getMessage()),
                            () -> null))
                    .filter(Objects::nonNull)
                    .forEach(this::putToCache);
        });
    }

    @Override
    public String getInstructionJSON(final String connectionToken) {

        final ClientInstructionRecord clientInstruction = getNextInstruction(connectionToken);
        if (clientInstruction == null) {
            return null;
        }

        final boolean needsConfirm = BooleanUtils.toBoolean(clientInstruction.getNeedsConfirmation());
        if (needsConfirm) {
            // add the instruction back to the queue's tail if it need a confirmation
            final SizedArrayNonBlockingQueue<ClientInstructionRecord> queue = this.instructions.get(connectionToken);
            if (queue != null) {
                queue.add(clientInstruction);
            }

        } else {
            // otherwise remove it also from the persistent storage
            final Result<Void> delete = this.clientInstructionDAO.delete(clientInstruction.getId());
            if (delete.hasError()) {
                log.error("Failed to delete SEB client instruction on persistent storage: ", delete.getError());
            }
        }

        // {"instruction":"%s", "attributes":%s}
        final String attributes = clientInstruction.getAttributes();
        final StringBuilder sBuilder = new StringBuilder()
                .append(Constants.CURLY_BRACE_OPEN)
                .append(Constants.DOUBLE_QUOTE)
                .append(JSON_INST)
                .append(Constants.DOUBLE_QUOTE)
                .append(Constants.COLON)
                .append(Constants.DOUBLE_QUOTE)
                .append(clientInstruction.getType())
                .append(Constants.DOUBLE_QUOTE);

        if (StringUtils.isNotBlank(attributes)) {
            sBuilder.append(Constants.COMMA)
                    .append(Constants.DOUBLE_QUOTE)
                    .append(JSON_ATTR)
                    .append(Constants.DOUBLE_QUOTE)
                    .append(Constants.COLON)
                    .append(attributes);
        }

        final String instructionJSON = sBuilder
                .append(Constants.CURLY_BRACE_CLOSE)
                .toString();

        if (log.isDebugEnabled()) {
            log.debug("Send SEB client instruction: {} to: {} ", instructionJSON, connectionToken);
        }

        return instructionJSON;
    }

    @Override
    public void confirmInstructionDone(final String connectionToken, final String instructionConfirm) {
        try {

            final SizedArrayNonBlockingQueue<ClientInstructionRecord> queue = this.instructions.get(connectionToken);
            final Long instructionId = Long.valueOf(instructionConfirm);
            this.clientInstructionDAO.delete(instructionId);
            if (queue.isEmpty()) {
                return;
            }

            queue.removeIf(instruction -> instructionId.equals(instruction.getId()));

        } catch (final Exception e) {
            log.error(
                    "Failed to remove SEB instruction after confirmation: connectionToken: {} instructionConfirm: {} connectionToken: {}",
                    connectionToken,
                    instructionConfirm,
                    connectionToken);
        }
    }

    @Override
    public void cleanupInstructions() {
        try {

            final long millisNowMinusOneMinute = DateTime
                    .now(DateTimeZone.UTC)
                    .minusMinutes(1)
                    .getMillis();

            if (this.lastClean < millisNowMinusOneMinute) {

                final Collection<EntityKey> deleted = this.clientInstructionDAO
                        .deleteAllInactive(millisNowMinusOneMinute)
                        .getOrThrow();

                if (!deleted.isEmpty()) {
                    log.info("Deleted out-dated instructions from persistent storage: {}", deleted);
                }

                cleanupCache();

                this.lastClean = System.currentTimeMillis();
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to cleanup instructions in persistent storage", e);
        }
    }

    private ClientInstructionRecord getNextInstruction(final String connectionToken) {
        // if we still have instruction for given connectionToken, process them first
        final long activeTime = DateTime.now(DateTimeZone.UTC).minusMinutes(1).getMillis();
        final SizedArrayNonBlockingQueue<ClientInstructionRecord> queue = this.instructions.computeIfAbsent(
                connectionToken,
                key -> new SizedArrayNonBlockingQueue<>(INSTRUCTION_QUEUE_MAX_SIZE));
        final ClientInstructionRecord nextActive = getNextActive(activeTime, queue);
        if (nextActive != null) {
            return nextActive;
        }

        // Since the queue is empty check periodically if there are active instructions on the persistent storage
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - this.lastRefresh > PERSISTENT_UPDATE_INTERVAL) {
            synchronized (this) {
                this.lastRefresh = currentTimeMillis;
                loadInstructions()
                        .onError(error -> log.error(
                                "Failed load instructions from persistent storage and to refresh cache: ",
                                error));

                if (!queue.isEmpty()) {
                    return getNextInstruction(connectionToken);
                }
            }
        }

        return null;
    }

    private void cleanupCache() {
        // check if there are still queues in the cache, whether they are empty or not,
        // for closed or disposed client connections and remove them from cache
        synchronized (this.instructions) {

            final Result<Collection<String>> result = this.clientConnectionDAO
                    .getInactiveConnctionTokens(this.instructions.keySet());

            if (result.hasValue()) {
                result.get().stream().forEach(token -> this.instructions.remove(token));
            }
        }
    }

    // Go recursively through the given queue to find the next active instruction
    private ClientInstructionRecord getNextActive(
            final long activeTime,
            final SizedArrayNonBlockingQueue<ClientInstructionRecord> queue) {

        if (queue != null && !queue.isEmpty()) {
            final ClientInstructionRecord rec = queue.poll();
            if (rec.getTimestamp().longValue() < activeTime) {
                return getNextActive(activeTime, queue);
            } else {
                return rec;
            }
        } else {
            return null;
        }
    }

    private Result<Void> loadInstructions() {
        return Result.tryCatch(() -> this.clientInstructionDAO.getAllActive()
                .getOrThrow()
                .forEach(this::putToCacheIfAbsent));
    }

    // NOTE: In a distributed setup we only fill the cache from persistent storage
    //       whereas in a none distributed setup we can put the instruction directly in the cache
    //       and store the instruction into persistent only for recovering reasons.
    private ClientInstructionRecord putToCache(final ClientInstructionRecord instruction) {
        if (!this.webserviceInfo.isDistributed()) {
            return putToCacheIfAbsent(instruction);
        }
        return instruction;
    }

    private ClientInstructionRecord putToCacheIfAbsent(final ClientInstructionRecord instruction) {
        final SizedArrayNonBlockingQueue<ClientInstructionRecord> queue = this.instructions.computeIfAbsent(
                instruction.getConnectionToken(),
                key -> new SizedArrayNonBlockingQueue<>(INSTRUCTION_QUEUE_MAX_SIZE));

        if (queue.contains(instruction)) {
            return instruction;
        }

        if (log.isDebugEnabled()) {
            log.debug("Put SEB instruction into instruction queue: {}", instruction);
        }

        queue.add(instruction);
        return instruction;
    }

}
