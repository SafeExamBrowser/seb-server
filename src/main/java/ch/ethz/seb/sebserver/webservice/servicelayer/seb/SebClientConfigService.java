/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.seb;

import java.io.InputStream;

import ch.ethz.seb.sebserver.gbl.model.institution.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface SebClientConfigService {

    boolean hasSebClientConfigurationForIntitution(Long institutionId);

    Result<SebClientConfig> autoCreateSebClientConfigurationForIntitution(Long institutionId);

    Result<InputStream> exportSebClientConfigurationOfInstitution(Long institutionId);

    default Result<InputStream> exportSebClientConfiguration(final String modelId) {
        return Result.tryCatchOf(() -> {
            return exportSebClientConfigurationOfInstitution(Long.parseLong(modelId));
        });
    }

}
