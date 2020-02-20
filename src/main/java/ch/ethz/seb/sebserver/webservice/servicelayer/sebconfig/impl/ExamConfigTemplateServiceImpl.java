/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.OrientationDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ViewDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigTemplateService;

@Lazy
@Service
@WebServiceProfile
public class ExamConfigTemplateServiceImpl implements ExamConfigTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ExamConfigTemplateServiceImpl.class);

    private final ViewDAO viewDAO;
    private final ConfigurationDAO configurationDAO;
    private final OrientationDAO orientationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;

    protected ExamConfigTemplateServiceImpl(
            final ViewDAO viewDAO,
            final ConfigurationDAO configurationDAO,
            final OrientationDAO orientationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO) {

        this.viewDAO = viewDAO;
        this.configurationDAO = configurationDAO;
        this.orientationDAO = orientationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
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
                            && attr.isInView(filterMap.getLong(TemplateAttribute.FILTER_ATTR_VIEW))
                            && attr.hasType(extractTypes(filterMap)))
                    .collect(Collectors.toList());

            if (!StringUtils.isBlank(sort)) {
                final String sortBy = PageSortOrder.decode(sort);
                final PageSortOrder sortOrder = PageSortOrder.getSortOrder(sort);
                switch (sortBy) {
                    case Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME:
                        attrs.sort(TemplateAttribute.nameComparator(sortOrder == PageSortOrder.DESCENDING));
                        break;
                    case Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE:
                        attrs.sort(TemplateAttribute.typeComparator(sortOrder == PageSortOrder.DESCENDING));
                        break;
                    case Domain.ORIENTATION.ATTR_VIEW_ID:
                        attrs.sort(this.getViewComparator(
                                institutionId,
                                templateId,
                                sortOrder == PageSortOrder.DESCENDING));
                        break;
                    case Domain.ORIENTATION.ATTR_GROUP_ID:
                        attrs.sort(TemplateAttribute.groupComparator(sortOrder == PageSortOrder.DESCENDING));
                        break;
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
    public Result<TemplateAttribute> setDefaultValues(
            final Long institutionId,
            final Long templateId,
            final Long attributeId) {

        return this.configurationDAO.getFollowupConfiguration(templateId)
                .flatMap(config -> this.configurationValueDAO
                        .setDefaultValues(
                                institutionId,
                                config.id,
                                attributeId))
                .flatMap(vals -> getAttribute(
                        institutionId,
                        templateId,
                        attributeId));
    }

    @Override
    public Result<TemplateAttribute> removeOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId) {

        return Result.tryCatch(() -> {
            final Orientation orientation = getOrientation(templateId, attributeId);

            this.orientationDAO.delete(new HashSet<>(Arrays.asList(orientation.getEntityKey())))
                    .getOrThrow();

            final TemplateAttribute attribute = getAttribute(institutionId, templateId, attributeId)
                    .getOrThrow();

            if (attribute.getOrientation() != null) {
                throw new IllegalStateException(
                        "Failed to remove Orientation, expecting no Orientatoin for attribute: " + attribute);
            }

            return attribute;
        });
    }

    @Override
    public Result<TemplateAttribute> attachDefaultOrientation(
            final Long institutionId,
            final Long templateId,
            final Long attributeId,
            final Long viewId) {

        return Result.tryCatch(() -> {
            final Orientation orientation = getOrientation(templateId, attributeId);
            final Orientation devOrientation = getOrientation(ConfigurationNode.DEFAULT_TEMPLATE_ID, attributeId);

            if (orientation != null) {
                this.orientationDAO.delete(new HashSet<>(Arrays.asList(orientation.getEntityKey())))
                        .getOrThrow();
            }

            final Long _viewId;
            if (viewId == null) {
                _viewId = this.viewDAO.getDefaultViewForTemplate(templateId, devOrientation.viewId)
                        .getOrThrow().id;
            } else {
                _viewId = viewId;
            }

            final Orientation newOrientation = new Orientation(
                    null,
                    attributeId,
                    templateId,
                    _viewId,
                    devOrientation.groupId,
                    devOrientation.xPosition,
                    devOrientation.yPosition,
                    devOrientation.width,
                    devOrientation.height,
                    devOrientation.title);

            this.orientationDAO.createNew(newOrientation)
                    .getOrThrow();

            final TemplateAttribute attribute = getAttribute(institutionId, templateId, attributeId)
                    .getOrThrow();

            if (attribute.getOrientation() == null) {
                throw new IllegalStateException(
                        "Failed to attach default Orientation, expecting Orientatoin for attribute: " + attribute);
            }

            return attribute;
        });
    }

    private Orientation getOrientation(final Long templateId, final Long attributeId) {
        final FilterMap filterMap = new FilterMap.Builder()
                .put(Orientation.FILTER_ATTR_TEMPLATE_ID, String.valueOf(templateId))
                .put(Orientation.FILTER_ATTR_ATTRIBUTE_ID, String.valueOf(attributeId))
                .create();

        return this.orientationDAO.allMatching(filterMap)
                .get(error -> {
                    log.warn("Unexpecrted error while get Orientation: ", error);
                    return Collections.emptyList();
                })
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Comparator<TemplateAttribute> getViewComparator(
            final Long institutionId,
            final Long templateId,
            final boolean descending) {

        final Map<Long, View> viewMap = this.viewDAO.allMatching(new FilterMap.Builder()
                .add(View.FILTER_ATTR_INSTITUTION, String.valueOf(institutionId))
                .add(View.FILTER_ATTR_TEMPLATE, String.valueOf(templateId))
                .create())
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(v -> v.id, Function.identity()));

        return (attr1, attr2) -> getViewName(attr1, viewMap)
                .compareToIgnoreCase(getViewName(attr2, viewMap))
                * ((descending) ? -1 : 1);
    }

    private EnumSet<AttributeType> extractTypes(final FilterMap filterMap) {
        final EnumSet<AttributeType> result = EnumSet.noneOf(AttributeType.class);
        final String types = filterMap.getString(TemplateAttribute.FILTER_ATTR_TYPE);
        if (StringUtils.isBlank(types)) {
            return result;
        }

        final String[] split = StringUtils.split(types, Constants.LIST_SEPARATOR);
        if (split != null) {
            for (int i = 0; i < split.length; i++) {
                result.add(AttributeType.valueOf(split[i]));
            }
        }

        return result;
    }

    private static String getViewName(
            final TemplateAttribute attribute,
            final Map<Long, View> viewMap) {

        final Orientation orientation = attribute.getOrientation();
        if (orientation == null || orientation.viewId == null) {
            return Constants.EMPTY_NOTE;
        }

        final View view = viewMap.get(orientation.viewId);
        if (view != null) {
            return view.name;
        }

        return Constants.EMPTY_NOTE;
    }

}
