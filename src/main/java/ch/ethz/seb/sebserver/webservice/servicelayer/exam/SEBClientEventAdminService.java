/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import java.io.OutputStream;
import java.util.Collection;

import org.springframework.scheduling.annotation.Async;

import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface SEBClientEventAdminService {

    Result<EntityProcessingReport> deleteAllClientEvents(Collection<String> ids);

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    void exportSEBClientLogs(
            OutputStream output,
            FilterMap filterMap,
            String sort,
            ExportType exportType,
            boolean includeConnectionDetails,
            boolean includeExamDetails,
            final SEBServerUser currentUser);

}
