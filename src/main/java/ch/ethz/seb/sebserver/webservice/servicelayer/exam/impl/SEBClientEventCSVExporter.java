/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.io.OutputStream;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventExporter;

public class SEBClientEventCSVExporter implements SEBClientEventExporter {

    @Override
    public ExportType exportType() {
        return ExportType.CSV;
    }

    @Override
    public void streamHeader(
            final OutputStream output,
            final boolean includeConnectionDetails,
            final boolean includeExamDetails) {

        // TODO
    }

    @Override
    public void streamData(
            final OutputStream output,
            final ClientEventRecord eventData,
            final ClientConnectionRecord connectionData,
            final Exam examData) {

        // TODO
    }

    private String toCSVString(final String text) {

    }

}
