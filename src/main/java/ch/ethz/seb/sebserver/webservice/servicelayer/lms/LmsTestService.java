/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;

public interface LmsTestService {

    /** use this to the specified LmsAPITemplate.
     *
     * @param template the LmsAPITemplate
     * @return LmsSetupTestResult containing list of errors if happened */
    LmsSetupTestResult test(LmsAPITemplate template);

    /** This can be used to test an LmsSetup connection parameter without saving or heaving
     * an already persistent version of an LmsSetup.
     *
     * @param lmsSetup the LmsSetup instance
     * @return LmsSetupTestResult containing list of errors if happened */
    LmsSetupTestResult testAdHoc(LmsSetup lmsSetup);
}
