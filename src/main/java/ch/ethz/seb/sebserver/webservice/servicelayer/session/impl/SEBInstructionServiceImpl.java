/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientInstructionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBInstructionService;

@Lazy
@Service
@WebServiceProfile
public class SEBInstructionServiceImpl implements SEBInstructionService {

    private static final Logger log = LoggerFactory.getLogger(SEBInstructionServiceImpl.class);

    private static final String JSON_INST = "instruction";
    private static final String JSON_ATTR = "attributes";

    private final WebserviceInfo webserviceInfo;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientInstructionDAO clientInstructionDAO;
    private final Map<String, ClientInstructionRecord> instructions;

    private long lastRefresh = 0;

    public SEBInstructionServiceImpl(
            final WebserviceInfo webserviceInfo,
            final ClientConnectionDAO clientConnectionDAO,
            final ClientInstructionDAO clientInstructionDAO) {

        this.webserviceInfo = webserviceInfo;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientInstructionDAO = clientInstructionDAO;
        this.instructions = new ConcurrentHashMap<>();
    }

    @Override
    public WebserviceInfo getWebserviceInfo() {
        return this.webserviceInfo;
    }

    @EventListener(SEBServerInitEvent.class)
    public void init() {
        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Run SEBInstructionService...");

        loadInstruction()
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
            final Set<String> connectionTokens) {

        return Result.tryCatch(() -> {

            final String attributesString = Utils.toJsonArrayValue(attributes);
            final Set<String> activeConnections = new HashSet<>(this.clientConnectionDAO
                    .getConnectionTokens(examId)
                    .getOrElse(Collections::emptyList));

            connectionTokens
                    .stream()
                    .filter(activeConnections::contains)
                    .map(token -> this.clientInstructionDAO.insert(examId, type, attributesString, token))
                    .map(result -> result.get(
                            error -> log.error("Failed to put instruction: ", error),
                            () -> null))
                    .filter(Objects::nonNull)
                    .forEach(inst -> this.instructions.putIfAbsent(inst.getConnectionToken(), inst));
        });

    }

    @Override
    public String getInstructionJSON(final String connectionToken) {
        refreshCache();
        if (this.instructions.isEmpty()) {
            return null;
        }

        final ClientInstructionRecord clientInstruction = this.instructions.remove(connectionToken);
        if (clientInstruction != null) {
            final Result<Void> delete = this.clientInstructionDAO.delete(clientInstruction.getId());
            if (delete.hasError()) {
                log.error("Failed to delete SEB client instruction on persistent storage: ", delete.getError());
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
                        .append(Constants.CURLY_BRACE_OPEN)
                        .append(attributes)
                        .append(Constants.CURLY_BRACE_CLOSE);
            }

            return sBuilder
                    .append(Constants.CURLY_BRACE_CLOSE)
                    .toString();
        }

        return null;
    }

    private void refreshCache() {
        if (!this.webserviceInfo.isDistributed()) {
            return;
        }

        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - this.lastRefresh > Constants.SECOND_IN_MILLIS) {
            this.lastRefresh = currentTimeMillis;

            loadInstruction()
                    .onError(error -> log.error(
                            "Failed load instructions from persistent storage and to refresh cache: ",
                            error));
        }
    }

    private Result<Void> loadInstruction() {
        return Result.tryCatch(() -> this.clientInstructionDAO.getAllActive()
                .getOrThrow()
                .forEach(inst -> this.instructions.putIfAbsent(inst.getConnectionToken(), inst)));
    }

}
