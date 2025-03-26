/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import static org.junit.Assert.*;

import java.util.Collections;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleMockupRestTemplateFactory;

public class MoodlePluginCourseRestrictionTest {

    @Test
    public void testSetup() {

        final MoodlePluginCourseRestriction candidate = crateMockup();

        assertEquals("MoodlePluginCourseRestriction [restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, testLog=[], callLog=[]]]", candidate.toTestString());

        final LmsSetupTestResult testCourseRestrictionAPI = candidate.testCourseRestrictionAPI();

        assertTrue(testCourseRestrictionAPI.isOk());
        assertEquals(
                "MoodlePluginCourseRestriction [restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=[testAPIConnection functions: [quizaccess_sebserver_get_restriction, quizaccess_sebserver_set_restriction]], "
                        + "callLog=[]]]",
                candidate.toTestString());
    }

    @Test
    public void getNoneExistingRestriction() {
        final MoodlePluginCourseRestriction candidate = crateMockup();
        final Exam exam = new Exam(1L, 1L, 1L, "101:1:c1:i1",
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        final Result<SEBRestriction> sebClientRestriction = candidate.getSEBClientRestriction(exam);

        assertFalse(sebClientRestriction.hasError());
        final SEBRestriction sebRestriction = sebClientRestriction.get();
        assertEquals(
                "SEBRestriction [examId=1, configKeys=[], browserExamKeys=[], additionalProperties={}]",
                sebRestriction.toString());
    }

    @Test
    public void getSetGetRestriction() {
        final MoodlePluginCourseRestriction candidate = crateMockup();
        final Exam exam = new Exam(1L, 1L, 1L, "101:1:c1:i1",
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        final SEBRestriction restriction = new SEBRestriction(
                exam.id,
                Lists.list("configKey1"),
                Lists.list("BEK1", "BEK2"),
                Collections.emptyMap(), null);

        assertFalse(candidate.hasSEBClientRestriction(exam));

        final Result<SEBRestriction> applySEBClientRestriction = candidate.applySEBClientRestriction(
                exam,
                restriction);

        assertFalse(applySEBClientRestriction.hasError());
        SEBRestriction sebRestriction = applySEBClientRestriction.get();
        assertEquals(
                "SEBRestriction ["
                        + "examId=1, "
                        + "configKeys=[configKey1], "
                        + "browserExamKeys=[BEK1, BEK2], "
                        + "additionalProperties={quitsecret=quitSecret, quitlink=quitLink}]",
                sebRestriction.toString());

        Result<SEBRestriction> sebClientRestriction = candidate.getSEBClientRestriction(exam);

        assertFalse(sebClientRestriction.hasError());
        sebRestriction = sebClientRestriction.get();
        assertEquals(
                "SEBRestriction ["
                        + "examId=1, "
                        + "configKeys=[configKey1], "
                        + "browserExamKeys=[BEK1, BEK2], "
                        + "additionalProperties={quitsecret=quitSecret, quitlink=quitLink}]",
                sebRestriction.toString());

        assertTrue(candidate.hasSEBClientRestriction(exam));

        candidate.releaseSEBClientRestriction(exam);
        assertFalse(candidate.hasSEBClientRestriction(exam));

        sebClientRestriction = candidate.getSEBClientRestriction(exam);

        if (sebClientRestriction.hasError()) {
            sebClientRestriction.getError().printStackTrace();
        }

        assertFalse(sebClientRestriction.hasError());
        sebRestriction = sebClientRestriction.get();
        assertEquals(
                "SEBRestriction ["
                        + "examId=1, "
                        + "configKeys=[], "
                        + "browserExamKeys=[], "
                        + "additionalProperties={quitsecret=null, quitlink=null}]",
                sebRestriction.toString());
    }

    private MoodlePluginCourseRestriction crateMockup() {
        final JSONMapper jsonMapper = new JSONMapper();
        final LmsSetup lmsSetup =
                new LmsSetup(1L, 1L, "test-Moodle", LmsType.MOODLE_PLUGIN, "lms-user", "lms-user-secret",
                        "https://test.org/", null, null, null, null, null, null, null,
                        null, false);

        final APITemplateDataSupplier apiTemplateDataSupplier = new APITemplateDataSupplier() {

            @Override
            public LmsSetup getLmsSetup() {
                return lmsSetup;
            }

            @Override
            public ClientCredentials getLmsClientCredentials() {
                return new ClientCredentials("lms-user", "lms-user-secret");
            }

            @Override
            public ProxyData getProxyData() {
                return null;
            }

        };

        final MoodleMockupRestTemplateFactory moodleMockupRestTemplateFactory =
                new MoodleMockupRestTemplateFactory(apiTemplateDataSupplier);

        final ExamConfigurationValueService examConfigurationValueService =
                Mockito.mock(ExamConfigurationValueService.class);
        Mockito.when(examConfigurationValueService.getQuitLink(Mockito.anyLong())).thenReturn("quitLink");
        Mockito.when(examConfigurationValueService.getQuitPassword(Mockito.anyLong())).thenReturn("quitSecret");

        return new MoodlePluginCourseRestriction(jsonMapper, moodleMockupRestTemplateFactory,
                examConfigurationValueService);
    }

}
