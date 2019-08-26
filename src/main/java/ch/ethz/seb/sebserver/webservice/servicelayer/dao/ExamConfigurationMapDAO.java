/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

public interface ExamConfigurationMapDAO extends
        EntityDAO<ExamConfigurationMap, ExamConfigurationMap>,
        BulkActionSupportDAO<ExamConfigurationMap> {

    /** Get a specific ExamConfigurationMap by the mapping identifiers
     *
     * @param examId The Exam mapping identifier
     * @param configurationNodeId the ConfigurationNode mapping identifier
     * @return Result refer to the ExamConfigurationMap with specified mapping or to an exception if happened */
    public Result<ExamConfigurationMap> byMapping(Long examId, Long configurationNodeId);

    /** Get the password cipher of a specific ExamConfigurationMap by the mapping identifiers
     *
     * @param examId The Exam mapping identifier
     * @param configurationNodeId the ConfigurationNode mapping identifier
     * @return Result refer to the password cipher of specified mapping or to an exception if happened */
    public Result<CharSequence> getConfigPasswortCipher(Long examId, Long configurationNodeId);

    /** Get the ConfigurationNode identifier of the default Exam Configuration of
     * the Exam with specified identifier.
     *
     * @param examId The Exam identifier
     * @return ConfigurationNode identifier of the default Exam Configuration of
     *         the Exam with specified identifier */
    public Result<Long> getDefaultConfigurationForExam(Long examId);

    /** Get the ConfigurationNode identifier of the Exam Configuration of
     * the Exam for a specified user identifier.
     *
     * @param examId The Exam identifier
     * @param userId the user identifier
     * @return ConfigurationNode identifier of the Exam Configuration of
     *         the Exam for a specified user identifier */
    public Result<Long> getUserConfigurationIdForExam(final Long examId, final String userId);

}
