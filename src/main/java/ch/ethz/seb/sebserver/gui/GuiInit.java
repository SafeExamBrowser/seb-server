/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Component
@GuiProfile
public class GuiInit implements ApplicationListener<ApplicationReadyEvent> {

    static final Logger INIT_LOGGER = LoggerFactory.getLogger("SEB SERVER INIT");

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        INIT_LOGGER.info("---->   ___  ___  ___   ___                          ");
        INIT_LOGGER.info("---->  / __|| __|| _ ) / __| ___  _ _ __ __ ___  _ _ ");
        INIT_LOGGER.info("---->  \\__ \\| _| | _ \\ \\__ \\/ -_)| '_|\\ V // -_)| '_|");
        INIT_LOGGER.info("---->  |___/|___||___/ |___/\\___||_|   \\_/ \\___||_|  ");
        INIT_LOGGER.info("---->");
        INIT_LOGGER.info("---->  **** GUI Service ****");
        INIT_LOGGER.info("---->");

        INIT_LOGGER.info("---->  GUI Service sucessfully successfully started up!");
        INIT_LOGGER.info("---->");
    }

}
