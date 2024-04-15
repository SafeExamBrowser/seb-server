/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.mockup;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;

public class MockupFullIntegration implements FullLmsIntegrationAPI {


    @Override
    public Result<Void> applyConnectionDetails(FullLmsIntegrationService.IntegrationData data) {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> deleteConnectionDetails() {
        return Result.ofRuntimeError("TODO");
    }
}
