/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;

public final class QuizData {

    public static final String FILTER_ATTR_NAME = "name_like";
    public static final String FILTER_ATTR_START_TIME = "start_timestamp";

    public static final String PAGE_ATTR_NUMBER = "page_number";
    public static final String PAGE_ATTR_SIZE = "page_size";
    public static final String PAGE_ATTR_SORT_BY = "sort_by";
    public static final String PAGE_ATTR_SORT_ORDER = "sort_order";

    public static final String QUIZ_ATTR_ID = "quiz_id";
    public static final String QUIZ_ATTR_NAME = "quiz_name";
    public static final String QUIZ_ATTR_DESCRIPTION = "quiz_description";
    public static final String QUIZ_ATTR_START_TIME = "quiz_start_time";
    public static final String QUIZ_ATTR_END_TIME = "quiz_end_time";
    public static final String QUIZ_ATTR_START_URL = "quiz_start_url";

    @JsonProperty(QUIZ_ATTR_ID)
    public final String id;

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
            @JsonProperty(QUIZ_ATTR_NAME) final String name,
            @JsonProperty(QUIZ_ATTR_DESCRIPTION) final String description,
            @JsonProperty(QUIZ_ATTR_START_TIME) final DateTime startTime,
            @JsonProperty(QUIZ_ATTR_END_TIME) final DateTime endTime,
            @JsonProperty(QUIZ_ATTR_START_URL) final String startURL) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startURL = startURL;
    }

    public QuizData(
            final String id,
            final String name,
            final String description,
            final String startTime,
            final String endTime,
            final String startURL) {

        this.id = id;
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

    public String geId() {
        return this.id;
    }

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
        return true;
    }

    @Override
    public String toString() {
        return "QuizData [id=" + this.id + ", name=" + this.name + ", description=" + this.description + ", startTime="
                + this.startTime
                + ", endTime=" + this.endTime + ", startURL=" + this.startURL + "]";
    }

}
