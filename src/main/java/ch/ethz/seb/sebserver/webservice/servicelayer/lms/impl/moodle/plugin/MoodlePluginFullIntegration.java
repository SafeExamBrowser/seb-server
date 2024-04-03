/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;

public class MoodlePluginFullIntegration implements FullLmsIntegrationAPI {

    private final JSONMapper jsonMapper;
    private final MoodleRestTemplateFactory restTemplateFactory;

    public MoodlePluginFullIntegration(
            final JSONMapper jsonMapper,
            final MoodleRestTemplateFactory restTemplateFactory) {

        this.jsonMapper = jsonMapper;
        this.restTemplateFactory = restTemplateFactory;
    }

    @Override
    public Result<Void> createConnectionDetails() {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> updateConnectionDetails() {
        return Result.ofRuntimeError("TODO");
    }

    @Override
    public Result<Void> deleteConnectionDetails() {
        return Result.ofRuntimeError("TODO");
    }
}
