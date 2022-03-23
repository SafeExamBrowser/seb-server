/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.io.OutputStream;
import java.util.Collection;
import java.util.function.Predicate;

import org.springframework.cache.CacheManager;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringSEBConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ClientConnectionDataInternal;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

/** A Service to handle running exam sessions */
public interface ExamSessionService {

    public static final Predicate<ClientConnection> ACTIVE_CONNECTION_FILTER =
            cc -> cc.status == ConnectionStatus.ACTIVE;
    public static final Predicate<ClientConnectionData> ACTIVE_CONNECTION_DATA_FILTER =
            ccd -> ccd.clientConnection.status == ConnectionStatus.ACTIVE;

    /** Get the underling ExamDAO service.
     *
     * @return the underling ExamDAO service. */
    ExamDAO getExamDAO();

    /** get the underling ClientConnectionDAO
     *
     * @return the underling ClientConnectionDAO */
    ClientConnectionDAO getClientConnectionDAO();

    /** Get the underling ExamSessionCacheService
     *
     * @return the underling ExamSessionCacheService */
    ExamSessionCacheService getExamSessionCacheService();

    /** Get the underling CacheManager
     *
     * @return the underling CacheManager */
    CacheManager getCacheManager();

    /** Get the underling LmsAPIService
     *
     * @return the underling LmsAPIService */
    LmsAPIService getLmsAPIService();

    /** Use this to check the consistency of a running Exam.
     * Current consistency checks are:
     * - Check if there is at least one Exam supporter attached to the Exam
     * - Check if we have access to LMS for the exam
     * - Check if there is one default SEB Exam Configuration attached to the Exam
     * - Check if SEB restriction API is available and the exam is running but not yet restricted on LMS side
     * - Check if there is at least one Indicator defined for the monitoring of the Exam
     *
     * @param examId the identifier of the Exam to check
     * @return Result of one APIMessage per consistency check if the check failed. An empty Collection of everything is
     *         okay. */
    Result<Collection<APIMessage>> checkExamConsistency(Long examId);

    /** Use this to check if a specified Exam has currently active SEB Client connections.
     *
     * Active SEB Client connections are established connections that are not yet closed and
     * connection attempts that are older the a defined time interval.
     *
     * @param examId The Exam identifier
     * @return true if the given Exam has currently no active client connection, false otherwise. */
    default boolean hasActiveSEBClientConnections(final Long examId) {
        if (examId == null || !this.isExamRunning(examId)) {
            return false;
        }

        return !this.getActiveConnectionTokens(examId)
                .getOrThrow()
                .isEmpty();
    }

    /** Checks if a specified Exam has at least a default SEB Exam configuration attached.
     *
     * @param examId the identifier if the Exam to check
     * @return true if there is a default SEB Exam Configuration attached or false if not */
    boolean hasDefaultConfigurationAttached(final Long examId);

    /** Indicates whether an Exam is currently running or not.
     *
     * @param examId the PK of the Exam to test
     * @return true if an Exam is currently running */
    boolean isExamRunning(Long examId);

    /** Indicates if the Exam with specified Id is currently locked for new SEB Client connection attempts.
     *
     * @param examId The Exam identifier
     * @return true if the specified Exam is currently locked for new SEB Client connections. */
    boolean isExamLocked(Long examId);

    /** Use this to get currently running exams by exam identifier.
     * This test first if the Exam with the given identifier is currently/still
     * running. If true the Exam is returned within the result. Otherwise the
     * returned Result contains a NoSuchElementException error.
     *
     * @param examId The Exam identifier (PK)
     * @return Result referencing the running Exam or an error if the Exam not exists or is not currently running */
    Result<Exam> getRunningExam(Long examId);

    /** Gets all currently running Exams for a particular Institution.
     *
     * @param institutionId the Institution identifier
     * @return Result referencing the list of all currently running Exams of the institution or to an error if
     *         happened. */
    Result<Collection<Exam>> getRunningExamsForInstitution(Long institutionId);

