/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.ValidateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Component
@WebServiceProfile
public class SEBServerMigrationStrategy implements FlywayMigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(SEBServerMigrationStrategy.class);

    private final boolean cleanDBOnStartup;
    private final WebserviceInfo webserviceInfo;
    private final WebserviceInfoDAO webserviceInfoDAO;
    private Flyway flyway;
    private final boolean migrationApplied = false;

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
        this.flyway = flyway;

    }

    public void applyMigration() {
        if (this.webserviceInfo.hasProfile("test")) {
            SEBServerInit.INIT_LOGGER.info("No migration applies for test profile");
            return;
        }

        final String webserviceUUID = this.webserviceInfo.getWebserviceUUID();
        if (this.migrationApplied) {
            SEBServerInit.INIT_LOGGER.warn("Migration already applied for this webservice: {}", webserviceUUID);
            return;
        }

        try {

            SEBServerInit.INIT_LOGGER.info("----> ** Migration check START **");
            SEBServerInit.INIT_LOGGER.info("----> Check database status");

            final MigrationInfoService info = this.flyway.info();
            if (SEBServerInit.INIT_LOGGER.isDebugEnabled()) {
                SEBServerInit.INIT_LOGGER.debug("----> ** Migration Info **");
                SEBServerInit.INIT_LOGGER.debug("----> {}", info);
            }

            final MigrationInfo[] pendingMigrations = info.pending();
            if (pendingMigrations != null && pendingMigrations.length > 0) {

                SEBServerInit.INIT_LOGGER.info("----> Found pending migrations: {}", pendingMigrations.length);
                // If we are in a distributed setup only apply migration task if this is the master service
                // or if there was no data base initialization yet at all.
                if (this.webserviceInfo.isDistributed() && this.webserviceInfoDAO.isInitialized()) {

                    SEBServerInit.INIT_LOGGER.info("----> This is distributed setup, check master...");

                    if (this.webserviceInfoDAO.isInitialized()) {
                        final boolean isMaster = this.webserviceInfoDAO.isMaster(webserviceUUID);
                        if (!isMaster) {
                            SEBServerInit.INIT_LOGGER.info(
                                    "----> Skip migration task since this is not a master instance: {}",
                                    this.webserviceInfo.getWebserviceUUID());
                        } else {
                            doMigration();
                        }
                    }
                } else {
                    doMigration();
                }

            } else {
                SEBServerInit.INIT_LOGGER.info("----> ");
                SEBServerInit.INIT_LOGGER.info("----> No pending migrations found. Last migration --> {} --> {}",
                        info.current().getVersion(),
                        info.current().getDescription());
            }

            SEBServerInit.INIT_LOGGER.info("----> ** Migration check END **");
        } catch (final Exception e) {
            log.error("Failed to apply migration task: ", e);
        }
    }

    private void doMigration() {

        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("----> **** Start Migration ************************************");

        if (this.cleanDBOnStartup) {

            SEBServerInit.INIT_LOGGER
                    .info("----> !!! Cleanup database as it was set on sebserver.webservice.clean-db-on-startup !!!");

            this.flyway.clean();
        }

        // repair checksum mismatch if needed
        final ValidateResult validateWithResult = this.flyway.validateWithResult();
        if (!validateWithResult.validationSuccessful
                && validateWithResult.getAllErrorMessages().contains("checksum mismatch")) {

            SEBServerInit.INIT_LOGGER.info("----> Migration validation checksum mismatch error detected: ");
            SEBServerInit.INIT_LOGGER.info("----> {}", validateWithResult.getAllErrorMessages());
            SEBServerInit.INIT_LOGGER.info("----> Try to run repair task...");

            this.flyway.repair();

        }

        this.flyway.migrate();

        final MigrationInfoService info = this.flyway.info();
        SEBServerInit.INIT_LOGGER.info("----> Migration finished, new current version is: {} --> {}",
                info.current().getVersion(),
                info.current().getDescription());

        SEBServerInit.INIT_LOGGER.info("----> **** End Migration **************************************");
        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
    }

}
