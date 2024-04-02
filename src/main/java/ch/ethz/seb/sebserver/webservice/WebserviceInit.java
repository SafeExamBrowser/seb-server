/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import javax.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo.ScreenProctoringServiceBundle;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Component
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class WebserviceInit implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext applicationContext;
    private final SEBServerInit sebServerInit;
    private final Environment environment;
    private final WebserviceInfo webserviceInfo;
    private final AdminUserInitializer adminUserInitializer;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final WebserviceInfoDAO webserviceInfoDAO;
    private final DBIntegrityChecker dbIntegrityChecker;
    private final SEBServerMigrationStrategy sebServerMigrationStrategy;

    protected WebserviceInit(
            final SEBServerInit sebServerInit,
            final WebserviceInfo webserviceInfo,
            final AdminUserInitializer adminUserInitializer,
            final ApplicationEventPublisher applicationEventPublisher,
            final WebserviceInfoDAO webserviceInfoDAO,
            final DBIntegrityChecker dbIntegrityChecker,
            final ApplicationContext applicationContext,
            final SEBServerMigrationStrategy sebServerMigrationStrategy) {

        this.applicationContext = applicationContext;
        this.sebServerInit = sebServerInit;
        this.environment = applicationContext.getEnvironment();
        this.webserviceInfo = webserviceInfo;
        this.adminUserInitializer = adminUserInitializer;
        this.applicationEventPublisher = applicationEventPublisher;
        this.webserviceInfoDAO = webserviceInfoDAO;
        this.dbIntegrityChecker = dbIntegrityChecker;
        this.sebServerMigrationStrategy = sebServerMigrationStrategy;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {

        this.sebServerInit.init();

        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("----> *** Webservice starting up...                         ***");
        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Register Webservice: {}", this.webserviceInfo.getWebserviceUUID());

        try {
            if (this.webserviceInfoDAO.isInitialized()) {
                this.registerWebservice();

                // Apply migration if needed and possible
                SEBServerInit.INIT_LOGGER.info("----> ");
                this.sebServerMigrationStrategy.applyMigration();
                SEBServerInit.INIT_LOGGER.info("----> ");

            } else {

                // Apply migration if needed and possible
                SEBServerInit.INIT_LOGGER.info("----> ");
                this.sebServerMigrationStrategy.applyMigration();
                SEBServerInit.INIT_LOGGER.info("----> ");

                this.registerWebservice();
            }
        } catch (final Exception e) {
            SEBServerInit.INIT_LOGGER.error("Failed to apply data import and migration --> ", e);
        }

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Initialize Services...");
        SEBServerInit.INIT_LOGGER.info("----> ");

        // Run the database integrity checks and fixes if configured
        this.dbIntegrityChecker.checkIntegrity();

        // Create an initial admin account if requested and not already in the database
        this.adminUserInitializer.initAdminAccount();

        //emits SEBServerInitEvent
        this.applicationEventPublisher.publishEvent(new SEBServerInitEvent(this));


        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("----> *** Webservice Info:                                  ***");
        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("---->");

        SEBServerInit.INIT_LOGGER.info("----> JDBC connection pool max size: {}",
                this.environment.getProperty("spring.datasource.hikari.maximumPoolSize"));



        if (this.webserviceInfo.isDistributed()) {
            if (this.webserviceInfo.isLightSetup()) {
                throw new IllegalStateException("Illegal invalid setup configuration detected, SEB Serer light and distributed setup cannot be applied within the same setup.");
            }
            SEBServerInit.INIT_LOGGER.info("----> ");
            SEBServerInit.INIT_LOGGER.info("----> Distributed Setup: {}", this.webserviceInfo.getWebserviceUUID());
            SEBServerInit.INIT_LOGGER.info("----> Connection update time: {}",
                    this.environment.getProperty("sebserver.webservice.distributed.connectionUpdate", "2000"));
        } else if (this.webserviceInfo.isLightSetup()) {
            SEBServerInit.INIT_LOGGER.info("----> ");
            SEBServerInit.INIT_LOGGER.info("----> SEB Server light setup enabled!");
        }

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Configured Features:");
        this.webserviceInfo.configuredFeatures().entrySet().stream().forEach(entry  -> {
            SEBServerInit.INIT_LOGGER.info("---->   {} --> {}", entry.getKey(), entry.getValue());
        });

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Working with ping service: {}",
                this.environment.getProperty("sebserver.webservice.ping.service.strategy"));

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Server address: {}", this.environment.getProperty("server.address"));
        SEBServerInit.INIT_LOGGER.info("----> Server port: {}", this.environment.getProperty("server.port"));
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Local-Host address: {}", this.webserviceInfo.getLocalHostAddress());
        SEBServerInit.INIT_LOGGER.info("----> Local-Host name: {}", this.webserviceInfo.getLocalHostName());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Remote-Host address: {}", this.webserviceInfo.getLoopbackHostAddress());
        SEBServerInit.INIT_LOGGER.info("----> Remote-Host name: {}", this.webserviceInfo.getLoopbackHostName());

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Context Path: {}", this.webserviceInfo.getContextPath());
        SEBServerInit.INIT_LOGGER.info("----> External-Host URL: {}", this.webserviceInfo.getExternalServerURL());
        SEBServerInit.INIT_LOGGER.info("----> LMS-External-Address-Alias: {}",
                this.webserviceInfo.getLmsExternalAddressAlias());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> HTTP Scheme {}", this.webserviceInfo.getHttpScheme());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Access-Tokens:");
        SEBServerInit.INIT_LOGGER.info(
                "----> admin API access token validity: " + this.webserviceInfo.getAdminAccessTokenValSec() + "s");
        SEBServerInit.INIT_LOGGER.info(
                "----> admin API refresh token validity: " + this.webserviceInfo.getAdminRefreshTokenValSec() + "s");
        SEBServerInit.INIT_LOGGER.info(
                "----> exam API access token validity: " + this.webserviceInfo.getExamAPITokenValiditySeconds() + "s");

        final ScreenProctoringServiceBundle spsBundle = this.webserviceInfo.getScreenProctoringServiceBundle();
        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Screen Proctoring Bundle enabled: {}", spsBundle.bundled);
        if (spsBundle.bundled) {
            SEBServerInit.INIT_LOGGER.info("------> {}", spsBundle);
        }

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Property Override Test: {}", this.webserviceInfo.getTestProperty());

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
        SEBServerInit.INIT_LOGGER.info("----> *** Webservice successfully started up!               ***");
        SEBServerInit.INIT_LOGGER.info("----> *********************************************************");
    }

    private boolean registerWebservice() {
        boolean registered = false;
        try {
            final String webserviceUUID = this.webserviceInfo.getWebserviceUUID();
            final String hostAddress = this.webserviceInfo.getLocalHostAddress();
            registered = this.webserviceInfoDAO.register(webserviceUUID, hostAddress);
            if (registered) {
                SEBServerInit.INIT_LOGGER.info("----> Successfully register Webservice instance. uuid: {}, address: {}",
                        webserviceUUID, hostAddress);
            }
        } catch (final Exception e) {
            SEBServerInit.INIT_LOGGER.error("----> Failed to register webservice: ", e);
        }
        return registered;
    }

    @PreDestroy
    public void gracefulShutdown() {
        SEBServerInit.INIT_LOGGER.info("*********************************************************");
        SEBServerInit.INIT_LOGGER.info("**** Gracefully Shutdown of SEB Server instance {}",
                this.webserviceInfo.getHostAddress());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Unregister Webservice: {}", this.webserviceInfo.getWebserviceUUID());

        this.webserviceInfoDAO.unregister(this.webserviceInfo.getWebserviceUUID());

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Webservice down");
    }

}
