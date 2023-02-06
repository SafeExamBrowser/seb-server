/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

public interface ExamConfigurationValueService {

    public static final String CONFIG_ATTR_NAME_QUIT_LINK = "quitURL";
    public static final String CONFIG_ATTR_NAME_QUIT_SECRET = "hashedQuitPassword";

    /** Get the actual SEB settings attribute value for the exam configuration mapped as default configuration
     * to the given exam
     *
     * @param examId The exam identifier
     * @param configAttributeName The name of the SEB settings attribute
     * @return The current value of the above SEB settings attribute and given exam. */
    String getMappedDefaultConfigAttributeValue(Long examId, String configAttributeName);

    /** Get the quitPassword SEB Setting from the Exam Configuration that is applied to the given exam.
     *
     * @param examId Exam identifier
     * @return the vlaue of the quitPassword SEB Setting */
    String getQuitSecret(Long examId);

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
