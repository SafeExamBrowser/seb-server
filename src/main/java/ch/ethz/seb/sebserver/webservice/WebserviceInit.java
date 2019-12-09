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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Component
@WebServiceProfile
@Import(DataSourceAutoConfiguration.class)
public class WebserviceInit implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(WebserviceInit.class);

    static final Logger INIT_LOGGER = LoggerFactory.getLogger("SEB SERVER INIT");

    private final Environment environment;
    private final WebserviceInfo webserviceInfo;
    private final AdminUserInitializer adminUserInitializer;

    protected WebserviceInit(
            final Environment environment,
            final WebserviceInfo webserviceInfo,
            final AdminUserInitializer adminUserInitializer) {

        this.environment = environment;
        this.webserviceInfo = webserviceInfo;
        this.adminUserInitializer = adminUserInitializer;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        log.info("Initialize SEB-Server Web-Service Component");

        INIT_LOGGER.info("---->   ___  ___  ___   ___                          ");
        INIT_LOGGER.info("---->  / __|| __|| _ ) / __| ___  _ _ __ __ ___  _ _ ");
        INIT_LOGGER.info("---->  \\__ \\| _| | _ \\ \\__ \\/ -_)| '_|\\ V // -_)| '_|");
        INIT_LOGGER.info("---->  |___/|___||___/ |___/\\___||_|   \\_/ \\___||_|  ");
        INIT_LOGGER.info("---->");
        INIT_LOGGER.info("----> SEB Server successfully started up!");
        INIT_LOGGER.info("---->");
        INIT_LOGGER.info("----> Version: {}", this.webserviceInfo.getSebServerVersion());

        try {
            INIT_LOGGER.info("----> Server address: {}", this.environment.getProperty("server.address"));
            INIT_LOGGER.info("----> Server port: {}", this.environment.getProperty("server.port"));
            INIT_LOGGER.info("---->");
            INIT_LOGGER.info("----> Local-Host address: {}", InetAddress.getLocalHost().getHostAddress());
            INIT_LOGGER.info("----> Local-Host name: {}", InetAddress.getLocalHost().getHostName());
            INIT_LOGGER.info("---->");
            INIT_LOGGER.info("----> Remote-Host address: {}", InetAddress.getLoopbackAddress().getHostAddress());
            INIT_LOGGER.info("----> Remote-Host name: {}", InetAddress.getLoopbackAddress().getHostName());
        } catch (final UnknownHostException e) {
            log.error("Unknown Host: ", e);
        }

        INIT_LOGGER.info("---->");
        INIT_LOGGER.info("----> HTTP Scheme {}", this.webserviceInfo.getHttpScheme());
        INIT_LOGGER.info("----> Property Override Test: {}", this.webserviceInfo.getTestProperty());

        // TODO integration of Flyway for database initialization and migration:  https://flywaydb.org
        //      see also https://flywaydb.org/getstarted/firststeps/api

        // Create an initial admin account if requested and not already in the data-base
        this.adminUserInitializer.initAdminAccount();
    }

    @PreDestroy
    public void gracefulShutdown() {
        log.info("**** Gracefully Shutdown of SEB Server instance {} ****", this.webserviceInfo.getHostAddress());
    }

}
