/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.AppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey.KeyType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamTemplateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.SecurityKeyService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_ADMINISTRATION_ENDPOINT)
public class ExamAdministrationController extends EntityController<Exam, Exam> {

    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final ExamAdminService examAdminService;
    private final ExamTemplateService examTemplateService;
    private final LmsAPIService lmsAPIService;
    private final ExamSessionService examSessionService;
    private final SEBRestrictionService sebRestrictionService;
    private final SecurityKeyService securityKeyService;

    public ExamAdministrationController(
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamDAO examDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final BeanValidationService beanValidationService,
            final LmsAPIService lmsAPIService,
            final UserDAO userDAO,
            final ExamAdminService examAdminService,
            final ExamTemplateService examTemplateService,
            final ExamSessionService examSessionService,
            final SEBRestrictionService sebRestrictionService,
            final SecurityKeyService securityKeyService) {

        super(authorization,
                bulkActionService,
                examDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.examAdminService = examAdminService;
        this.examTemplateService = examTemplateService;
        this.lmsAPIService = lmsAPIService;
        this.examSessionService = examSessionService;
        this.sebRestrictionService = sebRestrictionService;
        this.securityKeyService = securityKeyService;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamRecordDynamicSqlSupport.examRecord;
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CHECK_IMPORTED_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<EntityKey> checkImported(
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);
        return this.examDAO.allInstitutionIdsByQuizId(modelId)
                .map(ids -> ids
                        .stream()
                        .map(id -> new EntityKey(id, EntityType.INSTITUTION))
                        .collect(Collectors.toList()))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CONSISTENCY_CHECK_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<APIMessage> checkExamConsistency(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(
                    name = API.EXAM_ADMINISTRATION_CONSISTENCY_CHECK_INCLUDE_RESTRICTION,
                    defaultValue = "false") final boolean includeRestriction) {

        checkReadPrivilege(institutionId);
        final Collection<APIMessage> result = this.examSessionService
                .checkExamConsistency(modelId)
                .getOrThrow();

        return result;
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_ARCHIVE_PATH_SEGMENT,
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam archive(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkWritePrivilege(institutionId);
        return this.examDAO.byPK(modelId)
                .flatMap(this::checkWriteAccess)
                .flatMap(this.examAdminService::archiveExam)
                .flatMap(this::logModify)
                .getOrThrow();
    }

    // ****************************************************************************
    // **** SEB Security Key and App Signature Key

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_INFO_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<AppSignatureKeyInfo> getAppSignatureKeyInfo(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        return this.examDAO.byPK(examId)
                .flatMap(this::checkReadAccess)
                .flatMap(exam -> this.securityKeyService.getAppSignatureKeyInfo(institutionId, examId))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_INFO_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void saveAppSignatureKeySettings(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED) final Boolean enableKeyCheck,
            @RequestParam(Exam.ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD) final Integer threshold) {

        this.examDAO.byPK(examId)
                .flatMap(this::checkReadAccess)
                .flatMap(exam -> this.examAdminService.saveSecurityKeySettings(
                        institutionId,
                        examId,
                        enableKeyCheck,
                        threshold))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_GRANTS_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<SecurityKey> getSecurityKeyEntries(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        return this.examDAO.byPK(examId)
                .flatMap(this::checkReadAccess)
                .flatMap(exam -> this.securityKeyService.getSecurityKeyEntries(
                        institutionId,
                        examId,
                        KeyType.APP_SIGNATURE_KEY))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT +
                    API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_GRANTS_PATH_SEGMENT +
                    API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SecurityKey grantAppSignatureKey(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long connectionId,
            @RequestParam(name = Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG, required = false) final String tagName) {

        this.checkWritePrivilege(institutionId);

        return this.examDAO.byPK(examId)
                .flatMap(this::checkReadAccess)
                .flatMap(exam -> this.securityKeyService.grantAppSignatureKey(
                        institutionId,
                        examId,
                        connectionId,
                        tagName))
                .flatMap(this.userActivityLogDAO::logCreate)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_SECURITY_KEY_GRANTS_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityKey deleteSecurityGrant(
            @PathVariable(name = API.PARAM_PARENT_MODEL_ID, required = true) final Long examId,
            @PathVariable(name = API.PARAM_MODEL_ID, required = true) final Long keyId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.checkWritePrivilege(institutionId);
        return this.examDAO.byPK(examId)
                .flatMap(this::checkReadAccess)
                .flatMap(exam -> this.securityKeyService.deleteSecurityKeyGrant(keyId))
                .flatMap(this.userActivityLogDAO::logDelete)
                .getOrThrow();
    }

    // **** SEB Security Key
    // ****************************************************************************

    // ****************************************************************************
    // **** SEB Restriction

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CHECK_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean checkSEBRestriction(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);
        return this.examDAO
                .byPK(modelId)
                .flatMap(this.examAdminService::isRestricted)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBRestriction getSEBRestriction(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final Long modelId) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.sebRestrictionService::getSEBRestrictionFromExam)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam saveSEBRestrictionData(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examId,
            @Valid @RequestBody final SEBRestriction sebRestriction) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO.byPK(examId)
                .flatMap(this.authorization::checkModify)
                .flatMap(exam -> this.sebRestrictionService.saveSEBRestrictionToExam(exam, sebRestriction))
                .flatMap(exam -> this.examAdminService.isRestricted(exam).getOrThrow()
                        ? this.applySEBRestriction(exam, true)
                        : Result.of(exam))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam applySEBRestriction(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examlId) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO.byPK(examlId)
                .flatMap(this.authorization::checkModify)
                .flatMap(exam -> this.applySEBRestriction(exam, true))
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam deleteSEBRestriction(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examlId) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO.byPK(examlId)
                .flatMap(this.authorization::checkModify)
                .flatMap(exam -> this.applySEBRestriction(exam, false))
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_CHAPTERS_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Chapters getChapters(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examlId) {

        checkReadPrivilege(institutionId);
        return this.entityDAO.byPK(examlId)
                .flatMap(this.authorization::checkRead)
                .flatMap(exam -> this.lmsAPIService
                        .getLmsAPITemplate(exam.lmsSetupId)
                        .getOrThrow()
                        .getCourseChapters(exam.externalId))
                .getOrThrow();
    }

    // **** SEB Restriction
    // ****************************************************************************

    // ****************************************************************************
    // **** Proctoring

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProctoringServiceSettings getProctoringServiceSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable final Long modelId) {

        checkReadPrivilege(institutionId);
        return this.examAdminService
                .getProctoringServiceSettings(modelId)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Exam saveProctoringServiceSettings(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @PathVariable(API.PARAM_MODEL_ID) final Long examId,
            @Valid @RequestBody final ProctoringServiceSettings proctoringServiceSettings) {

        checkModifyPrivilege(institutionId);
        return this.entityDAO
                .byPK(examId)
                .flatMap(this.authorization::checkModify)
                .map(exam -> {
                    this.examAdminService
                            .saveProctoringServiceSettings(examId, proctoringServiceSettings)
                            .getOrThrow();
                    return exam;
                })
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    // **** Proctoring
    // ****************************************************************************

    @Override
    protected Exam createNew(final POSTMapper postParams) {

        final Long lmsSetupId = postParams.getLong(QuizData.QUIZ_ATTR_LMS_SETUP_ID);
        final String quizId = postParams.getString(QuizData.QUIZ_ATTR_ID);
        final SEBServerUser currentUser = this.authorization.getUserService().getCurrentUser();
        postParams.putIfAbsent(EXAM.ATTR_OWNER, currentUser.uuid());

        return this.lmsAPIService
                .getLmsAPITemplate(lmsSetupId)
                .map(template -> {
                    this.authorization.checkRead(template.lmsSetup());
                    return template;
                })
                .flatMap(template -> template.getQuiz(quizId))
                .map(quiz -> new Exam(null, quiz, postParams))
                .getOrThrow();
    }

    @Override
    protected Result<Exam> notifyCreated(final Exam entity) {
        final List<APIMessage> errors = new ArrayList<>();

        this.examTemplateService
                .addDefinedIndicators(entity)
                .onErrorDo(error -> {
                    errors.add(ErrorMessage.EXAM_IMPORT_ERROR_AUTO_INDICATOR.of(error));
                    return entity;
                })
                .flatMap(this.examTemplateService::addDefinedClientGroups)
                .onErrorDo(error -> {
                    errors.add(ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CLIENT_GROUPS.of(error));
                    return entity;
                })
                .flatMap(this.examTemplateService::initAdditionalAttributes)
                .onErrorDo(error -> {
                    errors.add(ErrorMessage.EXAM_IMPORT_ERROR_AUTO_ATTRIBUTES.of(error));
                    return entity;
                })
                .flatMap(this.examTemplateService::initExamConfiguration)
                .onErrorDo(error -> {
                    if (error instanceof APIMessageException) {
                        errors.addAll(((APIMessageException) error).getAPIMessages());
                    } else {
                        errors.add(ErrorMessage.EXAM_IMPORT_ERROR_AUTO_CONFIG.of(error));
                    }
                    return entity;
                })
                .flatMap(this.examAdminService::applyAdditionalSEBRestrictions)
                .onErrorDo(error -> {
                    errors.add(ErrorMessage.EXAM_IMPORT_ERROR_AUTO_RESTRICTION.of(error));
                    return entity;
                });

        if (!errors.isEmpty()) {
            errors.add(0, ErrorMessage.EXAM_IMPORT_ERROR_AUTO_SETUP.of(
                    entity.getModelId(),
                    API.PARAM_MODEL_ID + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR + entity.getModelId()));
            throw new APIMessageException(errors);
        } else {
            return Result.of(entity);
        }
    }

    @Override
    protected Result<Exam> notifySaved(final Exam entity) {
        return Result.tryCatch(() -> {
            this.examSessionService.flushCache(entity);
            return entity;
        }).flatMap(this.examAdminService::saveLMSAttributes);
    }

    @Override
    protected Result<Exam> validForCreate(final Exam entity) {
        return super.validForCreate(entity)
                .map(this::checkExamSupporterRole);
    }

    @Override
    protected Result<Exam> validForSave(final Exam entity) {
        return super.validForSave(entity)
                .map(this::checkExamSupporterRole);
    }

    @Override
    protected void populateFilterMap(final FilterMap filterMap, final Long institutionId, final String sort) {
        super.populateFilterMap(filterMap, institutionId, sort);

        // If sorting is on lms setup name we need to join the lms setup table
        if (sort != null && sort.contains(Domain.EXAM.ATTR_LMS_SETUP_ID)) {
            filterMap.putIfAbsent(FilterMap.ATTR_ADD_LMS_SETUP_JOIN, Constants.TRUE_STRING);
        }
    }

    @Override
    protected Result<Exam> validForDelete(final Exam entity) {
        return checkNoActiveSEBClientConnections(entity);
    }

    private Exam checkExamSupporterRole(final Exam exam) {
        final Set<String> examSupporter = this.userDAO.all(
                this.authorization.getUserService().getCurrentUser().getUserInfo().institutionId,
                true)
                .map(users -> users.stream()
                        .filter(user -> user.getRoles().contains(UserRole.EXAM_SUPPORTER.name()))
                        .map(user -> user.uuid)
                        .collect(Collectors.toSet()))
                .getOrThrow();

        for (final String supporterUUID : exam.getSupporter()) {
            if (!examSupporter.contains(supporterUUID)) {
                throw new APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.EXAM.TYPE_NAME,
                                Domain.EXAM.ATTR_SUPPORTER,
                                "exam:supporter:grantDenied:" + supporterUUID)));
            }
        }
        return exam;
    }

    private Result<Exam> checkNoActiveSEBClientConnections(final Exam exam) {
        if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
            return Result.ofError(new APIMessageException(
                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                            .of("Exam currently has active SEB Client connections.")));
        }

        return Result.of(exam);
    }

    private Result<Exam> applySEBRestriction(final Exam exam, final boolean restrict) {

        return Result.tryCatch(() -> {
            final LmsSetup lmsSetup = this.lmsAPIService.getLmsSetup(exam.lmsSetupId)
                    .getOrThrow();

            if (!lmsSetup.lmsType.features.contains(Features.SEB_RESTRICTION)) {
                throw new UnsupportedOperationException(
                        "SEB Restriction feature not available for LMS type: " + lmsSetup.lmsType);
            }

            if (restrict) {
                if (!this.lmsAPIService
                        .getLmsSetup(exam.lmsSetupId)
                        .getOrThrow().lmsType.features.contains(Features.SEB_RESTRICTION)) {

                    throw new APIMessageException(
                            APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT
                                    .of("The LMS for this Exam has no SEB restriction feature"));
                }

                if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
                    throw new APIMessageException(
                            APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                                    .of("Exam currently has active SEB Client connections."));
                }

                return this.checkNoActiveSEBClientConnections(exam)
                        .flatMap(this.sebRestrictionService::applySEBClientRestriction)
                        .flatMap(e -> this.examDAO.setSEBRestriction(exam.id, restrict))
                        .getOrThrow();
            } else {
                return this.sebRestrictionService.releaseSEBClientRestriction(exam)
                        .flatMap(e -> this.examDAO.setSEBRestriction(exam.id, restrict))
                        .getOrThrow();
            }
        });
    }

    static Function<Collection<Exam>, List<Exam>> pageSort(final String sort) {

        final String sortBy = PageSortOrder.decode(sort);
        return exams -> {
            final List<Exam> list = exams.stream().collect(Collectors.toList());
            if (StringUtils.isBlank(sort)) {
                return list;
            }

            if (sortBy.equals(Exam.FILTER_ATTR_NAME) || sortBy.equals(QuizData.QUIZ_ATTR_NAME)) {
                list.sort(Comparator.comparing(exam -> (exam.name != null) ? exam.name : StringUtils.EMPTY));
            }
            if (sortBy.equals(Exam.FILTER_ATTR_TYPE)) {
                list.sort(Comparator.comparing(exam -> (exam.type != null) ? exam.type : ExamType.UNDEFINED));
            }
            if (sortBy.equals(QuizData.FILTER_ATTR_START_TIME) || sortBy.equals(QuizData.QUIZ_ATTR_START_TIME)) {
                list.sort(Comparator.comparing(exam -> (exam.startTime != null) ? exam.startTime : new DateTime(0)));
            }

            if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
                Collections.reverse(list);
            }
            return list;
        };
    }

}
