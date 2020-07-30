/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamProctoring;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamProctoring.ServerType;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamAdminService {

    /** Adds a default indicator that is defined by configuration to a given exam.
     *
     * @param exam The Exam to add the default indicator
     * @return the Exam with added default indicator */
    Result<Exam> addDefaultIndicator(Exam exam);

    /** Applies all additional SEB restriction attributes that are defined by the
     * type of the LMS of a given Exam to this given Exam.
     *
     * @param exam the Exam to apply all additional SEB restriction attributes
     * @return the Exam */
    Result<Exam> applyAdditionalSEBRestrictions(Exam exam);

    /** Indicates whether a specific exam is been restricted with SEB restriction feature on the LMS or not.
     *
     * @param exam The exam instance
     * @return Result refer to the restriction flag or to an error when happened */
    Result<Boolean> isRestricted(Exam exam);

    /** Get ExamProctoring data for a certain exam to an error when happened.
     *
     * @param examId the exam instance
     * @return Result refer to ExamProctoring data for the exam. */
    default Result<ExamProctoring> getExamProctoring(final Exam exam) {
        if (exam == null || exam.id == null) {
            return Result.ofRuntimeError("Invalid Exam model");
        }
        return getExamProctoring(exam.id);
    }

    /** Get ExamProctoring data for a certain exam to an error when happened.
     *
     * @param examId the exam identifier
     * @return Result refer to ExamProctoring data for the exam. */
    Result<ExamProctoring> getExamProctoring(Long examId);

    /** Save the given ExamProctoring data for an existing Exam.
     *
     * @param examId the exam identifier
     * @param examProctoring The ExamProctoring data to save for the exam
     * @return Result refer to saved ExamProctoring data or to an error when happened. */
    Result<ExamProctoring> saveExamProctoring(Long examId, ExamProctoring examProctoring);

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam instance
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    default Result<Boolean> isExamProctoringEnabled(final Exam exam) {
        if (exam == null || exam.id == null) {
            return Result.ofRuntimeError("Invalid Exam model");
        }
        return isExamProctoringEnabled(exam.id);
    }

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    Result<Boolean> isExamProctoringEnabled(final Long examId);

    public Result<ExamProctoringService> getExamProctoringService(final ServerType type);

}
