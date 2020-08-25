/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBClientProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamProctoringService {

    ServerType getType();

    Result<Boolean> testExamProctoring(final ProctoringSettings examProctoring);

    public Result<SEBClientProctoringConnectionData> createProctoringConnectionData(
            final ProctoringSettings examProctoring,
            final String connectionToken,
            final boolean server);

    Result<SEBClientProctoringConnectionData> createProctoringConnectionData(
            final ProctoringSettings examProctoring,
            ClientConnection clientConnection,
            boolean server);

    Result<SEBClientProctoringConnectionData> createProcotringDataForRoom(
            final ProctoringSettings examProctoring,
            final String roomName,
            final boolean server);

}
