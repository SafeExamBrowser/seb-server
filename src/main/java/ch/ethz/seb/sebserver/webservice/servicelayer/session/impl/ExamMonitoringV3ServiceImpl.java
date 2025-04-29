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

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.session.*;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification.NotificationType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientGroupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
    private final ExamSessionService examSessionService;

    public ExamMonitoringV3ServiceImpl(
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientNotificationService sebClientNotificationService,
            final ScreenProctoringService screenProctoringService,
            final ExamAdminService examAdminService,
            final ClientConnectionDAO clientConnectionDAO,
            final ClientGroupDAO clientGroupDAO, 
            final ExamSessionService examSessionService) {
        
        this.examSessionCacheService = examSessionCacheService;
        this.sebClientNotificationService = sebClientNotificationService;
        this.screenProctoringService = screenProctoringService;
        this.examAdminService = examAdminService;
        this.clientConnectionDAO = clientConnectionDAO;
        this.clientGroupDAO = clientGroupDAO;
        this.examSessionService = examSessionService;
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
                    StringUtils.EMPTY));
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
                    clientStates.calcTotal();
                    
                    // incidences on indicators
                    if (cc.hasIncident(IndicatorType.BATTERY_STATUS)) {
                        indicators.BATTERY_STATUS++;
                    }
                    if (cc.hasIncident(IndicatorType.WLAN_STATUS)) {
                        indicators.WLAN_STATUS++;
                    }
                    indicators.calcTotal();;
                    
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
                    notifications.calcTotal();
                    
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
            final Predicate<ClientConnectionData> filter) {
        
        final List<? extends ClientMonitoringDataView> filteredConnections = this.clientConnectionDAO
                .getConnectionTokens(runningExam.id)
                .getOrThrow()
                .stream()
                .map(this.examSessionService::getConnectionDataInternal)
                .filter(Objects::nonNull)
                .filter(filter)
                .map(ccd -> ccd.monitoringDataView)
                .toList();

                return new MonitoringFullPageData(
                runningExam.id,
                new MonitoringSEBConnectionData(
                        null,
                        null,
                        null,
                        filteredConnections),
                null); // NOTE: we need no screen proctoring data here anymore!?
    }

    @Override
    public Predicate<ClientConnectionData> createMonitoringFilter(
            final String showStates, 
            final String showClientGroups, 
            final String showIndicators, 
            final String showNotifications) {
        
        if (StringUtils.isBlank(showStates) && 
                StringUtils.isBlank(showClientGroups) && 
                StringUtils.isBlank(showIndicators) && 
                StringUtils.isBlank(showNotifications) ) {
            
            return Utils.falsePredicate();
        }

        final EnumSet<ConnectionStatus> states = EnumSet.noneOf(ConnectionStatus.class);
        if (StringUtils.isNotBlank(showStates)) {
            for (final String s : StringUtils.split(showStates, Constants.LIST_SEPARATOR)) {
                states.add(ConnectionStatus.valueOf(s));
            }
        }
        final Set<Long> showInClientGroups = showClientGroups != null 
                ? Arrays.stream(StringUtils.split(showClientGroups, Constants.LIST_SEPARATOR))
                    .map(Long::parseLong)
                    .collect(Collectors.toSet())
                : null;
        
        final boolean checkStates = !states.isEmpty();
        final boolean checkGroups = showInClientGroups != null && !showInClientGroups.isEmpty();
        final boolean showFallbackGroup = showInClientGroups != null && showInClientGroups.contains(-1L);
        final boolean showWLANIncident = showIndicators != null && showIndicators.contains(IndicatorType.WLAN_STATUS.name);
        final boolean showBatteryIncident = showIndicators != null && showIndicators.contains(IndicatorType.BATTERY_STATUS.name);
        final boolean showLockScreenNotifications = showNotifications != null && showNotifications.contains(NotificationType.LOCK_SCREEN.name());
        final boolean showRaiseHandNotifications = showNotifications != null && showNotifications.contains(NotificationType.RAISE_HAND.name());

        return cc -> {
            
            // state filter
            if (checkStates && !states.contains(cc.clientConnection.status)) {
                return false;
            }

            // groups filter
            if (checkGroups) {
                if (cc.groups != null && !cc.groups.isEmpty()) {
                    for (final Long gId : cc.groups) {
                        if (!showInClientGroups.contains(gId)) {
                            return false;
                        }
                    }
                } else {
                    if (!showFallbackGroup) {
                        return false;
                    }
                }
            }

            // indicators filter
            if (showWLANIncident || showBatteryIncident) {
                final boolean wlan = showWLANIncident && ((ClientConnectionDataInternal) cc).hasIncident(IndicatorType.WLAN_STATUS);
                final boolean battery = showBatteryIncident && ((ClientConnectionDataInternal) cc).hasIncident(IndicatorType.BATTERY_STATUS);
                if (!(wlan || battery)) {
                    return false;
                }
            }
            
            // notifications filter
            if (BooleanUtils.isTrue(cc.pendingNotification) && (showLockScreenNotifications || showRaiseHandNotifications)) {
                final boolean lock = showLockScreenNotifications && sebClientNotificationService
                        .hasPendingNotification(cc.clientConnection, NotificationType.LOCK_SCREEN );
                final boolean raise =  showRaiseHandNotifications && sebClientNotificationService
                        .hasPendingNotification(cc.clientConnection, NotificationType.RAISE_HAND );
                if (!(lock || raise)) {
                    return false;
                }
            }
            
            // pass
            return true;
        };
    }
}
