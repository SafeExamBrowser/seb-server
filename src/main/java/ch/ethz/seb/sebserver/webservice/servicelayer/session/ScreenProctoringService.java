/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import org.springframework.context.event.EventListener;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;

public interface ScreenProctoringService extends SessionUpdateTask {

    @Override
    default int sessionUpdateTaskProcessingOrder() {
        return 2;
    }

    @Override
    default void processSessionUpdateTask() {
        updateClientConnections();
    }

    /** This is testing the given ScreenProctoringSettings on integrity and if we can
     * connect to the given SEB screen proctoring service.
     *
     * @param settings ScreenProctoringSettings
     * @return Result refer to the settings or to an error when happened */
    Result<ScreenProctoringSettings> testSettings(ScreenProctoringSettings settings);

    //Result<ScreenProctoringSettings> saveSettingsForExam(ScreenProctoringSettings settings);

    /** This applies the screen proctoring for the given exam.
     * If screen proctoring for the exam is enabled, this initializes or re-activate all
     * needed things for screen proctoring for the given exam.
     * If screen proctoring is set to disable, this disables the whole service for the
     * given exam also on SPS side.
     *
     * @param settings the actual ScreenProctoringSettings of the exam
     * @return Result refer to the given ScreenProctoringSettings or to an error when happened */
    Result<ScreenProctoringSettings> applyScreenProctoingForExam(ScreenProctoringSettings settings);

    /** This is been called just before an Exam gets deleted on the permanent storage.
     * This deactivates and dispose or deletes all exam relevant domain entities on the SPS service side.
     *
     * @param event The ExamDeletionEvent reference all PKs of Exams that are going to be deleted. */
    @EventListener(ExamDeletionEvent.class)
    void notifyExamDeletion(final ExamDeletionEvent event);

    /** This is used to update the exam equivalent on the screen proctoring service side
     * if screen proctoring is enabled for the specified exam.
     *
     * @param examId The SEB Server exam identifier
     * @return Result refer to the the given exam data or to an error when happened */
    Result<Exam> updateExamOnScreenProctoingService(Long examId);

    /** This is internally used to update client connections that are active but has no groups assignment yet.
     * This attaches SEB client connections to proctoring group of an exam in one batch by checking for
     * each unassigned SEB client connection if it is ready to attach to a screen proctoring group
     * and if yes, attaching the respective SEB client connection, updating the group and sending
     * SPS connection instruction to SEB client to connect and start sending screenshots. */
    void updateClientConnections();

}
