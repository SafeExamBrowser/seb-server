/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import java.util.EnumSet;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;

public interface ProctoringAdminService {

    EnumSet<EntityType> SUPPORTED_PARENT_ENTITES = EnumSet.of(
            EntityType.EXAM,
            EntityType.EXAM_TEMPLATE);

    /** Get proctoring service settings for a certain entity (SUPPORTED_PARENT_ENTITES).
     *
     * @param parentEntityKey the entity key of the parent entity to get the proctoring service settings from
     * @return Result refer to proctoring service settings or to an error when happened. */
    Result<ProctoringServiceSettings> getProctoringSettings(EntityKey parentEntityKey);

    /** Save the given proctoring service settings for a certain entity (SUPPORTED_PARENT_ENTITES).
     *
     * @param parentEntityKey the entity key of the parent entity to save the proctoring service settings to
     * @param proctoringServiceSettings The proctoring service settings to save
     * @return Result refer to saved proctoring service settings or to an error when happened. */
    Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            EntityKey parentEntityKey,
            ProctoringServiceSettings proctoringServiceSettings);

    /** Get the exam proctoring service implementation of specified type.
     *
     * @param type exam proctoring service server type
     * @return ExamProctoringService instance */
    Result<ExamProctoringService> getExamProctoringService(final ProctoringServerType type);

    /** Use this to test the proctoring service settings against the remote proctoring server.
     *
     * @param proctoringSettings the settings to test
     * @return Result refer to true if the settings are correct and the proctoring server can be accessed. */
    default Result<Boolean> testExamProctoring(final ProctoringServiceSettings proctoringSettings) {
        return getExamProctoringService(proctoringSettings.serverType)
                .flatMap(service -> service.testExamProctoring(proctoringSettings));
    }

}
