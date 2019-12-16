/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.SebInstruction;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SebInstructionService;

@Lazy
@Service
@WebServiceProfile
public class SebInstructionServiceImpl implements SebInstructionService {

    public SebInstructionServiceImpl() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public SebInstruction getInstruction(final String connectionToken) {
        // TODO Auto-generated method stub
        return null;
    }

}
