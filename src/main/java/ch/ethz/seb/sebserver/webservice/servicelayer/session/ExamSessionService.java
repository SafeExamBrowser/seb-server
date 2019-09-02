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

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

/** A Service to handle running exam sessions */
public interface ExamSessionService {

    /** Get the underling ExamDAO service.
     *
     * @return the underling ExamDAO service. */
    ExamDAO getExamDAO();

    /** Indicates whether an Exam is currently running or not.
     *
     * @param examId the PK of the Exam to test
     * @return true if an Exam is currently running */
    boolean isExamRunning(Long examId);

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

    /** Streams the default SEB Exam Configuration to a ClientConnection with given connectionToken.
     *
     * @param connectionToken The connection token that identifiers the ClientConnection
     * @param out The OutputStream to stream the data to */
    void streamDefaultExamConfig(String connectionToken, OutputStream out);

    /** Get current ClientConnectionData for a specified active SEB client connection.
     *
     * active SEB client connections are connections that were initialized by a SEB client
     * on the particular server instance.
     *
     * @param connectionToken the connection token of the active SEB client connection
     * @return Result refer to the ClientConnectionData instance or to an error if happened */
    Result<ClientConnectionData> getConnectionData(String connectionToken);

    /** Get the collection of ClientConnectionData of all active SEB client connections
     * of a running exam.
     *
     * active SEB client connections are connections that were initialized by a SEB client
     * on the particular server instance. This may not be the all connections of an exam but
     * a subset of them.
     *
     * @param examId The exam identifier
     * @return collection of ClientConnectionData of all active SEB client connections
     *         of a running exam */
    Result<Collection<ClientConnectionData>> getConnectionData(Long examId);

}
