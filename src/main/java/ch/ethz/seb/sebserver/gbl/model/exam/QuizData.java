/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class QuizData implements GrantEntity {

    public static final String FILTER_ATTR_QUIZ_NAME = "quiz_name";
    public static final String FILTER_ATTR_START_TIME = "start_timestamp";

    public static final String ATTR_ADDITIONAL_ATTRIBUTES = "ADDITIONAL_ATTRIBUTES";

    public static final String QUIZ_ATTR_ID = "quiz_id";
    public static final String QUIZ_ATTR_INSTITUTION_ID = Domain.EXAM.ATTR_INSTITUTION_ID;
    public static final String QUIZ_ATTR_LMS_SETUP_ID = "lms_setup_id";
    public static final String QUIZ_ATTR_LMS_TYPE = "lms_setup_type";
    public static final String QUIZ_ATTR_NAME = "quiz_name";
    public static final String QUIZ_ATTR_DESCRIPTION = "quiz_description";
    public static final String QUIZ_ATTR_START_TIME = "quiz_start_time";
    public static final String QUIZ_ATTR_END_TIME = "quiz_end_time";
    public static final String QUIZ_ATTR_START_URL = "quiz_start_url";

    public static final String ATTR_ADDITIONAL_CREATION_TIME = "time_created";
    public static final String ATTR_ADDITIONAL_SHORT_NAME = "course_short_name";
    public static final String ATTR_ADDITIONAL_ID_NUMBER = "idnumber";
    public static final String ATTR_ADDITIONAL_FULL_NAME = "course_full_name";
    public static final String ATTR_ADDITIONAL_DISPLAY_NAME = "course_display_name";
    public static final String ATTR_ADDITIONAL_SUMMARY = "course_summary";
    public static final String ATTR_ADDITIONAL_TIME_LIMIT = "time_limit";

    @JsonProperty(QUIZ_ATTR_ID)
    public final String id;

    @JsonProperty(QUIZ_ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(QUIZ_ATTR_LMS_SETUP_ID)
    public final Long lmsSetupId;

    @JsonProperty(QUIZ_ATTR_LMS_TYPE)
    public final LmsType lmsType;

    @JsonProperty(QUIZ_ATTR_NAME)
    public final String name;

    @JsonProperty(QUIZ_ATTR_DESCRIPTION)
    public final String description;

    @JsonProperty(QUIZ_ATTR_START_TIME)
    public final DateTime startTime;

    @JsonProperty(QUIZ_ATTR_END_TIME)
    public final DateTime endTime;

    @JsonProperty(QUIZ_ATTR_START_URL)
    public final String startURL;

    @JsonProperty(ATTR_ADDITIONAL_ATTRIBUTES)
    public final Map<String, String> additionalAttributes;

    @JsonCreator
    public QuizData(
            @JsonProperty(QUIZ_ATTR_ID) final String id,
            @JsonProperty(QUIZ_ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(QUIZ_ATTR_LMS_SETUP_ID) final Long lmsSetupId,
            @JsonProperty(QUIZ_ATTR_LMS_TYPE) final LmsType lmsType,
            @JsonProperty(QUIZ_ATTR_NAME) final String name,
            @JsonProperty(QUIZ_ATTR_DESCRIPTION) final String description,
            @JsonProperty(QUIZ_ATTR_START_TIME) final DateTime startTime,
            @JsonProperty(QUIZ_ATTR_END_TIME) final DateTime endTime,
            @JsonProperty(QUIZ_ATTR_START_URL) final String startURL,
            @JsonProperty(ATTR_ADDITIONAL_ATTRIBUTES) final Map<String, String> additionalAttributes) {

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.lmsType = lmsType;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startURL = startURL;
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);
    }

    public QuizData(
            final String id,
            final Long institutionId,
            final Long lmsSetupId,
            final LmsType lmsType,
            final String name,
            final String description,
            final String startTime,
            final String endTime,
            final String startURL) {

        this(id, institutionId, lmsSetupId, lmsType, name, description,
                startTime, endTime, startURL, Collections.emptyMap());
    }

    public QuizData(
            final String id,
            final Long institutionId,
            final Long lmsSetupId,
            final LmsType lmsType,
            final String name,
            final String description,
            final String startTime,
            final String endTime,
            final String startURL,
            final Map<String, String> additionalAttributes) {

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.lmsType = lmsType;
        this.name = name;
        this.description = description;
        this.startTime = (startTime != null)
                ? LocalDateTime
                        .parse(startTime, Constants.STANDARD_DATE_TIME_FORMATTER)
                        .toDateTime(DateTimeZone.UTC)
                : null;
        this.endTime = (endTime != null)
                ? LocalDateTime
                        .parse(endTime, Constants.STANDARD_DATE_TIME_FORMATTER)
                        .toDateTime(DateTimeZone.UTC)
                : null;
        this.startURL = startURL;
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);
    }

    @Override
    public String getModelId() {
        if (this.id == null) {
            return null;
        }

        return this.id;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM;
    }

    public String geId() {
        return this.id;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getLmsSetupId() {
        return this.lmsSetupId;
    }

    public LmsType getLmsType() {
        return this.lmsType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public DateTime getStartTime() {
        return this.startTime;
    }

    public DateTime getEndTime() {
        return this.endTime;
    }

    public String getStartURL() {
        return this.startURL;
    }

    public Map<String, String> getAdditionalAttributes() {
        return this.additionalAttributes;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("QuizData [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", lmsSetupId=");
        builder.append(this.lmsSetupId);
        builder.append(", lmsType=");
        builder.append(this.lmsType);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append(", startTime=");
        builder.append(this.startTime);
        builder.append(", endTime=");
        builder.append(this.endTime);
        builder.append(", startURL=");
        builder.append(this.startURL);
        builder.append("]");
        return builder.toString();
    }

    public static Comparator<QuizData> getIdComparator(final boolean descending) {
        return (qd1, qd2) -> ((qd1 == qd2)
                ? 0
                : (qd1 == null || qd1.id == null)
                        ? 1
                        : (qd2 == null || qd2.id == null)
                                ? -1
                                : qd1.id.compareTo(qd2.id))
                * ((descending) ? -1 : 1);
    }

    public static Comparator<QuizData> getNameComparator(final boolean descending) {
        return (qd1, qd2) -> ((qd1 == qd2)
                ? 0
                : (qd1 == null || qd1.name == null)
                        ? 1
                        : (qd2 == null || qd2.name == null)
                                ? -1
                                : qd1.name.compareTo(qd2.name))
                * ((descending) ? -1 : 1);
    }

    public static Comparator<QuizData> getStartTimeComparator(final boolean descending) {
        return (qd1, qd2) -> ((qd1 == qd2)
                ? 0
                : (qd1 == null || qd1.startTime == null)
                        ? 1
                        : (qd2 == null || qd2.startTime == null)
                                ? -1
                                : qd1.startTime.compareTo(qd2.startTime))
                * ((descending) ? -1 : 1);
    }

    public static Comparator<QuizData> getEndTimeComparator(final boolean descending) {
        return (qd1, qd2) -> ((qd1 == qd2)
                ? 0
                : (qd1 == null || qd1.endTime == null)
                        ? 1
                        : (qd2 == null || qd2.endTime == null)
                                ? -1
                                : qd1.endTime.compareTo(qd2.endTime))
                * ((descending) ? -1 : 1);
    }

    public static Comparator<QuizData> getComparator(final String sort) {
        final boolean descending = PageSortOrder.getSortOrder(sort) == PageSortOrder.DESCENDING;
        final String sortParam = PageSortOrder.decode(sort);
        if (QUIZ_ATTR_NAME.equals(sortParam)) {
            return getNameComparator(descending);
        } else if (QUIZ_ATTR_START_TIME.equals(sortParam)) {
            return getStartTimeComparator(descending);
        } else if (QUIZ_ATTR_END_TIME.equals(sortParam)) {
            return getEndTimeComparator(descending);
        }

        return getIdComparator(descending);
    }

}
