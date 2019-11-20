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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
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
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_ADMINISTRATION_ENDPOINT)
public class ExamAdministrationController extends EntityController<Exam, Exam> {

    private static final Logger log = LoggerFactory.getLogger(ExamAdministrationController.class);

    private final ExamDAO examDAO;
    private final UserDAO userDAO;
    private final LmsAPIService lmsAPIService;
    private final ExamConfigService sebExamConfigService;
    private final ExamSessionService examSessionService;
    private final ExamConfigUpdateService examConfigUpdateService;

    public ExamAdministrationController(
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamDAO examDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final BeanValidationService beanValidationService,
            final LmsAPIService lmsAPIService,
            final UserDAO userDAO,
            final ExamConfigService sebExamConfigService,
            final ExamSessionService examSessionService,
            final ExamConfigUpdateService examConfigUpdateService) {

        super(authorization,
                bulkActionService,
                examDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDAO = examDAO;
        this.userDAO = userDAO;
        this.lmsAPIService = lmsAPIService;
        this.sebExamConfigService = sebExamConfigService;
        this.examSessionService = examSessionService;
        this.examConfigUpdateService = examConfigUpdateService;
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
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        checkReadPrivilege(institutionId);

        // NOTE: several attributes for sorting may be originated by the QuizData from LMS not by the database
        //       of the SEB Server. Therefore in the case we have no or the default sorting we can use the
        //       native PaginationService within MyBatis and SQL. For the other cases we need an in-line sorting and paging
        if (StringUtils.isBlank(sort) ||
                this.paginationService.isNativeSortingSupported(ExamRecordDynamicSqlSupport.examRecord, sort)) {

            return super.getPage(institutionId, pageNumber, pageSize, sort, allRequestParams);

        } else {

            this.authorization.check(
                    PrivilegeType.READ,
                    EntityType.EXAM,
                    institutionId);

            final List<Exam> exams = new ArrayList<>(
                    this.examDAO
                            .allMatching(new FilterMap(allRequestParams), this::hasReadAccess)
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT,
            method = RequestMethod.PATCH,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Exam switchSebRestriction(
            @PathVariable final Long modelId,
            @RequestParam(name = Domain.EXAM.ATTR_LMS_SEB_RESTRICTION, required = true) final boolean sebRestriction,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        checkModifyPrivilege(institutionId);

        return this.entityDAO.byPK(modelId)
                .flatMap(this.authorization::checkModify)
                .flatMap(this::checkNoActiveSebClientConnections)
                .flatMap(exam -> this.applySebRestriction(exam, sebRestriction))
                .flatMap(this.userActivityLogDAO::logModify)
                .getOrThrow();
    }

    public static Page<Exam> buildSortedExamPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final List<Exam> exams) {

        if (!StringUtils.isBlank(sort)) {
            final String sortBy = PageSortOrder.decode(sort);
            if (sortBy.equals(Exam.FILTER_ATTR_NAME)) {
                Collections.sort(exams, (exam1, exam2) -> exam1.name.compareTo(exam2.name));
            }
            if (sortBy.equals(Exam.FILTER_ATTR_TYPE)) {
                Collections.sort(exams, (exam1, exam2) -> exam1.type.compareTo(exam2.type));
            }
            if (sortBy.equals(QuizData.FILTER_ATTR_START_TIME)) {
                Collections.sort(exams, (exam1, exam2) -> exam1.startTime.compareTo(exam2.startTime));
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

    private Result<Exam> checkNoActiveSebClientConnections(final Exam exam) {
        if (this.examConfigUpdateService.hasActiveSebClientConnections(exam.id)) {
            return Result.ofError(new APIMessageException(
                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                            .of("Exam currently has active SEB Client connections.")));
        }

        return Result.of(exam);
    }

    private Result<Exam> applySebRestriction(final Exam exam, final boolean sebRestriction) {

        if (BooleanUtils.toBoolean(exam.lmsSebRestriction) == sebRestriction) {
            return Result.of(exam);
        }

        if (sebRestriction) {
            return this.examConfigUpdateService.applySebClientRestriction(exam)
                    .flatMap(e -> this.examDAO.setSebRestriction(exam.id, sebRestriction));
        } else {
            return this.examConfigUpdateService.releaseSebClientRestriction(exam)
                    .flatMap(e -> this.examDAO.setSebRestriction(exam.id, sebRestriction));
        }
    }

}
