/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;

public interface MoodleRestTemplateFactory {

    LmsSetupTestResult test();

    APITemplateDataSupplier getApiTemplateDataSupplier();

    Set<String> getKnownTokenAccessPaths();

    Result<MoodleAPIRestTemplate> createRestTemplate();

    Result<MoodleAPIRestTemplate> createRestTemplate(final String accessTokenPath);

}
