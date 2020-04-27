/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.webservice.WebserviceInfo;

@Lazy
@Component
public class SEBServerInit {

    public static final Logger INIT_LOGGER = LoggerFactory.getLogger("ch.ethz.seb.SEB_SERVER_INIT");

    private final Environment environment;
    private final WebserviceInfo webserviceInfo;

    private boolean initialized = false;

    protected SEBServerInit(final Environment environment, final WebserviceInfo webserviceInfo) {
        this.environment = environment;
        this.webserviceInfo = webserviceInfo;
    }

    public void init() {

        if (!this.initialized) {
            INIT_LOGGER.info("---->   ___  ___  ___   ___                          ");
            INIT_LOGGER.info("---->  / __|| __|| _ ) / __| ___  _ _ __ __ ___  _ _ ");
            INIT_LOGGER.info("---->  \\__ \\| _| | _ \\ \\__ \\/ -_)| '_|\\ V // -_)| '_|");
            INIT_LOGGER.info("---->  |___/|___||___/ |___/\\___||_|   \\_/ \\___||_|  ");
            INIT_LOGGER.info("---->");
            INIT_LOGGER.info("---->");
            INIT_LOGGER.info("----> Version: {}", this.webserviceInfo.getSebServerVersion());
            INIT_LOGGER.info("---->");
            INIT_LOGGER.info("----> Active profiles: {}", Arrays.toString(this.environment.getActiveProfiles()));
            INIT_LOGGER.info("---->");

            this.initialized = true;
        }
    }

}
