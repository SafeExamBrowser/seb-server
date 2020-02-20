/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class WebServiceShutdown implements DisposableBean, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(WebServiceShutdown.class);

    public WebServiceShutdown() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void destroy() {
        log.info("SEB-Server Web-Service shutdown...");

    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
        log.info("SEB-Server Web-Service shutdown...");

    }

}
