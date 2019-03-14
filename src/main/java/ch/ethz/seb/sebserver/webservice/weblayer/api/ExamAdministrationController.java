/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.IndicatorDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.EXAM_ADMINISTRATION_ENDPOINT)
public class ExamAdministrationController extends ActivatableEntityController<Exam, Exam> {

    private final ExamDAO examDAO;
    private final IndicatorDAO indicatorDAO;
    private final LmsAPIService lmsAPIService;

    public ExamAdministrationController(
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO,
            final ExamDAO examDAO,
            final PaginationService paginationService,
            final BulkActionService bulkActionService,
            final BeanValidationService beanValidationService,
            final IndicatorDAO indicatorDAO,
            final LmsAPIService lmsAPIService) {

        super(authorization,
                bulkActionService,
                examDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.examDAO = examDAO;
        this.indicatorDAO = indicatorDAO;
        this.lmsAPIService = lmsAPIService;
    }

    @Override
    protected Class<Exam> modifiedDataType() {
        return Exam.class;
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamRecordDynamicSqlSupport.examRecord;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public Page<Exam> getAll(
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

            return super.getAll(institutionId, pageNumber, pageSize, sort, allRequestParams);

        } else {

            this.authorization.check(
                    PrivilegeType.READ_ONLY,
                    EntityType.EXAM,
                    institutionId);

            final int pageNum = this.paginationService.getPageNumber(pageNumber);
            final int pSize = this.paginationService.getPageSize(pageSize);

            final List<Exam> exams = new ArrayList<>(
                    this.examDAO.allMatching(new FilterMap(allRequestParams)).getOrThrow());

            if (!StringUtils.isBlank(sort)) {
                final String sortBy = SortOrder.decode(sort);
                if (sortBy.equals(QuizData.QUIZ_ATTR_NAME)) {
                    Collections.sort(exams, (exam1, exam2) -> exam1.name.compareTo(exam2.name));
                }
                if (sortBy.equals(QuizData.FILTER_ATTR_START_TIME)) {
                    Collections.sort(exams, (exam1, exam2) -> exam1.startTime.compareTo(exam2.startTime));
                }
            }

            if (SortOrder.DESCENDING == SortOrder.getSortOrder(sort)) {
                Collections.reverse(exams);
            }

            return new Page<>(
                    exams.size() / pSize,
                    pageNum,
                    sort,
                    exams.subList(pageNum * pSize, pageNum * pSize + pSize));
        }
    }

    @RequestMapping(path = "/{examId}/indicator", method = RequestMethod.GET)
    public Collection<Indicator> getIndicatorOfExam(@PathVariable final Long examId) {
        // check read-only grant on Exam
        this.examDAO.byPK(examId)
                .map(this.authorization::checkReadonly)
                .getOrThrow();

        return this.indicatorDAO.allForExam(examId)
                .getOrThrow();
    }

    @RequestMapping(path = "/{examId}/indicator/{indicatorId}", method = RequestMethod.DELETE)
    public Collection<Indicator> deleteIndicatorOfExam(
            @PathVariable final Long examId,
            @PathVariable(required = false) final Long indicatorId) {

        // check write grant on Exam
        this.examDAO.byPK(examId)
                .map(this.authorization::checkWrite)
                .getOrThrow();

        final Set<EntityKey> toDelete = (indicatorId != null)
                ? this.indicatorDAO.allForExam(examId)
                        .getOrThrow()
                        .stream()
                        .map(ind -> new EntityKey(String.valueOf(ind.id), EntityType.INDICATOR))
                        .collect(Collectors.toSet())
                : Utils.immutableSetOf(new EntityKey(String.valueOf(indicatorId), EntityType.INDICATOR));

        this.indicatorDAO.delete(toDelete);

        return this.indicatorDAO.allForExam(examId)
                .getOrThrow();
    }

    @RequestMapping(path = "/{examId}/indicator", method = RequestMethod.POST)
    public Indicator addNewIndicatorToExam(
            @PathVariable final Long examId,
            @Valid @RequestBody final Indicator indicator) {

        // check write grant on Exam
        this.examDAO.byPK(examId)
                .flatMap(this.authorization::checkWrite)
                .getOrThrow();

        if (indicator.id != null) {
            return this.indicatorDAO.byPK(indicator.id)
                    .getOrThrow();
        }

        return this.indicatorDAO
                .createNew(indicator)
                .getOrThrow();
    }

    @RequestMapping(path = "/{examId}/indicator/{id}", method = RequestMethod.PUT)
    public Indicator putIndicatorForExam(
            @PathVariable final String id,
            @Valid @RequestBody final Indicator indicator) {

        // check modify grant on Exam
        this.examDAO.byPK(indicator.examId)
                .flatMap(this.authorization::checkModify)
                .getOrThrow();

        return this.indicatorDAO
                .save(indicator)
                .getOrThrow();
    }

//  @RequestMapping(path = "/{examId}/indicator/{id}", method = RequestMethod.PATCH)
//  public Indicator patchSaveIndicatorForExam(
//          @PathVariable final Long examId,
//          @Valid @RequestBody final Indicator indicator) {
//
//      // check modify grant on Exam
//      this.examDAO.byPK(examId)
//              .map(exam -> this.authorizationGrantService.checkGrantOnEntity(exam, PrivilegeType.MODIFY))
//              .getOrThrow();
//
//      return this.indicatorDAO.save(new Indicator(
//              indicator.id,
//              examId,
//              indicator.name,
//              indicator.type,
//              indicator.defaultColor,
//              indicator.thresholds)).getOrThrow();
//  }

    @Override
    protected Exam createNew(final POSTMapper postParams) {

        final Long lmsSetupId = postParams.getLong(LMS_SETUP.ATTR_ID);
        final String quizId = postParams.getString(QuizData.QUIZ_ATTR_ID);

        final LmsAPITemplate lmsAPITemplate = this.lmsAPIService
                .getLmsAPITemplate(lmsSetupId)
                .getOrThrow();

        final QuizData quiz = lmsAPITemplate.getQuizzes(new HashSet<>(Arrays.asList(quizId)))
                .iterator()
                .next()
                .getOrThrow();

        return new Exam(null, quiz, postParams);
    }

}
