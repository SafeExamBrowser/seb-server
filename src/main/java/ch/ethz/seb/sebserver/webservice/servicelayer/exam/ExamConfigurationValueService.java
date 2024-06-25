/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamConfigurationValueService {

    String CONFIG_ATTR_NAME_QUIT_LINK = "quitURL";
    String CONFIG_ATTR_NAME_QUIT_SECRET = "hashedQuitPassword";
    String CONFIG_ATTR_NAME_ALLOWED_SEB_VERSION = "sebAllowedVersions";

    /** Get the actual SEB settings attribute value for the exam configuration mapped as default configuration
     * to the given exam
     *
     * @param examId The exam identifier
     * @param configAttributeName The name of the SEB settings attribute
     * @return The current value of the above SEB settings attribute and given exam. */
    default String getMappedDefaultConfigAttributeValue(final Long examId, final String configAttributeName) {
        return getMappedDefaultConfigAttributeValue(examId, configAttributeName, null);
    }

    /** Get the actual SEB settings attribute value for the exam configuration mapped as default configuration
     * to the given exam
     *
     * @param examId The exam identifier
     * @param configAttributeName The name of the SEB settings attribute
     * @param defaultValue default value that is given back if there is no value from configuration
     * @return The current value of the above SEB settings attribute and given exam. */
    String getMappedDefaultConfigAttributeValue(
            Long examId,
            String configAttributeName,
            String defaultValue);

    /** Get the quitPassword SEB Setting from the Exam Configuration that is applied to the given exam.
     *
     * @param examId Exam identifier
     * @return the value of the quitPassword SEB Setting */
    String getQuitPassword(Long examId);

    String getQuitPasswordFromConfigTemplate(Long configTemplateId);

    /** Used to apply the quit pass given from the exam to all exam configuration for the exam.
     *
     * @param examId The exam identifier
     * @param quitPassword The quit password to set to all exam configuration of the given exam
     * @return Result to the given exam id or to an error when happened
     */
    Result<Long> applyQuitPasswordToConfigs(Long examId, String quitPassword);

    /** Used to apply the quit pass given from the exam to all exam configuration for the exam.
     *
     * @param examId The exam identifier
     * @param quitLink The quit link to set to all exam configuration of the given exam
     * @return Result to the given exam id or to an error when happened
     */
    Result<Long> applyQuitURLToConfigs(Long examId, String quitLink);

    /** Get the quitLink SEB Setting from the Exam Configuration that is applied to the given exam.
     *
     * @param examId Exam identifier
     * @return the value of the quitLink SEB Setting */
    String getQuitLink(Long examId);

    /** Get the allowedSEBVersions SEB Setting from the Exam Configuration that is applied to the given exam.
     *
     * @param examId Exam identifier
     * @return the value of the allowedSEBVersions SEB Setting */
    String getAllowedSEBVersion(Long examId);

}
