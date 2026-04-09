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

import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Info", description = "Public information and current user privilege endpoints")
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.INFO_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
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

    @Operation(operationId = "getInstitutionLogo", summary = "Get the institution logo image by URL suffix")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not found")
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

    @Operation(operationId = "getInstitutionInfo", summary = "Get a list of all active institutions")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
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

    @Operation(operationId = "getInstitutionInfoByUrlSuffix", summary = "Get institution info filtered by URL suffix")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
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

    @Operation(operationId = "getUserPrivileges", summary = "Get all privileges for the current user")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.PRIVILEGES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Privilege> privileges() {
        return this.authorizationGrantService.getAllPrivileges();
    }

    @Operation(operationId = "getServiceFeatures", summary = "Get all configured service-level feature flags")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path =  API.FEATURES_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> getServiceFeatures() {
        return webserviceInfo.configuredFeatures();
    }

}
