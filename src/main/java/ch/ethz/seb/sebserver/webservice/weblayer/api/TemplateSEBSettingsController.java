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
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@WebServiceProfile
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
