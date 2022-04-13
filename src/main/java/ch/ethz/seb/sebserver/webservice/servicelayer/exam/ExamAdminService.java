/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import org.apache.commons.lang3.BooleanUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;

public interface ExamAdminService {

    /** Get the exam domain object for the exam identifier (PK).
     *
     * @param examId the exam identifier
     * @return Result refer to the domain object or to an error when happened */
    Result<Exam> examForPK(Long examId);

    /** Saves additional attributes for the exam that are specific to a type of LMS
     *
     * @param exam The Exam to add the LMS specific attributes
     * @return Result refer to the created exam or to an error when happened */
    Result<Exam> saveLMSAttributes(Exam exam);

    /** Applies all additional SEB restriction attributes that are defined by the
     * type of the LMS of a given Exam to this given Exam.
     *
     * @param exam the Exam to apply all additional SEB restriction attributes
     * @return Result refer to the created exam or to an error when happened */
    Result<Exam> applyAdditionalSEBRestrictions(Exam exam);

    /** Indicates whether a specific exam is been restricted with SEB restriction feature on the LMS or not.
     *
     * @param exam The exam instance
     * @return Result refer to the restriction flag or to an error when happened */
    Result<Boolean> isRestricted(Exam exam);

    /** Get proctoring service settings for a certain exam to an error when happened.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring service settings for the exam. */
    Result<ProctoringServiceSettings> getProctoringServiceSettings(Long examId);

    /** Save the given proctoring service settings for an existing Exam.
     *
     * @param examId the exam identifier
     * @param proctoringServiceSettings The proctoring service settings to save for the exam
     * @return Result refer to saved proctoring service settings or to an error when happened. */
    Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            Long examId,
            ProctoringServiceSettings proctoringServiceSettings);

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam instance
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    default Result<Boolean> isProctoringEnabled(final Exam exam) {
        if (exam == null || exam.id == null) {
            return Result.ofRuntimeError("Invalid Exam model");
        }

        if (exam.additionalAttributesIncluded()) {
            return Result.tryCatch(() -> {
                return BooleanUtils.toBooleanObject(
                        exam.getAdditionalAttribute(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING));
            });
        }

        return isProctoringEnabled(exam.id);
    }

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    Result<Boolean> isProctoringEnabled(final Long examId);

    /** Get the exam proctoring service implementation for specified exam.
     *
     * @param examId the exam identifier
     * @return ExamProctoringService instance */
    Result<ExamProctoringService> getExamProctoringService(final Long examId);

}
