/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.*;
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
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_SETTINGS_EXAM_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class ExamSEBSettingsController {

    private static final Logger log = LoggerFactory.getLogger(ExamSEBSettingsController.class);
    
    private final SEBSettingsService sebSettingsService;
    private final AuthorizationService authorizationService;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ExamDAO examDAO;
    private final UserActivityLogDAO userActivityLogDAO;

    public ExamSEBSettingsController(
            final SEBSettingsService sebSettingsService,
            final AuthorizationService authorizationService,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ExamDAO examDAO, 
            final UserActivityLogDAO userActivityLogDAO) {
        
        this.sebSettingsService = sebSettingsService;
        this.authorizationService = authorizationService;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.examDAO = examDAO;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView getSEBSettings(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId, 
            @RequestParam(name = SEBSettingsView.ATTR_VIEW_TYPE) final SEBSettingsView.ViewType viewType) {
        
        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());
        
        return sebSettingsService
                .getSEBSettingsOfExam(examId, viewType)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_EXAM_CONFIG_MAPPING,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ExamConfigurationMap> getExamConfigMappings(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId) {

        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());

        return examConfigurationMapDAO
                .allOfExam(examId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> getTableValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());

        return sebSettingsService
                .getTableValuesOfExam(examId, attributeName)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.Value saveSingleValue(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_ID) final Long valueId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_VALUE) final String value) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());
        
        System.out.println("******** value: " + value);

        return sebSettingsService
                .saveSingleValueForExam(examId, valueId, value)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.TableRowValues saveTableRowValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestBody final SEBSettingsView.TableRowValues values) {

        final Exam exam = examDAO.byPK(examId).getOrThrow();
        authorizationService.hasModifyGrant(exam);

        return sebSettingsService
                .saveTableRowValuesForExam(examId, values)
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
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());

        return sebSettingsService
                .addNewTableRowForExam(examId, attributeName)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + 
                    API.SEB_SETTINGS_TABLE_PATH_SEGMENT + 
                    API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> deleteTableRow(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_LIST_INDEX) final int index) {
        
        if (index < 0) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Negative row index not allowed"));
        }

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());

        return sebSettingsService
                .deleteTableRowForExam(examId, attributeName, index)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_ACTIVE_SEB_CLIENTS,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer getActiveSEBClients(@PathVariable(name =API.PARAM_MODEL_ID) final Long examId) {

        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());

        return sebSettingsService
                .getActiveSEBClientsForExam(examId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT +
                    API.SEB_SETTINGS_PUBLISH,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam publish(@PathVariable(name =API.PARAM_MODEL_ID) final Long examId) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());
        
        System.out.println("************* publish SEB Settings");

        return sebSettingsService
                .applySettingsForExam(examId)
                .flatMap(examDAO::byPK)
                .flatMap(userActivityLogDAO::logModify)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT +
                    API.SEB_SETTINGS_UNDO,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam undoChanges(@PathVariable(name =API.PARAM_MODEL_ID) final Long examId) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());

        System.out.println("************* undo SEB Setting changes");

        return sebSettingsService
                .undoSettingsForExam(examId)
                .flatMap(examDAO::byPK)
                .flatMap(userActivityLogDAO::logModify)
                .getOrThrow();
    }
}
