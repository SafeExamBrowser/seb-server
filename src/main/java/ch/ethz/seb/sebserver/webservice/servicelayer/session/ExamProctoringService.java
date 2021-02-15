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

    /** Get the proctoring server type of the specific implementation
     *
     * @return the proctoring service type of the specific implementation */
    ProctoringServerType getType();

    /** Use this to test the proctoring service settings against the remote proctoring server.
     *
     * @param examProctoring the settings to test
     * @return Result refer to true if the settings are correct and the proctoring server can be accessed. */
    Result<Boolean> testExamProctoring(final ProctoringSettings examProctoring);

    /** Used to get the proctor's room connection data.
     *
     * @param proctoringSettings the proctoring settings
     * @param roomName the name of the room
     * @param subject name of the room
     * @return SEBProctoringConnectionData that contains all connection data */
    Result<SEBProctoringConnectionData> createProctorPublicRoomConnection(
            final ProctoringSettings proctoringSettings,
            final String roomName,
            final String subject);

    Result<SEBProctoringConnectionData> getClientExamCollectingRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final String connectionToken);

    Result<SEBProctoringConnectionData> getClientExamCollectingRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final ClientConnection connection);

    Result<SEBProctoringConnectionData> getClientExamCollectingRoomConnectionData(
            final ProctoringSettings proctoringSettings,
            final String connectionToken,
            final String roomName,
            final String subject);

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
            final Long expTime,
            final boolean moderator);

    Result<String> createClientAccessToken(
            final ProctoringSettings proctoringSettings,
            final String connectionToken,
            final String roomName);

}
