/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.batch;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;

@Component
@DisallowConcurrentExecution
public class SimpleBatchJob implements Job {

    private final ClientConnectionDAO clientConnectionDAO;

    public SimpleBatchJob(final ClientConnectionDAO clientConnectionDAO) {
        this.clientConnectionDAO = clientConnectionDAO;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        System.out.print("*********************** " + this.clientConnectionDAO);
    }

}
