/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamTemplateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.UserService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.EXAM_TEMPLATE_ENDPOINT)
public class ExamTemplateController extends EntityController<ExamTemplate, ExamTemplate> {

    private static final Logger log = LoggerFactory.getLogger(ExamTemplateController.class);

    private final JSONMapper jsonMapper;

    protected ExamTemplateController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<ExamTemplate, ExamTemplate> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final JSONMapper jsonMapper) {

        super(authorization, bulkActionService, entityDAO, userActivityLogDAO, paginationService,
                beanValidationService);
        this.jsonMapper = jsonMapper;
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Indicator> getIndicatorPage(
            @PathVariable final String modelId,
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

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM_TEMPLATE,
                institutionId);

        final ExamTemplate examTemplate = super.entityDAO.byModelId(modelId)
                .getOrThrow();

        return this.paginationService.buildPageFromList(
                pageNumber,
                pageSize,
                sort,
                examTemplate.indicatorTemplates,
                pageSort(sort));
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Indicator getIndicatorBy(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        this.authorization.check(
                PrivilegeType.READ,
                EntityType.EXAM_TEMPLATE,
                institutionId);

        final ExamTemplate examTemplate = super.entityDAO
                .byModelId(parentModelId)
                .getOrThrow();

        return examTemplate.indicatorTemplates
                .stream()
                .filter(i -> modelId.equals(i.getModelId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(EntityType.INDICATOR, parentModelId));
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Indicator createIndicator(
            @PathVariable final String parentModelId,
            @RequestParam final MultiValueMap<String, String> allRequestParams,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            final HttpServletRequest request) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);

        final ExamTemplate examTemplate = super.entityDAO
                .byModelId(parentModelId)
                .getOrThrow();

        final POSTMapper postMap = new POSTMapper(allRequestParams, request.getQueryString())
                .putIfAbsent(API.PARAM_INSTITUTION_ID, String.valueOf(institutionId));

        final Indicator newIndicator = new Indicator(
                (long) examTemplate.getIndicatorTemplates().size(),
                Long.parseLong(parentModelId),
                postMap);

        final ArrayList<Indicator> indicators = new ArrayList<>(examTemplate.indicatorTemplates);
        indicators.add(newIndicator);
        final ExamTemplate newExamTemplate = new ExamTemplate(
                examTemplate.id,
                null, null, null, null, null, null,
                indicators,
                null);

        super.entityDAO
                .save(newExamTemplate)
                .getOrThrow();

        return newIndicator;
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Indicator saveIndicatorPut(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId,
            @Valid @RequestBody final Indicator modifyData) {

        // check modify privilege for requested institution and concrete entityType
        this.checkModifyPrivilege(institutionId);

        final ExamTemplate examTemplate = super.entityDAO
                .byModelId(parentModelId)
                .getOrThrow();

        final List<Indicator> newIndicators = examTemplate.indicatorTemplates
                .stream()
                .map(i -> {
                    if (modelId.equals(i.getModelId())) {
                        return modifyData;
                    } else {
                        return i;
                    }
                })
                .collect(Collectors.toList());

        final ExamTemplate newExamTemplate = new ExamTemplate(
                examTemplate.id,
                null, null, null, null, null, null,
                newIndicators,
                null);

        super.entityDAO
                .save(newExamTemplate)
                .getOrThrow();

        return modifyData;
    }

    @RequestMapping(
            path = API.PARENT_MODEL_ID_VAR_PATH_SEGMENT
                    + API.EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT
                    + API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityKey deleteIndicator(
            @PathVariable final String parentModelId,
            @PathVariable final String modelId,
            @RequestParam(
                    name = API.PARAM_INSTITUTION_ID,
                    required = true,
                    defaultValue = UserService.USERS_INSTITUTION_AS_DEFAULT) final Long institutionId) {

        // check write privilege for requested institution and concrete entityType
        this.checkWritePrivilege(institutionId);

        final ExamTemplate examTemplate = super.entityDAO
                .byModelId(parentModelId)
                .getOrThrow();

        final List<Indicator> newIndicators = examTemplate.indicatorTemplates
                .stream()
                .filter(i -> !modelId.equals(i.getModelId()))
                .collect(Collectors.toList());

        final ExamTemplate newExamTemplate = new ExamTemplate(
                examTemplate.id,
                null, null, null, null, null, null,
                newIndicators,
                null);

        super.entityDAO
                .save(newExamTemplate)
                .getOrThrow();

        return new EntityKey(modelId, EntityType.INDICATOR);
    }

    @Override
    protected ExamTemplate createNew(final POSTMapper postParams) {
        final Long institutionId = postParams.getLong(API.PARAM_INSTITUTION_ID);
        final String attributesJson = postParams.getString(Domain.EXAM_TEMPLATE.ATTR_EXAM_ATTRIBUTES);
        Map<String, String> examAttributes;
        try {
            examAttributes = (StringUtils.isNotBlank(attributesJson))
                    ? this.jsonMapper.readValue(attributesJson, new TypeReference<Map<String, String>>() {
                    })
                    : null;
            return new ExamTemplate(institutionId, examAttributes, postParams);

        } catch (final JsonProcessingException e) {
            log.error("Failed to parse exam template attributes: ", e);
            return new ExamTemplate(institutionId, null, postParams);
        }
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return ExamTemplateRecordDynamicSqlSupport.examTemplateRecord;
    }

    static Function<Collection<Indicator>, List<Indicator>> pageSort(final String sort) {

        final String sortBy = PageSortOrder.decode(sort);
        return indicators -> {
            final List<Indicator> list = indicators.stream().collect(Collectors.toList());
            if (StringUtils.isBlank(sort)) {
                return list;
            }

            if (sortBy.equals(Indicator.FILTER_ATTR_NAME)) {
                list.sort(Comparator.comparing(indicator -> indicator.name));
            }

            if (PageSortOrder.DESCENDING == PageSortOrder.getSortOrder(sort)) {
                Collections.reverse(list);
            }
            return list;
        };
    }

}
