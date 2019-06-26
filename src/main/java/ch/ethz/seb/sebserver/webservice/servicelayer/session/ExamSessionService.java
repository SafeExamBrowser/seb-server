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
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamSessionService {

    boolean isExamRunning(Long examId);

    Result<Exam> getRunningExam(Long examId);

    Result<Collection<Exam>> getRunningExamsForInstitution(Long institutionId);

    void notifyPing(Long connectionId, long timestamp, int pingNumber);

    void notifyClientEvent(final ClientEvent event, Long connectionId);

}
