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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Component
@GuiProfile
public class GuiInit implements ApplicationListener<ApplicationReadyEvent> {

    private final SEBServerInit sebServerInit;
    private final Environment environment;

    protected GuiInit(
            final SEBServerInit sebServerInit,
            final Environment environment) {

        this.sebServerInit = sebServerInit;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {

        this.sebServerInit.init();

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("---->  **** GUI Service starting up... ****");

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("---->  GUI Service successfully successfully started up!");
        SEBServerInit.INIT_LOGGER.info("---->");

        final String webServiceProtocol = this.environment.getProperty("sebserver.gui.webservice.protocol", "http");
        final String webServiceAddress = this.environment.getRequiredProperty("sebserver.gui.webservice.address");
        final String webServicePort = this.environment.getProperty("sebserver.gui.webservice.port", "80");

        SEBServerInit.INIT_LOGGER.info("----> Webservice connection: " + webServiceProtocol + "://" + webServiceAddress
                + ":" + webServicePort);

        final String webServiceAdminAPIEndpoint =
                this.environment.getRequiredProperty("sebserver.gui.webservice.apipath");
        final String webServiceExamAPIEndpoint =
                this.environment.getRequiredProperty("sebserver.webservice.api.exam.endpoint");

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Webservice admin API endpoint: " + webServiceAdminAPIEndpoint);
        SEBServerInit.INIT_LOGGER.info("----> Webservice exam API endpoint: " + webServiceExamAPIEndpoint);

        final String webServiceAPIBasicAccess =
                this.environment.getRequiredProperty("sebserver.webservice.api.admin.clientId");

        SEBServerInit.INIT_LOGGER.info("---->");
        SEBServerInit.INIT_LOGGER.info("----> Webservice admin API basic access: --" + webServiceAPIBasicAccess + "--");

    }

}
