/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ExamConfigUpdateService {

    Logger log = LoggerFactory.getLogger(ExamConfigUpdateService.class);

    Result<Collection<Long>> processSEBExamConfigurationChange(Long configurationNodeId);

    @Transactional
    default Result<Configuration> processSEBExamConfigurationChange(final Configuration config) {
        if (config == null) {
            return Result.ofError(new NullPointerException("Configuration has null reference"));
        }

        return Result.tryCatch(() -> {
            processSEBExamConfigurationChange(config.configurationNodeId)
                    .map(ids -> {
                        log.info("Successfully updated SEB Configuration for exams: {}", ids);
                        return ids;
                    })
                    .getOrThrow();
            return config;
        });
    }

    void forceReleaseUpdateLocks(Long configurationId);

    Collection<Result<Long>> forceReleaseUpdateLocks(Collection<Long> examIds);

    Result<Collection<Long>> checkRunningExamIntegrity(final Long configurationNodeId);

}
