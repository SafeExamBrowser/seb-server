/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import java.util.Collection;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringRoomConnection;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;

@Lazy
@Service
@WebServiceProfile
public class ExamZOOMProctoringService implements ExamProctoringService {

    @Override
    public ProctoringServerType getType() {
        return ProctoringServerType.ZOOM;
    }

    @Override
    public Result<Boolean> testExamProctoring(final ProctoringServiceSettings examProctoring) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ProctoringRoomConnection> getProctorRoomConnection(
            final ProctoringServiceSettings proctoringSettings,
            final String roomName,
            final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ProctoringRoomConnection> sendJoinRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens,
            final String roomName, final String subject) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void> sendJoinCollectingRoomToClients(
            final ProctoringServiceSettings proctoringSettings,
            final Collection<String> clientConnectionTokens) {

        // TODO Auto-generated method stub
        return null;
    }

}
