/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;

@Lazy
@Component
@WebServiceProfile
public class ExamConfigResetToTemplate implements BatchActionExec {

    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigService sebExamConfigService;
    private final ExamConfigUpdateService examConfigUpdateService;
    private final AuthorizationService authorizationService;

    public ExamConfigResetToTemplate(
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigService sebExamConfigService,
            final ExamConfigUpdateService examConfigUpdateService,
            final AuthorizationService authorizationService) {

        this.configurationNodeDAO = configurationNodeDAO;
        this.sebExamConfigService = sebExamConfigService;
        this.examConfigUpdateService = examConfigUpdateService;
        this.authorizationService = authorizationService;
    }

    @Override
    public BatchActionType actionType() {
        return BatchActionType.EXAM_CONFIG_REST_TEMPLATE_SETTINGS;
    }

    @Override
    public APIMessage checkConsistency(final Map<String, String> actionAttributes) {
        // no additional check here
        return null;
    }

    @Override
    public Result<EntityKey> doSingleAction(final String modelId, final BatchAction batchAction) {

        return this.configurationNodeDAO
                .byModelId(modelId)
                .flatMap(node -> this.authorizationService.check(PrivilegeType.MODIFY, node))
                .map(this::checkConsistency)
                .flatMap(this.sebExamConfigService::resetToTemplateSettings)
                .map(node -> {
                    this.examConfigUpdateService
                            .processExamConfigurationChange(node.id)
                            .getOrThrow();
                    return node;
                })
                .map(Entity::getEntityKey);

    }

    private ConfigurationNode checkConsistency(final ConfigurationNode configurationNode) {
        return configurationNode;
    }

}
