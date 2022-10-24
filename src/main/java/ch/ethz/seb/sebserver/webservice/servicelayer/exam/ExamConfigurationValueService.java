/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

public interface ExamConfigurationValueService {

    /** Get the actual SEB settings attribute value for the exam configuration mapped as default configuration
     * to the given exam
     *
     * @param examId The exam identifier
     * @param configAttributeName The name of the SEB settings attribute
     * @return The current value of the above SEB settings attribute and given exam. */
    String getMappedDefaultConfigAttributeValue(Long examId, String configAttributeName);

}
