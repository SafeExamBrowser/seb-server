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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleUtils.MoodleQuizRestriction;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.plugin.MoodlePluginCourseRestriction;

public class MockupRestTemplateFactory implements MoodleRestTemplateFactory {

    private final APITemplateDataSupplier apiTemplateDataSupplier;

    public MockupRestTemplateFactory(final APITemplateDataSupplier apiTemplateDataSupplier) {
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
    public Result<MoodleAPIRestTemplate> createRestTemplate() {
        return Result.of(new MockupMoodleRestTemplate(this.apiTemplateDataSupplier.getLmsSetup().lmsApiUrl));
    }

    @Override
    public Result<MoodleAPIRestTemplate> createRestTemplate(final String accessTokenPath) {
        return Result.of(new MockupMoodleRestTemplate(this.apiTemplateDataSupplier.getLmsSetup().lmsApiUrl));
    }

    public static final class MockupMoodleRestTemplate implements MoodleAPIRestTemplate {

        private final String accessToken = UUID.randomUUID().toString();
        private final String url;

        public MockupMoodleRestTemplate(final String url) {
            this.url = url;
        }

        @Override
        public String getService() {
            return "mockup-service";
        }

        @Override
        public void setService(final String service) {
        }

        @Override
        public CharSequence getAccessToken() {
            System.out.println("***** getAccessToken: " + this.accessToken);
            return this.accessToken;
        }

        @Override
        public void testAPIConnection(final String... functions) {
            System.out.println("***** testAPIConnection functions: " + functions);
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

            System.out.println("***** callMoodleAPIFunction HttpEntity: " + functionReqEntity);

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
                this.coursemodule = courseId;
                this.course = courseId;
                this.name = "quiz " + num;
                this.intro = this.name;
                this.timeopen = Long.valueOf(num);
                this.timeclose = null;
            }
        }

        private String respondCourses(final MultiValueMap<String, String> queryAttributes) {
            try {
                final List<String> ids = queryAttributes.get(MoodlePluginCourseAccess.CRITERIA_COURSE_IDS);
                final String from = queryAttributes.getFirst(MoodlePluginCourseAccess.CRITERIA_LIMIT_FROM);
                System.out.println("************* from: " + from);
                final List<MockCD> courses;
                if (ids != null && !ids.isEmpty()) {
                    courses = ids
                            .stream()
                            .map(id -> new MockCD(
                                    id,
                                    getQuizzesForCourse(Integer.parseInt(id))))
                            .collect(Collectors.toList());
                } else if (from != null && Integer.valueOf(from) < 11) {
                    courses = new ArrayList<>();
                    final int num = (Integer.valueOf(from) > 0) ? 10 : 1;
                    for (int i = 0; i < 10; i++) {
                        courses.add(new MockCD(String.valueOf(num + i), getQuizzesForCourse(num + i)));
                    }
                } else {
                    courses = new ArrayList<>();
                }

                final Map<String, Object> response = new HashMap<>();
                response.put("courses", courses);
                final JSONMapper jsonMapper = new JSONMapper();
                final String result = jsonMapper.writeValueAsString(response);
                System.out.println("******** courses response: " + result);
                return result;
            } catch (final JsonProcessingException e) {
                e.printStackTrace();
                return "";
            }
        }

        private final Map<String, MoodleQuizRestriction> restrcitions = new HashMap<>();

        private String respondSetRestriction(final String quizId, final MultiValueMap<String, String> queryAttributes) {
            final List<String> configKeys = queryAttributes.get(MoodlePluginCourseRestriction.ATTRIBUTE_CONFIG_KEYS);
            final List<String> beks = queryAttributes.get(MoodlePluginCourseRestriction.ATTRIBUTE_BROWSER_EXAM_KEYS);
            final String quitURL = queryAttributes.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIT_URL);
            final String quitSecret = queryAttributes.getFirst(MoodlePluginCourseRestriction.ATTRIBUTE_QUIT_SECRET);

            final MoodleQuizRestriction moodleQuizRestriction = new MoodleQuizRestriction(
                    quizId,
                    StringUtils.join(configKeys, Constants.LIST_SEPARATOR),
                    StringUtils.join(beks, Constants.LIST_SEPARATOR),
                    quitURL,
                    quitSecret);
            this.restrcitions.put(quizId, moodleQuizRestriction);

            final JSONMapper jsonMapper = new JSONMapper();
            try {
                return jsonMapper.writeValueAsString(moodleQuizRestriction);
            } catch (final JsonProcessingException e) {
                e.printStackTrace();
                return "";
            }
        }

        private String respondGetRestriction(final String quizId, final MultiValueMap<String, String> queryAttributes) {
            final MoodleQuizRestriction moodleQuizRestriction = this.restrcitions.get(quizId);
            if (moodleQuizRestriction != null) {
                final JSONMapper jsonMapper = new JSONMapper();
                try {
                    return jsonMapper.writeValueAsString(moodleQuizRestriction);
                } catch (final JsonProcessingException e) {
                    e.printStackTrace();
                    return "";
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
            // TODO
            return "";
        }

    }
}
