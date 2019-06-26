/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.EventHandlingStrategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import io.micrometer.core.instrument.util.StringUtils;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionCacheService examSessionCacheService;
    private final ExamDAO examDAO;
    private final EventHandlingStrategy eventHandlingStrategy;

    protected ExamSessionServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final ExamDAO examDAO,
            final Environment environment,
            final ApplicationContext applicationContext) {

        this.examSessionCacheService = examSessionCacheService;
        this.examDAO = examDAO;

        String eventHandlingStrategyProperty =
                environment.getProperty(EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_CONFIG_PROPERTY_KEY);

        if (StringUtils.isBlank(eventHandlingStrategyProperty)) {
            eventHandlingStrategyProperty = EventHandlingStrategy.EVENT_CONSUMER_STRATEGY_SINGLE_EVENT_STORE;
        }

        this.eventHandlingStrategy = applicationContext.getBean(
                eventHandlingStrategyProperty,
                EventHandlingStrategy.class);
    }

    @Override
    public boolean isExamRunning(final Long examId) {
        return this.examSessionCacheService
                .isRunning(this.examSessionCacheService.getRunningExam(examId));
    }

    @Override
    public Result<Exam> getRunningExam(final Long examId) {
        final Exam exam = this.examSessionCacheService.getRunningExam(examId);
        if (this.examSessionCacheService.isRunning(exam)) {
            return Result.of(exam);
        } else {
            if (exam != null) {
                this.examSessionCacheService.evict(exam);
            }
            return Result.ofError(new NoSuchElementException(
                    "No currenlty running exam found for id: " + examId));
        }
    }

    @Override
    public Result<Collection<Exam>> getRunningExamsForInstitution(final Long institutionId) {
        return this.examDAO.allIdsOfInstituion(institutionId)
                .map(col -> col.stream()
                        .map(examId -> this.examSessionCacheService.getRunningExam(examId))
                        .filter(exam -> exam != null)
                        .collect(Collectors.toList()));
    }

    @Override
    public void notifyPing(final Long connectionId, final long timestamp, final int pingNumber) {
        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionId);

        if (activeClientConnection != null) {
            activeClientConnection.pingMappings
                    .stream()
                    .forEach(pingIndicator -> pingIndicator.notifyPing(timestamp, pingNumber));
        }
    }

    @Override
    public void notifyClientEvent(final ClientEvent event, final Long connectionId) {
        this.eventHandlingStrategy.accept(event);

        final ClientConnectionDataInternal activeClientConnection =
                this.examSessionCacheService.getActiveClientConnection(connectionId);

        if (activeClientConnection != null) {
            activeClientConnection.getindicatorMapping(event.eventType)
                    .stream()
                    .forEach(indicator -> indicator.notifyValueChange(event));
        }
    }

}
