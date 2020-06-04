/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.Features;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_ADMINISTRATION_ENDPOINT)
public class ExamAdministrationController extends EntityController<Exam, Exam> {

    private static final Logger log = LoggerFactory.getLogger(ExamAdministrationController.class);

    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final ExamAdminService examAdminService;
    private final LmsAPIService lmsAPIService;
    private final ExamConfigService sebExamConfigService;
    private final ExamSessionService examSessionService;
    private final SEBRestrictionService sebRestrictionService;

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
            final ExamConfigService sebExamConfigService,
            final ExamSessionService examSessionService,
            final SEBRestrictionService sebRestrictionService) {

        super(authorization,
                bulkActionService,
                examDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.examAdminService = examAdminService;
        this.lmsAPIService = lmsAPIService;
        this.sebExamConfigService = sebExamConfigService;
        this.examSessionService = examSessionService;
        this.sebRestrictionService = sebRestrictionService;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamRecordDynamicSqlSupport.examRecord;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Override
    public Page<Exam> getPage(
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            final HttpServletRequest request) {

        checkReadPrivilege(institutionId);

        // NOTE: several attributes for sorting may be originated by the QuizData from LMS not by the database
        //       of the SEB Server. Therefore in the case we have no or the default sorting we can use the
        //       native PaginationService within MyBatis and SQL. For the other cases we need an in-line sorting and paging
        if (StringUtils.isBlank(sort) ||
                this.paginationService.isNativeSortingSupported(ExamRecordDynamicSqlSupport.examRecord, sort)) {

            return super.getPage(institutionId, pageNumber, pageSize, sort, allRequestParams, request);

        } else {

            this.authorization.check(
                    PrivilegeType.READ,
                    EntityType.EXAM,
                    institutionId);

            final List<Exam> exams = new ArrayList<>(
                    this.examDAO
                            .allMatching(new FilterMap(allRequestParams, request.getQueryString()), this::hasReadAccess)
                            .getOrThrow());

            return buildSortedExamPage(
                    this.paginationService.getPageNumber(pageNumber),
                    this.paginationService.getPageSize(pageSize),
                    sort,
                    exams);
        }
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_DOWNLOAD_CONFIG_PATH_SEGMENT
                    + API.PARENT_MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadPlainXMLConfig(
            @PathVariable final Long modelId,
            @PathVariable final Long parentModelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletResponse response) throws IOException {

        this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkRead)
                .flatMap(this.userActivityLogDAO::logExport);

        final ServletOutputStream outputStream = response.getOutputStream();

        try {

            this.sebExamConfigService.exportForExam(
                    outputStream,
                    institutionId,
                    parentModelId,
                    modelId);

            response.setStatus(HttpStatus.OK.value());

        } catch (final Exception e) {
            log.error("Unexpected error while trying to downstream exam config: ", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CHECK_IMPORTED_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Collection<EntityKey> checkImported(
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);
        return this.examDAO.allByQuizId(modelId)
                .map(ids -> ids
                        .stream()
                        .map(id -> new EntityKey(id, EntityType.EXAM))
                        .collect(Collectors.toList()))
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CONSISTENCY_CHECK_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Collection<APIMessage> checkExamConsistency(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkModifyPrivilege(institutionId);
        return this.examSessionService
                .checkRunningExamConsistency(modelId)
                .getOrThrow();
    }

    // ****************************************************************************
    // **** SEB Restriction

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_CHECK_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Boolean checkSEBRestriction(
            @PathVariable final Long modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkReadPrivilege(institutionId);
        return this.examDAO.byPK(modelId)
                .flatMap(this.examAdminService::isRestricted)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
        return this.examAdminService
                .addDefaultIndicator(entity)
                .flatMap(this.examAdminService::applyAdditionalSEBRestrictions);
    }

    @Override
    protected Result<Exam> notifySaved(final Exam entity) {
        return Result.tryCatch(() -> {
            this.examSessionService.flushCache(entity);
            return entity;
        });
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
        final LmsSetup lmsSetup = this.lmsAPIService.getLmsSetup(exam.lmsSetupId)
                .getOrThrow();

        if (!lmsSetup.lmsType.features.contains(Features.SEB_RESTRICTION)) {
            return Result.ofError(new UnsupportedOperationException(
                    "SEB Restriction feature not available for LMS type: " + lmsSetup.lmsType));
        }

        if (restrict) {
            if (!this.lmsAPIService
                    .getLmsSetup(exam.lmsSetupId)
                    .getOrThrow().lmsType.features.contains(Features.SEB_RESTRICTION)) {

                return Result.ofError(new APIMessageException(
                        APIMessage.ErrorMessage.ILLEGAL_API_ARGUMENT
                                .of("The LMS for this Exam has no SEB restriction feature")));
            }

            if (this.examSessionService.hasActiveSEBClientConnections(exam.id)) {
                return Result.ofError(new APIMessageException(
                        APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                                .of("Exam currently has active SEB Client connections.")));
            }

            return this.checkNoActiveSEBClientConnections(exam)
                    .flatMap(this.sebRestrictionService::applySEBClientRestriction)
                    .flatMap(e -> this.examDAO.setSEBRestriction(exam.id, restrict));
        } else {
            return this.sebRestrictionService.releaseSEBClientRestriction(exam)
                    .flatMap(e -> this.examDAO.setSEBRestriction(exam.id, restrict));
        }
    }

    public static Page<Exam> buildSortedExamPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final List<Exam> exams) {

        if (!StringUtils.isBlank(sort)) {
            final String sortBy = PageSortOrder.decode(sort);
            if (sortBy.equals(Exam.FILTER_ATTR_NAME)) {
                exams.sort(Comparator.comparing(exam -> exam.name));
            }
            if (sortBy.equals(Exam.FILTER_ATTR_TYPE)) {
                exams.sort(Comparator.comparing(exam -> exam.type));
            }
            if (sortBy.equals(QuizData.FILTER_ATTR_START_TIME)) {
                exams.sort(Comparator.comparing(exam -> exam.startTime));
            }
        }

        if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
            Collections.reverse(exams);
        }

        final int start = (pageNumber - 1) * pageSize;
        int end = start + pageSize;
        if (exams.size() < end) {
            end = exams.size();
        }
        return new Page<>(
                exams.size() / pageSize,
                pageNumber,
                sort,
                exams.subList(start, end));
    }

}
