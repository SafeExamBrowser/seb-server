/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PreDestroy;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Component
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class WebserviceInit implements ApplicationListener<ApplicationReadyEvent> {

    private final SEBServerInit sebServerInit;
    private final Environment environment;
    private final WebserviceInfo webserviceInfo;
    private final AdminUserInitializer adminUserInitializer;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final WebserviceInfoDAO webserviceInfoDAO;
    private final DBIntegrityChecker dbIntegrityChecker;

    protected WebserviceInit(
            final SEBServerInit sebServerInit,
            final Environment environment,
            final WebserviceInfo webserviceInfo,
            final AdminUserInitializer adminUserInitializer,
            final ApplicationEventPublisher applicationEventPublisher,
            final WebserviceInfoDAO webserviceInfoDAO,
            final DBIntegrityChecker dbIntegrityChecker) {

        this.sebServerInit = sebServerInit;
        this.environment = environment;
        this.webserviceInfo = webserviceInfo;
        this.adminUserInitializer = adminUserInitializer;
        this.applicationEventPublisher = applicationEventPublisher;
        this.webserviceInfoDAO = webserviceInfoDAO;
        this.dbIntegrityChecker = dbIntegrityChecker;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {

        this.sebServerInit.init();

        SEBServerInit.INIT_LOGGER.info("---->  **** Webservice starting up... ****");

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Register Webservice: {}", this.webserviceInfo.getWebserviceUUID());

        try {
            this.webserviceInfoDAO.register(
                    this.webserviceInfo.getWebserviceUUID(),
                    InetAddress.getLocalHost().getHostAddress());
        } catch (final Exception e) {
            SEBServerInit.INIT_LOGGER.error("Failed to register webservice: ", e);
        }

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> Initialize Services...");
        SEBServerInit.INIT_LOGGER.info("----> ");

        this.applicationEventPublisher.publishEvent(new SEBServerInitEvent(this));

        SEBServerInit.INIT_LOGGER.info("----> ");
        SEBServerInit.INIT_LOGGER.info("----> **** Webservice successfully started up! **** ");
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> *** Info:");

        SEBServerInit.INIT_LOGGER.info("----> JDBC connection pool max size: {}",
                this.environment.getProperty("spring.datasource.hikari.maximumPoolSize"));

        if (this.webserviceInfo.isDistributed()) {
            SEBServerInit.INIT_LOGGER.info("----> Distributed Setup: {}", this.webserviceInfo.getWebserviceUUID());
            SEBServerInit.INIT_LOGGER.info("------> Ping update time: {}",
                    this.environment.getProperty("sebserver.webservice.distributed.pingUpdate"));
            SEBServerInit.INIT_LOGGER.info("------> Connection update time: {}",
                    this.environment.getProperty("sebserver.webservice.distributed.connectionUpdate", "2000"));
        }

        try {
            SEBServerInit.INIT_LOGGER.info("----> Server address: {}", this.environment.getProperty("server.address"));
            SEBServerInit.INIT_LOGGER.info("----> Server port: {}", this.environment.getProperty("server.port"));
            SEBServerInit.INIT_LOGGER.info("---->");
            SEBServerInit.INIT_LOGGER.info("----> Local-Host address: {}", InetAddress.getLocalHost().getHostAddress());
            SEBServerInit.INIT_LOGGER.info("----> Local-Host name: {}", InetAddress.getLocalHost().getHostName());
            SEBServerInit.INIT_LOGGER.info("---->");
            SEBServerInit.INIT_LOGGER.info("----> Remote-Host address: {}",
                    InetAddress.getLoopbackAddress().getHostAddress());
            SEBServerInit.INIT_LOGGER.info("----> Remote-Host name: {}",
                    InetAddress.getLoopbackAddress().getHostName());
        } catch (final UnknownHostException e) {
            SEBServerInit.INIT_LOGGER.error("Unknown Host: ", e);
        }

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Context Path: {}", this.webserviceInfo.getContextPath());
        SEBServerInit.INIT_LOGGER.info("----> External-Host URL: {}", this.webserviceInfo.getExternalServerURL());
        SEBServerInit.INIT_LOGGER.info("----> LMS-External-Address-Alias: {}",
                this.webserviceInfo.getLmsExternalAddressAlias());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> HTTP Scheme {}", this.webserviceInfo.getHttpScheme());
        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Property Override Test: {}", this.webserviceInfo.getTestProperty());

        // Run the data base integrity checks and fixes if configured
        this.dbIntegrityChecker.checkIntegrity();

        // Create an initial admin account if requested and not already in the data-base
        this.adminUserInitializer.initAdminAccount();

        SEBServerInit.INIT_LOGGER.info("----> log4j2.formatMsgNoLookups = {}",
                this.environment.getProperty("log4j2.formatMsgNoLookups", "none"));

    }

    @PreDestroy
    public void gracefulShutdown() {
        SEBServerInit.INIT_LOGGER.info("**** Gracefully Shutdown of SEB Server instance {} ****",
                this.webserviceInfo.getHostAddress());

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Unregister Webservice: {}", this.webserviceInfo.getWebserviceUUID());

        this.webserviceInfoDAO.unregister(this.webserviceInfo.getWebserviceUUID());

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Webservice down");
    }

}
