/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.*;
import org.apache.commons.lang3.BooleanUtils;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.RemoteProctoringService;

public interface ExamAdminService {

    ProctoringAdminService getProctoringAdminService();

    Result<Exam> applyExamImportInitialization(Exam exam);

    /** Get the exam domain object for the exam identifier (PK).
     *
     * @param examId the exam identifier
     * @return Result refer to the domain object or to an error when happened */
    Result<Exam> examForPK(Long examId);

    /** Initializes initial additional attributes for a yet created exam.
     *
     * @param exam The exam that has been created
     * @return The exam with the initial additional attributes */
    Result<Exam> initAdditionalAttributes(final Exam exam);

    /** Saves the security key settings for an specific exam.
     *
     * @param institutionId The institution identifier
     * @param examId The exam identifier
     * @param enabled The enabled setting that indicates if the security key check is enabled or not
     * @param numThreshold the numerical SEB client connection number grant threshold
     * @return Result refer to the exam with the new settings (additional attributes) or to an error when happened */
    Result<Exam> saveSecurityKeySettings(
            Long institutionId,
            Long examId,
            Boolean enabled,
            Integer numThreshold);

    /** Applies all additional SEB restriction attributes that are defined by the
     * type of the LMS of a given Exam to this given Exam.
     *
     * @param exam the Exam to apply all additional SEB restriction attributes
     * @return Result refer to the created exam or to an error when happened */
    Result<Exam> applyAdditionalSEBRestrictions(Exam exam);

    /** Indicates whether a specific exam is being restricted with SEB restriction feature on the LMS or not.
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
     * @param exam the exam instance
     * @return proctoring is enabled flag */
    default boolean isProctoringEnabled(final Exam exam) {
        if (exam == null || exam.id == null) {
            return false;
        }

        if (exam.additionalAttributesIncluded()) {
            return BooleanUtils.toBoolean(
                    exam.getAdditionalAttribute(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING));
        }

        return isProctoringEnabled(exam.id).getOr(false);
    }

    /** This indicates if screen proctoring is set and enabled for a certain exam.
     *
     * @param exam the exam instance
     * @return screen proctoring is enabled flag */
    default boolean isScreenProctoringEnabled(final Exam exam) {
        if (exam == null || exam.id == null) {
            return false;
        }

        if (exam.additionalAttributesIncluded()) {
            return BooleanUtils.toBoolean(
                    exam.getAdditionalAttribute(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING));
        }

        return isScreenProctoringEnabled(exam.id).getOr(false);
    }

    /** Updates needed additional attributes from assigned exam configuration for the exam
     *
     * @param examId The exam identifier */
    void updateAdditionalExamConfigAttributes(final Long examId);

    /** This indicates if proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam identifier
     * @return Result refer to proctoring is enabled flag or to an error when happened. */
    Result<Boolean> isProctoringEnabled(final Long examId);

    /** This indicates if screen proctoring is set and enabled for a certain exam.
     *
     * @param examId the exam identifier
     * @return Result refer to screen proctoring is enabled flag or to an error when happened. */
    Result<Boolean> isScreenProctoringEnabled(final Long examId);

    /** Get the exam proctoring service implementation for specified exam.
     *
     * @param examId the exam identifier
     * @return ExamProctoringService instance */
    Result<RemoteProctoringService> getExamProctoringService(final Long examId);

    /** This resets the proctoring settings for a given exam and stores the default settings.
     *
     * @param exam The exam reference
     * @return Result refer to the given exam or to an error when happened */
    Result<Exam> resetProctoringSettings(Exam exam);

    /** This archives a finished exam and set it to archived state as well as the assigned
     * exam configurations that are also set to archived state.
     *
     * @param exam The exam to archive
     * @return Result refer to the archived exam or to an error when happened */
    Result<Exam> archiveExam(Exam exam);

    /** Gets invoked after an exam has been changed and saved.
     *
     * @param exam the exam that has been changed and saved */
    Result<Exam> notifyExamSaved(Exam exam);

    Result<Exam>  applyQuitPassword(Exam exam);

    Result<Exam> findExamByLmsIdentity(String courseId, String quizId, String identity);

}
