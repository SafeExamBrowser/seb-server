/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;

@WebServiceProfile
@RestController
public class LogoController {

    private final InstitutionDAO institutionDAO;

    protected LogoController(final InstitutionDAO institutionDAO) {
        this.institutionDAO = institutionDAO;
    }

    @RequestMapping(API.INSTITUTIONAL_LOGO_PATH)
    public String logo(@PathVariable final String institutionId) throws IOException {
        return this.institutionDAO
                .byModelId(institutionId)
                .getOrThrow().logoImage;
    }

}
