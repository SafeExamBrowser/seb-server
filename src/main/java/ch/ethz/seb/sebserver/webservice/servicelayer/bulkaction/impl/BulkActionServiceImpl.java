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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport.ErrorEntry;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionEntityException;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Service
@WebServiceProfile
public class BulkActionServiceImpl implements BulkActionService {

    private static final Logger log = LoggerFactory.getLogger(BulkActionServiceImpl.class);

    private final EnumMap<EntityType, EnumSet<EntityType>> directDependancyMap =
            new EnumMap<>(EntityType.class);

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

        this.directDependancyMap.put(EntityType.INSTITUTION, EnumSet.of(
                EntityType.LMS_SETUP,
                EntityType.SEB_CLIENT_CONFIGURATION,
                EntityType.CONFIGURATION_NODE,
                EntityType.USER));
        this.directDependancyMap.put(EntityType.LMS_SETUP, EnumSet.of(
                EntityType.EXAM));
        this.directDependancyMap.put(EntityType.EXAM, EnumSet.of(
                EntityType.EXAM_CONFIGURATION_MAP,
                EntityType.INDICATOR,
                EntityType.CLIENT_CONNECTION));
        this.directDependancyMap.put(EntityType.CONFIGURATION_NODE,
                EnumSet.of(EntityType.EXAM_CONFIGURATION_MAP));
    }

    @Override
    public void collectDependencies(final BulkAction action) {
        checkProcessing(action);
        updateDependencyTypes(action);
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

            updateDependencyTypes(action);
            collectDependencies(action);

            if (!action.dependencies.isEmpty()) {
                // process dependencies first...
                final List<BulkActionSupportDAO<?>> dependencySupporter =
                        getDependencySupporter(action);

                for (final BulkActionSupportDAO<?> support : dependencySupporter) {
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
            return new EntityProcessingReport(
                    action.sources,
                    action.result.stream()
                            .map(result -> result.getOr(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()),
                    action.result.stream()
                            .filter(Result::hasError)
                            .map(this::createErrorEntry)
                            .collect(Collectors.toList()),
                    action.type);
        });
    }

    private ErrorEntry createErrorEntry(final Result<EntityKey> bulkActionSingleResult) {
        if (!bulkActionSingleResult.hasError()) {
            return null;
        }

        final Exception error = bulkActionSingleResult.getError();

        log.error(
                "Unexpected error on bulk action processing. This error is reported to the caller: ",
                error);

        if (error instanceof BulkActionEntityException) {
            return new ErrorEntry(
                    ((BulkActionEntityException) error).key,
                    APIMessage.ErrorMessage.UNEXPECTED.of(error, error.getMessage()));

        } else {
            return new ErrorEntry(
                    null,
                    APIMessage.ErrorMessage.UNEXPECTED.of(error, error.getMessage()));
        }
    }

    private void processUserActivityLog(final BulkAction action) {
        final UserLogActivityType activityType = action.getActivityType();

        if (activityType == null) {
            return;
        }

        for (final EntityDependency dependency : action.dependencies) {

            this.userActivityLogDAO.log(
                    activityType,
                    dependency.self.entityType,
                    dependency.self.modelId,
                    "Bulk Action - Dependency : " + toLogMessage(dependency));
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

    private String toLogMessage(final EntityDependency dependency) {
        String entityAsString;
        try {
            entityAsString = this.jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dependency);
        } catch (final JsonProcessingException e) {
            entityAsString = dependency.toString();
        }
        return entityAsString;
    }

    private List<BulkActionSupportDAO<?>> getDependencySupporter(final BulkAction action) {
        switch (action.type) {
            case ACTIVATE:
            case DEACTIVATE:
            case HARD_DELETE: {
                final List<BulkActionSupportDAO<?>> dependantSupporterInHierarchicalOrder =
                        getDependantSupporterInHierarchicalOrder(action);
                Collections.reverse(dependantSupporterInHierarchicalOrder);
                return dependantSupporterInHierarchicalOrder
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(dao -> action.includeDependencies == null ||
                                action.includeDependencies.contains(dao.entityType()))
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
            case USER:
                return Arrays.asList(
                        this.supporter.get(EntityType.EXAM),
                        this.supporter.get(EntityType.INDICATOR),
                        this.supporter.get(EntityType.CLIENT_CONNECTION),
                        this.supporter.get(EntityType.CONFIGURATION_NODE),
                        this.supporter.get(EntityType.EXAM_CONFIGURATION_MAP));
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

    private void updateDependencyTypes(final BulkAction action) {
        // complete this.directDependancyMap if needed
        if (action.includeDependencies != null && !action.includeDependencies.isEmpty()) {
            this.directDependancyMap.entrySet().stream()
                    .forEach(entry -> {
                        if (action.includeDependencies.contains(entry.getKey())) {
                            action.includeDependencies.addAll(entry.getValue());
                        }
                    });
        }
    }

}
