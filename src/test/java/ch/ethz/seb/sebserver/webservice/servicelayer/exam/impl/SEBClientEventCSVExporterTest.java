/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;

public class SEBClientEventCSVExporterTest {
    @Test
    public void streamHeaderTestWithoutConnectionAndExamDetails() {
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, false, false);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals("Event Type,Message,Value,Client Time (UTC),Server Time (UTC)\n", string);
    }

    @Test
    public void streamHeaderTestWithConnectionDetails() {
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, true, false);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "Event Type,Message,Value,Client Time (UTC),Server Time (UTC),User Session-ID,Client Machine,Connection Token\n",
                string);
    }

    @Test
    public void streamHeaderTestWithExamDetails() {
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, false, true);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "Event Type,Message,Value,Client Time (UTC),Server Time (UTC),Exam Name,Exam Type,Start Time (LMS),End Time (LMS)\n",
                string);
    }

    @Test
    public void streamHeaderTestWithConnectionAndExamDetails() {
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamHeader(output, true, true);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "Event Type,Message,Value,Client Time (UTC),Server Time (UTC),User Session-ID,Client Machine,Connection Token,Exam Name,Exam Type,Start Time (LMS),End Time (LMS)\n",
                string);
    }

    @Test
    public void streamDataTestWithConnection() {
        final ClientConnectionRecord connection = new ClientConnectionRecord(0L, 1L, 2L, "status", "token", "sessionid",
                "clientaddress", "virtualaddress", 3, "vdi", 4L, 5L, 6L, (byte) 0, 6L, 7,
                "seb_os_name", "seb_machine_name", "seb_version",
                (byte) 0, null, null);
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, connection, null);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "INFO_LOG,\"text\",5,1970-01-01T00:00:00.003,1970-01-01T00:00:00.004,\"sessionid\",\"clientaddress\",token\n",
                string);
    }

    @Test
    public void streamDataTestWithExam() {

        final Map<String, String> attrs = Stream.of(new Tuple<>(QuizData.QUIZ_ATTR_DESCRIPTION, "description"))
                .collect(Collectors.toMap(t -> t._1, t -> t._2));

        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        final Exam exam = new Exam(0L, 1L, 3L, "externalid", true, "name", new DateTime(1L),
                new DateTime(1L),
                Exam.ExamType.BYOD, "owner", new ArrayList<>(), Exam.ExamStatus.RUNNING,
                null, false, "bek", true,
                "lastUpdate", 4L, null, null, attrs);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, null, exam);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "INFO_LOG,\"text\",5,1970-01-01T00:00:00.003,1970-01-01T00:00:00.004,\"name\",BYOD,1970-01-01T00:00:00.001,1970-01-01T00:00:00.001\n",
                string);
    }

    @Test
    public void streamDataTestWithConnectionAndExam() {

        final Map<String, String> attrs = Stream.of(new Tuple<>(QuizData.QUIZ_ATTR_DESCRIPTION, "description"))
                .collect(Collectors.toMap(t -> t._1, t -> t._2));

        final ClientConnectionRecord connection = new ClientConnectionRecord(0L, 1L, 2L, "status", "token", "sessionid",
                "clientaddress", "virtualaddress", 3, "vdi", 4L, 5L, 6L, (byte) 0, 6L, 7,
                "seb_os_name", "seb_machine_name", "seb_version",
                (byte) 0, null, null);
        final SEBClientEventCSVExporter exporter = new SEBClientEventCSVExporter();
        final ClientEventRecord event = new ClientEventRecord(0L, 1L, 2, 3L, 4L, new BigDecimal(5), "text");
        final Exam exam = new Exam(0L, 1L, 3L, "externalid", true, "name", new DateTime(1L),
                new DateTime(1L),
                Exam.ExamType.BYOD, "owner", new ArrayList<>(), Exam.ExamStatus.RUNNING,
                null, false, "bek", true,
                "lastUpdate", 4L, null, null, attrs);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final BufferedOutputStream output = new BufferedOutputStream(stream);

        exporter.streamData(output, event, connection, exam);

        final byte[] array = stream.toByteArray();
        final String string = Utils.toString(array);

        Assert.assertEquals(
                "INFO_LOG,\"text\",5,1970-01-01T00:00:00.003,1970-01-01T00:00:00.004,\"sessionid\",\"clientaddress\",token,\"name\",BYOD,1970-01-01T00:00:00.001,1970-01-01T00:00:00.001\n",
                string);
    }
}
