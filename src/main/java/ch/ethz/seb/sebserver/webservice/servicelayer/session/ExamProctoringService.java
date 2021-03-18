/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.NewRoom;

public interface ExamProctoringService {

    /** Get the proctoring server type of the specific implementation
     *
     * @return the proctoring service type of the specific implementation */
    ProctoringServerType getType();

    /** Use this to test the proctoring service settings against the remote proctoring server.
     *
     * @param proctoringSettings the settings to test
     * @return Result refer to true if the settings are correct and the proctoring server can be accessed. */
    Result<Boolean> testExamProctoring(ProctoringServiceSettings proctoringSettings);

    /** Gets the room connection data for a certain room for the proctor.
     *
     * @param proctoringSettings the proctoring service settings
     * @param roomName the name of the room
     * @param subject the subject of the room
     * @return Result refer to the room connection data for the proctor to connect to specified room or to an error when
     *         happened */
    Result<ProctoringRoomConnection> getProctorRoomConnection(
            ProctoringServiceSettings proctoringSettings,
            String roomName,
            String subject);

    Result<ProctoringRoomConnection> getClientRoomConnection(
            ProctoringServiceSettings proctoringSettings,
            String connectionToken,
            String roomName,
            String subject);

    Map<String, String> createJoinInstructionAttributes(ProctoringRoomConnection proctoringConnection);

    Result<Void> disposeServiceRoomsForExam(ProctoringServiceSettings proctoringSettings, Exam exam);

    default String verifyRoomName(final String requestedRoomName, final String connectionToken) {
        if (StringUtils.isNotBlank(requestedRoomName)) {
            return requestedRoomName;
        }

        throw new RuntimeException("Test Why: " + connectionToken);
    }

    Result<NewRoom> newCollectingRoom(ProctoringServiceSettings proctoringSettings, Long roomNumber);

    Result<NewRoom> newBreakOutRoom(ProctoringServiceSettings proctoringSettings, String subject);

    Result<Void> disposeBreakOutRoom(ProctoringServiceSettings proctoringSettings, String roomName);

    Map<String, String> getDefaultInstructionAttributes();

    Map<String, String> getInstructionAttributes(Map<String, String> attributes);

}
