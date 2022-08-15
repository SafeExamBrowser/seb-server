/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Lazy
@Service
@WebServiceProfile
public class MoodlePluginCheck {

    /** Used to check if the moodle SEB Server plugin is available for a given LMSSetup.
     *
     * @param lmsSetup The LMS Setup
     * @return true if the SEB Server plugin is available */
    public boolean checkPluginAvailable(final LmsSetup lmsSetup) {
        // TODO check if the moodle plugin is installed for the specified LMS Setup
        return false;
    }

}
