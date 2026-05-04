package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import ch.ethz.seb.sebserver.gbl.util.Result;

public interface LmsLiveCycleService {

    /** Called after an LMS Setup has been activated and applies all post-processing for the LMS Setup activation
     * in the right order as well as deals with error handling and rollback when something goes wrong on post-processing
     * @param lmsSetupId the LMS Setup Identifier
     * @return Result refer to the LMS Setup Identifier or to an error when happened. */
    Result<Long> processPostLmsActivation(Long lmsSetupId);

    /** Called after an LMS Setup has been deactivated and applies all post-processing for the LMS Setup deactivation
     * in the right order as well as deals with error handling and rollback when something goes wrong on post-processing
     * @param lmsSetupId the LMS Setup Identifier
     * @return Result refer to the LMS Setup Identifier or to an error when happened. */
    Result<Long> processPostLmsDeactivation(Long lmsSetupId);

}
