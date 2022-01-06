/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;

public class SEBClientEventCSVExporterTest {
    @Test
    public void streamHeaderTestWithoutConnectionAndExamDetails() {
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, false, false);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("Event Type,Message,Value,Client Time (UTC),Server Time (UTC)\n", string);
    }

    @Test
    public void streamHeaderTestWithConnectionDetails() {
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, true, false);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("Event Type,Message,Value,Client Time (UTC),Server Time (UTC),User Session-ID,Client Machine,Connection Status,Connection Token\n", string);
    }

    @Test
    public void streamHeaderTestWithExamDetails() {
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, false, true);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("Event Type,Message,Value,Client Time (UTC),Server Time (UTC),Exam Name,Exam Description,Exam Type,Start Time (LMS),End Time (LMS)\n", string);
    }

    @Test
    public void streamHeaderTestWithConnectionAndExamDetails() {
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, true, true);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("Event Type,Message,Value,Client Time (UTC),Server Time (UTC),User Session-ID,Client Machine,Connection Status,Connection Token,Exam Name,Exam Description,Exam Type,Start Time (LMS),End Time (LMS)\n", string);
    }

    @Test
    public void streamDataTestWithConnection() {
        ClientConnectionRecord connection = new ClientConnectionRecord(0L, 1L, 2L, "status", "token", "sessionid", "clientaddress", "virtualaddress", 3, "vdi", 4L, 5L, 6L, 7);
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, connection, null);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("INFO_LOG,\"text\",5,1970-01-01T00:00:00.003Z,1970-01-01T00:00:00.004Z,\"sessionid\",\"clientaddress\",status,token\n", string);
    }

    @Test
    public void streamDataTestWithExam() {
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        Exam exam = new Exam(0L, 1L, 3L, "externalid", "name", "description", new DateTime(1L), new DateTime(1L), "startURL", Exam.ExamType.BYOD, "owner", new ArrayList<>(), Exam.ExamStatus.RUNNING, false, "bek", true, "lastUpdate", 4L);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, null, exam);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("INFO_LOG,\"text\",5,1970-01-01T00:00:00.003Z,1970-01-01T00:00:00.004Z,\"name\",\"description\",BYOD,1970-01-01T01:00:00.001+01:00,1970-01-01T01:00:00.001+01:00\n", string);
    }

    @Test
    public void streamDataTestWithConnectionAndExam() {
        ClientConnectionRecord connection = new ClientConnectionRecord(0L, 1L, 2L, "status", "token", "sessionid", "clientaddress", "virtualaddress", 3, "vdi", 4L, 5L, 6L, 7);
        SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        Exam exam = new Exam(0L, 1L, 3L, "externalid", "name", "description", new DateTime(1L), new DateTime(1L), "startURL", Exam.ExamType.BYOD, "owner", new ArrayList<>(), Exam.ExamStatus.RUNNING, false, "bek", true, "lastUpdate", 4L);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, connection, exam);

        byte[] array = stream.toByteArray();
        String string = Utils.toString(array);

        Assert.assertEquals("INFO_LOG,\"text\",5,1970-01-01T00:00:00.003Z,1970-01-01T00:00:00.004Z,\"sessionid\",\"clientaddress\",status,token,\"name\",\"description\",BYOD,1970-01-01T01:00:00.001+01:00,1970-01-01T01:00:00.001+01:00\n", string);
    }
}
