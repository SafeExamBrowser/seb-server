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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate.Warning;

public abstract class MoodleUtils {

    private static final Logger log = LoggerFactory.getLogger(MoodleUtils.class);

    public static final String getInternalQuizId(
            final String quizId,
            final String courseId,
            final String shortname,
            final String idnumber) {

        return StringUtils.join(
                new String[] {
                        quizId,
                        courseId,
                        StringUtils.isNotBlank(shortname) ? shortname : Constants.EMPTY_NOTE,
                        StringUtils.isNotBlank(idnumber) ? idnumber : Constants.EMPTY_NOTE
                },
                Constants.COLON);
    }

    public static final String getQuizId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[0];
    }

    public static final String getCourseId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[1];
    }

    public static final String getShortname(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        final String[] split = StringUtils.split(internalQuizId, Constants.COLON);
        if (split.length < 3) {
            return null;
        }

        final String shortName = split[2];
        return shortName.equals(Constants.EMPTY_NOTE) ? null : shortName;
    }

    public static final String getIdnumber(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }
        final String[] split = StringUtils.split(internalQuizId, Constants.COLON);
        if (split.length < 4) {
            return null;
        }

        final String idNumber = split[3];
        return idNumber.equals(Constants.EMPTY_NOTE) ? null : idNumber;
    }

    public static final void logMoodleWarning(
            final Collection<Warning> warnings,
            final String lmsSetupName,
            final String function) {

        log.warn(
                "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                lmsSetupName,
                function,
                warnings.size(),
                warnings.iterator().next().toString());
        if (log.isTraceEnabled()) {
            log.trace("All warnings from Moodle: {}", warnings.toString());
        }
    }

    private static final Pattern ACCESS_DENIED_PATTERN_1 =
            Pattern.compile(Pattern.quote("No access rights"), Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCESS_DENIED_PATTERN_2 =
            Pattern.compile(Pattern.quote("access denied"), Pattern.CASE_INSENSITIVE);

    public static final boolean checkAccessDeniedError(final String courseKeyPageJSON) {
        return ACCESS_DENIED_PATTERN_1
                .matcher(courseKeyPageJSON)
                .find() ||
                ACCESS_DENIED_PATTERN_2
                        .matcher(courseKeyPageJSON)
                        .find();
    }

    public static Predicate<CourseData> getCourseFilter() {
        final long now = Utils.getSecondsNow();
        return course -> {
            if (course.start_date != null
                    && course.start_date < Utils.toUnixTimeInSeconds(DateTime.now(DateTimeZone.UTC).minusYears(3))) {
                return false;
            }

            if (course.end_date == null || course.end_date == 0 || course.end_date > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.info("remove course {} end_time {} now {}",
                        course.short_name,
                        course.end_date,
                        now);
            }
            return false;
        };
    }

    public static Predicate<CourseQuiz> getQuizFilter() {
        final long now = Utils.getSecondsNow();
        return quiz -> {
            if (quiz.time_close == null || quiz.time_close == 0 || quiz.time_close > now) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.debug("remove quiz {} end_time {} now {}",
                        quiz.name,
                        quiz.time_close,
                        now);
            }
            return false;
        };
    }

    public static List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix,
            final boolean prependShortCourseName) {

        final Map<String, String> additionalAttrs = new HashMap<>();
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_CREATION_TIME, String.valueOf(courseData.time_created));
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SHORT_NAME, courseData.short_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_ID_NUMBER, courseData.idnumber);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_FULL_NAME, courseData.full_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_DISPLAY_NAME, courseData.display_name);
        additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SUMMARY, courseData.summary);

        final List<QuizData> courseAndQuiz = courseData.quizzes
                .stream()
                .map(courseQuizData -> {
                    final String startURI = uriPrefix + courseQuizData.course_module;
                    additionalAttrs.put(QuizData.ATTR_ADDITIONAL_TIME_LIMIT, String.valueOf(courseQuizData.time_limit));
                    return new QuizData(
                            MoodleUtils.getInternalQuizId(
                                    courseQuizData.course_module,
                                    courseData.id,
                                    courseData.short_name,
                                    courseData.idnumber),
                            lmsSetup.getInstitutionId(),
                            lmsSetup.id,
                            lmsSetup.getLmsType(),
                            (prependShortCourseName)
                                    ? courseData.short_name + " : " + courseQuizData.name
                                    : courseQuizData.name,
                            courseQuizData.intro,
                            (courseQuizData.time_open != null && courseQuizData.time_open > 0)
                                    ? Utils.toDateTimeUTCUnix(courseQuizData.time_open)
                                    : Utils.toDateTimeUTCUnix(courseData.start_date),
                            (courseQuizData.time_close != null && courseQuizData.time_close > 0)
                                    ? Utils.toDateTimeUTCUnix(courseQuizData.time_close)
                                    : Utils.toDateTimeUTCUnix(courseData.end_date),
                            startURI,
                            additionalAttrs);
                })
                .collect(Collectors.toList());

        return courseAndQuiz;
    }

    public static final void fillSelectedQuizzes(
            final Set<String> quizIds,
            final Map<String, CourseData> finalCourseDataRef,
            final CourseQuiz quiz) {
        try {
            final CourseData course = finalCourseDataRef.get(quiz.course);
            if (course != null) {
                final String internalQuizId = MoodleUtils.getInternalQuizId(
                        quiz.course_module,
                        course.id,
                        course.short_name,
                        course.idnumber);
                if (quizIds.contains(internalQuizId)) {
                    course.quizzes.add(quiz);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to verify selected quiz for course: {}", e.getMessage());
        }
    }

    // ---- Mapping Classes ---

    /** Maps the Moodle course API course data */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CourseData {
        public final String id;
        public final String short_name;
        public final String idnumber;
        public final String full_name;
        public final String display_name;
        public final String summary;
        public final Long start_date; // unix-time seconds UTC
        public final Long end_date; // unix-time seconds UTC
        public final Long time_created; // unix-time seconds UTC
        public final String category_id;

        @JsonIgnore
        public final Collection<CourseQuiz> quizzes = new ArrayList<>();

        @JsonCreator
        public CourseData(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String short_name,
                @JsonProperty(value = "idnumber") final String idnumber,
                @JsonProperty(value = "fullname") final String full_name,
                @JsonProperty(value = "displayname") final String display_name,
                @JsonProperty(value = "summary") final String summary,
                @JsonProperty(value = "startdate") final Long start_date,
                @JsonProperty(value = "enddate") final Long end_date,
                @JsonProperty(value = "timecreated") final Long time_created,
                @JsonProperty(value = "categoryid") final String category_id) {

            this.id = id;
            this.short_name = short_name;
            this.idnumber = idnumber;
            this.full_name = full_name;
            this.display_name = display_name;
            this.summary = summary;
            this.start_date = start_date;
            this.end_date = end_date;
            this.time_created = time_created;
            this.category_id = category_id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Courses {
        public final Collection<CourseData> courses;
        public final Collection<Warning> warnings;

        @JsonCreator
        public Courses(
                @JsonProperty(value = "courses") final Collection<CourseData> courses,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.courses = courses;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CourseQuizData {
        public final Collection<CourseQuiz> quizzes;
        public final Collection<Warning> warnings;

        @JsonCreator
        public CourseQuizData(
                @JsonProperty(value = "quizzes") final Collection<CourseQuiz> quizzes,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {
            this.quizzes = quizzes;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CourseQuiz {
        public final String id;
        public final String course;
        public final String course_module;
        public final String name;
        public final String intro; // HTML
        public final Long time_open; // unix-time seconds UTC
        public final Long time_close; // unix-time seconds UTC
        public final Long time_limit; // unix-time seconds UTC

        @JsonCreator
        public CourseQuiz(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "course") final String course,
                @JsonProperty(value = "coursemodule") final String course_module,
                @JsonProperty(value = "name") final String name,
                @JsonProperty(value = "intro") final String intro,
                @JsonProperty(value = "timeopen") final Long time_open,
                @JsonProperty(value = "timeclose") final Long time_close,
                @JsonProperty(value = "timelimit") final Long time_limit) {

            this.id = id;
            this.course = course;
            this.course_module = course_module;
            this.name = name;
            this.intro = intro;
            this.time_open = time_open;
            this.time_close = time_close;
            this.time_limit = time_limit;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MoodleUserDetails {
        public final String id;
        public final String username;
        public final String firstname;
        public final String lastname;
        public final String fullname;
        public final String email;
        public final String department;
        public final Long firstaccess;
        public final Long lastaccess;
        public final String auth;
        public final Boolean suspended;
        public final Boolean confirmed;
        public final String lang;
        public final String theme;
        public final String timezone;
        public final String description;
        public final Integer mailformat;
        public final Integer descriptionformat;

        @JsonCreator
        public MoodleUserDetails(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "username") final String username,
                @JsonProperty(value = "firstname") final String firstname,
                @JsonProperty(value = "lastname") final String lastname,
                @JsonProperty(value = "fullname") final String fullname,
                @JsonProperty(value = "email") final String email,
                @JsonProperty(value = "department") final String department,
                @JsonProperty(value = "firstaccess") final Long firstaccess,
                @JsonProperty(value = "lastaccess") final Long lastaccess,
                @JsonProperty(value = "auth") final String auth,
                @JsonProperty(value = "suspended") final Boolean suspended,
                @JsonProperty(value = "confirmed") final Boolean confirmed,
                @JsonProperty(value = "lang") final String lang,
                @JsonProperty(value = "theme") final String theme,
                @JsonProperty(value = "timezone") final String timezone,
                @JsonProperty(value = "description") final String description,
                @JsonProperty(value = "mailformat") final Integer mailformat,
                @JsonProperty(value = "descriptionformat") final Integer descriptionformat) {

            this.id = id;
            this.username = username;
            this.firstname = firstname;
            this.lastname = lastname;
            this.fullname = fullname;
            this.email = email;
            this.department = department;
            this.firstaccess = firstaccess;
            this.lastaccess = lastaccess;
            this.auth = auth;
            this.suspended = suspended;
            this.confirmed = confirmed;
            this.lang = lang;
            this.theme = theme;
            this.timezone = timezone;
            this.description = description;
            this.mailformat = mailformat;
            this.descriptionformat = descriptionformat;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MoodlePluginUserDetails {
        public final String id;
        public final String fullname;
        public final String username;
        public final String firstname;
        public final String lastname;
        public final String idnumber;
        public final String email;
        public final Map<String, String> customfields;

        @JsonCreator
        public MoodlePluginUserDetails(
                final String id,
                final String username,
                final String firstname,
                final String lastname,
                final String idnumber,
                final String email,
                final Map<String, String> customfields) {

            this.id = id;
            if (firstname != null && lastname != null) {
                this.fullname = firstname + Constants.SPACE + lastname;
            } else if (firstname != null) {
                this.fullname = firstname;
            } else {
                this.fullname = lastname;
            }
            this.username = username;
            this.firstname = firstname;
            this.lastname = lastname;
            this.idnumber = idnumber;
            this.email = email;
            this.customfields = Utils.immutableMapOf(customfields);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CoursePage {
        public final Collection<CourseKey> courseKeys;
        public final Collection<Warning> warnings;

        public CoursePage(
                @JsonProperty(value = "courses") final Collection<CourseKey> courseKeys,
                @JsonProperty(value = "warnings") final Collection<Warning> warnings) {

            this.courseKeys = courseKeys;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CourseKey {
        public final String id;
        public final String short_name;
        public final String category_name;
        public final String sort_order;

        @JsonCreator
        public CourseKey(
                @JsonProperty(value = "id") final String id,
                @JsonProperty(value = "shortname") final String short_name,
                @JsonProperty(value = "categoryname") final String category_name,
                @JsonProperty(value = "sortorder") final String sort_order) {

            this.id = id;
            this.short_name = short_name;
            this.category_name = category_name;
            this.sort_order = sort_order;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("CourseKey [id=");
            builder.append(this.id);
            builder.append(", short_name=");
            builder.append(this.short_name);
            builder.append(", category_name=");
            builder.append(this.category_name);
            builder.append(", sort_order=");
            builder.append(this.sort_order);
            builder.append("]");
            return builder.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MoodleQuizRestriction {
        public final String quiz_id;
        public final String config_keys;
        public final String browser_exam_keys;
        public final String quit_link;
        public final String quit_secret;

        @JsonCreator
        public MoodleQuizRestriction(
                final String quiz_id,
                final String config_keys,
                final String browser_exam_keys,
                final String quit_link,
                final String quit_secret) {

            this.quiz_id = quiz_id;
            this.config_keys = config_keys;
            this.browser_exam_keys = browser_exam_keys;
            this.quit_link = quit_link;
            this.quit_secret = quit_secret;
        }
    }

}
