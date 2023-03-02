/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;

@Lazy
@Component
@WebServiceProfile
public class DeleteExamConfig implements BatchActionExec {

    private final ConfigurationNodeDAO configurationNodeDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final AuthorizationService authorization;
    private final UserActivityLogDAO userActivityLogDAO;

    public DeleteExamConfig(
            final ConfigurationNodeDAO configurationNodeDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final AuthorizationService authorization,
            final UserActivityLogDAO userActivityLogDAO) {

        this.configurationNodeDAO = configurationNodeDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.authorization = authorization;
        this.userActivityLogDAO = userActivityLogDAO;
    }

    @Override
    public BatchActionType actionType() {
        return BatchActionType.EXAM_CONFIG_DELETE;
    }

    @Override
    public APIMessage checkConsistency(final Map<String, String> actionAttributes) {
        // no additional check here
        return null;
    }

    @Override
    public Result<EntityKey> doSingleAction(final String modelId, final BatchAction batchAction) {
        // TODO Auto-generated method stub
        return null;
    }

}
