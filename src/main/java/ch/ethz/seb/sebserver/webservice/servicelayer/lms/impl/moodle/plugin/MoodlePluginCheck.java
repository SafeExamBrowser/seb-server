/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleRestTemplateFactory;

@Lazy
@Service
@WebServiceProfile
public class MoodlePluginCheck {

    private static final Logger log = LoggerFactory.getLogger(MoodlePluginCheck.class);

    /** Used to check if the moodle SEB Server plugin is available for a given LMSSetup.
     *
     * @param lmsSetup The LMS Setup
     * @return true if the SEB Server plugin is available */
    public boolean checkPluginAvailable(final MoodleRestTemplateFactory restTemplateFactory) {
        try {

            log.info("Check Moodle SEB Server Plugin available...");

            final LmsSetupTestResult test = restTemplateFactory.test();

            if (!test.isOk()) {
                log.warn("Failed to check Moodle SEB Server Plugin because of invalid LMS Setup: ", test);
                return false;
            }

            final MoodleAPIRestTemplate restTemplate = restTemplateFactory
                    .createRestTemplate(MooldePluginLmsAPITemplateFactory.SEB_SERVER_SERVICE_NAME)
                    .getOrThrow();

            try {
                restTemplate.testAPIConnection(
                        MoodlePluginCourseAccess.COURSES_API_FUNCTION_NAME,
                        MoodlePluginCourseAccess.USERS_API_FUNCTION_NAME);
            } catch (final Exception e) {
                log.info("Moodle SEB Server Plugin not available: {}", e.getMessage());
                return false;
            }

            log.info("Moodle SEB Server Plugin not available for: {}",
                    restTemplateFactory.getApiTemplateDataSupplier().getLmsSetup());

            return true;

        } catch (final Exception e) {
            log.error("Failed to check Moodle SEB Server Plugin because of unexpected error: ", e);
            return false;
        }
    }

}
