/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.util.Collection;

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
    Result<ExamConfigurationMap> byMapping(Long examId, Long configurationNodeId);

    /** Get the password cipher of a specific ExamConfigurationMap by the mapping identifiers
     *
     * @param examId The Exam mapping identifier
     * @param configurationNodeId the ConfigurationNode mapping identifier
     * @return Result refer to the password cipher of specified mapping or to an exception if happened */
    Result<CharSequence> getConfigPasswordCipher(Long examId, Long configurationNodeId);

    /** Get the ConfigurationNode identifier of the default Exam Configuration of
     * the Exam with specified identifier.
     *
     * @param examId The Exam identifier
     * @return ConfigurationNode identifier of the default Exam Configuration of
     *         the Exam with specified identifier */
    Result<Long> getDefaultConfigurationNode(Long examId);

    /** Get the ConfigurationNode identifier of the Exam Configuration of
     * the Exam for a specified user identifier.
     *
     * @param examId The Exam identifier
     * @param userId the user identifier
     * @return ConfigurationNode identifier of the Exam Configuration of
     *         the Exam for a specified user identifier */
    Result<Long> getUserConfigurationNodeId(final Long examId, final String userId);

    /** Get a list of all ConfigurationNode identifiers of configurations that currently are attached to a given Exam
     *
     * @param examId the Exam identifier
     * @return Result refers to a list of ConfigurationNode identifiers or refer to an error if happened */
    Result<Collection<Long>> getConfigurationNodeIds(Long examId);

    /** Get all id of Exams that has a relation to the given configuration id.
     *
     * @param configurationNodeId the configuration node identifier (PK)
     * @return Result referencing the List of exam identifiers (PK) for a given configuration node identifier */
    Result<Collection<Long>> getExamIdsForConfigNodeId(Long configurationNodeId);

    /** Get all id of Exams that has a relation to the given configuration id.
     *
     * @param configurationId the configuration identifier
     * @return Result referencing the List of exam identifiers (PK) for a given configuration identifier */
    Result<Collection<Long>> getExamIdsForConfigId(Long configurationId);

    /** Use this to check if a specified exam configuration don't have any relations
     * to an currently active exam, meaning in upcoming or running state.
     * Exams in finished state are not active and will not go into account here.
     *
     * @param configurationNodeId the identifier of exam configuration to check
     * @return Result refer to true if config has no relations to active exams, or false of it has or refer to an error
     *         if happened */
    Result<Boolean> checkNoActiveExamReferences(Long configurationNodeId);

}
