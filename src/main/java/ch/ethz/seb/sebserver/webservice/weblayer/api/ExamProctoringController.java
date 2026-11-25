/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Collection;

import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_PROCTORING_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class ExamProctoringController {

    private static final Logger log = LoggerFactory.getLogger(ExamProctoringController.class);

    private final ExamAdminService examAdminService;
    private final AuthorizationService authorizationService;
    private final ExamSessionService examSessionService;
    private final ScreenProctoringService screenProctoringService;

    public ExamProctoringController(
            final ExamAdminService examAdminService,
            final AuthorizationService authorizationService,
            final ExamSessionService examSessionService,
            final ScreenProctoringService screenProctoringService) {

        this.examAdminService = examAdminService;
        this.authorizationService = authorizationService;
        this.examSessionService = examSessionService;
        this.screenProctoringService = screenProctoringService;
    }

    /** This is called by Spring to initialize the WebDataBinder and is used here to
     * initialize the default value binding for the institutionId request-parameter
     * that has the current users insitutionId as default.
     *
     * See also UserService.addUsersInstitutionDefaultPropertySupport */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        this.authorizationService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_SCREEN_PROCTORING_GROUPS_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ScreenProctoringGroup> getScreenProctoringGroupsOfExam(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_MODEL_ID) final Long examId) {

        checkAccess(institutionId, examId);
        return this.screenProctoringService
                .getCollectingGroups(examId)
                .getOrThrow();
    }

    private void checkAccess(final Long institutionId, final Long examId) {
        this.authorizationService.check(
                PrivilegeType.READ,
                EntityType.EXAM,
                institutionId);

        this.authorizationService.checkRead(this.examSessionService
                .getExamDAO()
                .examGrantEntityByPK(examId)
                .getOrThrow());
    }

}
