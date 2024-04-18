/*
 * Copyright (c) 2022 ETH Zürich, IT Services
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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.moodle.MoodleAPIRestTemplate.Warning;

public abstract class MoodleUtils {

    private static final Logger log = LoggerFactory.getLogger(MoodleUtils.class);

    public static String getInternalQuizId(
            final String quizId,
            final String courseId,
            final String shortname,
            final String idnumber) {

        return StringUtils.join(
                new String[] {
                        quizId,
                        courseId,
                        StringUtils.isNotBlank(shortname) ? maskShortName(shortname) : Constants.EMPTY_NOTE,
                        StringUtils.isNotBlank(idnumber) ? idnumber : Constants.EMPTY_NOTE
                },
                Constants.COLON);
    }

    private static String maskShortName(final String shortname) {
        return shortname
                .replace(Constants.SEMICOLON.toString(), "_SC_")
                .replace(Constants.COLON.toString(), "_COLON_")
                .replace(Constants.SLASH.toString(), "_SL_")
                .replace(Constants.BACKSLASH.toString(), "_BSL_")
                .replace(Constants.AMPERSAND.toString(), "_AMP_")
                .replace(Constants.ANGLE_BRACE_OPEN.toString(), "_AO_")
                .replace(Constants.ANGLE_BRACE_CLOSE.toString(), "_AC_");
    }

    private static String unmaskShortName(final String shortname) {
        return shortname
                .replace("_SC_", Constants.SEMICOLON.toString())
                .replace("_COLON_", Constants.COLON.toString())
                .replace("_SL_", Constants.SLASH.toString())
                .replace("_BSL_", Constants.BACKSLASH.toString())
                .replace("_AMP_", Constants.AMPERSAND.toString())
                .replace("_AO_", Constants.ANGLE_BRACE_OPEN.toString())
                .replace("_AC_", Constants.ANGLE_BRACE_CLOSE.toString());
    }

    public static String getQuizId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[0];
    }

    public static String getCourseId(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        return StringUtils.split(internalQuizId, Constants.COLON)[1];
    }

    public static String getShortname(final String internalQuizId) {
        if (StringUtils.isBlank(internalQuizId)) {
            return null;
        }

        final String[] split = StringUtils.split(internalQuizId, Constants.COLON);
        if (split.length < 3) {
            return null;
        }

        final String shortName = split[2];
        return shortName.equals(Constants.EMPTY_NOTE) ? null : unmaskShortName(shortName);
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

    public static void logMoodleWarning(
            final Collection<Warning> warnings,
            final String lmsSetupName,
            final String function) {

        if (log.isDebugEnabled()) {
            log.warn(
                    "There are warnings from Moodle response: Moodle: {} request: {} warnings: {} warning sample: {}",
                    lmsSetupName,
                    function,
                    warnings.size(),
                    warnings.iterator().next().toString());
        }
        if (log.isTraceEnabled()) {
            log.trace("All warnings from Moodle: {}", warnings.toString());
        }
    }

    public static void checkJSONFormat(final String userDetailsJSON) {
        if (!userDetailsJSON.startsWith(String.valueOf(Constants.CURLY_BRACE_OPEN)) &&
                !userDetailsJSON.startsWith(String.valueOf(Constants.SQUARE_BRACE_OPEN))) {
            throw new RuntimeException("Illegal response format detected: " + userDetailsJSON);
        }
    }

    private static final Pattern ACCESS_DENIED_PATTERN_1 =
            Pattern.compile(Pattern.quote("No access rights"), Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCESS_DENIED_PATTERN_2 =
            Pattern.compile(Pattern.quote("access denied"), Pattern.CASE_INSENSITIVE);

    public static boolean checkAccessDeniedError(final String courseKeyPageJSON) {
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
        return quizDataOf(lmsSetup, courseData, uriPrefix, prependShortCourseName, true);
    }

    public static List<QuizData> quizDataOf(
            final LmsSetup lmsSetup,
            final CourseData courseData,
            final String uriPrefix,
            final boolean prependShortCourseName,
            final boolean useQuizId) {

        final Map<String, String> additionalAttrs = new HashMap<>();
        if (courseData.time_created != null) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_CREATION_TIME, String.valueOf(courseData.time_created));
        }
        if (courseData.short_name != null) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SHORT_NAME, courseData.short_name);
        }
        if (courseData.idnumber != null) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_ID_NUMBER, courseData.idnumber);
        }
        if (StringUtils.isNotBlank(courseData.full_name)) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_FULL_NAME, courseData.full_name);
        }
        if (StringUtils.isNotBlank(courseData.display_name)) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_DISPLAY_NAME, courseData.display_name);
        }
        if (StringUtils.isNotBlank(courseData.summary)) {
            additionalAttrs.put(QuizData.ATTR_ADDITIONAL_SUMMARY, courseData.summary);
        }

        final List<QuizData> courseAndQuiz = courseData.quizzes
                .stream()
                .map(courseQuizData -> {
                    final String startURI = uriPrefix + courseQuizData.course_module;

                    additionalAttrs.put(
                            QuizData.ATTR_ADDITIONAL_TIME_LIMIT,
                            (courseQuizData.time_limit == null)
                                ? StringUtils.EMPTY
                                : String.valueOf(courseQuizData.time_limit));

                    return new QuizData(
                            MoodleUtils.getInternalQuizId(
                                    (useQuizId) ? courseQuizData.id : courseQuizData.course_module,
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
        public final Collection<CourseQuiz> quizzes;

        @JsonCreator
        public CourseData(
                @JsonProperty("id") final String id,
                @JsonProperty("shortname") final String short_name,
                @JsonProperty("idnumber") final String idnumber,
                @JsonProperty("fullname") final String full_name,
                @JsonProperty("displayname") final String display_name,
                @JsonProperty("summary") final String summary,
                @JsonProperty("startdate") final Long start_date,
                @JsonProperty("enddate") final Long end_date,
                @JsonProperty("timecreated") final Long time_created,
                @JsonProperty("categoryid") final String category_id,
                @JsonProperty("quizzes") final Collection<CourseQuiz> quizzes) {

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
            this.quizzes = (quizzes == null) ? new ArrayList<>() : quizzes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CoursesPagePlugin {
        public final String coursecount;
        public final Integer needle;
        public final Integer perpage;

        public CoursesPagePlugin(
                @JsonProperty("coursecount") final String coursecount,
                @JsonProperty("needle") final Integer needle,
                @JsonProperty("perpage") final Integer perpage) {
            this.coursecount = coursecount;
            this.needle = needle;
            this.perpage = perpage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CoursesPlugin {
        public final CoursesPagePlugin stats;
        public final Collection<CourseData> results;
        public final Collection<Warning> warnings;

        @JsonCreator
        public CoursesPlugin(
                @JsonProperty("stats") final CoursesPagePlugin stats,
                @JsonProperty("courses") final Collection<CourseData> results,
                @JsonProperty("warnings") final Collection<Warning> warnings) {
            this.stats = stats;
            this.results = results;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Courses {
        public final Collection<CourseData> courses;
        public final Collection<Warning> warnings;

        @JsonCreator
        public Courses(
                @JsonProperty("courses") final Collection<CourseData> courses,
                @JsonProperty("warnings") final Collection<Warning> warnings) {
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
                @JsonProperty("quizzes") final Collection<CourseQuiz> quizzes,
                @JsonProperty("warnings") final Collection<Warning> warnings) {
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
                @JsonProperty("id") final String id,
                @JsonProperty("course") final String course,
                @JsonProperty("coursemodule") final String course_module,
                @JsonProperty("name") final String name,
                @JsonProperty("intro") final String intro,
                @JsonProperty("timeopen") final Long time_open,
                @JsonProperty("timeclose") final Long time_close,
                @JsonProperty("timelimit") final Long time_limit) {

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
                @JsonProperty("id") final String id,
                @JsonProperty("username") final String username,
                @JsonProperty("firstname") final String firstname,
                @JsonProperty("lastname") final String lastname,
                @JsonProperty("fullname") final String fullname,
                @JsonProperty("email") final String email,
                @JsonProperty("department") final String department,
                @JsonProperty("firstaccess") final Long firstaccess,
                @JsonProperty("lastaccess") final Long lastaccess,
                @JsonProperty("auth") final String auth,
                @JsonProperty("suspended") final Boolean suspended,
                @JsonProperty("confirmed") final Boolean confirmed,
                @JsonProperty("lang") final String lang,
                @JsonProperty("theme") final String theme,
                @JsonProperty("timezone") final String timezone,
                @JsonProperty("description") final String description,
                @JsonProperty("mailformat") final Integer mailformat,
                @JsonProperty("descriptionformat") final Integer descriptionformat) {

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
    public static final class CoursePage {
        public final Collection<CourseKey> courseKeys;
        public final Collection<Warning> warnings;

        @JsonCreator
        public CoursePage(
                @JsonProperty("courses") final Collection<CourseKey> courseKeys,
                @JsonProperty("warnings") final Collection<Warning> warnings) {

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
                @JsonProperty("id") final String id,
                @JsonProperty("shortname") final String short_name,
                @JsonProperty("categoryname") final String category_name,
                @JsonProperty("sortorder") final String sort_order) {

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
    public static final class MoodleQuizRestrictions {
        public final Collection<MoodleQuizRestriction> data;
        public final Collection<Warning> warnings;

        public MoodleQuizRestrictions(
                @JsonProperty("data") final Collection<MoodleQuizRestriction> data,
                @JsonProperty("warnings") final Collection<Warning> warnings) {

            this.data = data;
            this.warnings = warnings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MoodleQuizRestriction {
        public final String quizid;
        public final List<String> configkeys;
        public final List<String> browserkeys;
        public final String quitlink;
        public final String quitsecret;

        @JsonCreator
        public MoodleQuizRestriction(
                @JsonProperty("quizid") final String quizid,
                @JsonProperty("configkeys") final List<String> configkeys,
                @JsonProperty("browserkeys") final List<String> browserkeys,
                @JsonProperty("quitlink") final String quitlink,
                @JsonProperty("quitsecret") final String quitsecret) {

            this.quizid = quizid;
            this.configkeys = configkeys;
            this.browserkeys = browserkeys;
            this.quitlink = quitlink;
            this.quitsecret = quitsecret;
        }
    }

}
