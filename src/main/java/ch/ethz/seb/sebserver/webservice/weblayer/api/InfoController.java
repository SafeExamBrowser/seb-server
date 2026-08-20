/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.authorization.Privilege;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.INFO_ENDPOINT)
@Tag(name = "Info", description = "Public server information endpoints.")
public class InfoController {

    private final InstitutionDAO institutionDAO;
    private final AuthorizationService authorizationGrantService;
    private final WebserviceInfo webserviceInfo;

    protected InfoController(
            final InstitutionDAO institutionDAO,
            final AuthorizationService authorizationGrantService,
            final WebserviceInfo webserviceInfo) {

        this.institutionDAO = institutionDAO;
        this.authorizationGrantService = authorizationGrantService;
        this.webserviceInfo = webserviceInfo;
    }


    @Operation(
            operationId = "getInstitutionInfo",
            summary = "Gets the names of all active institutions for selection.",
            description = "Publicly reachable; it backs the institution selection of the self-registration page.")
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


    @Operation(hidden = true)
    @RequestMapping(
            path = API.PRIVILEGES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Privilege> privileges() {
        return this.authorizationGrantService.getAllPrivileges();
    }

    @Operation(hidden = true)
    @RequestMapping(
            path =  API.FEATURES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> getServiceFeatures() {
        return webserviceInfo.configuredFeatures();
    }

}
