/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Service
@WebServiceProfile
public class BulkActionServiceImpl implements BulkActionService {

    private final Map<EntityType, BulkActionSupportDAO<?>> supporter;
    private final UserActivityLogDAO userActivityLogDAO;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JSONMapper jsonMapper;

    public BulkActionServiceImpl(
            final Collection<BulkActionSupportDAO<?>> supporter,
            final UserActivityLogDAO userActivityLogDAO,
            final ApplicationEventPublisher applicationEventPublisher,
            final JSONMapper jsonMapper) {

        this.supporter = new HashMap<>();
        for (final BulkActionSupportDAO<?> support : supporter) {
            this.supporter.put(support.entityType(), support);
        }
        this.userActivityLogDAO = userActivityLogDAO;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void collectDependencies(final BulkAction action) {
        checkProcessing(action);
        for (final BulkActionSupportDAO<?> sup : this.supporter.values()) {
            action.dependencies.addAll(sup.getDependencies(action));
        }
        action.alreadyProcessed = true;
    }

    @Override

    public Result<BulkAction> doBulkAction(final BulkAction action) {
        return Result.tryCatch(() -> {

            checkProcessing(action);

            final BulkActionSupportDAO<?> supportForSource = this.supporter
                    .get(action.sourceType);
            if (supportForSource == null) {
                action.alreadyProcessed = true;
                throw new IllegalArgumentException("No bulk action support for: " + action);
            }

            collectDependencies(action);

            if (!action.dependencies.isEmpty()) {
                // process dependencies first...
                final List<BulkActionSupportDAO<?>> dependancySupporter =
                        getDependancySupporter(action);

                for (final BulkActionSupportDAO<?> support : dependancySupporter) {
                    action.result.addAll(support.processBulkAction(action));
                }
            }

            action.result.addAll(supportForSource.processBulkAction(action));

            processUserActivityLog(action);
            action.alreadyProcessed = true;

            this.applicationEventPublisher.publishEvent(new BulkActionEvent(action));

            return action;
        });
    }

    @Override
    public Result<EntityProcessingReport> createReport(final BulkAction action) {
        if (!action.alreadyProcessed) {
            return doBulkAction(action)
                    .flatMap(this::createFullReport);
        } else {
            return createFullReport(action);
        }
    }

    private Result<EntityProcessingReport> createFullReport(final BulkAction action) {
        return Result.tryCatch(() -> {

            // TODO
            return new EntityProcessingReport(
                    action.sources,
                    Collections.emptyList(),
                    Collections.emptyList());
        });
    }

    private void processUserActivityLog(final BulkAction action) {
        final UserLogActivityType activityType = action.getActivityType();

        if (activityType == null) {
            return;
        }

        for (final EntityKey key : action.dependencies) {

            this.userActivityLogDAO.log(
                    activityType,
                    key.entityType,
                    key.modelId,
                    "Bulk Action - Dependency : " + toLogMessage(key));
        }

        for (final EntityKey key : action.sources) {
            this.userActivityLogDAO.log(
                    activityType,
                    key.entityType,
                    key.modelId,
                    "Bulk Action - Source : " + toLogMessage(key));
        }
    }

    private String toLogMessage(final EntityKey key) {
        String entityAsString;
        try {
            entityAsString = this.jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(key);
        } catch (final JsonProcessingException e) {
            entityAsString = key.toString();
        }
        return entityAsString;
    }

    private List<BulkActionSupportDAO<?>> getDependancySupporter(final BulkAction action) {
        switch (action.type) {
            case ACTIVATE:
            case DEACTIVATE:
            case HARD_DELETE: {
                final List<BulkActionSupportDAO<?>> dependantSupporterInHierarchicalOrder =
                        getDependantSupporterInHierarchicalOrder(action);
                Collections.reverse(dependantSupporterInHierarchicalOrder);
                return dependantSupporterInHierarchicalOrder
                        .stream()
                        .filter(v -> v != null)
                        .collect(Collectors.toList());
            }
            default:
                return getDependantSupporterInHierarchicalOrder(action);
        }
    }

    private List<BulkActionSupportDAO<?>> getDependantSupporterInHierarchicalOrder(final BulkAction action) {
        switch (action.sourceType) {
            case INSTITUTION:
                return Arrays.asList(
                        this.supporter.get(EntityType.LMS_SETUP),
                        this.supporter.get(EntityType.USER),
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.INDICATOR),
                        this.supporter.get(EntityType.SEB_CLIENT_CONFIGURATION),
                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP),
                        this.supporter.get(EntityType.CLIENT_CONNECTION),
                        this.supporter.get(EntityType.CONFIGURATION_NODE));
//            case USER:
//                return Arrays.asList(
//                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP),
//                        this.supporter.get(EntityType.EXAM),
//                        this.supporter.get(EntityType.INDICATOR),
//                        this.supporter.get(EntityType.CLIENT_CONNECTION),
//                        this.supporter.get(EntityType.CONFIGURATION_NODE));
            case LMS_SETUP:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.INDICATOR),
                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP),
                        this.supporter.get(EntityType.CLIENT_CONNECTION));
            case EXAM:
                return Arrays.asList(
                        this.supporter.get(EntityType.INDICATOR),
                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP),
                        this.supporter.get(EntityType.CLIENT_CONNECTION));
            case CONFIGURATION_NODE:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP));
            default:
                return Collections.emptyList();
        }
    }

    private void checkProcessing(final BulkAction action) {
        if (action.alreadyProcessed) {
            throw new IllegalStateException("Given BulkAction has already been processed. Use a new one");
        }
    }

}
