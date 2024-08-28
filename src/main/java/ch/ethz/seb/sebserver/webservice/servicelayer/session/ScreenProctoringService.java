/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.LmsSetupChangeEvent;
import org.springframework.context.event.EventListener;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import org.springframework.scheduling.annotation.Async;

public interface ScreenProctoringService extends SessionUpdateTask {

    @Override
    default int sessionUpdateTaskProcessingOrder() {
        return 2;
    }

    @Override
    default void processSessionUpdateTask() {
        updateClientConnections();
        updateActiveGroups();
    }

    boolean isScreenProctoringEnabled(Long examId);

    /** This is testing the given ScreenProctoringSettings on integrity and if we can
     * connect to the given SEB screen proctoring service.
     *
     * @param settings ScreenProctoringSettings
     * @return Result refer to the settings or to an error when happened */
    Result<ScreenProctoringSettings> testSettings(ScreenProctoringSettings settings);

    /** This applies the stored screen proctoring for the given exam.
     * If screen proctoring for the exam is enabled, this initializes or re-activate all
     * needed things for screen proctoring for the given exam.
     * If screen proctoring is set to disable, this disables the whole service for the
     * given exam also on SPS side.
     *
     * @param entityKey use the screen proctoring settings of the exam with the given exam id
     * @return Result refer to the given Exam or to an error when happened */
    Result<Exam> applyScreenProctoringForExam(EntityKey entityKey);

    /** Get list of all screen proctoring collecting groups for a particular exam.
     *
     * @param examId The exam identifier (PK)
     * @return Result refer to the list of ScreenProctoringGroup or to an error when happened */
    Result<Collection<ScreenProctoringGroup>> getCollectingGroups(Long examId);

    /** Gets invoked after an exam has been changed and saved.
     *
     * @param exam the exam that has been changed and saved */
    void notifyExamSaved(Exam exam);

    @EventListener(ExamStartedEvent.class)
    void notifyExamStarted(ExamStartedEvent event);

    @EventListener(ExamFinishedEvent.class)
    void notifyExamFinished(ExamFinishedEvent event);

    @EventListener(ExamResetEvent.class)
    void notifyExamReset(ExamResetEvent event);

    /** This is being called just before an Exam gets deleted on the permanent storage.
     * This deactivates and dispose or deletes all exam relevant domain entities on the SPS service side.
     *
     * @param event The ExamDeletionEvent reference all PKs of Exams that are going to be deleted. */
    @EventListener(ExamDeletionEvent.class)
    void notifyExamDeletion(final ExamDeletionEvent event);

    @EventListener
    void notifyLmsSetupChange(final LmsSetupChangeEvent event);

    /** This is used to update the exam equivalent on the screen proctoring service side
     * if screen proctoring is enabled for the specified exam.
     *
     * @param examId The SEB Server exam identifier
     * @return Result refer to the given exam data or to an error when happened */
    Result<Exam> updateExamOnScreenProctoringService(Long examId);

    /** This is internally used to update client connections that are active but has no groups assignment yet.
     * This attaches SEB client connections to proctoring group of an exam in one batch by checking for
     * each unassigned SEB client connection if it is ready to attach to a screen proctoring group
     * and if yes, attaching the respective SEB client connection, updating the group and sending
     * SPS connection instruction to SEB client to connect and start sending screenshots. */
    void updateClientConnections();

    /** This goes through all running exams with screen proctoring enabled and updates the group attributes
     *  (mainly the number of active clients in the group) by calling SPS API and store newest data. */
    void updateActiveGroups();

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void synchronizeSPSUser(final String userUUID);


    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void synchronizeSPSUserForExam(final Long examId);

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void deleteSPSUser(String userUUID);

}
