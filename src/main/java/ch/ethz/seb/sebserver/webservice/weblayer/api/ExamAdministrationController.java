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
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.Page.SortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction.Type;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO.ActivityType;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + RestAPI.ENDPOINT_EXAM_ADMINISTRATION)
public class ExamAdministrationController {

    private final AuthorizationGrantService authorizationGrantService;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ExamDAO examDAO;
    private final PaginationService paginationService;
    private final BulkActionService bulkActionService;

    public ExamAdministrationController(
            final AuthorizationGrantService authorizationGrantService,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamDAO examDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService) {

        this.authorizationGrantService = authorizationGrantService;
        this.userActivityLogDAO = userActivityLogDAO;
        this.examDAO = examDAO;
        this.paginationService = paginationService;
        this.bulkActionService = bulkActionService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) throws Exception {
        this.authorizationGrantService
                .getUserService()
                .addUsersInstitutionDefaultPropertySupport(binder);
    }

    @RequestMapping(method = RequestMethod.GET)
    public Collection<Exam> getAll(
            @RequestParam(
                    name = Exam.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Exam.FILTER_ATTR_LMS_SETUP, required = false) final Long lmsSetupId,
            @RequestParam(name = Exam.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = Exam.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = Exam.FILTER_ATTR_FROM, required = false) final String from,
            @RequestParam(name = Exam.FILTER_ATTR_STATUS, required = false) final ExamStatus status,
            @RequestParam(name = Exam.FILTER_ATTR_TYPE, required = false) final ExamType type,
            @RequestParam(name = Exam.FILTER_ATTR_OWNER, required = false) final String owner) {

        checkBaseReadPrivilege(institutionId);

        this.paginationService.setDefaultLimit(ExamRecordDynamicSqlSupport.examRecord);
        return getExams(institutionId, lmsSetupId, active, name, from, status, type, owner);
    }

    @RequestMapping(path = "/page", method = RequestMethod.GET)
    public Page<Exam> getPage(
            @RequestParam(
                    name = Exam.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Exam.FILTER_ATTR_LMS_SETUP, required = false) final Long lmsSetupId,
            @RequestParam(name = Exam.FILTER_ATTR_ACTIVE, required = false) final Boolean active,
            @RequestParam(name = Exam.FILTER_ATTR_NAME, required = false) final String name,
            @RequestParam(name = Exam.FILTER_ATTR_FROM, required = false) final String from,
            @RequestParam(name = Exam.FILTER_ATTR_STATUS, required = false) final ExamStatus status,
            @RequestParam(name = Exam.FILTER_ATTR_TYPE, required = false) final ExamType type,
            @RequestParam(name = Exam.FILTER_ATTR_OWNER, required = false) final String owner,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT_BY, required = false) final String sortBy,
            @RequestParam(name = Page.ATTR_SORT_ORDER, required = false) final Page.SortOrder sortOrder) {

        checkBaseReadPrivilege(institutionId);

        // NOTE: several attributes for sorting may be originated by the QuizData from LMS not by the database
        //       of the SEB Server. Therefore in the case we have no or the default sorting we can use the
        //       native PaginationService within MyBatis and SQL. For the other cases we need an in-line sorting and paging
        if (StringUtils.isBlank(sortBy) ||
                this.paginationService.isNativeSortingSupported(ExamRecordDynamicSqlSupport.examRecord, sortBy)) {

            return this.paginationService.getPage(
                    pageNumber,
                    pageSize,
                    sortBy,
                    sortOrder,
                    ExamRecordDynamicSqlSupport.examRecord,
                    () -> getExams(institutionId, lmsSetupId, active, name, from, status, type, owner));

        } else {

            final int pageNum = this.paginationService.getPageNumber(pageNumber);
            final int pSize = this.paginationService.getPageSize(pageSize);

            final List<Exam> exams = new ArrayList<>(
                    getExams(institutionId, lmsSetupId, active, name, from, status, type, owner));

            if (!StringUtils.isBlank(sortBy)) {
                if (sortBy.equals(QuizData.QUIZ_ATTR_NAME)) {
                    Collections.sort(exams, (exam1, exam2) -> exam1.name.compareTo(exam2.name));
                }
                if (sortBy.equals(QuizData.FILTER_ATTR_START_TIME)) {
                    Collections.sort(exams, (exam1, exam2) -> exam1.startTime.compareTo(exam2.startTime));
                }
            }

            if (SortOrder.DESCENDING == sortOrder) {
                Collections.reverse(exams);
            }

            return new Page<>(
                    exams.size() / pSize,
                    pageNum,
                    sortBy,
                    sortOrder,
                    exams.subList(pageNum * pSize, pageNum * pSize + pSize));
        }
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public Exam byId(@PathVariable final Long id) {
        return this.examDAO
                .byPK(id)
                .flatMap(exam -> this.authorizationGrantService.checkGrantOnEntity(
                        exam,
                        PrivilegeType.READ_ONLY))
                .getOrThrow();
    }

    @RequestMapping(path = "/save", method = RequestMethod.POST)
    public Exam save(@Valid @RequestBody final Exam exam) {
        return this.authorizationGrantService
                .checkGrantOnEntity(exam, PrivilegeType.MODIFY)
                .flatMap(this.examDAO::save)
                .flatMap(entity -> this.userActivityLogDAO.log(ActivityType.MODIFY, entity))
                .getOrThrow();
    }

    @RequestMapping(path = "/{id}/activate", method = RequestMethod.POST)
    public EntityProcessingReport activate(@PathVariable final Long id) {
        return setActive(id, true)
                .getOrThrow();
    }

    @RequestMapping(value = "/{id}/deactivate", method = RequestMethod.POST)
    public EntityProcessingReport deactivate(@PathVariable final Long id) {
        return setActive(id, false)
                .getOrThrow();
    }

    private Result<EntityProcessingReport> setActive(final Long id, final boolean active) {

        return this.bulkActionService.createReport(new BulkAction(
                (active) ? Type.ACTIVATE : Type.DEACTIVATE,
                EntityType.LMS_SETUP,
                new EntityKey(id, EntityType.LMS_SETUP)));

    }

    private Collection<Exam> getExams(
            final Long institutionId,
            final Long lmsSetupId,
            final Boolean active,
            final String name,
            final String from,
            final ExamStatus status,
            final ExamType type,
            final String owner) {

        this.authorizationGrantService.checkPrivilege(
                EntityType.EXAM,
                PrivilegeType.READ_ONLY,
                institutionId);

        final DateTime _from = (from != null)
                ? DateTime.parse(from, Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS)
                : null;

        return this.examDAO.allMatching(
                institutionId,
                lmsSetupId,
                name,
                status,
                type,
                _from,
                owner,
                active).getOrThrow();
    }

    private void checkBaseReadPrivilege(final Long institutionId) {
        this.authorizationGrantService.checkPrivilege(
                EntityType.EXAM,
                PrivilegeType.READ_ONLY,
                institutionId);
    }

}
