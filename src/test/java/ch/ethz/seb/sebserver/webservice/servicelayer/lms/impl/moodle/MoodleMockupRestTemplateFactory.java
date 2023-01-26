/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Arrays;
import org.assertj.core.util.Lists;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleQuizRestriction;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleQuizRestrictions;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleUserDetails;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseRestriction;

public class MoodleMockupRestTemplateFactory implements MoodleRestTemplateFactory {

    private final APITemplateDataSupplier apiTemplateDataSupplier;

    public MoodleMockupRestTemplateFactory(final APITemplateDataSupplier apiTemplateDataSupplier) {
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
    }

    @Override
    public LmsSetupTestResult test() {
        return LmsSetupTestResult.ofOkay(LmsType.MOODLE_PLUGIN);
    }

    @Override
    public APITemplateDataSupplier getApiTemplateDataSupplier() {
        return this.apiTemplateDataSupplier;
    }

    @Override
    public Set<String> getKnownTokenAccessPaths() {
        final Set<String> paths = new HashSet<>();
        paths.add(MoodleAPIRestTemplate.MOODLE_DEFAULT_TOKEN_REQUEST_PATH);
        return paths;
    }

    @Override
    public Result<MoodleAPIRestTemplate> createRestTemplate(final String service) {
        return Result.of(new MockupMoodleRestTemplate(this.apiTemplateDataSupplier.getLmsSetup().lmsApiUrl));
    }

    @Override
    public Result<MoodleAPIRestTemplate> createRestTemplate(final String service, final String accessTokenPath) {
        return Result.of(new MockupMoodleRestTemplate(this.apiTemplateDataSupplier.getLmsSetup().lmsApiUrl));
    }

    public static final class MockupMoodleRestTemplate implements MoodleAPIRestTemplate {

        private final String accessToken = "MockupMoodleRestTemplate-Test-Token";
        private final String url;
        public final Collection<String> testLog = new ArrayList<>();
        public final Collection<HttpEntity<?>> callLog = new ArrayList<>();

        private final List<MockCD> courses = new ArrayList<>();

        public MockupMoodleRestTemplate(final String url) {
            this.url = url;

            for (int i = 0; i < 20; i++) {
                this.courses.add(new MockCD(
                        String.valueOf(i),
                        getQuizzesForCourse(i)));
            }
        }

        @Override
        public String getService() {
            return "mockup-service";
        }

        @Override
        public CharSequence getAccessToken() {
            return this.accessToken;
        }

        @Override
        public void testAPIConnection(final String... functions) {
            this.testLog.add("testAPIConnection functions: " + Arrays.asList(functions));
        }

        @Override
        public String callMoodleAPIFunction(final String functionName) {
            return callMoodleAPIFunction(functionName, null, null);
        }

        @Override
        public String callMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryAttributes) {
            return callMoodleAPIFunction(functionName, null, queryAttributes);
        }

        @Override
        public String callMoodleAPIFunction(
                final String functionName,
                final MultiValueMap<String, String> queryParams,
                final MultiValueMap<String, String> queryAttributes) {

            final UriComponentsBuilder queryParam = UriComponentsBuilder
                    .fromHttpUrl(this.url + MOODLE_DEFAULT_REST_API_PATH)
                    .queryParam(REST_REQUEST_TOKEN_NAME, this.accessToken)
                    .queryParam(REST_REQUEST_FUNCTION_NAME, functionName)
                    .queryParam(REST_REQUEST_FORMAT_NAME, "json");

            if (queryParams != null && !queryParams.isEmpty()) {
                queryParam.queryParams(queryParams);
            }

            final boolean usePOST = queryAttributes != null && !queryAttributes.isEmpty();
            HttpEntity<?> functionReqEntity;
            if (usePOST) {
                final HttpHeaders headers = new HttpHeaders();
                headers.set(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                final String body = Utils.toAppFormUrlEncodedBody(queryAttributes);
                functionReqEntity = new HttpEntity<>(body, headers);

            } else {
                functionReqEntity = new HttpEntity<>(new LinkedMultiValueMap<>());
            }

            this.testLog.add("callMoodleAPIFunction: " + functionName);
            this.callLog.add(functionReqEntity);

            if (MoodlePluginCourseAccess.COURSES_API_FUNCTION_NAME.equals(functionName)) {
                return respondCourses(queryAttributes);
            } else if (MoodlePluginCourseAccess.USERS_API_FUNCTION_NAME.equals(functionName)) {
                return respondUsers(queryAttributes);
            } else if (MoodlePluginCourseRestriction.RESTRICTION_GET_FUNCTION_NAME.equals(functionName)) {

                return respondGetRestriction(
                        queryParams.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIZ_ID),
                        queryAttributes);
            } else if (MoodlePluginCourseRestriction.RESTRICTION_SET_FUNCTION_NAME.equals(functionName)) {
                return respondSetRestriction(
                        queryParams.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIZ_ID),
                        queryAttributes);
            }

