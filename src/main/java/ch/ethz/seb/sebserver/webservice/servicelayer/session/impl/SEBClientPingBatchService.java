/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientPingService;

@Lazy
@Component
@WebServiceProfile
@ConditionalOnExpression("'${sebserver.webservice.ping.service.strategy}'.equals('BATCH')")
public class SEBClientPingBatchService implements SEBClientPingService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientPingBatchService.class);

    private final ExamSessionCacheService examSessionCacheService;
    private final SEBClientInstructionService sebClientInstructionService;


    private final Set<String> pingKeys = new HashSet<>();
    private final Map<String, String> pings = new ConcurrentHashMap<>();
    private final Map<String, String> instructions = new ConcurrentHashMap<>();

    public SEBClientPingBatchService(
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientInstructionService sebClientInstructionService) {

        this.examSessionCacheService = examSessionCacheService;
        this.sebClientInstructionService = sebClientInstructionService;
    }

    @Scheduled(fixedDelayString = "${sebserver.webservice.api.exam.session.ping.batch.interval:500}")
    public void processPings() {
        if (this.pings.isEmpty()) {
            return;
        }

        final int size = this.pings.size();
        if (size > 1000) {
            log.warn("----> There are more then 1000 SEB client logs in the waiting queue: {}", size);
        }

        try {
            this.pingKeys.clear();
            this.pingKeys.addAll(this.pings.keySet());
            //final Set<String> connections = new HashSet<>(this.pings.keySet());
            this.pingKeys.stream().forEach(cid -> processPing(
                    cid,
                    this.pings.remove(cid),
                    Utils.getMillisecondsNow()));

        } catch (final Exception e) {
            log.error("Failed to process SEB pings from pingDataQueue: ", e);
        }
    }

    @Override
    public PingServiceType pingServiceType() {
        return PingServiceType.BATCH;
    }

    @Override
    public final String notifyPing(
            final String connectionToken,
            final String instructionConfirm) {

        final String instruction = this.instructions.remove(connectionToken);

        if (instructionConfirm != null) {
            System.out.println("************ put instructionConfirm: " + instructionConfirm + " instructions: "
                    + this.instructions);
            this.pings.put(connectionToken, instructionConfirm);
//            // TODO is this a good idea or is there another better way to deal with instruction confirm synchronization?
            if (instruction != null && instruction.contains("\"instruction-confirm\":\"" + instructionConfirm + "\"")) {
                return null;
            }
        } else if (!this.pings.containsKey(connectionToken)) {
            this.pings.put(connectionToken, StringUtils.EMPTY);
        }

//        System.out.println(
//                "**************** notifyPing instructionConfirm: " + instructionConfirm + " pings: " + this.pings);

        return instruction;
    }

    private void processPing(
            final String connectionToken,
            final String instructionConfirm,
            final long timestamp) {

        if (connectionToken == null) {
            return;
        }

        final ClientConnectionDataInternal activeClientConnection = this.examSessionCacheService
                .getClientConnection(connectionToken);

        if (activeClientConnection != null) {
            activeClientConnection.notifyPing(timestamp);
        } else {
            log.error("Failed to get ClientConnectionDataInternal for: {}", connectionToken);
        }

        if (StringUtils.isNotBlank(instructionConfirm)) {
            this.sebClientInstructionService.confirmInstructionDone(connectionToken, instructionConfirm);
        }

        if (this.instructions.containsKey(connectionToken)) {
            return;
        }

        final String instructionJSON = this.sebClientInstructionService.getInstructionJSON(connectionToken);
        if (instructionJSON != null) {
            this.instructions.put(connectionToken, instructionJSON);
        }
    }
}
