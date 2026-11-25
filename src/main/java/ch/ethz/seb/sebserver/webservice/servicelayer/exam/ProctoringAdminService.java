/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import java.util.Arrays;
import java.util.EnumSet;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ProctoringAdminService {

    Logger log = LoggerFactory.getLogger(ProctoringAdminService.class);

    EnumSet<EntityType> SUPPORTED_PARENT_ENTITES = EnumSet.of(
            EntityType.EXAM,
            EntityType.EXAM_TEMPLATE);

    /** Get screen proctoring service settings for a certain entity (SUPPORTED_PARENT_ENTITES).
     *
     * @param parentEntityKey the entity key of the parent entity to get the screen proctoring service settings from
     * @return Result refer to proctoring service settings or to an error when happened. */
    Result<ScreenProctoringSettings> getScreenProctoringSettings(EntityKey parentEntityKey);

    /** Save the given screen proctoring service settings for a certain entity (SUPPORTED_PARENT_ENTITES).
     *
     * @param parentEntityKey the entity key of the parent entity to save the screen proctoring service settings to
     * @param screenProctoringSettings The screen proctoring service settings to save
     * @return Result refer to saved screen proctoring service settings or to an error when happened. */
    Result<ScreenProctoringSettings> saveScreenProctoringSettings(
            EntityKey parentEntityKey,
            ScreenProctoringSettings screenProctoringSettings);

    /** Gets invoked after an exam has been changed and saved.
     *
     * @param exam the exam that has been changed and saved */
    void notifyExamSaved(Exam exam);

}
