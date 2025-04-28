/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.function.Predicate;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ExamMonitoringOverviewData;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;

public interface ExamMonitoringV3Service {
    
    ExamMonitoringOverviewData getExamMonitoringOverviewData(Exam runningExam);

    // TODO maybe we can reduce the MonitoringFullPageData model to just raw monitoring data
    MonitoringFullPageData getFullMonitoringPageData(
            Exam runningExam,
            Predicate<ClientConnectionData> filter);

    Predicate<ClientConnectionData> createMonitoringFilter(
            String showStates,
            String showClientGroups,
            String showIndicators,
            String showNotifications);
    
}
