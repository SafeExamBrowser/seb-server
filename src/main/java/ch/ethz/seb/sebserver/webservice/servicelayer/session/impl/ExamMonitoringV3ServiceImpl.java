/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ch.ethz.seb.sebserver.gbl.model.session.ExamMonitoringOverviewData.*;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ExamMonitoringOverviewData;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class ExamMonitoringV3ServiceImpl implements ExamMonitoringV3Service {
    
    private final ExamSessionCacheService examSessionCacheService;
    private final SEBClientNotificationService sebClientNotificationService;
    private final ScreenProctoringService screenProctoringService;
    private final ExamAdminService examAdminService;
    private final ClientConnectionDAO clientConnectionDAO;
    private final ClientGroupDAO clientGroupDAO;

    public ExamMonitoringV3ServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientNotificationService sebClientNotificationService,
            final ScreenProctoringService screenProctoringService,
            final ExamAdminService examAdminService,
            final ClientConnectionDAO clientConnectionDAO, 
            final ClientGroupDAO clientGroupDAO) {
        
        this.examSessionCacheService = examSessionCacheService;
        this.sebClientNotificationService = sebClientNotificationService;
        this.screenProctoringService = screenProctoringService;
        this.examAdminService = examAdminService;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientGroupDAO = clientGroupDAO;
    }

    @Override
    public ExamMonitoringOverviewData getExamMonitoringOverviewData(final Exam runningExam) {
        
        final boolean screenProctoringEnabled = this.examAdminService.isScreenProctoringEnabled(runningExam);
        
        // TODO apply caching here
        final Map<Long, ScreenProctoringGroup> spsGroups =  (screenProctoringEnabled)
                ? screenProctoringService.getCollectingGroups(runningExam.id)
                .getOr(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        g -> g.sebGroupId != null ? g.sebGroupId : -1,
                        g -> g
                ))
                : Collections.emptyMap();

        final Map<Long, ClientGroup> groups = this.clientGroupDAO
                .allForExam(runningExam.id)
                .getOr(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        g -> g.id,
                        g -> new ClientGroup(
                                g.id, 
                                g.name,
                                spsGroups.containsKey(g.id) ? spsGroups.get(g.id).uuid : null, 
                                g.type.name(), 
                                g.displayValue())));
        
        if (spsGroups.containsKey(-1L)) {
            final ScreenProctoringGroup spsFallbackGroup = spsGroups.get(-1L);
            groups.put(-1L, new ClientGroup(1L, spsFallbackGroup.name,
                    spsFallbackGroup.uuid,
                    "SP_FALLBACK_GROUP",
                    ""));
        }
        
        final ClientStatesData clientStates = new ClientStatesData();
        final IndicatorData indicators = new IndicatorData();
        final NotificationData notifications = new NotificationData();

        this.clientConnectionDAO
                .getConnectionTokens(runningExam.id)
                .getOrThrow()
                .stream()
                .map(this.examSessionCacheService::getClientConnection)
                .filter(Objects::nonNull)
                .forEach(cc -> {
                    
                    // states
                    if (cc.missingPing != null && cc.missingPing) {
                        clientStates.MISSING++;
                    } else {
                        switch (cc.clientConnection.status) {
                            case CONNECTION_REQUESTED -> clientStates.CONNECTION_REQUESTED++;
                            case READY -> clientStates.READY++;
                            case ACTIVE -> clientStates.ACTIVE++;
                            case CLOSED -> clientStates.CLOSED++;
                            case DISABLED -> clientStates.DISABLED++;
                            default -> {}
                        }
                    }
                    
                    // incidences on indicators
                    if (cc.hasIncident(Indicator.IndicatorType.BATTERY_STATUS)) {
                        indicators.BATTERY_STATUS++;
                    }
                    if (cc.hasIncident(Indicator.IndicatorType.WLAN_STATUS)) {
                        indicators.WLAN_STATUS++;
                    }
                    
                    // notifications
                    if (cc.pendingNotification != null && cc.pendingNotification) {
                        sebClientNotificationService
                                .getPendingNotifications(cc.getConnectionId())
                                .getOr(Collections.emptyList())
                                .forEach( n -> {
                                    switch (n.notificationType) {
                                        case LOCK_SCREEN -> notifications.LOCK_SCREEN++;
                                        case RAISE_HAND -> notifications.RAISE_HAND++;
                                        default -> {}
                                    }
                                });
                    }
                    
                    // groups
                    if (cc.groups != null) {
                        if (cc.groups.isEmpty()) {
                            if (screenProctoringEnabled) {
                                final ClientGroup fallbackGroup = groups.get(-1L);
                                fallbackGroup.clientAmount++;
                            }
                        } else {
                            cc.groups.forEach(gId -> {
                                final ClientGroup clientGroup = groups.get(gId);
                                clientGroup.clientAmount++;
                            });
                        }
                    } else  if (screenProctoringEnabled) {
                        final ClientGroup fallbackGroup = groups.get(-1L);
                        fallbackGroup.clientAmount++;
                    }

                });
        
        return new ExamMonitoringOverviewData(clientStates, groups.values(), indicators, notifications);
    }

    @Override
    public MonitoringFullPageData getFullMonitoringPageData(
            final Exam runningExam, 
            final boolean showAll, 
            final Predicate<ClientConnectionData> filter) {
        
        return null;
    }

    @Override
    public Predicate<ClientConnectionData> createMonitoringFilter(
            final String showStates, 
            final String showClientGroups, 
            final String showIndicators, 
            final String showNotifications) {
        
        return null;
    }
}
