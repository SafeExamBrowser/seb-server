/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;

@Lazy
@Component
@GuiProfile
public class WebserviceURIBuilderSupplier {

    private final UriComponentsBuilder webserviceURIBuilder;

    public WebserviceURIBuilderSupplier(
            @Value("${sebserver.gui.webservice.protocol}") final String webserviceProtocol,
            @Value("${sebserver.gui.webservice.address}") final String webserviceServerAdress,
            @Value("${sebserver.gui.webservice.portol}") final String webserviceServerPort,
            @Value("${sebserver.gui.webservice.apipath}") final String webserviceAPIPath) {

        this.webserviceURIBuilder = UriComponentsBuilder
                .fromHttpUrl(webserviceProtocol + "://" + webserviceServerAdress)
                .port(webserviceServerPort)
                .path(webserviceAPIPath);
    }

    public UriComponentsBuilder getBuilder() {
        return this.webserviceURIBuilder.cloneBuilder();
    }
}
