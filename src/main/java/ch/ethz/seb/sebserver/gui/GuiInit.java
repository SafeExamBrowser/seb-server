/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Component
@GuiProfile
public class GuiInit implements ApplicationListener<ApplicationReadyEvent> {

    private final SEBServerInit sebServerInit;

    protected GuiInit(final SEBServerInit sebServerInit) {
        this.sebServerInit = sebServerInit;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {

        this.sebServerInit.init();

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("---->  **** GUI Service starting up... ****");

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("---->  GUI Service successfully successfully started up!");
        SEBServerInit.INIT_LOGGER.info("---->");
    }

}
