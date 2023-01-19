/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.RemoteProctoringRoom;
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

    /** Get specified proctoring room connection data for a given connected SEB client
     *
     * @param proctoringSettings The proctoring service settings of the exam where the client belogs to
     * @param connectionToken The connection token of the SEB client connection (identification)
     * @param roomName The name of the room to connect to
     * @param subject The room subject (display name of the room when the client enter the room)
     * @return Result refer to the proctoring room connection data or to an error when happened */
    Result<ProctoringRoomConnection> getClientRoomConnection(
            ProctoringServiceSettings proctoringSettings,
            String connectionToken,
            String roomName,
            String subject);

    /** Used to create join-instruction attribute for joining a given room
     * This attributes are added to the join-instruction that is sent to the SEB client
     *
     * @param proctoringConnection the proctoring room connection data of the room to join
     * @return Map containing additional join-instruction attributes that are added to the join-instruction */
    Map<String, String> createJoinInstructionAttributes(ProctoringRoomConnection proctoringConnection);

    /** Dispose or delete all rooms or meetings or other data on the proctoring service side for
     * a given exam.
     *
     * @param examId The exam identifier
     * @param proctoringSettings The proctoring service settings
     * @return Result that is empty or refer to an error if happened */
    Result<Void> disposeServiceRoomsForExam(Long examId, ProctoringServiceSettings proctoringSettings);

    /** Creates a new collecting room on proctoring service side.
     *
     * @param proctoringSettings The proctoring service settings to connect to the service
     * @param roomNumber the collecting room number
     * @return Result refer to the new room or to an error when happened */
    Result<NewRoom> newCollectingRoom(ProctoringServiceSettings proctoringSettings, Long roomNumber);

    /** Create a new break-out room on service side.
     *
     * @param proctoringSettings The proctoring service settings to connect to the service
     * @param subject The subject of the new break-out room
     * @return Result refer to the new room or to an error when happened */
    Result<NewRoom> newBreakOutRoom(ProctoringServiceSettings proctoringSettings, String subject);

    /** Dispose or delete a given break-out room on proctoring service side.
     *
     * @param proctoringSettings The proctoring service settings to connect to the service
     * @param roomName the room name
     * @return Result refer to void or to an error when happened */
    Result<Void> disposeBreakOutRoom(ProctoringServiceSettings proctoringSettings, String roomName);

    /** Used to get the default SEB client proctoring reconfiguration instruction attributes for the specified
     * proctoring service.
     *
     * @return Map containing the default SEB client instruction attributes */
    Map<String, String> getDefaultReconfigInstructionAttributes();

    /** Used to map SEB client proctoring reconfiguration instruction attributes from SEB Server API name
     * to SEB key name.
     *
     * @param attributes Map containing the internal SEB Server API names of the attributes
     * @return Map containing the external SEB settings attribute names */
    Map<String, String> mapReconfigInstructionAttributes(Map<String, String> attributes);

    /** Gets called when a proctor opened a break-out room.
     * This can be used to do some post processing after a proctor opened a break-out room
     *
     * @param proctoringSettings The proctoring service settings to connect to the service
     * @param room The room data of the break-out room
     * @return Result refer to void or to an error when happened */
    Result<Void> notifyBreakOutRoomOpened(ProctoringServiceSettings proctoringSettings, RemoteProctoringRoom room);

    /** Gets called when a proctor opened a collecting room.
     * This can be used to do some post processing after a proctor opened a collecting room
     *
     * @param proctoringSettings The proctoring service settings to connect to the service
     * @param room The room data of the collecting room
     * @return Result refer to void or to an error when happened */
    Result<Void> notifyCollectingRoomOpened(
            ProctoringServiceSettings proctoringSettings,
            RemoteProctoringRoom room,
            Collection<ClientConnection> clientConnections);

    public void clearRestTemplateCache(final Long examId);

}
