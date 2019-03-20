/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Comparator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.GrantEntity;

public final class QuizData implements GrantEntity {

    public static final String FILTER_ATTR_START_TIME = "start_timestamp";

    public static final String QUIZ_ATTR_ID = "quiz_id";
    public static final String QUIZ_ATTR_INSTITUION_ID = Domain.EXAM.ATTR_INSTITUTION_ID;
    public static final String QUIZ_ATTR_LMS_SETUP_ID = "lms_setup_id";
    public static final String QUIZ_ATTR_LMS_TYPE = "lms_setup_type";
    public static final String QUIZ_ATTR_NAME = "quiz_name";
    public static final String QUIZ_ATTR_DESCRIPTION = "quiz_description";
    public static final String QUIZ_ATTR_START_TIME = "quiz_start_time";
    public static final String QUIZ_ATTR_END_TIME = "quiz_end_time";
    public static final String QUIZ_ATTR_START_URL = "quiz_start_url";

    @JsonProperty(QUIZ_ATTR_ID)
    public final String id;

    @JsonProperty(QUIZ_ATTR_INSTITUION_ID)
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

    @JsonCreator
    public QuizData(
            @JsonProperty(QUIZ_ATTR_ID) final String id,
            @JsonProperty(QUIZ_ATTR_INSTITUION_ID) final Long institutionId,
            @JsonProperty(QUIZ_ATTR_LMS_SETUP_ID) final Long lmsSetupId,
            @JsonProperty(QUIZ_ATTR_LMS_TYPE) final LmsType lmsType,
            @JsonProperty(QUIZ_ATTR_NAME) final String name,
            @JsonProperty(QUIZ_ATTR_DESCRIPTION) final String description,
            @JsonProperty(QUIZ_ATTR_START_TIME) final DateTime startTime,
            @JsonProperty(QUIZ_ATTR_END_TIME) final DateTime endTime,
            @JsonProperty(QUIZ_ATTR_START_URL) final String startURL) {

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.lmsType = lmsType;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startURL = startURL;
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

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.lmsType = lmsType;
        this.name = name;
        this.description = description;
        this.startTime = LocalDateTime
                .parse(startTime, Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS)
                .toDateTime(DateTimeZone.UTC);
        this.endTime = LocalDateTime
                .parse(endTime, Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS)
                .toDateTime(DateTimeZone.UTC);
        this.startURL = startURL;
    }

    @Override
    public String getModelId() {
        if (this.id == null) {
            return null;
        }

        return String.valueOf(this.id);
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
    public String getOwnerId() {
        return null;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.institutionId == null) ? 0 : this.institutionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final QuizData other = (QuizData) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        if (this.institutionId == null) {
            if (other.institutionId != null)
                return false;
        } else if (!this.institutionId.equals(other.institutionId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "QuizData [id=" + this.id + ", institutionId=" + this.institutionId + ", lmsSetupId=" + this.lmsSetupId
                + ", lmsType="
                + this.lmsType + ", name=" + this.name + ", description=" + this.description + ", startTime="
                + this.startTime
                + ", endTime=" + this.endTime + ", startURL=" + this.startURL + "]";
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
        final boolean descending = SortOrder.getSortOrder(sort) == SortOrder.DESCENDING;
        final String sortParam = SortOrder.decode(sort);
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
