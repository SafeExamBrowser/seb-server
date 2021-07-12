/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Component
@WebServiceProfile
public class SEBServerMigrationStrategy implements FlywayMigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(SEBServerMigrationStrategy.class);

    private final boolean cleanDBOnStartup;
    private final WebserviceInfo webserviceInfo;
    private final WebserviceInfoDAO webserviceInfoDAO;

    public SEBServerMigrationStrategy(
            final WebserviceInfo webserviceInfo,
            final WebserviceInfoDAO webserviceInfoDAO,
            @Value("${sebserver.webservice.clean-db-on-startup:false}") final boolean cleanDBOnStartup) {

        this.webserviceInfo = webserviceInfo;
        this.webserviceInfoDAO = webserviceInfoDAO;
        this.cleanDBOnStartup = cleanDBOnStartup;
    }

    @Override
    public void migrate(final Flyway flyway) {
        try {

            // If we are in a distributed setup only apply migration task if this is the master service
            // or if there was no data base initialization yet at all.
            if (this.webserviceInfo.isDistributed()) {
                if (this.webserviceInfoDAO.isInitialized()) {
                    final boolean isMaster = this.webserviceInfoDAO.isMaster(this.webserviceInfo.getWebserviceUUID());
                    if (!isMaster) {
                        log.info(
                                "Skip migration task since this is not a master instance: {}",
                                this.webserviceInfo.getWebserviceUUID());

                        return;
                    }
                }
            }

            if (this.cleanDBOnStartup) {
                flyway.clean();
            }
            flyway.migrate();

        } catch (final Exception e) {
            log.error("Failed to apply migration task: ", e);
        }
    }

}
