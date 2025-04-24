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

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.model.session.ExamMonitoringOverviewData;
import ch.ethz.seb.sebserver.gbl.model.session.ProctoringGroupMonitoringData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class ExamMonitoringV3ServiceImpl implements ExamMonitoringV3Service {

    private final ExamSessionService examSessionService;
    private final ExamSessionCacheService examSessionCacheService;
    private final SEBClientConnectionService sebClientConnectionService;
    private final SEBClientInstructionService sebClientInstructionService;
    private final SEBClientNotificationService sebClientNotificationService;
    private final ScreenProctoringService screenProctoringService;
    private final ExamAdminService examAdminService;
    private final ClientConnectionDAO clientConnectionDAO;

    public ExamMonitoringV3ServiceImpl(
            final ExamSessionService examSessionService,
            final ExamSessionCacheService examSessionCacheService,
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientInstructionService sebClientInstructionService,
            final SEBClientNotificationService sebClientNotificationService,
            final ScreenProctoringService screenProctoringService,
            final ExamAdminService examAdminService, 
            final ClientConnectionDAO clientConnectionDAO) {
        
        this.examSessionService = examSessionService;
        this.examSessionCacheService = examSessionCacheService;
        this.sebClientConnectionService = sebClientConnectionService;
        this.sebClientInstructionService = sebClientInstructionService;
        this.sebClientNotificationService = sebClientNotificationService;
        this.screenProctoringService = screenProctoringService;
        this.examAdminService = examAdminService;
        this.clientConnectionDAO = clientConnectionDAO;
    }

    @Override
    public ExamMonitoringOverviewData getExamMonitoringOverviewData(final Exam runningExam) {
        final boolean screenProctoringEnabled = this.examAdminService.isScreenProctoringEnabled(runningExam);
        final Collection<ProctoringGroupMonitoringData> screenProctoringData = (screenProctoringEnabled)
                ? this.screenProctoringService
                .getCollectingGroupsMonitoringData(runningExam.id)
                .getOr(Collections.emptyList())
                : Collections.emptyList();

 //       int[] stateAmounts = new int[] {0, 0, 0, 0, 0, 0};
        
//        final Map<String, Integer> clientStates = new HashMap<>();
//        final Collection<ExamMonitoringOverviewData.ClientGroup> groups = new ArrayList<>();
//        final Map<String, Integer> indicators = new HashMap<>();
//        final Map<String, Integer> notifications = new HashMap<>();

        this.clientConnectionDAO
                .getConnectionTokens(runningExam.id)
                .getOrThrow()
                .stream()
                .map(this.examSessionCacheService::getClientConnection)
                .filter(Objects::nonNull)
                .forEach(cc -> {
//                    if (cc.missingPing) {
//                        stateAmounts[ExamMonitoringOverviewData.ClientStates.MISSING.ordinal()]++;
//                    } else {
//                        
//                    }
//                    cc.clientConnection.status
                });


        return null;
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
