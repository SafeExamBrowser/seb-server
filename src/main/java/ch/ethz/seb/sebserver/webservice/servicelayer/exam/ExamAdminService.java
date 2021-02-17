/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;

public interface ExamAdminService {

    /** Adds a default indicator that is defined by configuration to a given exam.
     *
     * @param exam The Exam to add the default indicator
     * @return Result refer to the Exam with added default indicator or to an error if happened */
    Result<Exam> addDefaultIndicator(Exam exam);

    /** Saves additional attributes for a specified Exam on creation or on update.
     *
     * @param exam The Exam to add the default indicator
     * @return Result refer */
    Result<Exam> saveAdditionalAttributes(Exam exam);

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

    /** Get the proctoring service settings for a certain exam to an error when happened.
     *
     * @param examId the exam instance
     * @return Result refer to proctoring service settings for the exam. */
    default Result<ProctoringServiceSettings> getProctoringServiceSettings(final Exam exam) {
        if (exam == null || exam.id == null) {
            return Result.ofRuntimeError("Invalid Exam model");
        }
        return getProctoringServiceSettings(exam.id);
    }

    /** Get proctoring service settings for a certain exam to an error when happened.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring service settings for the exam. */
    Result<ProctoringServiceSettings> getProctoringServiceSettings(Long examId);

    /** Save the given proctoring service settings for an existing Exam.
     *
     * @param examId the exam identifier
     * @param examProctoring The proctoring service settings to save for the exam
     * @return Result refer to saved proctoring service settings or to an error when happened. */
    Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            Long examId,
            ProctoringServiceSettings examProctoring);

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam instance
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    default Result<Boolean> isProctoringEnabled(final Exam exam) {
        if (exam == null || exam.id == null) {
            return Result.ofRuntimeError("Invalid Exam model");
        }
        return isProctoringEnabled(exam.id);
    }

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    Result<Boolean> isProctoringEnabled(final Long examId);

    /** Get the exam proctoring service implementation of specified type.
     *
     * @param type exam proctoring service server type
     * @return ExamProctoringService instance */
    Result<ExamProctoringService> getExamProctoringService(final ProctoringServerType type);

    /** Get the exam proctoring service implementation of specified type.
     *
     * @param settings the ProctoringSettings that defines the ProctoringServerType
     * @return ExamProctoringService instance */
    default Result<ExamProctoringService> getExamProctoringService(final ProctoringServiceSettings settings) {
        return Result.tryCatch(() -> getExamProctoringService(settings.serverType).getOrThrow());
    }

    /** Get the exam proctoring service implementation for specified exam.
     *
     * @param exam the exam instance
     * @return ExamProctoringService instance */
    default Result<ExamProctoringService> getExamProctoringService(final Exam exam) {
        return Result.tryCatch(() -> getExamProctoringService(exam.id).getOrThrow());
    }

    /** Get the exam proctoring service implementation for specified exam.
     *
     * @param examId the exam identifier
     * @return ExamProctoringService instance */
    default Result<ExamProctoringService> getExamProctoringService(final Long examId) {
        return getProctoringServiceSettings(examId)
                .flatMap(this::getExamProctoringService);
    }

}
