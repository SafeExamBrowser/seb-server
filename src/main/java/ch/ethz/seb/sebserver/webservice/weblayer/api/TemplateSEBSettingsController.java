/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.List;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBSettingsView;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Template SEB Settings", description = "Exam template SEB settings management")
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_SETTINGS_TEMPLATE_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class TemplateSEBSettingsController {

    private static final Logger log = LoggerFactory.getLogger(TemplateSEBSettingsController.class);

    private final SEBSettingsService sebSettingsService;
    private final AuthorizationService authorizationService;
    private final ConfigurationNodeDAO configurationNodeDAO;


    public TemplateSEBSettingsController(
            final SEBSettingsService sebSettingsService,
            final AuthorizationService authorizationService, 
            final ConfigurationNodeDAO configurationNodeDAO) {
        
        this.sebSettingsService = sebSettingsService;
        this.configurationNodeDAO = configurationNodeDAO;
        this.authorizationService = authorizationService;
    }

    @Operation(operationId = "getTemplateSEBSettings", summary = "Get SEB settings view for a given exam template")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView getSEBSettings(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestParam(name = SEBSettingsView.ATTR_VIEW_TYPE) final SEBSettingsView.ViewType viewType) {

        authorizationService.hasReadGrant(configurationNodeDAO.byPK(templateId).getOrThrow());

        return sebSettingsService
                .getSEBSettingsOfTemplate(templateId, viewType)
                .getOrThrow();
    }

    @Operation(operationId = "getTemplateSEBSettingsTableValues", summary = "Get table attribute values for a given exam template and attribute name")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> getTableValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasReadGrant(configurationNodeDAO.byPK(templateId).getOrThrow());

        return sebSettingsService
                .getTableValuesOfTemplate(templateId, attributeName)
                .getOrThrow();
    }

    @Operation(operationId = "saveTemplateSEBSettings", summary = "Save a single SEB settings value for a given exam template")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.Value saveSingleValue(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_ID) final Long valueId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_VALUE) final String value) {

        authorizationService.hasModifyGrant(configurationNodeDAO.byPK(templateId).getOrThrow());
        
        return sebSettingsService
                .saveSingleValueForTemplate(templateId, valueId, value)
                .getOrThrow();
    }

    @Operation(operationId = "saveTemplateSEBSettingsTableRow", summary = "Save a table row of SEB settings values for a given exam template")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.TableRowValues saveTableRowValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestBody final SEBSettingsView.TableRowValues values) {

        authorizationService.hasModifyGrant(configurationNodeDAO.byPK(templateId).getOrThrow());

        return sebSettingsService
                .saveTableRowValuesForTemplate(templateId, values)
                .getOrThrow();
    }

    @Operation(operationId = "addTemplateSEBSettingsTableRow", summary = "Add a new table row for a given SEB settings attribute in an exam template")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request / validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT +
                    API.SEB_SETTINGS_TABLE_PATH_SEGMENT +
                    API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.TableRowValues addNewTableRow(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasModifyGrant(configurationNodeDAO.byPK(templateId).getOrThrow());

        return sebSettingsService
                .addNewTableRowForTemplate(templateId, attributeName)
                .getOrThrow();
    }

    @Operation(operationId = "deleteTemplateSEBSettingsTableRow", summary = "Delete a table row for a given SEB settings attribute in an exam template")
    @ApiResponse(responseCode = "200", description = "Deleted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Not found")
    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT +
                    API.SEB_SETTINGS_TABLE_PATH_SEGMENT +
                    API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> deleteTableRow(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long templateId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_LIST_INDEX) final int index) {

        if (index < 0) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Negative row index not allowed"));
        }

        authorizationService.hasModifyGrant(configurationNodeDAO.byPK(templateId).getOrThrow());

        return sebSettingsService
                .deleteTableRowForTemplate(templateId, attributeName, index)
                .getOrThrow();
    }
}
