/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.SEBServerRestEndpoints;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationGrantService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + SEBServerRestEndpoints.ENDPOINT_QUIZ_IMPORT)
public class QuizImportController {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final LmsAPIService lmsAPIService;
    private final AuthorizationGrantService authorizationGrantService;

    public QuizImportController(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize,
            final LmsAPIService lmsAPIService,
            final AuthorizationGrantService authorizationGrantService) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.lmsAPIService = lmsAPIService;
        this.authorizationGrantService = authorizationGrantService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<QuizData> search(
            @RequestParam(name = LMS_SETUP.ATTR_ID, required = true) final Long lmsSetupId,
            @RequestParam(name = QuizData.FILTER_ATTR_NAME, required = false) final String nameLike,
            @RequestParam(name = QuizData.FILTER_ATTR_START_TIME, required = false) final String startTime,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort) {

        final LmsAPITemplate lmsAPITemplate = this.lmsAPIService
                .createLmsAPITemplate(lmsSetupId)
                .getOrThrow();

        this.authorizationGrantService.checkPrivilege(
                EntityType.EXAM,
                PrivilegeType.READ_ONLY,
                lmsAPITemplate.lmsSetup().institutionId);

        return lmsAPITemplate.getQuizzes(
                nameLike,
                Utils.dateTimeStringToTimestamp(startTime, null),
                sort,
                (pageNumber != null)
                        ? pageNumber
                        : 1,
                (pageSize != null)
                        ? (pageSize <= this.maxPageSize)
                                ? pageSize
                                : this.maxPageSize
                        : this.defaultPageSize)
                .getOrThrow();
    }

}
