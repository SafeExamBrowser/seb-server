/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface LmsAPIService {

    Result<LmsAPITemplate> createConnectionTemplate(Long lmsSetupId);

    Result<LmsAPITemplate> createConnectionTemplate(LmsSetup lmsSetup);

}
