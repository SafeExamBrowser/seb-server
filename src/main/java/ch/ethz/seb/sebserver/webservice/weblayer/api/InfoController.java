/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.INFO_ENDPOINT)
public class InfoController {

    private final InstitutionDAO institutionDAO;
    private final AuthorizationService authorizationGrantService;

    protected InfoController(
            final InstitutionDAO institutionDAO,
            final AuthorizationService authorizationGrantService) {

        this.institutionDAO = institutionDAO;
        this.authorizationGrantService = authorizationGrantService;
    }

    @RequestMapping(
            path = API.INSTITUTIONAL_LOGO_PATH,
            method = RequestMethod.GET,
            produces = MediaType.IMAGE_PNG_VALUE + ";base64")
    public String logo(@PathVariable final String urlSuffix) {
        if (urlSuffix == null) {
            return null;
        }

        return this.institutionDAO
                .all(null, true)
                .getOrThrow()
                .stream()
                .filter(inst -> urlSuffix.equals(inst.urlSuffix))
                .findFirst()
                .map(inst -> inst.logoImage)
                .orElse(null);
    }

    @RequestMapping(
            path = API.INFO_INST_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityName> getInstitutionInfo() {
        return this.institutionDAO
                .all(null, true)
                .getOrThrow()
                .stream()
                .filter(inst -> BooleanUtils.isTrue(inst.active))
                .map(inst -> new EntityName(inst.getEntityKey(), inst.name))
                .collect(Collectors.toList());
    }

    @RequestMapping(
            path = API.INFO_INST_ENDPOINT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityName> getInstitutionInfo(@PathVariable final String urlSuffix) {
        return this.institutionDAO
                .all(null, true)
                .getOrThrow()
                .stream()
                .filter(inst -> BooleanUtils.isTrue(inst.active) &&
                        inst.urlSuffix != null &&
                        urlSuffix.equals(inst.urlSuffix))
                .map(inst -> new EntityName(inst.getEntityKey(), inst.name))
                .collect(Collectors.toList());
    }

    @RequestMapping(
            path = API.PRIVILEGES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Privilege> privileges() {
        return this.authorizationGrantService.getAllPrivileges();
    }

}
