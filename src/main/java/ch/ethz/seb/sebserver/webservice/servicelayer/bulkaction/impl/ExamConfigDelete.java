package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionExec;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationNodeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamConfigurationMapDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ExamConfigService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

@Lazy
@Component
@WebServiceProfile
public class ExamConfigDelete implements BatchActionExec {

    private final ConfigurationNodeDAO configurationNodeDAO;
    private final AuthorizationService authorizationService;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;

    public ExamConfigDelete(
            final ExamConfigService sebExamConfigService,
            final ConfigurationNodeDAO configurationNodeDAO,
            final AuthorizationService authorizationService,
            final ExamConfigurationMapDAO examConfigurationMapDAO) {

        this.configurationNodeDAO = configurationNodeDAO;
        this.authorizationService = authorizationService;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
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
        return this.configurationNodeDAO
                .byModelId(modelId)
                .flatMap(examConfig -> this.checkWriteAccess(examConfig, batchAction.ownerId))
                .flatMap(this::checkDeletionRequirements)
                .flatMap(examConfig -> this.configurationNodeDAO.delete(new HashSet<>(Arrays.asList(new EntityKey(
                        modelId,
                        EntityType.CONFIGURATION_NODE))))
                )
                .map(res -> res.stream().toList().get(0));
    }

    private Result<ConfigurationNode> checkWriteAccess(final ConfigurationNode examConfig, final String ownerId) {
        if (examConfig != null) {
            this.authorizationService.checkWrite(examConfig);
        }
        return Result.of(examConfig);
    }

    private Result<ConfigurationNode> checkDeletionRequirements(final ConfigurationNode examConfig){
        final Result<Boolean> isNotActive = this.examConfigurationMapDAO.checkNoActiveExamReferences(examConfig.id);
        if(!isNotActive.getOrThrow()){
            return Result.ofError(new APIMessageException(
                    APIMessage.ErrorMessage.INTEGRITY_VALIDATION
                            .of("Exam Configuration has active Exam references")
            ));
        }

        return Result.of(examConfig);
    }

}