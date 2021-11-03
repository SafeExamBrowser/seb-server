/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventExporter;

public class SEBClientEventCSVExporter implements SEBClientEventExporter {

    private static final Logger log = LoggerFactory.getLogger(SEBClientEventCSVExporter.class);

    @Override
    public ExportType exportType() {
        return ExportType.CSV;
    }

    @Override
    public void streamHeader(
            final OutputStream output,
            final boolean includeConnectionDetails,
            final boolean includeExamDetails) {

        final StringBuilder builder = new StringBuilder();
        builder.append("Event Type,Message,Value,Client Time (UTC),Server Time (UTC)");

        if (includeConnectionDetails) {
            builder.append(",User Session-ID,Client Machine,Connection Status,Connection Token");
        }

        if (includeExamDetails) {
            builder.append("Exam Name,Exam Description,Exam Type,Start Time (LMS),End Time (LMS)");
        }

        builder.append(Constants.CARRIAGE_RETURN);

        try {
            output.write(Utils.toByteArray(builder));
        } catch (final IOException e) {
            log.error("Failed to stream header: ", e);
        }
    }

    @Override
    public void streamData(
            final OutputStream output,
            final ClientEventRecord eventData,
            final ClientConnectionRecord connectionData,
            final Exam examData) {

        final StringBuilder builder = new StringBuilder();
        final EventType type = EventType.byId(eventData.getType());

        builder.append(type.name());
        builder.append(Utils.toCSVString(eventData.getText()));
        builder.append(eventData.getNumericValue());
        builder.append(Utils.toDateTimeUTC(eventData.getClientTime()));
        builder.append(Utils.toDateTimeUTC(eventData.getServerTime()));

        if (connectionData != null) {
            builder.append(Utils.toCSVString(connectionData.getExamUserSessionId()));
            builder.append(Utils.toCSVString(connectionData.getClientAddress()));
            builder.append(connectionData.getStatus());
            builder.append(connectionData.getConnectionToken());
        }

        if (examData != null) {
            builder.append(Utils.toCSVString(examData.getName()));
            builder.append(Utils.toCSVString(examData.getDescription()));
            builder.append(examData.getType().name());
            builder.append(examData.getStartTime());
            builder.append(examData.getEndTime());
        }

        builder.append(Constants.CARRIAGE_RETURN);

        try {
            output.write(Utils.toByteArray(builder));
        } catch (final IOException e) {
            log.error("Failed to stream header: ", e);
        }
    }

}
