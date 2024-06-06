/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


public interface LmsAPITemplateCacheService {

    /** Get a LmsAPITemplate for specified LmsSetup configuration by model identifier.
     * Get it from cache if available of not, create new one and put it in cache if okay.
     *
     * @param lmsSetupId the identifier of LmsSetup
     * @return LmsAPITemplate for specified LmsSetup configuration */
    Result<LmsAPITemplate> getLmsAPITemplate(Long lmsSetupId);

    /** Get a LmsAPITemplate for specified LmsSetup configuration by model identifier.
     * Get it from cache if available of not, create new one and put it in cache if okay.
     *
     * @param lmsSetupId the identifier of LmsSetup
     * @return LmsAPITemplate for specified LmsSetup configuration */
    Result<LmsAPITemplate> getLmsAPITemplate(String lmsSetupId);

    Result<LmsAPITemplate> createInMemoryLmsAPITemplate(LmsSetup lmsSetup);

    void clearCache(String lmsSetupId);

    /** Reset and cleanup the caches if there are some */
    void cleanup();
}
