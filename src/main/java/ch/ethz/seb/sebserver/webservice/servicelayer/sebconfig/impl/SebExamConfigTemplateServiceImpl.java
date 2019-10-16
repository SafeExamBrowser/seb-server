/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.OrientationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ViewDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebExamConfigTemplateService;

@Lazy
@Service
@WebServiceProfile
public class SebExamConfigTemplateServiceImpl implements SebExamConfigTemplateService {

    private final ConfigurationNodeDAO ConfigurationNodeDAO;
    private final ConfigurationDAO configurationDAO;
    private final ViewDAO viewDAO;
    private final OrientationDAO orientationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final SebExamConfigService sebExamConfigService;

    protected SebExamConfigTemplateServiceImpl(
            final ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO configurationNodeDAO,
            final ConfigurationDAO configurationDAO, final ViewDAO viewDAO, final OrientationDAO orientationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final SebExamConfigService sebExamConfigService) {
        super();
        this.ConfigurationNodeDAO = configurationNodeDAO;
        this.configurationDAO = configurationDAO;
        this.viewDAO = viewDAO;
        this.orientationDAO = orientationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.sebExamConfigService = sebExamConfigService;
    }

    @Override
    public Result<List<TemplateAttribute>> getTemplateAttributes(
            final Long institutionId,
            final Long templateId,
            final String sort,
            final FilterMap filterMap) {

        return Result.tryCatch(() -> {
            final Map<Long, Orientation> orentiations = this.orientationDAO
                    .getAllOfTemplate(templateId)
                    .getOrThrow()
                    .stream()
                    .collect(Collectors.toMap(
                            o -> o.attributeId,
                            Function.identity()));

            final List<TemplateAttribute> attrs = this.configurationAttributeDAO
                    .getAllRootAttributes()
                    .getOrThrow()
                    .stream()
                    .map(attr -> new TemplateAttribute(institutionId, templateId, attr, orentiations.get(attr.id)))
                    .filter(attr -> attr.isNameLike(filterMap.getString(TemplateAttribute.FILTER_ATTR_NAME))
                            && attr.isGroupLike(filterMap.getString(TemplateAttribute.FILTER_ATTR_GROUP))
                            && attr.isInView(filterMap.getLong(TemplateAttribute.FILTER_ATTR_VIEW)))
                    .collect(Collectors.toList());

            if (!StringUtils.isBlank(sort)) {
                final String sortBy = PageSortOrder.decode(sort);
                final PageSortOrder sortOrder = PageSortOrder.getSortOrder(sort);
                if (sortBy.equals(Domain.CONFIGURATION_NODE.ATTR_NAME)) {
                    Collections.sort(attrs, TemplateAttribute.nameComparator(sortOrder == PageSortOrder.DESCENDING));
                }
            }
            return attrs;
        });
    }

    @Override
    public Result<TemplateAttribute> getAttribute(
            final Long institutionId,
            final Long templateId,
            final Long attributeId) {

        return Result.tryCatch(() -> {
            final ConfigurationAttribute attribute = this.configurationAttributeDAO
                    .byPK(attributeId)
                    .getOrThrow();

            final Orientation orientation = this.orientationDAO
                    .getAttributeOfTemplate(templateId, attributeId)
                    .getOr(null);

            return new TemplateAttribute(
                    institutionId,
                    templateId,
                    attribute,
                    orientation);
        });
    }

    @Override
    public Result<Set<EntityKey>> setDefaultValues(
            final Long institutionId,
            final Long templateId,
            final Long attributeId) {

        return this.configurationDAO.getFollowupConfiguration(templateId)
                .flatMap(config -> this.configurationValueDAO
                        .setDefaultValues(
                                institutionId,
                                config.id,
                                attributeId));
    }

}
