/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;

@WebServiceProfile
@RestController
@RequestMapping("/${sebserver.webservice.api.admin.endpoint}" + API.QUIZ_DISCOVERY_ENDPOINT)
public class QuizImportController {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final LmsAPIService lmsAPIService;
    private final AuthorizationService authorization;

    public QuizImportController(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize,
            final LmsAPIService lmsAPIService,
            final AuthorizationService authorization) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.lmsAPIService = lmsAPIService;
        this.authorization = authorization;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Page<QuizData> getQuizPage(
            @RequestParam(
                    name = Entity.FILTER_ATTR_INSTITUTION,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @RequestParam(name = Page.ATTR_PAGE_NUMBER, required = false) final Integer pageNumber,
            @RequestParam(name = Page.ATTR_PAGE_SIZE, required = false) final Integer pageSize,
            @RequestParam(name = Page.ATTR_SORT, required = false) final String sort,
            @RequestParam final MultiValueMap<String, String> allRequestParams) {

        this.authorization.check(
                PrivilegeType.READ_ONLY,
                EntityType.EXAM,
                institutionId);

        final FilterMap filterMap = new FilterMap(allRequestParams);
        filterMap.putIfAbsent(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(institutionId));

        return this.lmsAPIService.requestQuizDataPage(
                (pageNumber != null)
                        ? pageNumber
                        : 1,
                (pageSize != null)
                        ? (pageSize <= this.maxPageSize)
                                ? pageSize
                                : this.maxPageSize
                        : this.defaultPageSize,
                sort,
                filterMap)
                .getOrThrow();
    }

}
