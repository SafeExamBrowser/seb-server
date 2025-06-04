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

    /** This gets the actual monitoring overview data for a given running Exam.
     * 
     * @param runningExam The running exam instance
     * @return full and actual ExamMonitoringOverviewData */
    ExamMonitoringOverviewData getExamMonitoringOverviewData(Exam runningExam);

    /** Gets actual monitoring page data (list of SEB connections) for the given exam.
     * 
     * @param runningExam the running exam instance
     * @param filter filter for ClientConnectionData that is applied to the list of SEB client connections
     * @return MonitoringFullPageData containing only the data for the monitoring page list. */
    // TODO create filter cache per user!?
    MonitoringFullPageData getFullMonitoringPageData(
            Exam runningExam,
            Predicate<ClientConnectionData> filter);

    /** Use this to create a monitoring page list filter.
     * Inputs are comma separated lists of filter values for a filter-group
     * Logic is: OR in filter-groups, AND between filter-groups
     * 
     * @param showStates connection state filter group, comma separated list of ConnectionStatus
     * @param showClientGroups client group filter group, comma separated list of client group ids
     * @param showIndicators indicator filter group, comma separated list of IndicatorType. Currently only WLAN und BATTERY are supported
     * @param showNotifications notification filter group, comma separated list of NotificationType
     * @return monitoring page list filter */
    Predicate<ClientConnectionData> createMonitoringFilter(
            String showStates,
            String showClientGroups,
            String showIndicators,
            String showNotifications);
    
}