            else {
                throw new RuntimeException("Unknown function: " + functionName);
            }
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("MockupMoodleRestTemplate [accessToken=");
            builder.append(this.accessToken);
            builder.append(", url=");
            builder.append(this.url);
            builder.append(", testLog=");
            builder.append(this.testLog);
            builder.append(", callLog=");
            builder.append(this.callLog);
            builder.append("]");
            return builder.toString();
        }

        @SuppressWarnings("unused")
        private static final class MockCD {
            public final String id;
            public final String shortname;
            public final String categoryid;
            public final String fullname;
            public final String displayname;
            public final String idnumber;
            public final Long startdate; // unix-time seconds UTC
            public final Long enddate; // unix-time seconds UTC
            public final Long timecreated; // unix-time seconds UTC
            public final boolean visible;
            public final Collection<MockQ> quizzes;

            public MockCD(final String num, final Collection<MockQ> quizzes) {
                this.id = num;
                this.shortname = "c" + num;
                this.categoryid = "mock";
                this.fullname = "course" + num;
                this.displayname = this.fullname;
                this.idnumber = "i" + num;
                this.startdate = Long.valueOf(num);
                this.enddate = null;
                this.timecreated = Long.valueOf(num);
                this.visible = true;
                this.quizzes = quizzes;
            }
        }

        @SuppressWarnings("unused")
        private static final class MockQ {
            public final String id;
            public final String coursemodule;
            public final String course;
            public final String name;
            public final String intro;
            public final Long timeopen; // unix-time seconds UTC
            public final Long timeclose; // unix-time seconds UTC

            public MockQ(final String courseId, final String num) {
                this.id = num;
                this.coursemodule = num;
                this.course = courseId;
                this.name = "quiz " + num;
                this.intro = this.name;
                this.timeopen = Long.valueOf(num);
                this.timeclose = null;
            }
        }

        private String respondCourses(final MultiValueMap<String, String> queryAttributes) {
            try {
                final List<String> ids = queryAttributes.get(MoodlePluginCourseAccess.PARAM_COURSE_ID_ARRAY);
                final String from = queryAttributes.getFirst(MoodlePluginCourseAccess.PARAM_PAGE_START);
                final String size = queryAttributes.getFirst(MoodlePluginCourseAccess.PARAM_PAGE_SIZE);
                final List<MockCD> courses;

                if (ids != null && !ids.isEmpty() && !ids.get(0).equals("0")) {
                    courses = this.courses.stream().filter(c -> ids.contains(c.id)).collect(Collectors.toList());
                } else if (from != null && Integer.valueOf(from) < this.courses.size()) {

                    final int to = Integer.valueOf(from) + Integer.valueOf(size);
                    courses = this.courses.subList(
                            Integer.valueOf(from),
                            (to < this.courses.size()) ? to : this.courses.size() - 1);
                } else {
                    courses = new ArrayList<>();
                }

                final Map<String, Object> response = new HashMap<>();
                response.put("results", courses);
                final JSONMapper jsonMapper = new JSONMapper();
                final String result = jsonMapper.writeValueAsString(response);
                return result;
            } catch (final JsonProcessingException e) {
                e.printStackTrace();
                return "";
            }
        }

        private final Map<String, MoodleQuizRestrictions> restrcitions = new HashMap<>();

        private String respondSetRestriction(final String quizId, final MultiValueMap<String, String> queryAttributes) {
            final List<String> configKeys = queryAttributes.get(MoodlePluginCourseRestriction.ATTRIBUTE_CONFIG_KEYS);
            final List<String> beks = queryAttributes.get(MoodlePluginCourseRestriction.ATTRIBUTE_BROWSER_EXAM_KEYS);
            final String quitURL = queryAttributes.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIT_URL);
            final String quitSecret = queryAttributes.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIT_SECRET);

            final MoodleQuizRestriction moodleQuizRestriction = new MoodleQuizRestriction(
                    quizId,
                    trimList(configKeys),
                    trimList(beks),
                    quitURL,
                    quitSecret);

            this.restrcitions.put(
                    quizId,
                    new MoodleQuizRestrictions(Lists.list(moodleQuizRestriction), null));

            final JSONMapper jsonMapper = new JSONMapper();
            try {
                return jsonMapper.writeValueAsString(moodleQuizRestriction);
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private List<String> trimList(final List<String> list) {
            if (list.size() == 1 && StringUtils.isBlank(list.get(0))) {
                return Collections.emptyList();
            }
            return list;
        }

        private String respondGetRestriction(final String quizId, final MultiValueMap<String, String> queryAttributes) {
            final MoodleQuizRestrictions moodleQuizRestriction = this.restrcitions.get(quizId);
            if (moodleQuizRestriction != null) {
                final JSONMapper jsonMapper = new JSONMapper();
                try {
                    return jsonMapper.writeValueAsString(moodleQuizRestriction);
                } catch (final JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return "";
        }

        private Collection<MockQ> getQuizzesForCourse(final int courseId) {
            final String id = String.valueOf(courseId);
            final Collection<MockQ> result = new ArrayList<>();
            result.add(new MockQ(id, "10" + id));
            if (courseId % 2 > 0) {
                result.add(new MockQ(id, "11" + id));
            }

            return result;
        }

        private String respondUsers(final MultiValueMap<String, String> queryAttributes) {
            final String id = queryAttributes.getFirst(MoodlePluginCourseAccess.ATTR_VALUE_ARRAY);
            final String field = queryAttributes.getFirst(MoodlePluginCourseAccess.ATTR_FIELD);

            if (!field.equals(MoodlePluginCourseAccess.ATTR_ID)) {
                return "<error>";
            }

            if (id.contains("2")) {
                final MoodleUserDetails moodleUserDetails = new MoodleUserDetails(
                        id, "testuser", "test", "user", "test user", "text@user.mail",
                        null, null, null, null, null, null, null, null, null, null, null, null);
                final MoodleUserDetails[] array = new MoodleUserDetails[] { moodleUserDetails };
                try {
                    final JSONMapper jsonMapper = new JSONMapper();
                    return jsonMapper.writeValueAsString(array);
                } catch (final JsonProcessingException e) {
                    e.printStackTrace();
                    return "";
                }
            }

            return "<error>";
        }

    }
}