    /** Gets all currently running Exams for a particular FilterMap.
     *
     * @param filterMap the FilterMap containing the filter attributes
     * @param predicate additional filter predicate
     * @return Result referencing the list of all currently running Exams or to an error if happened. */
    Result<Collection<Exam>> getFilteredRunningExams(
            FilterMap filterMap,
            Predicate<Exam> predicate);

    /** Gets all finished Exams for a particular FilterMap.
     *
     * @param filterMap the FilterMap containing the filter attributes
     * @param predicate additional filter predicate
     * @return Result referencing the list of all currently finished Exams or to an error if happened. */
    Result<Collection<Exam>> getFilteredFinishedExams(
            FilterMap filterMap,
            Predicate<Exam> predicate);

    /** Streams the default SEB Exam Configuration to a ClientConnection with given connectionToken.
     *
     * @param institutionId the Institution identifier
     * @param connectionToken The connection token that identifiers the ClientConnection
     * @param out The OutputStream to stream the data to */
    void streamDefaultExamConfig(
            Long institutionId,
            String connectionToken,
            OutputStream out);

    /** Get current ClientConnectionData for a specified active SEB client connection.
     *
     * active SEB client connections are connections that were initialized by a SEB client
     * on the particular server instance.
     *
     * @param connectionToken the connection token of the active SEB client connection
     * @return Result refer to the ClientConnectionData instance or to an error if happened */
    Result<ClientConnectionData> getConnectionData(String connectionToken);

    /** Get the collection of ClientConnectionData of all SEB client connections
     * of a running exam that match the given filter criteria.
     *
     * @param examId The exam identifier
     * @param filter a filter predicate to apply
     * @return collection of ClientConnectionData of all active SEB client connections
     *         of a running exam */
    Result<Collection<ClientConnectionData>> getConnectionData(
            Long examId,
            Predicate<ClientConnectionData> filter);

    /** Get the MonitoringSEBConnectionsData containing a collection of ClientConnectionData of all
     * SEB client connections matching the given filter criteria.
     * And also containing a connection number per connection status mapping.
     *
     * @param examId The exam identifier
     * @param filter a filter predicate to apply
     * @return Result refer to MonitoringSEBConnectionsData of a running exam or to an error when happened */
    Result<MonitoringSEBConnectionData> getMonitoringSEBConnectionsData(
            final Long examId,
            final Predicate<ClientConnectionData> filter);

    /** Gets all connection tokens of active client connection that are related to a specified exam
     * from persistence storage without caching involved.
     *
     * @param examId the exam identifier
     * @return Result refer to the collection of connection tokens or to an error when happened. */
    Result<Collection<String>> getActiveConnectionTokens(Long examId);

    /** Use this to check if the current cached running exam is up to date
     * and if not to flush the cache.
     *
     * @param examId the Exam identifier
     * @return Result with updated Exam instance or refer to an error if happened */
    Result<Exam> updateExamCache(Long examId);

    /** Flush all the caches for an specified Exam.
     *
     * <pre>
     *  - Exam
     *  - Exam Configuration
     *  - All ClientConnection of the running exam
     * </pre>
     *
     * @param exam The Exam instance
     * @return Result with reference to the given Exam or to an error if happened */
    Result<Exam> flushCache(final Exam exam);

    /** Is is supposed to be the single access point to internally get client connection
     * data for a specified connection token.
     * This uses the client connection data cache for lookup and also synchronizes asynchronous
     * cache calls to prevent parallel creation of ClientConnectionDataInternal
     *
     * @param connectionToken the connection token of the active SEB client connection
     * @return ClientConnectionDataInternal by synchronized cache lookup or null if not available */
    ClientConnectionDataInternal getConnectionDataInternal(String connectionToken);

    /** Checks if the given ClientConnectionData is an active SEB client connection.
     *
     * @param connection ClientConnectionData instance
     * @return true if the given ClientConnectionData is an active SEB client connection */
    static boolean isActiveConnection(final ClientConnectionData connection) {
        return connection.clientConnection.status.clientActiveStatus;
    }

}
