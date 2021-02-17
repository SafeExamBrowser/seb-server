/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBProctoringConnection;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

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

    Result<SEBProctoringConnection> getProctorRoomConnection(
            ProctoringSettings proctoringSettings,
            String roomName,
            String subject);

    Result<SEBProctoringConnection> sendJoinRoomToClients(
            ProctoringSettings proctoringSettings,
            Collection<String> clientConnectionTokens,
            String roomName,
            String subject);

    Result<Void> sendJoinCollectingRoomToClients(
            ProctoringSettings proctoringSettings,
            Collection<String> clientConnectionTokens);

    default String verifyRoomName(final String requestedRoomName, final String connectionToken) {
        if (StringUtils.isNotBlank(requestedRoomName)) {
            return requestedRoomName;
        }

        final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        return urlEncoder.encodeToString(
                Utils.toByteArray(connectionToken));
    }

//    /** Used to get the proctor's room connection data.
//     *
//     * @param proctoringSettings the proctoring settings
//     * @param roomName the name of the room
//     * @param subject name of the room
//     * @return SEBProctoringConnectionData that contains all connection data */
//    Result<SEBProctoringConnection> createProctorPublicRoomConnection(
//            final ProctoringSettings proctoringSettings,
//            final String roomName,
//            final String subject);
//
//    Result<SEBProctoringConnection> getClientExamCollectingRoomConnection(
//            final ProctoringSettings proctoringSettings,
//            final ClientConnection connection);
//
//    Result<SEBProctoringConnection> getClientExamCollectingRoomConnection(
//            final ProctoringSettings proctoringSettings,
//            final String connectionToken,
//            final String roomName,
//            final String subject);
//
//    Result<SEBProctoringConnection> getClientRoomConnection(
//            final ProctoringSettings examProctoring,
//            final String connectionToken,
//            final String roomName,
//            final String subject);
//
//    Result<SEBProctoringConnection> createProctoringConnection(
//            final ProctoringServerType proctoringServerType,
//            final String connectionToken,
//            final String url,
//            final String appKey,
//            final CharSequence appSecret,
//            final String clientName,
//            final String clientKey,
//            final String roomName,
//            final String subject,
//            final Long expTime,
//            final boolean moderator);

}
