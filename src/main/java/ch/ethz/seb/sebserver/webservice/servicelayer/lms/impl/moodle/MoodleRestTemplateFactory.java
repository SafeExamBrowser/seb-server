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

/** Defines a MoodleAPIRestTemplate factory for moodle */
public interface MoodleRestTemplateFactory {

    /** Use this to test the LMSSetup input for MoodleAPIRestTemplate
     *
     * @return LmsSetupTestResult containing the result of the test */
    LmsSetupTestResult test();

    /** Get the LMSSetup data supplier for this factory.
     *
     * @return APITemplateDataSupplier */
    APITemplateDataSupplier getApiTemplateDataSupplier();

    /** Get all known, defined API access token request paths.
     *
     * @return Set of known and configured API access token paths */
    Set<String> getKnownTokenAccessPaths();

    /** Creates a MoodleAPIRestTemplate for the bundled LMSSetup of this factory.
     *
     * @param service The moodle web service name to within requesting an access token for
     * @return Result refer to the MoodleAPIRestTemplate or to an error when happened */
    Result<MoodleAPIRestTemplate> createRestTemplate(String service);

    /** Creates a MoodleAPIRestTemplate for the bundled LMSSetup of this factory.
     * Uses specified access token request path to request an access token.
     *
     * @param service The moodle web service name to within requesting an access token for
     * @param accessTokenPath access token request path to request an access token
     * @return Result refer to the MoodleAPIRestTemplate or to an error when happened */
    Result<MoodleAPIRestTemplate> createRestTemplate(String service, String accessTokenPath);

}
