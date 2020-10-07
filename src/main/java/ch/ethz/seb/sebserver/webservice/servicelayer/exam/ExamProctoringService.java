/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamProctoringService {

    ProctoringServerType getType();

    Result<Boolean> testExamProctoring(final ProctoringSettings examProctoring);

    Result<SEBProctoringConnectionData> createProctorPrivateRoomConnection(
            final ProctoringSettings examProctoring,
            final String connectionToken);

    Result<SEBProctoringConnectionData> createProctorPublicRoomConnection(
            final ProctoringSettings examProctoring,
            final String roomName);

    Result<SEBProctoringConnectionData> createClientPrivateRoomConnection(
            final ProctoringSettings examProctoring,
            final String connectionToken);

    Result<SEBProctoringConnectionData> createClientPublicRoomConnection(
            final ProctoringSettings examProctoring,
            final String connectionToken,
            final String roomName,
            final String subject);

}
