/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamProctoringService {

    ProctoringServerType getType();

    Result<Boolean> testExamProctoring(final ProctoringSettings examProctoring);

    Result<SEBProctoringConnectionData> createProctorPublicRoomConnection(
            final ProctoringSettings proctoringSettings,
            final String roomName,
            final String subject);

    Result<SEBProctoringConnectionData> getClientExamCollectionRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final String connectionToken);

    Result<SEBProctoringConnectionData> getClientExamCollectionRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final ClientConnection connection);

    Result<SEBProctoringConnectionData> getClientExamCollectionRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final String connectionToken,
            final String roomName);

    Result<SEBProctoringConnectionData> getClientRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final String connectionToken);

    Result<SEBProctoringConnectionData> getClientRoomConnectionData(
            final ProctoringSettings examProctoring,
            final String connectionToken,
            final String roomName,
            final String subject);

    Result<SEBProctoringConnectionData> createProctoringConnectionData(
            final ProctoringServerType proctoringServerType,
            final String connectionToken,
            final String url,
            final String appKey,
            final CharSequence appSecret,
            final String clientName,
            final String clientKey,
            final String roomName,
            final String subject,
            final Long expTime);

    Result<String> createClientAccessToken(
            final ProctoringSettings proctoringSettings,
            final String connectionToken,
            final String roomName);

}
