package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsLiveCycleService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.SEBRestrictionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class LmsLiveCycleServiceImpl implements LmsLiveCycleService {

    private static final Logger log = LoggerFactory.getLogger(LmsLiveCycleServiceImpl.class);

    private final SEBRestrictionService sebRestrictionService;
    private final FullLmsIntegrationService fullLmsIntegrationService;
    private final ScreenProctoringService screenProctoringService;
    private final BulkActionService bulkActionService;
    private final LmsSetupDAO lmsSetupDAO;

    public LmsLiveCycleServiceImpl(
            final SEBRestrictionService sebRestrictionService,
            final FullLmsIntegrationService fullLmsIntegrationService,
            final ScreenProctoringService screenProctoringService,
            final BulkActionService bulkActionService,
            final LmsSetupDAO lmsSetupDAO) {

        this.sebRestrictionService = sebRestrictionService;
        this.fullLmsIntegrationService = fullLmsIntegrationService;
        this.screenProctoringService = screenProctoringService;
        this.bulkActionService = bulkActionService;
        this.lmsSetupDAO = lmsSetupDAO;
    }


    @Override
    public Result<Long> processPostLmsActivation(final Long lmsSetupId) {
        return Result.tryCatch(() -> {

            // first activate LMS full integration. If fails just
            final Result<Long> activateLMSIntegration = fullLmsIntegrationService.processLmsSetupActivation(lmsSetupId);
            final Exception error = activateLMSIntegration.getError();
            if (error != null) {

                log.error("Failed to process LMS Integration activation: {}", lmsSetupId, error);
                log.info("Try to deactivate previously failed LMS Setup activation...");

                // handle rollback on error case
                final LmsSetup lmsSetup = lmsSetupDAO.byPK(lmsSetupId).getOrThrow();
                if (!lmsSetup.isActive()) {
                    // rollback the LMSSetup activation, set to inactive again
                    bulkActionService
                            .createReport(new BulkAction(
                                    API.BulkActionType.DEACTIVATE,
                                    EntityType.LMS_SETUP,
                                    new EntityName(lmsSetup.getModelId(), EntityType.LMS_SETUP, lmsSetup.name)))
                            .onError(err -> log.warn("Failed to deactivate LMS Setup for failed LMS activation: ", err));
                }

                // Notify error
                throw error;
            }

            // second apply SEB Restriction and only report errors in SEB Server logs
            sebRestrictionService
                    .processLmsSetupActivation(lmsSetupId)
                    .onError(err -> log.warn("Failed to enable SEB Restrictions on LMSSetup activation for LMSSetup: {}", lmsSetupId));

            // lastly apply SPS data and only report errors in SEB Server logs
            screenProctoringService
                    .processLmsSetupActivation(lmsSetupId)
                    .onError(err -> log.warn("Failed to enable SPS on LMSSetup activation for LMSSetup: {}", lmsSetupId));

            return lmsSetupId;
        });
    }

    @Override
    public Result<Long> processPostLmsDeactivation(final Long lmsSetupId) {
        return Result.tryCatch(() -> {

            // first try to deactivate SEB Restrictions but ignore it if not possible
            sebRestrictionService
                    .processLmsSetupDeactivation(lmsSetupId)
                    .onError(error -> log.warn("Failed to disable SEB Restriction on LMSSetup deactivation for LMSSetup: {}", lmsSetupId));

            // second process SPS update but also ignore if it as errors
            screenProctoringService
                    .processLmsSetupDeactivation(lmsSetupId)
                    .onError(error -> log.warn("Failed to disable SPS on LMSSetup deactivation for LMSSetup: {}", lmsSetupId));

            // lastly close the Full LMS Integration and if this causes error, try rollback by reactivate the LMS Setup
            final Result<Long> deactivateLMSIntegration = fullLmsIntegrationService
                    .processLmsSetupDeactivation(lmsSetupId);

            final Exception error = deactivateLMSIntegration.getError();
            if (error != null) {

                log.error("Failed to process LMS Integration deactivation: {}", lmsSetupId, error);
                log.info("Try to re-activate previously failed LMS Setup deactivation...");

                // handle rollback on error case
                final LmsSetup lmsSetup = lmsSetupDAO.byPK(lmsSetupId).getOrThrow();
                if (!lmsSetup.isActive()) {
                    // rollback the LMSSetup deactivation (set active again
                    bulkActionService
                            .createReport(new BulkAction(
                                    API.BulkActionType.ACTIVATE,
                                    EntityType.LMS_SETUP,
                                    new EntityName(lmsSetup.getModelId(), EntityType.LMS_SETUP, lmsSetup.name)))
                            .onError(err -> log.warn("Failed to re-activate LMS Setup for failed LMS deactivation: ", err));

                    // rollback SPS when possible
                    screenProctoringService
                            .processLmsSetupActivation(lmsSetupId)
                            .onError(err -> log.warn("Failed to re-activate SPS for failed LMS deactivation: ", err));

                    // rollback SEB restriction if needed
                    sebRestrictionService
                            .processLmsSetupActivation(lmsSetupId)
                            .onError(err -> log.warn("Failed to re-activate SEB Restrictions for failed LMS deactivation: ", err));
                }

                // Notify error
                throw error;
            }

            return lmsSetupId;
        });
    }
}
