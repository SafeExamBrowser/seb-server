/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTimeUtils;
import org.junit.Test;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.mock.env.MockEnvironment;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.CourseAccessAPI.AsyncQuizFetchBuffer;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleMockupRestTemplateFactory;

public class MoodlePluginCourseAccessTest {

    @Test
    public void testSetup() {
        final MoodlePluginCourseAccess candidate = crateMockup();

        assertEquals("MoodlePluginCourseAccess ["
                + "pageSize=500, "
                + "maxSize=10000, "
                + "cutoffTimeOffset=3, "
                + "restTemplate=null]", candidate.toTestString());

        final LmsSetupTestResult testCourseAccessAPI = candidate.testCourseAccessAPI();

        assertTrue(testCourseAccessAPI.isOk());
        assertEquals("MoodlePluginCourseAccess [pageSize=500, maxSize=10000, cutoffTimeOffset=3, "
                + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                + "testLog=[testAPIConnection functions: [quizaccess_sebserver_get_exams, core_user_get_users_by_field]], "
                + "callLog=[]]]",
                candidate.toTestString());
    }

    @Test
    public void testFetchQuizzes() {

        DateTimeUtils.setCurrentMillisFixed(0);

        final MoodlePluginCourseAccess candidate = crateMockup();
        final FilterMap filterMap = new FilterMap();
        final AsyncQuizFetchBuffer asyncQuizFetchBuffer = new CourseAccessAPI.AsyncQuizFetchBuffer();
        candidate.fetchQuizzes(filterMap, asyncQuizFetchBuffer);

        assertFalse(asyncQuizFetchBuffer.canceled);
        assertTrue(asyncQuizFetchBuffer.finished);
        assertNull(asyncQuizFetchBuffer.error);

        assertEquals(
                "MoodlePluginCourseAccess [pageSize=500, maxSize=10000, cutoffTimeOffset=3, "
                        + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=["
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams, "
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams], "
                        + "callLog=["
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=0&perpage=500,[Content-Type:\"application/x-www-form-urlencoded\"]>, "
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=500&perpage=500,[Content-Type:\"application/x-www-form-urlencoded\"]>]]]",
                candidate.toTestString());

        final List<String> ids =
                asyncQuizFetchBuffer.buffer.stream().map(q -> q.id).sorted().collect(Collectors.toList());

        assertEquals(
                "[100:0:c0:i0, "
                        + "1010:10:c10:i10, "
                        + "1011:11:c11:i11, "
                        + "1012:12:c12:i12, "
                        + "1013:13:c13:i13, "
                        + "1014:14:c14:i14, "
                        + "1015:15:c15:i15, "
                        + "1016:16:c16:i16, "
                        + "1017:17:c17:i17, "
                        + "1018:18:c18:i18, "
                        + "101:1:c1:i1, "
                        + "102:2:c2:i2, "
                        + "103:3:c3:i3, "
                        + "104:4:c4:i4, "
                        + "105:5:c5:i5, "
                        + "106:6:c6:i6, "
                        + "107:7:c7:i7, "
                        + "108:8:c8:i8, "
                        + "109:9:c9:i9, "
                        + "1111:11:c11:i11, "
                        + "1113:13:c13:i13, "
                        + "1115:15:c15:i15, "
                        + "1117:17:c17:i17, "
                        + "111:1:c1:i1, "
                        + "113:3:c3:i3, "
                        + "115:5:c5:i5, "
                        + "117:7:c7:i7, "
                        + "119:9:c9:i9]",
                ids.toString());

        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testFetchQuizzes_smallPaging() {

        DateTimeUtils.setCurrentMillisFixed(0);

        final Map<String, String> params = new HashMap<>();
        params.put("sebserver.webservice.cache.moodle.course.pageSize", "5");

        final MoodlePluginCourseAccess candidate = crateMockup(params);
        final FilterMap filterMap = new FilterMap();
        final AsyncQuizFetchBuffer asyncQuizFetchBuffer = new CourseAccessAPI.AsyncQuizFetchBuffer();
        candidate.fetchQuizzes(filterMap, asyncQuizFetchBuffer);

        assertFalse(asyncQuizFetchBuffer.canceled);
        assertTrue(asyncQuizFetchBuffer.finished);
        assertNull(asyncQuizFetchBuffer.error);

        assertEquals(
                "MoodlePluginCourseAccess [pageSize=5, maxSize=10000, cutoffTimeOffset=3, "
                        + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=["
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams, "
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams, "
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams, "
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams, "
                        + "callMoodleAPIFunction: quizaccess_sebserver_get_exams], "
                        + "callLog=["
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=0&perpage=5,[Content-Type:\"application/x-www-form-urlencoded\"]>, "
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=5&perpage=5,[Content-Type:\"application/x-www-form-urlencoded\"]>, "
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=10&perpage=5,[Content-Type:\"application/x-www-form-urlencoded\"]>, "
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=15&perpage=5,[Content-Type:\"application/x-www-form-urlencoded\"]>, "
                        + "<courseid[]=0&conditions=(startdate is null OR startdate = 0 OR startdate >= -94694400) AND (enddate is null or enddate = 0 OR enddate >= -94694400)&startneedle=20&perpage=5,[Content-Type:\"application/x-www-form-urlencoded\"]>]]]",
                candidate.toTestString());

        final List<String> ids =
                asyncQuizFetchBuffer.buffer.stream().map(q -> q.id).sorted().collect(Collectors.toList());

        assertEquals(
                "[100:0:c0:i0, "
                        + "1010:10:c10:i10, "
                        + "1011:11:c11:i11, "
                        + "1012:12:c12:i12, "
                        + "1013:13:c13:i13, "
                        + "1014:14:c14:i14, "
                        + "1015:15:c15:i15, "
                        + "1016:16:c16:i16, "
                        + "1017:17:c17:i17, "
                        + "1018:18:c18:i18, "
                        + "101:1:c1:i1, "
                        + "102:2:c2:i2, "
                        + "103:3:c3:i3, "
                        + "104:4:c4:i4, "
                        + "105:5:c5:i5, "
                        + "106:6:c6:i6, "
                        + "107:7:c7:i7, "
                        + "108:8:c8:i8, "
                        + "109:9:c9:i9, "
                        + "1111:11:c11:i11, "
                        + "1113:13:c13:i13, "
                        + "1115:15:c15:i15, "
                        + "1117:17:c17:i17, "
                        + "111:1:c1:i1, "
                        + "113:3:c3:i3, "
                        + "115:5:c5:i5, "
                        + "117:7:c7:i7, "
                        + "119:9:c9:i9]",
                ids.toString());

        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testGetQuizzesForCourseIds() {

        DateTimeUtils.setCurrentMillisFixed(0);
        final MoodlePluginCourseAccess candidate = crateMockup();

        final Set<String> ids = Stream.of(
                "101:1:c1:i1",
                "117:7:c7:i7")
                .collect(Collectors.toSet());

        final Result<Collection<QuizData>> quizzesCall = candidate.getQuizzes(ids);

        if (quizzesCall.hasError()) {
            quizzesCall.getError().printStackTrace();
        }

        assertFalse(quizzesCall.hasError());
        final Collection<QuizData> quizzes = quizzesCall.get();
        assertNotNull(quizzes);
        assertFalse(quizzes.isEmpty());
        assertTrue(quizzes.size() == 2);
        final QuizData q1 = quizzes.iterator().next();
        assertEquals(
                "QuizData [id=101:1:c1:i1, institutionId=1, lmsSetupId=1, lmsType=MOODLE_PLUGIN, name=c1 : quiz 101, description=quiz 101, startTime=1970-01-01T00:01:41.000Z, endTime=null, startURL=https://test.org/mod/quiz/view.php?id=101]",
                q1.toString());

        final Set<String> idsGet = quizzes.stream().map(q -> q.id).collect(Collectors.toSet());

        assertEquals(ids, idsGet);

        assertEquals(
                "MoodlePluginCourseAccess [pageSize=500, maxSize=10000, cutoffTimeOffset=3, "
                        + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=[callMoodleAPIFunction: quizaccess_sebserver_get_exams], "
                        + "callLog=[<courseid[]=1&courseid[]=7,[Content-Type:\"application/x-www-form-urlencoded\"]>]]]",
                candidate.toTestString());

        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testGetKnownUserDetails() {
        final MoodlePluginCourseAccess candidate = crateMockup();

        final Result<ExamineeAccountDetails> userDetailsResult = candidate.getExamineeAccountDetails("2");

        if (userDetailsResult.hasError()) {
            userDetailsResult.getError().printStackTrace();
        }

        assertFalse(userDetailsResult.hasError());
        final ExamineeAccountDetails examineeAccountDetails = userDetailsResult.get();
        assertEquals(
                "ExamineeAccountDetails [id=2, name=test user, username=testuser, email=text@user.mail, "
                        + "additionalAttributes={mailformat=null, firstname=test, auth=null, timezone=null, description=null, firstaccess=null, confirmed=null, suspended=null, lastname=user, lastaccess=null, theme=null, descriptionformat=null, department=null, lang=null}]",
                examineeAccountDetails.toString());

        assertEquals(
                "MoodlePluginCourseAccess [pageSize=500, maxSize=10000, cutoffTimeOffset=3, "
                        + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=["
                        + "callMoodleAPIFunction: core_user_get_users_by_field], "
                        + "callLog=["
                        + "<field=id&values[]=2,[Content-Type:\"application/x-www-form-urlencoded\"]>]]]",
                candidate.toTestString());
    }

    @Test
    public void testGetUnknownUserDetails() {
        final MoodlePluginCourseAccess candidate = crateMockup();

        final Result<ExamineeAccountDetails> userDetailsResult = candidate.getExamineeAccountDetails("1");

        assertTrue(userDetailsResult.hasError());
        assertEquals("Illegal response format detected: <error>", userDetailsResult.getError().getMessage());

        assertEquals(
                "MoodlePluginCourseAccess [pageSize=500, maxSize=10000, cutoffTimeOffset=3, "
                        + "restTemplate=MockupMoodleRestTemplate [accessToken=MockupMoodleRestTemplate-Test-Token, url=https://test.org/, "
                        + "testLog=[callMoodleAPIFunction: core_user_get_users_by_field], "
                        + "callLog=[<field=id&values[]=1,[Content-Type:\"application/x-www-form-urlencoded\"]>]]]",
                candidate.toTestString());
    }

    private MoodlePluginCourseAccess crateMockup() {
        return crateMockup(Collections.emptyMap());
    }

    private MoodlePluginCourseAccess crateMockup(final Map<String, String> env) {
        final JSONMapper jsonMapper = new JSONMapper();
        final AsyncService asyncService = new AsyncService(new AsyncRunner());
        final MockEnvironment mockEnvironment = new MockEnvironment();
        if (!env.isEmpty()) {
            env.entrySet().stream().forEach(entry -> mockEnvironment.setProperty(entry.getKey(), entry.getValue()));
        }

        final LmsSetup lmsSetup =
                new LmsSetup(1L, 1L, "test-Moodle", LmsType.MOODLE_PLUGIN, "lms-user", "lms-user-secret",
                        "https://test.org/", null, null, null, null, null, null, null,
                        null);

        final APITemplateDataSupplier apiTemplateDataSupplier = new APITemplateDataSupplier() {

            @Override
            public LmsSetup getLmsSetup() {
                return lmsSetup;
            }

            @Override
            public ClientCredentials getLmsClientCredentials() {
                return new ClientCredentials("lms-user", "lms-user-secret", "lms-user-token");
            }

            @Override
            public ProxyData getProxyData() {
                return null;
            }

        };

        final MoodleMockupRestTemplateFactory moodleMockupRestTemplateFactory =
                new MoodleMockupRestTemplateFactory(apiTemplateDataSupplier);

        return new MoodlePluginCourseAccess(
                jsonMapper,
                asyncService,
                moodleMockupRestTemplateFactory,
                new NoOpCacheManager(),
                mockEnvironment,
                false);
    }

}
