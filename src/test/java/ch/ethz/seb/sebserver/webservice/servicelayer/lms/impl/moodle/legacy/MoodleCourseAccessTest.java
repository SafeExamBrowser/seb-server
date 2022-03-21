/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.TreeMap;

import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.legacy.MoodleRestTemplateFactory.MoodleAPIRestTemplate;

public class MoodleCourseAccessTest {

    @Mock
    Environment env = new MockEnvironment();

    @Test
    public void testGetExamineeAccountDetails() {

        final MoodleRestTemplateFactory moodleRestTemplateFactory = mock(MoodleRestTemplateFactory.class);
        final MoodleAPIRestTemplate moodleAPIRestTemplate = mock(MoodleAPIRestTemplate.class);
        when(moodleRestTemplateFactory.createRestTemplate()).thenReturn(Result.of(moodleAPIRestTemplate));
        when(moodleAPIRestTemplate.callMoodleAPIFunction(
                anyString(),
                any())).thenReturn("[\r\n" +
                        "    {\r\n" +
                        "        \"id\": 15,\r\n" +
                        "        \"username\": \"seb1\",\r\n" +
                        "        \"firstname\": \"seb\",\r\n" +
                        "        \"lastname\": \"user1\",\r\n" +
                        "        \"fullname\": \"seb user1\",\r\n" +
                        "        \"email\": \"demo1@safeexambrowser.org\",\r\n" +
                        "        \"department\": \"\",\r\n" +
                        "        \"firstaccess\": 1523400731,\r\n" +
                        "        \"lastaccess\": 1596457337,\r\n" +
                        "        \"auth\": \"manual\",\r\n" +
                        "        \"suspended\": false,\r\n" +
                        "        \"confirmed\": true,\r\n" +
                        "        \"lang\": \"en\",\r\n" +
                        "        \"theme\": \"\",\r\n" +
                        "        \"timezone\": \"99\",\r\n" +
                        "        \"mailformat\": 1,\r\n" +
                        "        \"description\": \"\",\r\n" +
                        "        \"descriptionformat\": 1,\r\n" +
                        "        \"profileimageurlsmall\": \"https://demo.safeexambrowser.org/moodle/theme/image.php/boost/core/1561047635/u/f2\",\r\n"
                        +
                        "        \"profileimageurl\": \"https://demo.safeexambrowser.org/moodle/theme/image.php/boost/core/1561047635/u/f1\"\r\n"
                        +
                        "    }\r\n" +
                        "]");

        final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                new JSONMapper(),
                moodleRestTemplateFactory,
                null,
                this.env);

        final String examId = "123";
        final Result<ExamineeAccountDetails> examineeAccountDetails =
                moodleCourseAccess.getExamineeAccountDetails(examId);

        final MultiValueMap<String, String> queryAttributes = new LinkedMultiValueMap<>();
        queryAttributes.add("field", "id");
        queryAttributes.add("values[0]", examId);

        verify(moodleAPIRestTemplate, times(1)).callMoodleAPIFunction(
                eq("core_user_get_users_by_field"),
                eq(queryAttributes));
        assertNotNull(examineeAccountDetails);
        assertFalse(examineeAccountDetails.hasError());
        final ExamineeAccountDetails userDetails = examineeAccountDetails.getOrThrow();
        assertEquals("15", userDetails.id);
        assertEquals("seb1", userDetails.username);
        assertEquals("seb user1", userDetails.name);
        assertEquals("demo1@safeexambrowser.org", userDetails.email);
        assertEquals(
                "{auth=manual, "
                        + "confirmed=true, "
                        + "department=, "
                        + "description=, "
                        + "descriptionformat=1, "
                        + "firstaccess=1523400731, "
                        + "firstname=seb, "
                        + "lang=en, "
                        + "lastaccess=1596457337, "
                        + "lastname=user1, "
                        + "mailformat=1, "
                        + "suspended=false, "
                        + "theme=, timezone=99}",
                new TreeMap<>(userDetails.additionalAttributes).toString());
    }

    @Test
    public void testInitAPIAccessError1() {
        final MoodleRestTemplateFactory moodleRestTemplateFactory = mock(MoodleRestTemplateFactory.class);
        when(moodleRestTemplateFactory.createRestTemplate()).thenReturn(Result.ofRuntimeError("Error1"));
        when(moodleRestTemplateFactory.test()).thenReturn(LmsSetupTestResult.ofOkay(LmsType.MOODLE));

        final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                new JSONMapper(),
                moodleRestTemplateFactory,
                null,
                this.env);

        final LmsSetupTestResult initAPIAccess = moodleCourseAccess.testCourseAccessAPI();
        assertNotNull(initAPIAccess);
        assertFalse(initAPIAccess.errors.isEmpty());
        assertTrue(initAPIAccess.hasError(ErrorType.TOKEN_REQUEST));

    }

    @Test
    public void testInitAPIAccessError2() {
        final MoodleRestTemplateFactory moodleRestTemplateFactory = mock(MoodleRestTemplateFactory.class);
        final MoodleAPIRestTemplate moodleAPIRestTemplate = mock(MoodleAPIRestTemplate.class);
        when(moodleRestTemplateFactory.createRestTemplate()).thenReturn(Result.of(moodleAPIRestTemplate));
        doThrow(RuntimeException.class).when(moodleAPIRestTemplate).testAPIConnection(any());
        when(moodleRestTemplateFactory.test()).thenReturn(LmsSetupTestResult.ofOkay(LmsType.MOODLE));

        final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                new JSONMapper(),
                moodleRestTemplateFactory,
                null,
                this.env);

        final LmsSetupTestResult initAPIAccess = moodleCourseAccess.testCourseAccessAPI();
        assertNotNull(initAPIAccess);
        assertFalse(initAPIAccess.errors.isEmpty());
        assertTrue(initAPIAccess.hasError(ErrorType.QUIZ_ACCESS_API_REQUEST));

    }

    @Test
    public void testInitAPIAccessOK() {
        final MoodleRestTemplateFactory moodleRestTemplateFactory = mock(MoodleRestTemplateFactory.class);
        final MoodleAPIRestTemplate moodleAPIRestTemplate = mock(MoodleAPIRestTemplate.class);
        when(moodleRestTemplateFactory.createRestTemplate()).thenReturn(Result.of(moodleAPIRestTemplate));
        when(moodleRestTemplateFactory.test()).thenReturn(LmsSetupTestResult.ofOkay(LmsType.MOODLE));

        final MoodleCourseAccess moodleCourseAccess = new MoodleCourseAccess(
                new JSONMapper(),
                moodleRestTemplateFactory,
                null,
                this.env);

        final LmsSetupTestResult initAPIAccess = moodleCourseAccess.testCourseAccessAPI();
        assertNotNull(initAPIAccess);
        assertTrue(initAPIAccess.errors.isEmpty());

    }

}
