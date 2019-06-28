/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** A Service to handle running exam sessions */
public interface ExamSessionService {

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

    /** Gets all all currently running Exams for a particular Institution.
     *
     * @param institutionId the Institution identifier
     * @return Result referencing the list of all currently running Exams of the institution or to an error if
     *         happened. */
    Result<Collection<Exam>> getRunningExamsForInstitution(Long institutionId);

}
