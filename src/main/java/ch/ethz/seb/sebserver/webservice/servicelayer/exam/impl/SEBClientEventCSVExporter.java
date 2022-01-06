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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventExporter;

@Lazy
@Component
@WebServiceProfile
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

        builder
                .append("Event Type")
                .append(Constants.COMMA)
                .append("Message")
                .append(Constants.COMMA)
                .append("Value")
                .append(Constants.COMMA)
                .append("Client Time (UTC)")
                .append(Constants.COMMA)
                .append("Server Time (UTC)");

        if (includeConnectionDetails) {
            builder
                    .append(Constants.COMMA)
                    .append("User Session-ID")
                    .append(Constants.COMMA)
                    .append("Client Machine")
                    .append(Constants.COMMA)
                    .append("Connection Status")
                    .append(Constants.COMMA)
                    .append("Connection Token");
        }

        if (includeExamDetails) {
            builder
                    .append(Constants.COMMA)
                    .append("Exam Name")
                    .append(Constants.COMMA)
                    .append("Exam Description")
                    .append(Constants.COMMA)
                    .append("Exam Type")
                    .append(Constants.COMMA)
                    .append("Start Time (LMS)")
                    .append(Constants.COMMA)
                    .append("End Time (LMS)");
        }

        builder.append(Constants.CARRIAGE_RETURN);

        try {
            output.write(Utils.toByteArray(builder));
        } catch (final IOException e) {
            log.error("Failed to stream header: ", e);
        } finally {
            try {
                output.flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
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
        builder.append(Constants.COMMA);
        builder.append(Utils.toCSVString(eventData.getText()));
        builder.append(Constants.COMMA);
        builder.append(eventData.getNumericValue() != null ? eventData.getNumericValue() : "");
        builder.append(Constants.COMMA);
        builder.append(Utils.formatDate(Utils.toDateTimeUTC(eventData.getClientTime())));
        builder.append(Constants.COMMA);
        builder.append(Utils.formatDate(Utils.toDateTimeUTC(eventData.getServerTime())));

        if (connectionData != null) {
            builder.append(Constants.COMMA);
            builder.append(Utils.toCSVString(connectionData.getExamUserSessionId()));
            builder.append(Constants.COMMA);
            builder.append(Utils.toCSVString(connectionData.getClientAddress()));
            builder.append(Constants.COMMA);
            builder.append(connectionData.getStatus());
            builder.append(Constants.COMMA);
            builder.append(connectionData.getConnectionToken());
        }

        if (examData != null) {
            builder.append(Constants.COMMA);
            builder.append(Utils.toCSVString(examData.getName()));
            builder.append(Constants.COMMA);
            builder.append(Utils.toCSVString(examData.getDescription()));
            builder.append(Constants.COMMA);
            builder.append(examData.getType().name());
            builder.append(Constants.COMMA);
            builder.append(Utils.formatDate(examData.getStartTime()));
            builder.append(Constants.COMMA);
            builder.append(Utils.formatDate(examData.getEndTime()));
        }

        builder.append(Constants.CARRIAGE_RETURN);

        try {
            output.write(Utils.toByteArray(builder));
        } catch (final IOException e) {
            log.error("Failed to stream header: ", e);
        } finally {
            try {
                output.flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

}
